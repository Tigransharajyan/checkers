document.addEventListener('DOMContentLoaded', async () => {
  const tableWrap = document.querySelector('[data-history-table-wrap]');
  const empty = document.querySelector('[data-history-empty]');
  const guest = document.querySelector('[data-history-guest]');
  const tbody = document.querySelector('[data-history-body]');

  const status = document.querySelector('[data-history-status]');
  const retry = document.querySelector('[data-history-retry]');
  const scrollHint = document.querySelector('#history-scroll-hint');
  const updateOverflow = () => {
    const overflow = !tableWrap.classList.contains('hidden') && tableWrap.scrollWidth > tableWrap.clientWidth;
    scrollHint.classList.toggle('hidden', !overflow);
    tableWrap.tabIndex = overflow ? 0 : -1;
  };
  window.addEventListener('resize', updateOverflow);
  retry?.addEventListener('click', () => loadHistory());
  let cachedItems = null;

  function renderRows(items, user) {
    tbody.innerHTML = '';
    items.forEach((g) => {
      const tr = document.createElement('tr');
      const reasonKey = 'reason.' + (g.finishReason || '');
      const reason = g.finishReason ? Checkers.t(reasonKey) : '—';
      const vs = (g.whiteUsername || '—') + ' ' + Checkers.t('common.vs') + ' ' + (g.mode === 'BOT' ? Checkers.t('common.bot') : (g.blackUsername || '—'));

      let resultBadge = '';
      const winner = g.winnerUsername || (g.mode === 'BOT' && !Checkers.isDraw(g.finishReason) ? Checkers.t('common.bot') : '—');
      if (g.winnerUsername) {
        if (g.winnerUsername === user.username) {
          resultBadge = `<span class="res-badge res-win">${Checkers.t('play.result.win')}</span>`;
        } else {
          resultBadge = `<span class="res-badge res-loss">${Checkers.t('play.result.loss')}</span>`;
        }
      } else if (g.mode === 'BOT' && !Checkers.isDraw(g.finishReason)) {
        resultBadge = `<span class="res-badge res-loss">${Checkers.t('play.result.loss')}</span>`;
      } else if (Checkers.isDraw(g.finishReason)) {
        resultBadge = `<span class="res-badge res-draw">${Checkers.t('play.result.draw')}</span>`;
      }

      const dateValue = g.finishedAt || g.startedAt || g.createdAt;
      const date = dateValue ? new Date(dateValue) : null;
      const validDate = date && !Number.isNaN(date.valueOf());
      const dateText = validDate
        ? new Intl.DateTimeFormat(Checkers.locale(), { year: 'numeric', month: 'short', day: 'numeric' }).format(date)
        : '—';
      tr.innerHTML = `
        <td data-label="${Checkers.t('history.date')}"><time${validDate ? ` datetime="${date.toISOString()}"` : ''}>${Checkers.escapeHtml(dateText)}</time></td>
        <td data-label="${Checkers.t('history.mode')}"><span class="mode-badge">${Checkers.t('mode.' + g.mode)}</span></td>
        <td data-label="${Checkers.t('history.players')}" class="history-players">${Checkers.escapeHtml(vs)}</td>
        <td data-label="${Checkers.t('history.winner')}"><strong>${Checkers.escapeHtml(winner)}</strong></td>
        <td data-label="${Checkers.t('history.result')}">${resultBadge || (reason === reasonKey ? g.finishReason : reason)}</td>
        <td data-label="${Checkers.t('history.open')}">
          <a class="btn btn-ghost btn-sm" href="/play?id=${g.id}&lang=${Checkers.locale()}">
            <i data-lucide="eye" aria-hidden="true"></i>
            <span>${Checkers.t('history.open')}</span>
          </a>
        </td>
      `;
      tbody.appendChild(tr);
    });
    if (window.lucide && typeof window.lucide.createIcons === 'function') {
      window.lucide.createIcons();
    }
  }

  async function loadHistory() {
    const user = Checkers.Auth.user();
    status.textContent = ''; retry.classList.add('hidden');
    if (!user || user.guest) {
      guest?.classList.remove('hidden');
      tableWrap?.classList.add('hidden');
      empty?.classList.add('hidden');
      return;
    }

    status.textContent = Checkers.t('common.loading');
    try {
      if (!cachedItems) {
        const page = await Checkers.api('/api/games/history?size=30');
        cachedItems = page.content || [];
      }
      status.textContent = '';
      if (!cachedItems.length) {
        empty?.classList.remove('hidden');
        tableWrap?.classList.add('hidden');
        return;
      }
      empty?.classList.add('hidden');
      tableWrap?.classList.remove('hidden');
      renderRows(cachedItems, user);
      updateOverflow();
    } catch (e) {
      status.textContent = Checkers.apiErrorMessage(e);
      retry.classList.remove('hidden');
      tableWrap.classList.add('hidden');
    }
  }

  await Checkers.sessionReady;
  loadHistory();
  window.addEventListener('checkers:localechange', () => {
    const user = Checkers.Auth.user();
    if (user && !user.guest && cachedItems && cachedItems.length) {
      renderRows(cachedItems, user);
    }
  });
});
