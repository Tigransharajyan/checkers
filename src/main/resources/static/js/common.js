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
  }
};

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
    const code = body && body.errorCode ? body.errorCode : 'INTERNAL_ERROR';
    const err = new Error(code);
    err.errorCode = code;
    err.details = body && body.details;
    err.status = res.status;
    throw err;
  }
  return body;
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
      if (!raw || raw.startsWith('#') || raw.startsWith('mailto:') || raw.startsWith('tel:')) return;
      let url;
      try {
        url = new URL(raw, window.location.origin);
      } catch (_) {
        return;
      }
      if (url.origin !== window.location.origin) return;
      url.searchParams.set('lang', lang);
      link.setAttribute('href', url.pathname + url.search + url.hash);
    });
  }

  function markActive(lang) {
    document.querySelectorAll('[data-lang]').forEach((btn) => {
      btn.classList.toggle('is-active', btn.getAttribute('data-lang') === lang);
    });
  }

  document.querySelectorAll('[data-lang]').forEach((btn) => {
    btn.addEventListener('click', async () => {
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

      try {
        const messages = await Checkers.api('/api/i18n?lang=' + encodeURIComponent(lang));
        Checkers.setLocaleMessages(messages);
        window.history.replaceState(window.history.state, '', url.toString());
        markActive(lang);
        updateLangLinks(lang);
        window.dispatchEvent(new CustomEvent('checkers:localechange', { detail: { lang } }));
        Checkers.toast(Checkers.t('toast.localeSaved'));
      } catch (e) {
        Checkers.toast(Checkers.apiErrorMessage(e), 'error');
      }
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
    const label = u.username + (u.guest ? ' (' + Checkers.t('common.guest') + ')' : '');
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
      window.location.href = '/?lang=' + Checkers.locale();
    });
  });
};

Checkers.initSidebar = function () {
  const toggle = document.querySelector('[data-sidebar-toggle]');
  const backdrop = document.querySelector('[data-sidebar-backdrop]');
  const close = () => document.body.classList.remove('sidebar-open');
  toggle?.addEventListener('click', () => {
    document.body.classList.toggle('sidebar-open');
  });
  backdrop?.addEventListener('click', close);
  document.querySelectorAll('.sidebar .nav-link').forEach((link) => {
    link.addEventListener('click', close);
  });
  window.addEventListener('resize', () => {
    if (window.innerWidth > 768) close();
  });
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
  Checkers.initIcons();
});

window.addEventListener('load', () => {
  Checkers.initIcons();
});
