window.Checkers = window.Checkers || {};

Checkers.Auth = {
  KEY: 'checkers.auth',

  load() {
    try {
      return JSON.parse(localStorage.getItem(this.KEY) || 'null');
    } catch {
      return null;
    }
  },

  save(payload) {
    localStorage.setItem(this.KEY, JSON.stringify(payload));
  },

  clear() {
    localStorage.removeItem(this.KEY);
  },

  token() {
    const a = this.load();
    return a && a.accessToken;
  },

  user() {
    const a = this.load();
    return a && a.user;
  },

  isLoggedIn() {
    return !!this.token();
  },

  async refresh() {
    // Several requests can discover the same expired session at once.
    if (this._refreshPromise) return this._refreshPromise;
    this._refreshPromise = this.refreshSession();
    try { return await this._refreshPromise; }
    finally { this._refreshPromise = null; }
  },

  async refreshSession() {
    const auth = this.load();
    if (!auth || !auth.refreshToken) return false;
    try {
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: auth.refreshToken })
      });
      if (!response.ok) {
        if (response.status === 401) {
          const problem = await response.json().catch(() => null);
          if ((problem?.errorCode === 'INVALID_REFRESH_TOKEN' || problem?.errorCode === 'UNAUTHORIZED')
              && this.load()?.refreshToken === auth.refreshToken) this.clear();
        }
        return false;
      }
      this.save(await response.json());
      return true;
    } catch (_) { return false; }
  }
};

Checkers.Sound = (function () {
  let context = null;
  let moveIndex = 0;
  const buffers = new Map();
  const moves = ['move-1', 'move-2', 'move-3'];
  const clips = [...moves, 'capture', 'start', 'end'];

  function getContext() {
    if (!context) {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (!AudioContext) return null;
      try { context = new AudioContext(); } catch (_) { return null; }
    }
    if (context.state === 'suspended') context.resume().catch(() => {});
    return context;
  }

  function load(name, audio) {
    if (!buffers.has(name)) {
      const pending = fetch('/sounds/board/' + name + '.wav')
        .then(response => {
          if (!response.ok) throw new Error('Board sound unavailable');
          return response.arrayBuffer();
        })
        .then(bytes => audio.decodeAudioData(bytes))
        .catch(() => { buffers.delete(name); return null; });
      buffers.set(name, pending);
    }
    return buffers.get(name);
  }

  function warm() {
    if (!document.querySelector('[data-board], [data-task-board]')) return;
    const audio = getContext();
    if (audio) clips.forEach(name => load(name, audio));
  }

  function play(kind) {
    if (document.visibilityState === 'hidden') return;
    const audio = getContext();
    if (!audio) return;
    const name = kind === 'move' || !clips.includes(kind)
      ? moves[moveIndex++ % moves.length] : kind;
    const requested = performance.now();
    load(name, audio).then(buffer => {
      // Never replay an old click after a slow download or an autoplay unlock.
      if (!buffer || audio.state !== 'running' || document.visibilityState === 'hidden'
          || performance.now() - requested > 250) return;
      const source = audio.createBufferSource();
      const gain = audio.createGain();
      source.buffer = buffer;
      gain.gain.value = .85;
      source.connect(gain);
      gain.connect(audio.destination);
      source.onended = () => { source.disconnect(); gain.disconnect(); };
      source.start();
    });
  }

  ['pointerdown', 'keydown'].forEach(eventName => {
    document.addEventListener(eventName, warm, { once: true });
  });
  return { play };
})();

Checkers.api = async function (path, options = {}) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  const token = Checkers.Auth.token();
  if (token) {
    headers.Authorization = 'Bearer ' + token;
  }
  const res = await fetch(path, Object.assign({}, options, { headers }));
  if (res.status === 204) {
    return null;
  }
  const text = await res.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }
  if (!res.ok) {
    if (res.status === 401 && !options._retried) {
      const recovered = token !== Checkers.Auth.token()
        ? !!Checkers.Auth.token() : await Checkers.Auth.refresh();
      if (recovered) {
        const retryOptions = Object.assign({}, options, { _retried: true });
        return Checkers.api(path, retryOptions);
      }
    }
    const code = body && body.errorCode ? body.errorCode : 'INTERNAL_ERROR';
    const err = new Error(code);
    err.errorCode = code;
    err.details = body && body.details;
    err.status = res.status;
    throw err;
  }
  return body;
};

