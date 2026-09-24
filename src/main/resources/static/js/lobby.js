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
  const recentGamesBox = document.querySelector('[data-recent-games-box]');
  const recentGamesList = document.querySelector('[data-recent-games-list]');
  const recentEmpty = document.querySelector('[data-recent-empty]');
  const lobbyBoardRoot = document.querySelector('[data-lobby-board]');

  function isInteractiveTarget(target) {
    return !!target.closest('button, a, input, select, textarea, label, [data-no-card-click]');
  }

  document.querySelectorAll('[data-mode-card]').forEach((card) => {
    const run = () => {
      const selector = card.getAttribute('data-mode-action');
      const action = selector && card.querySelector(selector);
      action?.click();
    };
    card.addEventListener('click', (event) => {
      if (isInteractiveTarget(event.target)) return;
      run();
    });
    card.addEventListener('keydown', (event) => {
      if (event.key !== 'Enter' && event.key !== ' ') return;
      if (isInteractiveTarget(event.target)) return;
      event.preventDefault();
      run();
    });
  });

  // Initialize preview board
  if (lobbyBoardRoot && Checkers.BoardUI) {
    const initialSquares = {
      a1: 'WM', c1: 'WM', e1: 'WM', g1: 'WM',
      b2: 'WM', d2: 'WM', f2: 'WM', h2: 'WM',
      a3: 'WM', c3: 'WM', e3: 'WM', g3: 'WM',
      b6: 'BM', d6: 'BM', f6: 'BM', h6: 'BM',
      a7: 'BM', c7: 'BM', e7: 'BM', g7: 'BM',
      b8: 'BM', d8: 'BM', f8: 'BM', h8: 'BM'
    };
    const boardUi = Checkers.BoardUI.create(lobbyBoardRoot);
    boardUi.setBoard({ turn: 'WHITE', squares: initialSquares });
  }

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
      Checkers.initHeaderAuth();
      refreshUI();
      Checkers.toast(Checkers.t('auth.guest') || 'Guest logged in');
    } catch (e) {
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    }
  });

  function refreshStats() {
    const user = Checkers.Auth.user();
    const loggedIn = Checkers.Auth.isLoggedIn() && !!user;

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
      if (recentGamesBox) recentGamesBox.classList.remove('hidden');
      if (recentGamesList) recentGamesList.innerHTML = '';
      if (recentEmpty) recentEmpty.classList.remove('hidden');
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
      if (recentGamesBox) recentGamesBox.classList.remove('hidden');
      if (recentGamesList) recentGamesList.innerHTML = '';
      if (recentEmpty) recentEmpty.classList.remove('hidden');
    } else {
      if (statsGuest) statsGuest.classList.add('hidden');
      if (ratingHint) ratingHint.classList.add('hidden');
      if (statsGrid) {
        statsGrid.classList.remove('hidden');
        statsGrid.querySelector('[data-stat="wins"]').textContent = user.wins ?? 0;
        statsGrid.querySelector('[data-stat="losses"]').textContent = user.losses ?? 0;
        statsGrid.querySelector('[data-stat="draws"]').textContent = user.draws ?? 0;
      }
    }
  }

  async function loadActiveGames() {
    if (!Checkers.Auth.isLoggedIn()) return;
    try {
      const games = await Checkers.api('/api/games/active');
      if (!activeGamesBox || !activeGamesList) return;
      if (!games || !games.length) {
        activeGamesBox.classList.add('hidden');
        return;
      }
      activeGamesBox.classList.remove('hidden');
      activeGamesList.innerHTML = '';
      games.forEach((g) => {
        const item = document.createElement('div');
        item.className = 'active-game-item';
        const vs = (g.whiteUsername || '—') + ' ' + Checkers.t('common.vs') + ' ' + (g.blackUsername || '—');
        item.innerHTML = `
          <div class="active-game-info">
            <strong>${Checkers.t('mode.' + g.mode)}</strong>
            <span>${vs}</span>
          </div>
          <a class="btn btn-primary btn-sm" href="/play?id=${g.id}&lang=${Checkers.locale()}">
            ${Checkers.t('nav.play')}
          </a>
          <button type="button" class="btn btn-ghost btn-sm active-game-end" data-end-active-game="${g.id}">
            ${Checkers.t('lobby.active.end')}
          </button>
        `;
        activeGamesList.appendChild(item);
      });
      activeGamesList.querySelectorAll('[data-end-active-game]').forEach((button) => {
        button.addEventListener('click', async () => {
          const gameId = button.getAttribute('data-end-active-game');
          if (!gameId) return;
          button.disabled = true;
          try {
            await Checkers.api('/api/games/' + gameId, { method: 'DELETE' });
            Checkers.toast(Checkers.t('lobby.active.ended'));
            await loadActiveGames();
            await loadRecentGames();
          } catch (e) {
            button.disabled = false;
            Checkers.toast(Checkers.apiErrorMessage(e), 'error');
          }
        });
      });
    } catch (_) {
      if (activeGamesBox) activeGamesBox.classList.add('hidden');
    }
  }

  async function loadRecentGames() {
    const user = Checkers.Auth.user();
    if (!Checkers.Auth.isLoggedIn() || !user || user.guest) {
      if (recentGamesList) recentGamesList.innerHTML = '';
      if (recentEmpty) recentEmpty.classList.remove('hidden');
      return;
    }
    try {
      const page = await Checkers.api('/api/games/history?size=4');
      const items = page.content || [];
      if (!recentGamesBox || !recentGamesList) return;
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
        row.className = 'recent-row';
        const vs = (g.whiteUsername || '—') + ' ' + Checkers.t('common.vs') + ' ' + (g.blackUsername || '—');
        const reasonKey = 'reason.' + (g.finishReason || '');
        const reason = g.finishReason ? Checkers.t(reasonKey) : '—';
        row.innerHTML = `
          <span class="recent-avatar"><i data-lucide="user"></i></span>
          <strong>${vs}</strong>
          <span>${Checkers.t('mode.' + g.mode)}</span>
          <span>${reason === reasonKey ? g.finishReason : reason}</span>
          <small>${g.createdAt ? new Date(g.createdAt).toLocaleDateString() : ''}</small>
        `;
        recentGamesList.appendChild(row);
      });
      if (window.lucide) window.lucide.createIcons();
    } catch (_) {
      if (recentGamesBox) recentGamesBox.classList.remove('hidden');
      if (recentGamesList) recentGamesList.innerHTML = '';
      if (recentEmpty) recentEmpty.classList.remove('hidden');
    }
  }

  async function ensureAuth() {
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
    if (!Checkers.Auth.isLoggedIn()) return;
    try {
      const me = await Checkers.api('/api/auth/me');
      const auth = Checkers.Auth.load();
      auth.user = me;
      Checkers.Auth.save(auth);
      Checkers.initHeaderAuth();
    } catch (_) {
      Checkers.Auth.clear();
      Checkers.initHeaderAuth();
    }
  }

  function refreshUI() {
    refreshStats();
    loadActiveGames();
    loadRecentGames();
  }

  // Create Friend Game
  document.querySelector('[data-create-friend]')?.addEventListener('click', async () => {
    if (!(await ensureAuth())) return;
    try {
      const game = await Checkers.api('/api/games', {
        method: 'POST',
        body: JSON.stringify({ mode: 'FRIEND' })
      });
      if (inviteBox && inviteLink && game.inviteCode) {
        const link = location.origin + '/play?code=' + encodeURIComponent(game.inviteCode) + '&lang=' + Checkers.locale();
        inviteLink.textContent = link;
        inviteBox.classList.add('is-visible');
        inviteBox.dataset.gameId = game.id;
      }
      Checkers.toast(Checkers.t('lobby.invite.created'));
    } catch (e) {
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    }
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

  // Join Friend Game by code
  document.querySelector('[data-join-friend]')?.addEventListener('click', async () => {
    if (!(await ensureAuth())) return;
    const code = document.querySelector('[data-join-code]')?.value?.trim();
    if (!code) return;
    try {
      const game = await Checkers.api('/api/games/invite/join', {
        method: 'POST',
        body: JSON.stringify({ code })
      });
      location.href = '/play?id=' + game.id + '&lang=' + Checkers.locale();
    } catch (e) {
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    }
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
      startMatchmakingWs();
    } catch (e) {
      matchmakingSearching = false;
      findBtn.classList.remove('hidden');
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    } finally {
      matchmakingJoining = false;
      findBtn.disabled = false;
    }
  });

  cancelBtn?.addEventListener('click', async () => {
    try {
      await Checkers.api('/api/games/matchmaking/leave', { method: 'DELETE' });
    } catch (_) { /* ignore */ }
    stopSearchUi();
    Checkers.toast(Checkers.t('event.MATCHMAKING_CANCELLED'));
  });

  function stopSearchUi() {
    matchmakingSearching = false;
    matchmakingJoining = false;
    findBtn?.classList.remove('hidden');
    if (findBtn) findBtn.disabled = false;
    mmSearchState?.classList.add('hidden');
    if (window._mmClient) {
      try { window._mmClient.disconnect(); } catch (_) {}
      window._mmClient = null;
    }
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
        client.subscribe('/user/queue/matchmaking', (frame) => {
          const msg = JSON.parse(frame.body);
          Checkers.toast(Checkers.eventMessage(msg.type, msg.data));
          if (msg.type === 'MATCHMAKING_MATCHED' && msg.data && msg.data.gameId) {
            stopSearchUi();
            location.href = '/play?id=' + msg.data.gameId + '&lang=' + Checkers.locale();
          }
          if (msg.type === 'MATCHMAKING_TIMEOUT' || msg.type === 'MATCHMAKING_CANCELLED') {
            stopSearchUi();
          }
        });
        client.send('/app/matchmaking/join', {}, '{}');
      },
      () => {
        stopSearchUi();
        Checkers.toast(Checkers.t('toast.disconnected'), 'error');
      }
    );
  }

  // Play with Bot
  document.querySelector('[data-start-bot]')?.addEventListener('click', async () => {
    if (!(await ensureAuth())) return;
    const diff = document.querySelector('[data-bot-diff]')?.value || 'MEDIUM';
    try {
      const game = await Checkers.api('/api/games', {
        method: 'POST',
        body: JSON.stringify({ mode: 'BOT', botDifficulty: diff })
      });
      location.href = '/play?id=' + game.id + '&lang=' + Checkers.locale();
    } catch (e) {
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    }
  });

  refreshMe().then(refreshUI);
});
