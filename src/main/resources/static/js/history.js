document.addEventListener('DOMContentLoaded', async () => {
  if (!Checkers.requireAuth('/history')) return;
  const tableWrap = document.querySelector('[data-history-table-wrap]');
  const empty = document.querySelector('[data-history-empty]');
  const guest = document.querySelector('[data-history-guest]');
  const tbody = document.querySelector('[data-history-body]');

  const user = Checkers.Auth.user();
  if (user && user.guest) {
    guest?.classList.remove('hidden');
    tableWrap?.classList.add('hidden');
    empty?.classList.add('hidden');
    return;
  }

  try {
    const page = await Checkers.api('/api/games/history?size=30');
    const items = page.content || [];
    if (!items.length) {
      empty?.classList.remove('hidden');
      tableWrap?.classList.add('hidden');
      return;
    }
    empty?.classList.add('hidden');
    tableWrap?.classList.remove('hidden');
    tbody.innerHTML = '';
    items.forEach((g) => {
      const tr = document.createElement('tr');
      const reasonKey = 'reason.' + (g.finishReason || '');
      const reason = g.finishReason ? Checkers.t(reasonKey) : '—';
      const vs = (g.whiteUsername || '—') + ' ' + Checkers.t('common.vs') + ' ' + (g.blackUsername || '—');
      tr.innerHTML = `
        <td><span class="mode-badge">${Checkers.t('mode.' + g.mode)}</span></td>
        <td style="font-weight:var(--fw-medium)">${vs}</td>
        <td><strong>${g.winnerUsername || '—'}</strong></td>
        <td>${reason === reasonKey ? g.finishReason : reason}</td>
        <td>
          <a class="btn btn-ghost btn-sm" href="/play?id=${g.id}&lang=${Checkers.locale()}">
            ${Checkers.t('history.open')}
          </a>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (e) {
    Checkers.toast(Checkers.apiErrorMessage(e), 'error');
  }
});
