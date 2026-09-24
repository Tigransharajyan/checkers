document.addEventListener('DOMContentLoaded', async () => {
  if (!Checkers.requireAuth(location.pathname + location.search)) return;

  const params = new URLSearchParams(location.search);
  let gameId = params.get('id');
  const code = params.get('code');

  const statusEl = document.querySelector('[data-game-status]');
  const logEl = document.querySelector('[data-move-log]');
  const youName = document.querySelector('[data-you-name]');
  const oppName = document.querySelector('[data-opp-name]');
  const youColor = document.querySelector('[data-you-color]');
  const oppColor = document.querySelector('[data-opp-color]');
  const youCard = document.querySelector('[data-you-card]');
  const oppCard = document.querySelector('[data-opp-card]');
  const drawOfferBox = document.querySelector('[data-draw-offer]');
  const drawOfferBtn = document.querySelector('[data-draw-offer-btn]');
  const boardRoot = document.querySelector('[data-board]');

  // Modals
  const resignModal = document.querySelector('[data-resign-modal]');
  const gameoverModal = document.querySelector('[data-gameover-modal]');
  const gameoverTitle = document.querySelector('[data-gameover-title]');
  const gameoverReason = document.querySelector('[data-gameover-reason]');

  let game = null;
  let myColor = null;
  let stompClient = null;
  let boardUi = null;
  let disconnectTimer = null;
  let startSoundPlayed = false;
  let endSoundPlayed = false;

  function setStatus(text, kind) {
    if (!statusEl) return;
    statusEl.textContent = text;
    statusEl.className = 'status-banner' + (kind ? ' ' + kind : '');
  }

  function initialOf(name) {
    return name && name.charAt ? name.charAt(0).toUpperCase() : '?';
  }

  function renderPlayers() {
    const me = Checkers.Auth.user();
    if (!game || !me) return;
    const white = game.whitePlayer;
    const black = game.blackPlayer;
    myColor = white && white.id === me.id ? 'WHITE' : (black && black.id === me.id ? 'BLACK' : null);

    if (youName) youName.textContent = me.username;
    if (youColor) {
      const dot = myColor === 'WHITE' ? '<span class="piece-dot white"></span>' : '<span class="piece-dot black"></span>';
      youColor.innerHTML = dot + ' ' + (myColor ? Checkers.t('play.color.' + myColor) : '');
    }
    const youAvatar = document.querySelector('[data-you-avatar]');
    if (youAvatar) youAvatar.textContent = initialOf(me.username);

    let opp = null;
    if (game.mode === 'BOT') {
      opp = { username: Checkers.t('common.bot') + (game.botDifficulty ? ' (' + Checkers.t('lobby.difficulty.' + game.botDifficulty) + ')' : '') };
    } else if (myColor === 'WHITE') {
      opp = black;
    } else {
      opp = white;
    }

    if (oppName) oppName.textContent = opp ? opp.username : Checkers.t('play.waitingOpponent');
    if (oppColor) {
      const oc = myColor === 'WHITE' ? 'BLACK' : 'WHITE';
      const dot = oc === 'WHITE' ? '<span class="piece-dot white"></span>' : '<span class="piece-dot black"></span>';
      oppColor.innerHTML = dot + ' ' + (opp ? Checkers.t('play.color.' + oc) : '');
    }
    const oppAvatar = document.querySelector('[data-opp-avatar]');
    if (oppAvatar) {
      oppAvatar.textContent = opp ? initialOf(opp.username) : '…';
    }

    if (boardUi) boardUi.setFlip(myColor === 'BLACK');
  }

  function renderLog() {
    if (!logEl || !game || !game.moves) return;
    logEl.innerHTML = '';

    // Group moves into pairs (1. White Move, Black Move)
    const pairs = [];
    game.moves.forEach((m, idx) => {
      const pairIdx = Math.floor(idx / 2);
      if (!pairs[pairIdx]) {
        pairs[pairIdx] = { num: pairIdx + 1, white: '', black: '' };
      }
      const pathStr = m.path ? m.path.replace(/,/g, '-') : (m.fromSquare + '-' + m.toSquare);
      if (idx % 2 === 0) {
        pairs[pairIdx].white = pathStr;
      } else {
        pairs[pairIdx].black = pathStr;
      }
    });

    pairs.forEach((p) => {
      const row = document.createElement('div');
      row.className = 'move-log-row';
      row.innerHTML = `
        <span class="num">${p.num}.</span>
        <span class="white-move">${p.white}</span>
        <span class="black-move">${p.black || ''}</span>
      `;
      logEl.appendChild(row);
    });

    logEl.scrollTop = logEl.scrollHeight;
  }

  function showGameOverModal(title, reason) {
    if (!gameoverModal) return;
    if (gameoverTitle) gameoverTitle.textContent = title;
    if (gameoverReason) gameoverReason.textContent = reason;
    gameoverModal.classList.remove('hidden');
  }

  function updateTurnBanner(hasCaptures) {
    if (!game) return;

    if (game.status === 'WAITING') {
      setStatus(Checkers.t('play.waitingOpponent'), 'warn');
      youCard?.classList.remove('is-active-turn');
      oppCard?.classList.remove('is-active-turn');
      return;
    }

    if (game.status === 'FINISHED') {
      const me = Checkers.Auth.user();
      let result = Checkers.t('play.finished');
      let isWin = false;

      if (game.finishReason === 'DRAW') {
        result = Checkers.t('play.result.draw');
      } else if (game.winnerId && me && game.winnerId === me.id) {
        result = Checkers.t('play.result.win');
        isWin = true;
      } else if (game.winnerId) {
        result = Checkers.t('play.result.loss');
      } else if (game.mode === 'BOT' && game.finishReason === 'RESIGN') {
        result = Checkers.t('play.result.loss');
      }

      const reasonKey = 'reason.' + (game.finishReason || '');
      const reason = game.finishReason ? (Checkers.t(reasonKey) !== reasonKey ? Checkers.t(reasonKey) : game.finishReason) : '';
      setStatus(result + (reason ? ' — ' + reason : ''), isWin ? '' : 'danger');

      youCard?.classList.remove('is-active-turn');
      oppCard?.classList.remove('is-active-turn');

      showGameOverModal(result, reason);
      return;
    }

    if (game.currentTurn === myColor) {
      youCard?.classList.add('is-active-turn');
      oppCard?.classList.remove('is-active-turn');
      if (hasCaptures) {
        setStatus(Checkers.t('play.yourTurn') + ' — ' + (Checkers.t('error.move.CAPTURE_MANDATORY') || 'Обязательное взятие!'), 'warn');
      } else {
        setStatus(Checkers.t('play.yourTurn'));
      }
    } else {
      oppCard?.classList.add('is-active-turn');
      youCard?.classList.remove('is-active-turn');
      if (game.mode === 'BOT' && game.currentTurn === 'BLACK') {
        setStatus(Checkers.t('play.thinking'), 'warn');
      } else {
        setStatus(Checkers.t('play.opponentTurn'), 'warn');
      }
    }
  }

  async function loadLegal() {
    if (!game || game.status !== 'IN_PROGRESS' || game.currentTurn !== myColor) {
      boardUi.setLegal([]);
      updateTurnBanner(false);
      return;
    }
    try {
      const paths = await Checkers.api('/api/games/' + game.id + '/legal-moves');
      boardUi.setLegal(paths);
      const hasCaptures = paths.some(Checkers.BoardUI.isCapturePath);
      updateTurnBanner(hasCaptures);
    } catch (_) {
      boardUi.setLegal([]);
      updateTurnBanner(false);
    }
  }

  function lastMoveFromGame() {
    if (!game || !game.moves || !game.moves.length) return null;
    const m = game.moves[game.moves.length - 1];
    let from = m.fromSquare;
    let to = m.toSquare;
    if ((!from || !to) && m.path) {
      const parts = String(m.path).split(/[,-]/).filter(Boolean);
      if (parts.length >= 2) {
        from = parts[0];
        to = parts[parts.length - 1];
      }
    }
    return from && to ? { from, to } : null;
  }

  async function applyGame(g) {
    const previousStatus = game && game.status;
    game = g;
    renderPlayers();
    boardUi.setBoard(game.boardState, lastMoveFromGame());
    renderLog();
    await loadLegal();
    if (game.status === 'IN_PROGRESS' && !startSoundPlayed) {
      startSoundPlayed = true;
      Checkers.Sound.play('start');
    }
    if (game.status === 'FINISHED' && previousStatus !== 'FINISHED' && !endSoundPlayed) {
      endSoundPlayed = true;
      Checkers.Sound.play('end');
    }
  }

  function connectWs() {
    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') return;
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect(
      { Authorization: 'Bearer ' + Checkers.Auth.token() },
      () => {
        Checkers.toast(Checkers.t('toast.connected'));
        stompClient.subscribe('/topic/game/' + gameId, onGameEvent);
        stompClient.subscribe('/user/queue/errors', (frame) => {
          const msg = JSON.parse(frame.body);
          const moveErr = msg.data && msg.data.moveError;
          if (moveErr) {
            Checkers.toast(Checkers.t('error.move.' + moveErr), 'error');
          } else if (msg.data && msg.data.errorCode) {
            Checkers.toast(Checkers.t('error.api.' + msg.data.errorCode), 'error');
          } else {
            Checkers.toast(Checkers.eventMessage(msg.type, msg.data), 'error');
          }
          loadLegal();
        });
        stompClient.send('/app/game/' + gameId + '/presence', {}, '{}');
      },
      () => Checkers.toast(Checkers.t('toast.disconnected'), 'error')
    );
  }

  async function onGameEvent(frame) {
    const msg = JSON.parse(frame.body);
    const type = msg.type;
    const data = msg.data || {};
    const me = Checkers.Auth.user();

    if (type === 'MOVE_MADE' || type === 'GAME_FINISHED') {
      if (data.path) {
        if (!game.moves) game.moves = [];
        game.moves.push({
          moveNumber: game.moves.length + 1,
          path: Array.isArray(data.path) ? data.path.join(',') : data.path,
          fromSquare: data.fromSquare,
          toSquare: data.toSquare
        });
        renderLog();
      }
      if (data.boardState) {
        game.boardState = data.boardState;
        const pathArr = Array.isArray(data.path)
          ? data.path
          : (typeof data.path === 'string' ? data.path.split(/[,-]/).filter(Boolean) : []);
        const from = data.fromSquare || pathArr[0];
        const to = data.toSquare || pathArr[pathArr.length - 1];
        boardUi.setBoard(data.boardState, from && to ? { from, to } : lastMoveFromGame());
      }
      if (data.currentTurn) game.currentTurn = data.currentTurn;
      if (data.status) game.status = data.status;
      if (data.finishReason) game.finishReason = data.finishReason;
      if (data.winnerId !== undefined) game.winnerId = data.winnerId;

      const path = Array.isArray(data.path)
        ? data.path
        : (typeof data.path === 'string' ? data.path.split(/[,-]/).filter(Boolean) : []);
      if (path.length >= 2) {
        Checkers.Sound.play(Checkers.BoardUI.isCapturePath(path) ? 'capture' : 'move');
      }
      if (game.status === 'FINISHED' && !endSoundPlayed) {
        endSoundPlayed = true;
        Checkers.Sound.play('end');
      }

      await loadLegal();
      return;
    }

    if (type === 'DRAW_OFFERED') {
      // ONLY show accept/decline modal to the receiver, NOT the sender
      if (data.userId === me?.id) {
        Checkers.toast(Checkers.t('play.drawOffer') + ': ' + (Checkers.t('toast.moveSent') || 'Отправлено'));
        if (drawOfferBtn) drawOfferBtn.disabled = true;
      } else {
        drawOfferBox?.classList.remove('hidden');
        Checkers.toast(Checkers.eventMessage(type, data));
      }
      return;
    }

    if (type === 'DRAW_ACCEPTED' || type === 'DRAW_DECLINED' || type === 'GAME_RESIGNED' || type === 'TECHNICAL_DEFEAT') {
      drawOfferBox?.classList.add('hidden');
      if (drawOfferBtn) drawOfferBtn.disabled = false;
      Checkers.toast(Checkers.eventMessage(type, data));
      try {
        const fresh = await Checkers.api('/api/games/' + gameId);
        await applyGame(fresh);
      } catch (_) { /* ignore */ }
      return;
    }

    if (type === 'OPPONENT_DISCONNECTED') {
      let secondsLeft = data.reconnectTimeoutSeconds || 60;
      setStatus(Checkers.eventMessage(type, { reconnectTimeoutSeconds: secondsLeft }), 'warn');
      Checkers.toast(Checkers.eventMessage(type, data), 'error');

      if (disconnectTimer) clearInterval(disconnectTimer);
      disconnectTimer = setInterval(() => {
        secondsLeft--;
        if (secondsLeft <= 0) {
          clearInterval(disconnectTimer);
        } else {
          setStatus(Checkers.eventMessage(type, { reconnectTimeoutSeconds: secondsLeft }), 'warn');
        }
      }, 1000);
      return;
    }

    if (type === 'OPPONENT_RECONNECTED') {
      if (disconnectTimer) {
        clearInterval(disconnectTimer);
        disconnectTimer = null;
      }
      Checkers.toast(Checkers.eventMessage(type, data));
      try {
        const fresh = await Checkers.api('/api/games/' + gameId);
        await applyGame(fresh);
      } catch (_) { /* ignore */ }
      return;
    }

    if (type === 'GAME_STARTED' || type === 'INVITE_ACCEPTED') {
      Checkers.toast(Checkers.eventMessage(type, data));
      try {
        const fresh = await Checkers.api('/api/games/' + gameId);
        await applyGame(fresh);
      } catch (_) { /* ignore */ }
      return;
    }

    Checkers.toast(Checkers.eventMessage(type, data));
  }

  function sendMove(path) {
    if (!stompClient || !stompClient.connected) {
      Checkers.toast(Checkers.t('toast.disconnected'), 'error');
      return;
    }
    stompClient.send('/app/game/' + gameId + '/move', {}, JSON.stringify({
      path,
      fromSquare: path[0],
      toSquare: path[path.length - 1]
    }));
  }

  // Resign Modal handling
  document.querySelector('[data-resign-btn]')?.addEventListener('click', () => {
    resignModal?.classList.remove('hidden');
  });

  document.querySelector('[data-cancel-resign]')?.addEventListener('click', () => {
    resignModal?.classList.add('hidden');
  });

  document.querySelector('[data-confirm-resign]')?.addEventListener('click', () => {
    resignModal?.classList.add('hidden');
    if (!stompClient) return;
    stompClient.send('/app/game/' + gameId + '/resign', {}, '{}');
  });

  // Draw offer
  drawOfferBtn?.addEventListener('click', () => {
    if (!stompClient) return;
    stompClient.send('/app/game/' + gameId + '/draw', {}, '{}');
  });

  document.querySelector('[data-draw-accept]')?.addEventListener('click', () => {
    if (!stompClient) return;
    stompClient.send('/app/game/' + gameId + '/draw', {}, JSON.stringify({ accept: true }));
    drawOfferBox?.classList.add('hidden');
  });

  document.querySelector('[data-draw-decline]')?.addEventListener('click', () => {
    if (!stompClient) return;
    stompClient.send('/app/game/' + gameId + '/draw', {}, JSON.stringify({ accept: false }));
    drawOfferBox?.classList.add('hidden');
  });

  // Rematch / New Game from modal
  document.querySelector('[data-new-game-btn]')?.addEventListener('click', async () => {
    if (game && game.mode === 'BOT') {
      try {
        const freshGame = await Checkers.api('/api/games', {
          method: 'POST',
          body: JSON.stringify({ mode: 'BOT', botDifficulty: game.botDifficulty || 'MEDIUM' })
        });
        location.href = '/play?id=' + freshGame.id + '&lang=' + Checkers.locale();
        return;
      } catch (_) {}
    }
    location.href = '/?lang=' + Checkers.locale();
  });

  boardUi = Checkers.BoardUI.create(boardRoot, {
    onMove: sendMove
  });

  try {
    if (!gameId && code) {
      const joined = await Checkers.api('/api/games/invite/join', {
        method: 'POST',
        body: JSON.stringify({ code })
      });
      gameId = joined.id;
      history.replaceState(null, '', '/play?id=' + gameId + '&lang=' + Checkers.locale());
      await applyGame(joined);
    } else if (gameId) {
      const g = await Checkers.api('/api/games/' + gameId);
      await applyGame(g);
    } else {
      setStatus(Checkers.t('error.api.GAME_NOT_FOUND'), 'danger');
      return;
    }
    connectWs();
  } catch (e) {
    Checkers.toast(Checkers.apiErrorMessage(e), 'error');
    setStatus(Checkers.apiErrorMessage(e), 'danger');
  }
});
