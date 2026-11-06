document.addEventListener('DOMContentLoaded', () => {
  const score = document.querySelector('[data-rating-score]');
  const note = document.querySelector('[data-rating-note]');
  const status = document.querySelector('[data-rating-status]');
  const error = document.querySelector('[data-rating-error]');
  const errorText = document.querySelector('[data-rating-error-text]');
  const content = document.querySelector('[data-progress-content]');
  const privateResults = document.querySelector('[data-progress-private]');
  const rateRegion = document.querySelector('[data-progress-rate]');
  const rateLabel = document.querySelector('#progress-rate-title');
  const login = document.querySelector('[data-rating-login]');
  const register = document.querySelector('[data-rating-register]');
  const retry = document.querySelector('[data-rating-retry]');

  async function loadProgress() {
    status.classList.remove('hidden');
    status.textContent = Checkers.t('common.loading');
    error.classList.add('hidden');
    retry.disabled = false;
    try {
      await Checkers.sessionReady;
      if (Checkers.Auth.isLoggedIn()) {
        const user = await Checkers.api('/api/auth/me');
        const auth = Checkers.Auth.load();
        auth.user = user;
        Checkers.Auth.save(auth);
        Checkers.initHeaderAuth();
      }
      const user = Checkers.Auth.user();
      const stats = Checkers.personalStats(user);
      const number = new Intl.NumberFormat(Checkers.locale());
      if (!stats) {
        score.classList.add('hidden');
        rateLabel.classList.add('hidden');
        note.textContent = Checkers.t('rating.guest');
        privateResults.classList.add('hidden');
        login.classList.remove('hidden');
        register.classList.remove('hidden');
        content.classList.add('guest-mode');
      } else {
        content.classList.remove('guest-mode');
        rateRegion.classList.toggle('is-no-games', stats.total === 0);
        rateLabel.classList.toggle('hidden', stats.winRate == null);
        score.classList.toggle('hidden', stats.winRate == null);
        if (stats.winRate != null) score.textContent = number.format(stats.winRate) + '%';
        note.textContent = Checkers.t(stats.total ? 'rating.saved' : 'rating.empty');
        privateResults.classList.remove('hidden');
        login.classList.add('hidden');
        register.classList.add('hidden');
        document.querySelector('[data-rating-total]').textContent = number.format(stats.total);
        for (const key of ['wins', 'losses', 'draws']) {
          document.querySelector('[data-rating-' + key + ']').textContent = number.format(stats[key]);
        }
      }
      content.classList.remove('hidden');
      status.textContent = '';
      status.classList.add('hidden');
    } catch (failure) {
      status.textContent = '';
      status.classList.add('hidden');
      content.classList.add('hidden');
      errorText.textContent = Checkers.apiErrorMessage(failure);
      error.classList.remove('hidden');
    }
  }
  retry.addEventListener('click', loadProgress);
  loadProgress();
});
