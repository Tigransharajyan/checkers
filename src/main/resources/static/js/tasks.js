document.addEventListener('DOMContentLoaded', async () => {
  const root = document.querySelector('[data-task-board]');
  const feedback = document.querySelector('[data-task-feedback]');
  const instructions = document.querySelector('[data-task-instructions]');
  const reset = document.querySelector('[data-reset-task]');
  const retry = document.querySelector('[data-retry-task]');
  let initial, current, board;
  let busy = true;

  function storageKey() { return 'checkers.dailyTask.v2.' + initial.id; }
  function show(message, ok) {
    feedback.textContent = message;
    feedback.className = 'task-feedback ' + (ok ? 'ok' : 'bad');
  }
  function setBusy(value) {
    busy = value;
    reset.disabled = value;
    root.setAttribute('aria-busy', String(value));
  }
  function refreshText() {
    const solved = current.status === 'SOLVED';
    instructions.textContent = solved ? Checkers.t('tasks.completed') : Checkers.t('tasks.goal', initial.maxPlayerMoves);
    document.querySelector('[data-task-progress]').textContent = Checkers.t('tasks.progress', current.movesPlayed, initial.maxPlayerMoves);
    document.querySelector('[data-task-date]').textContent = new Intl.DateTimeFormat(Checkers.locale(), { day: 'numeric', month: 'long', timeZone: 'UTC' })
      .format(new Date(initial.date + 'T00:00:00Z'));
    board.setLegal(solved ? [] : current.legalMoves);
    if (solved) show(Checkers.t('tasks.correct'), true);
  }
  function save() {
    try { localStorage.setItem(storageKey(), JSON.stringify(current.playerMoves)); } catch (_) {}
  }
  async function submit(path) {
    if (busy) { board.cancelPendingMove(); return; }
    setBusy(true);
    feedback.className = 'task-feedback hidden';
    try {
      const response = await Checkers.api('/api/tasks/' + initial.id + '/moves', {
        method: 'POST', signal: AbortSignal.timeout(10000), body: JSON.stringify({ moves: [...current.playerMoves, path] })
      });
      if (response.status === 'INCORRECT') {
        board.cancelPendingMove();
        show(Checkers.t('tasks.wrong'), false);
      } else {
        for (let index = 0; index < response.events.length; index++) {
          const event = response.events[index];
          if (index > 0) instructions.textContent = Checkers.t('tasks.defending');
          await board.setBoard(event.boardState, { path: event.path, from: event.path[0], to: event.path.at(-1) });
          Checkers.Sound.play(event.capturedSquares.length ? 'capture' : 'move');
        }
      }
      current = response;
      save();
      refreshText();
    } catch (error) {
      board.cancelPendingMove();
      show(Checkers.apiErrorMessage(error), false);
      refreshText();
    } finally { setBusy(false); }
  }

  async function load() {
    retry.classList.add('hidden');
    setBusy(true);
    instructions.textContent = Checkers.t('common.loading');
    try {
      await Checkers.sessionReady;
      initial = await Checkers.DailyTask.load();
      current = initial;
      try {
        const moves = JSON.parse(localStorage.getItem(storageKey()) || '[]');
        if (Array.isArray(moves) && moves.length) {
          current = await Checkers.api('/api/tasks/' + initial.id + '/moves', {
            method: 'POST', body: JSON.stringify({ moves })
          });
        }
      } catch (_) { current = initial; }
      if (!board) board = Checkers.BoardUI.create(root, { onMove: path => { submit(path); } });
      await board.setBoard(current.boardState);
      refreshText();
      setBusy(false);
    } catch (_) {
      instructions.textContent = Checkers.t('common.loadFailed');
      retry.classList.remove('hidden');
    }
  }

  reset.addEventListener('click', async () => {
    if (busy || !initial) return;
    setBusy(true);
    board.cancelPendingMove();
    current = initial;
    save();
    await board.setBoard(initial.boardState);
    board.setLastMove(null, null);
    feedback.className = 'task-feedback hidden';
    refreshText();
    setBusy(false);
  });
  retry.addEventListener('click', load);
  await load();
});
