window.Checkers = window.Checkers || {};

Checkers.initAssistant = function () {
  const root = document.querySelector('.assistant');
  if (!root) return;

  const toggle = root.querySelector('[data-assistant-toggle]');
  const close = root.querySelector('[data-assistant-close]');
  const form = root.querySelector('[data-assistant-form]');
  const input = root.querySelector('[data-assistant-input]');
  const log = root.querySelector('[data-assistant-log]');
  const sendBtn = root.querySelector('[data-assistant-send]');
  const chips = root.querySelectorAll('[data-chip-key]');

  if (!log) return;

  const STORAGE_PREFIX = 'checkers.assistant.session.';
  let history = loadHistory();

  function storageKey() {
    return STORAGE_PREFIX + Checkers.locale();
  }

  function loadHistory() {
    try {
      return JSON.parse(sessionStorage.getItem(storageKey()) || '[]');
    } catch {
      return [];
    }
  }

  function saveHistory(history) {
    try {
      sessionStorage.setItem(storageKey(), JSON.stringify(history.slice(-30)));
    } catch (_) {}
  }

  function renderMessage(role, text) {
    const row = document.createElement('div');
    row.className = 'assistant-msg ' + role;
    const who = document.createElement('strong');
    who.textContent = role === 'user' ? Checkers.t('assistant.you') : Checkers.t('assistant.bot');
    const body = document.createElement('p');
    body.textContent = text;
    row.appendChild(who);
    row.appendChild(body);
    log.appendChild(row);
    log.scrollTop = log.scrollHeight;
  }

  function append(role, text) {
    renderMessage(role, text);
    history.push({ role, text });
    saveHistory(history);
  }

  function updateChromeText() {
    chips.forEach((btn) => {
      const questionKey = btn.getAttribute('data-chip-key');
      const labelKey = btn.getAttribute('data-chip-label-key');
      if (questionKey) btn.setAttribute('data-chip', Checkers.t(questionKey));
      if (labelKey) btn.textContent = Checkers.t(labelKey);
    });
    if (input) input.placeholder = Checkers.t('assistant.placeholder');
    if (sendBtn) sendBtn.setAttribute('aria-label', Checkers.t('assistant.send'));
    toggle?.setAttribute('aria-label', Checkers.t('assistant.open'));
    close?.setAttribute('aria-label', Checkers.t('assistant.close'));
    const title = root.querySelector('.assistant-head span');
    if (title) title.textContent = Checkers.t('assistant.title');
  }

  function renderHistory() {
    log.innerHTML = '';
    history = loadHistory();
    if (history.length) {
      history.forEach((m) => renderMessage(m.role, m.text));
    } else {
      append('bot', Checkers.t('assistant.welcome'));
    }
  }

  updateChromeText();
  renderHistory();

  toggle?.addEventListener('click', () => {
    root.classList.toggle('is-open');
    if (root.classList.contains('is-open')) {
      setTimeout(() => input?.focus(), 100);
    }
  });

  close?.addEventListener('click', () => root.classList.remove('is-open'));

  // Quick FAQ Chips
  root.querySelectorAll('[data-chip]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const q = btn.getAttribute('data-chip');
      if (q) {
        if (input) input.value = q;
        send();
      }
    });
  });

  async function send() {
    const message = (input?.value || '').trim();
    if (!message) return;
    input.value = '';
    append('user', message);
    if (sendBtn) sendBtn.disabled = true;
    try {
      const res = await Checkers.api('/api/assistant/chat', {
        method: 'POST',
        body: JSON.stringify({
          message,
          locale: Checkers.locale()
        })
      });
      append('bot', res.answer);
    } catch (e) {
      append('bot', Checkers.apiErrorMessage(e));
    } finally {
      if (sendBtn) sendBtn.disabled = false;
      input?.focus();
    }
  }

  form?.addEventListener('submit', (e) => {
    e.preventDefault();
    send();
  });
  sendBtn?.addEventListener('click', (e) => {
    e.preventDefault();
    send();
  });

  window.addEventListener('checkers:localechange', () => {
    updateChromeText();
    renderHistory();
  });
};

document.addEventListener('DOMContentLoaded', () => {
  Checkers.initAssistant();
  if (Checkers.initIcons) Checkers.initIcons();
});