Checkers.restoreSession = async function () {
  const auth = Checkers.Auth.load();
  if (!auth || !auth.refreshToken) return false;
  const token = auth.accessToken;
  try {
    const payload = token && JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (payload && payload.exp * 1000 > Date.now() + 60 * 1000) return true;
  } catch (_) { /* Refresh malformed or legacy tokens. */ }
  return Checkers.Auth.refresh();
};

Checkers.toast = function (message, type) {
  const root = document.getElementById('toast-root');
  if (!root) return;
  const el = document.createElement('div');
  el.className = 'toast' + (type === 'error' ? ' error' : '');
  el.textContent = message;
  root.appendChild(el);
  setTimeout(() => el.remove(), 3500);
};

Checkers.apiErrorMessage = function (err) {
  if (!err) return Checkers.t('common.error');
  if (err.details && err.details.moveError) {
    return Checkers.t('error.move.' + err.details.moveError);
  }
  const code = err.errorCode || err.message;
  const key = 'error.api.' + code;
  const msg = Checkers.t(key);
  return msg === key ? Checkers.t('common.error') : msg;
};

Checkers.eventMessage = function (type, data) {
  const key = 'event.' + type;
  if (type === 'OPPONENT_DISCONNECTED') {
    const sec = (data && data.reconnectTimeoutSeconds) || 60;
    return Checkers.t(key, sec);
  }
  const msg = Checkers.t(key);
  return msg === key ? type : msg;
};

Checkers.initLangSwitch = function () {
  function updateLangLinks(lang) {
    document.querySelectorAll('a[href]').forEach((link) => {
      const raw = link.getAttribute('href');
      if (link.hasAttribute('data-lang')) return;
      if (!raw || raw.startsWith('#') || raw.startsWith('mailto:') || raw.startsWith('tel:')) return;
      let url;
      try {
        url = new URL(raw, window.location.origin);
      } catch (_) {
        return;
      }
      if (url.origin !== window.location.origin || !['/', '/index', '/home', '/play', '/tasks', '/rating', '/history', '/faq', '/login', '/register'].includes(url.pathname)) return;
      url.searchParams.set('lang', lang);
      link.setAttribute('href', url.pathname + url.search + url.hash);
    });
  }

  function markActive(lang) {
    document.querySelectorAll('[data-lang]').forEach((btn) => {
      const active = btn.getAttribute('data-lang') === lang;
      btn.classList.toggle('is-active', active);
      if (active) btn.setAttribute('aria-current', 'page');
      else btn.removeAttribute('aria-current');
    });
  }

  document.querySelectorAll('[data-lang]').forEach((btn) => {
    btn.addEventListener('click', async (event) => {
      if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
      event.preventDefault();
      const lang = btn.getAttribute('data-lang');
      if (!lang || lang === Checkers.locale()) return;
      const url = new URL(window.location.href);
      url.searchParams.set('lang', lang);

      if (Checkers.Auth.isLoggedIn() && Checkers.Auth.user() && !Checkers.Auth.user().guest) {
        try {
          const updated = await Checkers.api('/api/users/me/locale', {
            method: 'PATCH',
            body: JSON.stringify({ locale: lang })
          });
          const auth = Checkers.Auth.load();
          auth.user = updated;
          Checkers.Auth.save(auth);
        } catch (_) { /* still switch via cookie */ }
      }

      // Server-rendered templates, FAQ content, titles and ARIA labels must switch together.
      window.location.assign(url.toString());
    });
  });

  markActive(Checkers.locale());
  updateLangLinks(Checkers.locale());
};

