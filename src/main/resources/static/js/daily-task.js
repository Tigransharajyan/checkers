window.Checkers = window.Checkers || {};
Checkers.DailyTask = {
  request: null,
  load() {
    if (!this.request) {
      this.request = Promise.resolve(Checkers.sessionReady).then(() => Checkers.api('/api/tasks/daily', { signal: AbortSignal.timeout(10000) })).catch(error => { this.request = null; throw error; });
    }
    return this.request;
  },
  async renderPreview(root) {
    if (!root) return;
    try {
      const task = await this.load();
      const { squares } = JSON.parse(task.boardState);
      root.replaceChildren();
      for (let rank = 8; rank >= 1; rank--) {
        for (let file = 0; file < 8; file++) {
          const cell = document.createElement('span');
          const code = squares['abcdefgh'[file] + rank];
          if (code) {
            const piece = document.createElement('i');
            piece.className = 'preview-piece ' + (code[0] === 'W' ? 'white' : 'black');
            cell.appendChild(piece);
          }
          root.appendChild(cell);
        }
      }
      const date = document.querySelector('[data-today]');
      if (date) {
        date.dateTime = task.date;
        date.textContent = new Intl.DateTimeFormat(Checkers.locale(), { day: 'numeric', month: 'short', timeZone: 'UTC' })
          .format(new Date(task.date + 'T00:00:00Z'));
      }
    } catch (_) {
      root.classList.add('is-unavailable');
      root.textContent = Checkers.t('common.loadFailed');
    }
  }
};
