document.addEventListener('DOMContentLoaded', () => {
  const welcome = document.querySelector('[data-welcome]');
  const statsGuest = document.querySelector('[data-stats-guest]');
  const ratingHint = document.querySelector('[data-rating-hint]');
  const statsGrid = document.querySelector('[data-stats-grid]');
  const inviteBox = document.querySelector('[data-invite-box]');
  const inviteLink = document.querySelector('[data-invite-link]');
  const mmSearchState = document.querySelector('[data-mm-search-state]');
  const unauthBox = document.querySelector('[data-unauth-box]');
  const activeGamesBox = document.querySelector('[data-active-games-box]');
  const activeGamesList = document.querySelector('[data-active-games-list]');
  const activeGamesLoading = document.querySelector('[data-active-games-loading]');
  const activeGamesError = document.querySelector('[data-active-games-error]');
  const recentGamesBox = document.querySelector('[data-recent-games-box]');
  const recentGamesList = document.querySelector('[data-recent-games-list]');
  const recentGamesLoading = document.querySelector('[data-recent-games-loading]');
  const recentEmpty = document.querySelector('[data-recent-empty]');
  const recentGamesError = document.querySelector('[data-recent-games-error]');
  const accountRefreshError = document.querySelector('[data-account-refresh-error]');
  const homeGrid = document.querySelector('.home-grid');
  const personalRateRow = document.querySelector('[data-personal-rate-row]');
  const personalEmpty = document.querySelector('[data-personal-empty]');
  let accountDataConfirmed = true;

  // Difficulty pill tabs
  document.querySelectorAll('[data-diff-tab]').forEach((tab) => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('[data-diff-tab]').forEach((t) => t.classList.remove('is-active'));
      tab.classList.add('is-active');
      const diffInput = document.querySelector('[data-bot-diff]');
      if (diffInput) diffInput.value = tab.getAttribute('data-diff-tab');
    });
  });

  // Guest fast start in lobby
  document.querySelector('[data-lobby-guest]')?.addEventListener('click', async () => {
    try {
      const res = await Checkers.api('/api/auth/guest', { method: 'POST' });
      Checkers.Auth.save(res);
      accountDataConfirmed = true;
      Checkers.initHeaderAuth();
      refreshUI();
      Checkers.toast(Checkers.t('auth.guest') || 'Guest logged in');
    } catch (e) {
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    }
  });

  function refreshStats() {
    const personal = accountDataConfirmed ? Checkers.personalStats(Checkers.Auth.user()) : null;
    document.querySelector('[data-personal-metrics]')?.classList.toggle('hidden', !personal);
    document.querySelector('[data-progress-summary]')?.classList.toggle('hidden', !personal);
    document.querySelector('[data-progress-track]')?.classList.toggle('hidden', !personal);
    if (personal) {
      const number = new Intl.NumberFormat(Checkers.locale());
      const fields = { 'personal-games': number.format(personal.total), 'personal-wins': number.format(personal.wins),
        'personal-losses': number.format(personal.losses), 'personal-draws': number.format(personal.draws) };
      Object.entries(fields).forEach(([key, value]) => {
        const el = document.querySelector('[data-' + key + ']');
        if (el) el.textContent = value;
      });
      personalRateRow?.classList.toggle('hidden', personal.winRate == null);
      personalEmpty?.classList.toggle('hidden', personal.total > 0);
      const rate = document.querySelector('[data-personal-rate]');
      if (rate && personal.winRate != null) rate.textContent = number.format(personal.winRate) + '%';
      const bar = document.querySelector('[data-progress-track] span');
      if (bar) bar.style.width = (personal.winRate || 0) + '%';
    }
    const user = Checkers.Auth.user();
    const loggedIn = Checkers.Auth.isLoggedIn() && !!user;
    homeGrid?.classList.toggle('has-account', loggedIn && !user.guest);
    homeGrid?.classList.toggle('has-active', loggedIn);

    if (unauthBox) {
      unauthBox.classList.toggle('hidden', loggedIn);
    }

    if (!loggedIn) {
      if (welcome) welcome.textContent = '';
      if (statsGuest) statsGuest.classList.remove('hidden');
      if (statsGuest) statsGuest.textContent = '—';
      if (ratingHint) {
        ratingHint.classList.remove('hidden');
        ratingHint.textContent = Checkers.t('lobby.rating.guestHint');
      }
      if (statsGrid) statsGrid.classList.add('hidden');
      if (activeGamesBox) activeGamesBox.classList.add('hidden');
      if (recentGamesBox) recentGamesBox.classList.add('hidden');
      if (recentGamesList) recentGamesList.innerHTML = '';
      activeGamesLoading?.classList.add('hidden');
      recentGamesLoading?.classList.add('hidden');
      if (recentEmpty) recentEmpty.classList.add('hidden');
      return;
    }

    if (welcome) {
      welcome.textContent = Checkers.t('lobby.welcome', user.username);
    }

    if (user.guest) {
      if (statsGuest) {
        statsGuest.classList.remove('hidden');
        statsGuest.textContent = '—';
      }
      if (ratingHint) {
        ratingHint.classList.remove('hidden');
        ratingHint.textContent = Checkers.t('lobby.rating.guestHint');
      }
      if (statsGrid) statsGrid.classList.add('hidden');
      if (recentGamesBox) recentGamesBox.classList.add('hidden');
      if (recentGamesList) recentGamesList.innerHTML = '';
      activeGamesLoading?.classList.add('hidden');
      recentGamesLoading?.classList.add('hidden');
      if (recentEmpty) recentEmpty.classList.add('hidden');
    } else {
      if (statsGuest) statsGuest.classList.add('hidden');
      if (ratingHint) ratingHint.classList.add('hidden');
      if (statsGrid) {
        statsGrid.classList.remove('hidden');
        statsGrid.querySelector('[data-stat="wins"]').textContent = user.wins ?? 0;
        statsGrid.querySelector('[data-stat="losses"]').textContent = user.losses ?? 0;
        statsGrid.querySelector('[data-stat="draws"]').textContent = user.draws ?? 0;
      }
      recentGamesBox?.classList.remove('hidden');
    }
  }

  async function loadActiveGames() {
    const user = Checkers.Auth.user();
    if (!activeGamesBox || !activeGamesList || !Checkers.Auth.isLoggedIn() || !user) return;
    activeGamesBox.classList.remove('hidden');
    activeGamesBox.setAttribute('aria-busy', 'true');
    activeGamesList.replaceChildren();
    activeGamesLoading?.classList.remove('hidden');
    activeGamesError?.classList.add('hidden');
    try {
      const games = await Checkers.api('/api/games/active');
      activeGamesLoading?.classList.add('hidden');
      activeGamesBox.setAttribute('aria-busy', 'false');
      if (!games || !games.length) {
        activeGamesBox.classList.add('hidden');
        homeGrid?.classList.remove('has-active');
        return;
      }
      homeGrid?.classList.add('has-active');
      games.forEach((g) => {
        const item = document.createElement('div');
        item.className = 'active-game-item';
        const vs = (g.whiteUsername || '—') + ' ' + Checkers.t('common.vs') + ' ' + (g.mode === 'BOT' ? Checkers.t('common.bot') : (g.blackUsername || '—'));
        item.innerHTML = `
          <div class="active-game-info">
            <strong>${Checkers.t('mode.' + g.mode)}</strong>
            <span>${Checkers.escapeHtml(vs)}</span>
          </div>
          <a class="btn btn-primary btn-sm" href="/play?id=${g.id}&lang=${Checkers.locale()}">
            ${Checkers.t('redesign.active.title')}
          </a>
          <button type="button" class="btn btn-ghost btn-sm active-game-end" data-end-active-game="${g.id}">
            ${Checkers.t(g.status === 'WAITING' ? 'redesign.active.cancel' : 'play.resign')}
          </button>
        `;
        activeGamesList.appendChild(item);
      });
      activeGamesList.querySelectorAll('[data-end-active-game]').forEach(button => {
        button.addEventListener('click', () => {
          const selected = games.find(game => String(game.id) === button.dataset.endActiveGame);
          if (selected) confirmEnd(selected, button);
        });
      });
    } catch (_) {
      activeGamesBox.classList.remove('hidden');
      homeGrid?.classList.add('has-active');
      activeGamesBox.setAttribute('aria-busy', 'false');
      activeGamesLoading?.classList.add('hidden');
      activeGamesList.replaceChildren();
      activeGamesError?.classList.remove('hidden');
    }
  }

  async function loadRecentGames() {
    const user = Checkers.Auth.user();
    if (!recentGamesBox || !recentGamesList) return;
    if (!Checkers.Auth.isLoggedIn() || !user || user.guest) {
      if (recentGamesList) recentGamesList.innerHTML = '';
      recentGamesBox.classList.add('hidden');
      return;
    }
    recentGamesBox.classList.remove('hidden');
    recentGamesBox.setAttribute('aria-busy', 'true');
    recentGamesList.replaceChildren();
    recentEmpty?.classList.add('hidden');
    recentGamesError?.classList.add('hidden');
    recentGamesLoading?.classList.remove('hidden');
    try {
      const page = await Checkers.api('/api/games/history?size=4');
      const items = page.content || [];
      recentGamesBox.setAttribute('aria-busy', 'false');
      recentGamesLoading?.classList.add('hidden');
      if (!items.length) {
        recentGamesList.innerHTML = '';
        recentEmpty?.classList.remove('hidden');
        return;
      }
      recentGamesBox.classList.remove('hidden');
      recentEmpty?.classList.add('hidden');
      recentGamesList.innerHTML = '';
      items.forEach((g) => {
        const row = document.createElement('div');
        row.className = 'recent-game-row';
        const vs = (g.whiteUsername || '—') + ' ' + Checkers.t('common.vs') + ' ' + (g.mode === 'BOT' ? Checkers.t('common.bot') : (g.blackUsername || '—'));
        const reasonKey = 'reason.' + (g.finishReason || '');
        const reason = g.finishReason ? Checkers.t(reasonKey) : '—';
        row.innerHTML = `<div class="recent-game-info"><strong>${Checkers.escapeHtml(vs)}</strong><span>${Checkers.t('mode.' + g.mode)} · ${Checkers.escapeHtml(reason === reasonKey ? g.finishReason : reason)}</span></div><a class="btn btn-ghost btn-sm" href="/play?id=${g.id}&lang=${Checkers.locale()}">${Checkers.t('history.open')}</a>`;
        recentGamesList.appendChild(row);
      });
      if (window.lucide) window.lucide.createIcons();
    } catch (_) {
      recentGamesBox.setAttribute('aria-busy', 'false');
      recentGamesLoading?.classList.add('hidden');
      recentGamesList.replaceChildren();
      recentEmpty?.classList.add('hidden');
      recentGamesError?.classList.remove('hidden');
    }
  }
  document.querySelector('[data-active-games-retry]')?.addEventListener('click', loadActiveGames);
  document.querySelector('[data-recent-games-retry]')?.addEventListener('click', loadRecentGames);
  document.querySelector('[data-account-refresh-retry]')?.addEventListener('click', async event => {
    const button = event.currentTarget;
    button.disabled = true;
    try { await refreshMe(); refreshUI(); }
    finally { button.disabled = false; }
  });

  async function ensureAuth() {
    await Checkers.sessionReady;
    if (Checkers.Auth.isLoggedIn()) return true;
    // Auto guest start if user hasn't explicitly signed in
    try {
      const res = await Checkers.api('/api/auth/guest', { method: 'POST' });
      Checkers.Auth.save(res);
      Checkers.initHeaderAuth();
      refreshUI();
      return true;
    } catch (_) {
      Checkers.requireAuth('/');
      return false;
    }
  }

  async function refreshMe() {
    if (!Checkers.Auth.isLoggedIn()) { accountDataConfirmed = true; accountRefreshError?.classList.add('hidden'); return true; }
    try {
      const me = await Checkers.api('/api/auth/me');
      const auth = Checkers.Auth.load();
      auth.user = me;
      Checkers.Auth.save(auth);
      accountDataConfirmed = true;
      Checkers.initHeaderAuth();
      accountRefreshError?.classList.add('hidden');
      return true;
    } catch (_) {
      accountDataConfirmed = !Checkers.Auth.isLoggedIn();
      Checkers.initHeaderAuth();
      if (Checkers.Auth.isLoggedIn()) accountRefreshError?.classList.remove('hidden');
      else accountRefreshError?.classList.add('hidden');
      return false;
    }
  }

  function refreshUI() {
    refreshStats();
    loadActiveGames();
    loadRecentGames();
  }

  const endModal = document.querySelector('[data-end-modal]');
  let endingGame = null, ending = false;
  function confirmEnd(game, trigger) {
    endingGame = game;
    const waiting = game.status === 'WAITING';
    const label = Checkers.t(waiting ? 'redesign.active.cancel' : 'play.resign');
    document.querySelector('[data-end-title]').textContent = label;
    document.querySelector('[data-end-description]').textContent = Checkers.t(waiting ? 'redesign.active.cancelConfirm' : 'redesign.active.resignConfirm') + ' ' + (game.whiteUsername || '—') + ' ' + Checkers.t('common.vs') + ' ' + (game.mode === 'BOT' ? Checkers.t('common.bot') : (game.blackUsername || Checkers.t('play.waitingOpponent')));
    document.querySelector('[data-end-error]').textContent = '';
    document.querySelector('[data-confirm-end]').textContent = label;
    Checkers.Overlay.open(endModal, { trigger, initial: document.querySelector('[data-cancel-end]'), canClose: () => !ending, onOpen: () => endModal.classList.remove('hidden'), onClose: () => endModal.classList.add('hidden') });
  }
  document.querySelector('[data-cancel-end]')?.addEventListener('click', () => { if (!ending) Checkers.Overlay.close(); });
  document.querySelector('[data-confirm-end]')?.addEventListener('click', async event => {
    if (ending || !endingGame) return;
    ending = true; event.currentTarget.disabled = true;
    const button = event.currentTarget;
    endModal.setAttribute('aria-busy','true');
    try {
      await Checkers.api('/api/games/' + endingGame.id, { method:'DELETE' });
      Checkers.Overlay.close();
      Checkers.toast(Checkers.t('lobby.active.ended'));
      await loadActiveGames(); await loadRecentGames();
    } catch (failure) { document.querySelector('[data-end-error]').textContent = Checkers.apiErrorMessage(failure); }
    finally { ending = false; button.disabled = false; endModal.setAttribute('aria-busy','false'); }
  });

  // Create Friend Game
  document.querySelector('[data-create-friend]')?.addEventListener('click', async (event) => {
    const button = event.currentTarget;
    if (button.disabled) return;
    button.disabled = true;
    try {
      if (!(await leaveSearchBeforeModeChange())) return;
      if (!(await ensureAuth())) return;
      const game = await Checkers.api('/api/games', {
        method: 'POST',
        body: JSON.stringify({ mode: 'FRIEND' })
      });
      if (inviteBox && inviteLink && game.inviteCode) {
        const link = location.origin + '/play?code=' + encodeURIComponent(game.inviteCode) + '&lang=' + Checkers.locale();
        inviteLink.textContent = link;
        inviteBox.classList.remove('hidden');
        inviteBox.classList.add('is-visible');
        inviteBox.dataset.gameId = game.id;
        inviteBox.focus({ preventScroll: false });
      }
      Checkers.toast(Checkers.t('lobby.invite.created'));
    } catch (e) {
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    } finally { button.disabled = false; }
  });

  // Copy Invite Link
  document.querySelector('[data-copy-invite]')?.addEventListener('click', async () => {
    const text = inviteLink?.textContent;
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      Checkers.toast(Checkers.t('toast.copied'));
    } catch (_) {
      Checkers.toast(text);
    }
  });

  // Open Game from Invite box
  document.querySelector('[data-open-invite-game]')?.addEventListener('click', () => {
    const id = inviteBox?.dataset.gameId;
    if (id) location.href = '/play?id=' + id + '&lang=' + Checkers.locale();
  });

  // Native form submission also supports Enter on the invitation code.
  document.querySelector('[data-join-form]')?.addEventListener('submit', async event => {
    event.preventDefault();
    const input = document.querySelector('[data-join-code]');
    const button = document.querySelector('[data-join-friend]');
    const error = document.querySelector('[data-join-error]');
    if (button.disabled) return;
    error.textContent = ''; input.removeAttribute('aria-invalid');
    const code = input.value.trim();
    if (!code) { error.textContent = Checkers.t('validation.required'); input.setAttribute('aria-invalid','true'); input.focus(); return; }
    button.disabled = true;
    try {
      if (!(await leaveSearchBeforeModeChange())) return;
      if (!(await ensureAuth())) return;
      const game = await Checkers.api('/api/games/invite/join', { method:'POST', body:JSON.stringify({ code }) });
      location.href = '/play?id=' + game.id + '&lang=' + Checkers.locale();
    } catch (failure) { error.textContent = Checkers.apiErrorMessage(failure); input.setAttribute('aria-invalid','true'); input.focus(); }
    finally { button.disabled = false; }
  });

  document.querySelector('[data-start-task]')?.addEventListener('click', async () => {
    if (!(await ensureAuth())) return;
    try {
      const game = await Checkers.api('/api/games', {
        method: 'POST',
        body: JSON.stringify({ mode: 'BOT', botDifficulty: 'HARD' })
      });
      location.href = '/play?id=' + game.id + '&lang=' + Checkers.locale();
    } catch (e) {
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    }
  });

  // Matchmaking (Random Opponent)
  const findBtn = document.querySelector('[data-find-random]');
  const cancelBtn = document.querySelector('[data-cancel-random]');
  let matchmakingSearching = false;
  let matchmakingJoining = false;
  let matchmakingSwitching = false;
  let pendingMatch = null;

  window.addEventListener('checkers:localechange', () => {
    const status = document.querySelector('[data-mm-status]');
    if (status) status.textContent = Checkers.t('lobby.matchmaking.queued');
    if (cancelBtn) cancelBtn.textContent = Checkers.t('lobby.mode.random.cancel');
  });

  findBtn?.addEventListener('click', async () => {
    if (matchmakingSearching || matchmakingJoining) return;
    matchmakingJoining = true;
    findBtn.disabled = true;
    if (!(await ensureAuth())) {
      matchmakingJoining = false;
      findBtn.disabled = false;
      return;
    }
    try {
      matchmakingSearching = true;
      findBtn.classList.add('hidden');
      if (mmSearchState) mmSearchState.classList.remove('hidden');
      cancelBtn?.focus({ preventScroll: false });
      startMatchmakingWs();
    } catch (e) {
      matchmakingSearching = false;
      findBtn.classList.remove('hidden');
      findBtn.focus({ preventScroll: true });
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    } finally {
      matchmakingJoining = false;
      findBtn.disabled = false;
    }
  });

  async function leaveSearchBeforeModeChange({ restoreFocus = false } = {}) {
    if (!matchmakingSearching && !matchmakingJoining) return true;
    if (matchmakingSwitching) throw new Error('Search cancellation in progress');
    matchmakingSwitching = true;
    try {
      if (window._mmClient?.connected) await Checkers.api('/api/games/matchmaking/leave', { method: 'DELETE' });
      if (pendingMatch) {
        const matchedId = pendingMatch; stopSearchUi();
        location.href = '/play?id=' + matchedId + '&lang=' + Checkers.locale();
        return false;
      }
      stopSearchUi({ restoreFocus });
      await loadActiveGames();
      return true;
    } catch (error) {
      if (pendingMatch) location.href = '/play?id=' + pendingMatch + '&lang=' + Checkers.locale();
      throw error;
    } finally { matchmakingSwitching = false; }
  }
  cancelBtn?.addEventListener('click', async () => {
    if (matchmakingSwitching) return;
    cancelBtn.disabled = true;
    try { await leaveSearchBeforeModeChange({ restoreFocus: true }); Checkers.toast(Checkers.t('event.MATCHMAKING_CANCELLED')); }
    catch (error) { Checkers.toast(Checkers.apiErrorMessage(error), 'error'); }
    finally { cancelBtn.disabled = false; }
  });

  function stopSearchUi({ restoreFocus = false } = {}) {
    matchmakingSearching = false;
    matchmakingJoining = false;
    pendingMatch = null;
    findBtn?.classList.remove('hidden');
    if (findBtn) findBtn.disabled = false;
    mmSearchState?.classList.add('hidden');
    if (window._mmClient) {
      try { window._mmClient.disconnect(); } catch (_) {}
      window._mmClient = null;
    }
    if (restoreFocus && findBtn) requestAnimationFrame(() => findBtn.focus({ preventScroll: true }));
  }

  function startMatchmakingWs() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
      throw new Error('Matchmaking socket is unavailable');
    }
    if (window._mmClient) return;
    const socket = new SockJS('/ws');
    const client = Stomp.over(socket);
    client.debug = null;
    window._mmClient = client;
    client.connect(
      { Authorization: 'Bearer ' + Checkers.Auth.token() },
      () => {
        if (!matchmakingSearching || window._mmClient !== client) { client.disconnect(); return; }
        client.subscribe('/user/queue/matchmaking', (frame) => {
          if (!matchmakingSearching || window._mmClient !== client) return;
          const msg = JSON.parse(frame.body);
          Checkers.toast(Checkers.eventMessage(msg.type, msg.data));
          if (msg.type === 'MATCHMAKING_MATCHED' && msg.data && msg.data.gameId) {
            if (matchmakingSwitching) { pendingMatch = msg.data.gameId; return; }
            stopSearchUi({ restoreFocus: true });
            location.href = '/play?id=' + msg.data.gameId + '&lang=' + Checkers.locale();
          }
          if (msg.type === 'MATCHMAKING_TIMEOUT' || msg.type === 'MATCHMAKING_CANCELLED') {
            stopSearchUi();
          }
        });
        client.send('/app/matchmaking/join', {}, '{}');
      },
      () => {
        stopSearchUi({ restoreFocus: true });
        Checkers.toast(Checkers.t('toast.disconnected'), 'error');
      }
    );
  }

  // Play with Bot
  document.querySelector('[data-start-bot]')?.addEventListener('click', async (event) => {
    const button = event.currentTarget;
    if (button.disabled) return;
    button.disabled = true;
    const diff = document.querySelector('[data-bot-diff]')?.value || 'MEDIUM';
    try {
      if (!(await leaveSearchBeforeModeChange())) return;
      if (!(await ensureAuth())) return;
      const game = await Checkers.api('/api/games', {
        method: 'POST',
        body: JSON.stringify({ mode: 'BOT', botDifficulty: diff })
      });
      location.href = '/play?id=' + game.id + '&lang=' + Checkers.locale();
    } catch (e) {
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    } finally { button.disabled = false; }
  });

  Promise.resolve(Checkers.sessionReady).then(refreshMe).then(refreshUI);
  Checkers.DailyTask.renderPreview(document.querySelector('[data-task-preview]'));
  const anchor = location.hash && document.getElementById(decodeURIComponent(location.hash.slice(1)));
  if (anchor && ['mode-random', 'mode-bot', 'mode-friend'].includes(anchor.id)) {
    requestAnimationFrame(() => { anchor.focus({ preventScroll: true }); anchor.scrollIntoView({ block: 'start' }); });
  }

});