Checkers.initHeaderAuth = function () {
  const guestEls = document.querySelectorAll('[data-auth="guest"]');
  const userEls = document.querySelectorAll('[data-auth="user"]');
  const nameEls = document.querySelectorAll('[data-user-name]');
  const loggedIn = Checkers.Auth.isLoggedIn();
  guestEls.forEach((el) => el.classList.toggle('hidden', loggedIn));
  userEls.forEach((el) => el.classList.toggle('hidden', !loggedIn));
  const u = loggedIn ? Checkers.Auth.user() : null;
  if (u) {
    const label = u.guest ? Checkers.t('common.guest') : u.username;
    nameEls.forEach((el) => { el.textContent = label; });
  }
  const avatars = document.querySelectorAll('[data-account-avatar]');
  avatars.forEach((el) => {
    if (u && u.username) {
      el.textContent = u.username.charAt(0).toUpperCase();
    } else {
      el.textContent = '?';
    }
  });

  document.querySelectorAll('[data-logout]').forEach((logout) => {
    if (logout.dataset.bound) return;
    logout.dataset.bound = '1';
    logout.addEventListener('click', async (e) => {
      e.preventDefault();
      try {
        const auth = Checkers.Auth.load();
        await Checkers.api('/api/auth/logout', {
          method: 'POST',
          body: JSON.stringify({ refreshToken: auth && auth.refreshToken })
        });
      } catch (_) { /* ignore */ }
      Checkers.Auth.clear();
      window.location.href = '/home?lang=' + Checkers.locale();
    });
  });
};

// One owner for focus, background inertness and Escape across all overlays.
Checkers.Overlay = (() => {
  let active = null;
  const focusable = root => [...root.querySelectorAll('a[href],button,input,select,textarea,[tabindex]')]
    .filter(el => el.tabIndex >= 0 && !el.disabled && !el.closest('[inert]') && el.getClientRects().length);
  const focusReturn = trigger => {
    const target = trigger?.isConnected && trigger.getClientRects().length && !trigger.disabled && !trigger.closest('[inert]')
      ? trigger : document.querySelector('#main-content');
    target?.focus({ preventScroll: true });
  };
  function close(restore = true) {
    if (!active) return;
    const previous = active;
    active = null;
    previous.inert.forEach(([element, value]) => { element.inert = value; });
    document.body.classList.remove('overlay-open');
    previous.onClose?.();
    if (restore) focusReturn(previous.trigger);
  }
  function open(root, options = {}) {
    if (!root) return;
    close(false);
    const entry = { root, trigger: options.trigger || document.activeElement, inert: [], ...options };
    active = entry;
    options.onOpen?.();
    if (options.modal !== false) {
      let branch = root;
      while (branch.parentElement) {
        [...branch.parentElement.children].forEach(sibling => {
          if (sibling !== branch && sibling !== entry.dismissSurface && !['SCRIPT','STYLE','LINK'].includes(sibling.tagName)) {
            entry.inert.push([sibling, sibling.inert]); sibling.inert = true;
          }
        });
        branch = branch.parentElement;
        if (branch === document.body) break;
      }
      document.body.classList.add('overlay-open');
    }
    (options.initial || focusable(root)[0] || root.querySelector('[role=dialog]') || root).focus({ preventScroll: true });
  }
  document.addEventListener('keydown', event => {
    if (!active) return;
    if (event.key === 'Escape') {
      event.preventDefault();
      if (!active.canClose || active.canClose()) close();
    } else if (event.key === 'Tab' && active.modal !== false) {
      const items = focusable(active.root);
      if (!items.length) { event.preventDefault(); return; }
      const first = items[0], last = items.at(-1);
      if (event.shiftKey && (document.activeElement === first || !active.root.contains(document.activeElement))) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && (document.activeElement === last || !active.root.contains(document.activeElement))) { event.preventDefault(); first.focus(); }
    }
  }, true);
  return { open, close, isOpen: root => active?.root === root };
})();

