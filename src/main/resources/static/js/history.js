document.addEventListener('DOMContentLoaded', async () => {
  const tableWrap = document.querySelector('[data-history-table-wrap]');
  const empty = document.querySelector('[data-history-empty]');
  const guest = document.querySelector('[data-history-guest]');
  const tbody = document.querySelector('[data-history-body]');

  let cachedItems = null;

  function renderRows(items, user) {
    tbody.innerHTML = '';
    items.forEach((g) => {
      const tr = document.createElement('tr');
      const reasonKey = 'reason.' + (g.finishReason || '');
      const reason = g.finishReason ? Checkers.t(reasonKey) : '—';
      const vs = (g.whiteUsername || '—') + ' ' + Checkers.t('common.vs') + ' ' + (g.mode === 'BOT' ? Checkers.t('common.bot') : (g.blackUsername || '—'));

      let resultBadge = '';
      const winner = g.winnerUsername || (g.mode === 'BOT' && g.finishReason !== 'DRAW' ? Checkers.t('common.bot') : '—');
      if (g.winnerUsername) {
        if (g.winnerUsername === user.username) {
          resultBadge = `<span class="res-badge res-win">${Checkers.t('play.result.win')}</span>`;
        } else {
          resultBadge = `<span class="res-badge res-loss">${Checkers.t('play.result.loss')}</span>`;
        }
      } else if (g.mode === 'BOT' && g.finishReason !== 'DRAW') {
        resultBadge = `<span class="res-badge res-loss">${Checkers.t('play.result.loss')}</span>`;
      } else if (g.finishReason === 'DRAW') {
        resultBadge = `<span class="res-badge res-draw">${Checkers.t('play.result.draw')}</span>`;
      }

      tr.innerHTML = `
        <td><span class="mode-badge">${Checkers.t('mode.' + g.mode)}</span></td>
        <td style="font-weight:var(--fw-medium)">${Checkers.escapeHtml(vs)}</td>
        <td><strong>${Checkers.escapeHtml(winner)}</strong></td>
        <td>${resultBadge || (reason === reasonKey ? g.finishReason : reason)}</td>
        <td>
          <a class="btn btn-ghost btn-sm" href="/play?id=${g.id}&lang=${Checkers.locale()}">
            <i data-lucide="eye"></i>
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
    if (!user || user.guest) {
      guest?.classList.remove('hidden');
      tableWrap?.classList.add('hidden');
      empty?.classList.add('hidden');
      return;
    }

    try {
      if (!cachedItems) {
        const page = await Checkers.api('/api/games/history?size=30');
        cachedItems = page.content || [];
      }
      if (!cachedItems.length) {
        empty?.classList.remove('hidden');
        tableWrap?.classList.add('hidden');
        return;
      }
      empty?.classList.add('hidden');
      tableWrap?.classList.remove('hidden');
      renderRows(cachedItems, user);
    } catch (e) {
      Checkers.toast(Checkers.apiErrorMessage(e), 'error');
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
