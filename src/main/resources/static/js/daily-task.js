window.Checkers = window.Checkers || {};
Checkers.formatTaskDate = function (isoDate, short = false) {
  const date = new Date(isoDate + 'T00:00:00Z');
  const months = Checkers.t('redesign.months.' + (short ? 'short' : 'long')).split('|');
  const month = months[date.getUTCMonth()];
  return Checkers.locale() === 'en' ? month + ' ' + date.getUTCDate() : date.getUTCDate() + ' ' + month;
};
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
    const retry = document.querySelector('[data-daily-retry]');
    const loading = document.querySelector('[data-task-preview-loading]');
    const error = document.querySelector('[data-task-preview-error]');
    retry?.classList.add('hidden');
    error?.classList.add('hidden');
    loading?.classList.remove('hidden');
    root.classList.add('hidden');
    try {
      const task = await this.load();
      const { squares } = JSON.parse(task.boardState);
      root.classList.remove('is-unavailable');
      root.classList.remove('hidden');
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
        date.textContent = Checkers.formatTaskDate(task.date, true);
      }
      loading?.classList.add('hidden');
    } catch (_) {
      root.replaceChildren();
      root.classList.add('hidden');
      loading?.classList.add('hidden');
      error?.classList.remove('hidden');
      retry?.classList.remove('hidden');
    }
  }
};
document.addEventListener('DOMContentLoaded', () => {
  document.querySelector('[data-daily-retry]')?.addEventListener('click', async event => {
    const button = event.currentTarget;
    button.disabled = true;
    await Checkers.DailyTask.renderPreview(document.querySelector('[data-task-preview]'));
    button.disabled = false;
  });
});
