document.addEventListener('DOMContentLoaded', () => {
  const user = Checkers.Auth.user();
  const score = document.querySelector('[data-rating-score]');
  const note = document.querySelector('[data-rating-note]');
  const wins = document.querySelector('[data-rating-wins]');
  const losses = document.querySelector('[data-rating-losses]');
  const draws = document.querySelector('[data-rating-draws]');

  if (!user || user.guest) {
    if (score) {
      score.textContent = Checkers.t('rating.none');
      score.classList.add('is-empty');
    }
    if (note) note.textContent = Checkers.t('rating.guest');
    return;
  }

  const w = user.wins || 0;
  const l = user.losses || 0;
  const d = user.draws || 0;
  const rating = 1200 + w * 18 + d * 4 - l * 12;

  if (score) {
    score.textContent = String(Math.max(100, rating));
    score.classList.remove('is-empty');
  }
  if (note) note.textContent = Checkers.t('rating.saved');
  if (wins) wins.textContent = w;
  if (losses) losses.textContent = l;
  if (draws) draws.textContent = d;
});
