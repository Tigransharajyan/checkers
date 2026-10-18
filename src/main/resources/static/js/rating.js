document.addEventListener('DOMContentLoaded', async () => {
  const score = document.querySelector('[data-rating-score]');
  const note = document.querySelector('[data-rating-note]');
  await Checkers.sessionReady;
  if (Checkers.Auth.isLoggedIn()) {
    try {
      const user = await Checkers.api('/api/auth/me');
      const auth = Checkers.Auth.load();
      auth.user = user;
      Checkers.Auth.save(auth);
      Checkers.initHeaderAuth();
    } catch (_) {
      note.textContent = Checkers.t('common.loadFailed');
      return;
    }
  }
  const stats = Checkers.personalStats(Checkers.Auth.user());
  if (!stats) {
    score.textContent = '—';
    note.textContent = Checkers.t('rating.guest');
    return;
  }
  const number = new Intl.NumberFormat(Checkers.locale());
  score.textContent = stats.winRate == null ? '—' : number.format(stats.winRate) + '%';
  note.textContent = Checkers.t(stats.total ? 'rating.saved' : 'rating.empty');
  for (const key of ['wins', 'losses', 'draws']) {
    document.querySelector('[data-rating-' + key + ']').textContent = number.format(stats[key]);
  }
  document.querySelector('[data-rating-progress]').style.width = (stats.winRate || 0) + '%';
});