Checkers.initSidebar = function () {
  const sidebar = document.querySelector('[data-sidebar]');
  if (!sidebar) return;
  const toggle = document.querySelector('[data-sidebar-toggle]');
  const backdrop = document.querySelector('[data-sidebar-backdrop]');
  const closeButton = document.querySelector('[data-sidebar-close]');
  // Must match app.css: ordinary900px, game1200px.
  const media = matchMedia(document.body.classList.contains('game-page') ? '(min-width: 0px)' : '(max-width: 900px)');
  const hide = () => {
    document.body.classList.remove('sidebar-open');
    toggle?.setAttribute('aria-expanded', 'false');
    sidebar.inert = media.matches;
    sidebar.removeAttribute('role'); sidebar.removeAttribute('aria-modal');
  };
  const close = () => { if (Checkers.Overlay.isOpen(sidebar)) Checkers.Overlay.close(); else hide(); };
  toggle?.addEventListener('click', () => {
    if (Checkers.Overlay.isOpen(sidebar)) { close(); return; }
    Checkers.Overlay.open(sidebar, {
      trigger: toggle, initial: closeButton, dismissSurface: backdrop,
      onOpen: () => { sidebar.inert = false; document.body.classList.add('sidebar-open'); toggle.setAttribute('aria-expanded', 'true'); sidebar.setAttribute('role', 'dialog'); sidebar.setAttribute('aria-modal', 'true'); sidebar.setAttribute('aria-label', Checkers.t('nav.main')); },
      onClose: hide
    });
  });
  closeButton?.addEventListener('click', close);
  backdrop?.addEventListener('click', close);
  sidebar.querySelectorAll('a').forEach(link => link.addEventListener('click', () => { if (media.matches && !link.classList.contains('skip-link')) close(); }));
  media.addEventListener('change', () => {
    const hadFocus = sidebar.contains(document.activeElement);
    close(); sidebar.inert = media.matches;
    if (hadFocus && media.matches) toggle?.focus({ preventScroll: true });
  });
  hide();
};

Checkers.initIcons = function () {
  if (window.lucide && typeof window.lucide.createIcons === 'function') {
    window.lucide.createIcons();
  }
};

Checkers.requireAuth = function (redirect) {
  if (!Checkers.Auth.isLoggedIn()) {
    const next = encodeURIComponent(redirect || window.location.pathname + window.location.search);
    window.location.href = '/login?lang=' + Checkers.locale() + '&next=' + next;
    return false;
  }
  return true;
};

document.addEventListener('DOMContentLoaded', () => {
  Checkers.initLangSwitch();
  Checkers.initHeaderAuth();
  Checkers.initSidebar();
  document.querySelectorAll(".is-active[href]").forEach(link => link.setAttribute("aria-current", "page"));
  Checkers.initIcons();
  Checkers.sessionReady = Checkers.restoreSession().then(() => Checkers.initHeaderAuth());
  Checkers.initSitePresence();
});

window.addEventListener('load', () => {
  Checkers.initIcons();
});

// Count real active browsers on every screen, including visitors without an account.
Checkers.initSitePresence = function () {
  let pending = false;
  async function heartbeat() {
    if (pending || document.visibilityState === 'hidden') return;
    pending = true;
    try {
      await Checkers.sessionReady;
      await Checkers.api('/api/site/presence', { method: 'POST' });
      if (document.querySelector('[data-site-online]')) {
        const stats = await Checkers.api('/api/site/stats');
        const number = new Intl.NumberFormat(Checkers.locale());
        document.querySelector('[data-site-online]').textContent = number.format(stats.online);
        document.querySelector('[data-site-games]').textContent = number.format(stats.completedGames);
      }
    } catch (_) {
      // A failed request is unknown, never a fabricated zero or a stale live count.
      document.querySelectorAll('[data-site-online], [data-site-games]').forEach(el => { el.textContent = '—'; });
    } finally { pending = false; }
  }
  heartbeat();
  setInterval(heartbeat, 15000);
  document.addEventListener('visibilitychange', heartbeat);
  window.addEventListener('online', heartbeat);
};

Checkers.personalStats = function (user) {
  if (!user || user.guest) return null;
  const wins = Number(user.wins) || 0, losses = Number(user.losses) || 0, draws = Number(user.draws) || 0;
  const total = wins + losses + draws;
  return { total, wins, losses, draws, winRate: total ? Math.round(wins / total * 100) : null };
};

Checkers.escapeHtml = function (value) {
  const element = document.createElement('span');
  element.textContent = value;
  return element.innerHTML;
};
