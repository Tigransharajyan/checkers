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
  const panel = root.querySelector('[data-assistant-panel]');
  const backdrop = root.querySelector('[data-assistant-backdrop]');
  const typing = root.querySelector('[data-assistant-typing]');

  if (!log) return;

  const STORAGE_PREFIX = 'checkers.assistant.session.';
  let history = loadHistory();
  let sending = false;

  function storageKey() {
    return STORAGE_PREFIX + Checkers.locale();
  }

  function loadHistory() {
    try {
      const saved = JSON.parse(sessionStorage.getItem(storageKey()) || '[]');
      return Array.isArray(saved) ? saved.filter((item) => item &&
        (item.role === 'user' || item.role === 'bot') && typeof item.text === 'string').slice(-30) : [];
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
    if (role !== 'user') {
      const avatar = document.createElement('span');
      avatar.className = 'assistant-msg-avatar';
      avatar.setAttribute('aria-hidden', 'true');
      // This constant icon contains no user or provider content.
      avatar.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><rect x="4" y="6" width="16" height="13" rx="5"/><path d="M12 3v3m-4 6h.01M16 12h.01M9 16h6"/></svg>';
      row.appendChild(avatar);
    }
    const content = document.createElement('div');
    content.className = 'assistant-msg-content';
    const who = document.createElement('strong');
    who.textContent = role === 'user' ? Checkers.t('assistant.you') : Checkers.t('assistant.bot');
    const body = document.createElement('p');
    body.textContent = text;
    content.append(who, body);
    row.appendChild(content);
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
    toggle?.setAttribute('title', Checkers.t('assistant.open'));
    log.setAttribute('aria-label', Checkers.t('assistant.title'));
    root.querySelectorAll('[data-assistant-i18n]').forEach((node) => {
      node.textContent = Checkers.t(node.dataset.assistantI18n);
    });
    root.querySelector('.assistant-chips')?.setAttribute('aria-label', Checkers.t('assistant.quickTopics'));
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

  const mobile = matchMedia('(max-width: 900px)');

  /** Keeps the growing composer inside the panel, including when the on-screen keyboard opens. */
  function resizeInput() {
    if (!input) return;
    input.style.height = '40px';
    input.style.height = Math.min(112, Math.max(40, input.scrollHeight)) + 'px';
  }

  /** Anchors the widget to the visible viewport rather than behind a mobile keyboard. */
  function updateViewport() {
    const viewport = window.visualViewport;
    const height = viewport ? viewport.height : innerHeight;
    const keyboard = viewport ? Math.max(0, innerHeight - height - viewport.offsetTop) : 0;
    root.style.setProperty('--assistant-viewport', height + 'px');
    root.style.setProperty('--assistant-keyboard', keyboard + 'px');
    root.classList.toggle('has-keyboard', keyboard > 100);
  }

  /** Shows transient typing feedback and prevents duplicate submissions and shortcut requests. */
  function setSending(value) {
    sending = value;
    root.classList.toggle('is-sending', value);
    if (typing) typing.hidden = !value;
    if (sendBtn) sendBtn.disabled = value;
    chips.forEach((button) => { button.disabled = value; });
    form?.setAttribute('aria-busy', String(value));
    log.setAttribute('aria-busy', String(value));
    log.scrollTop = log.scrollHeight;
  }

  /** Removes the dialog from keyboard navigation and restores the launcher after any close path. */
  function hide() {
    root.classList.remove('is-open');
    toggle.setAttribute('aria-expanded', 'false');
    panel.setAttribute('aria-hidden', 'true');
    panel.removeAttribute('aria-modal');
    panel.inert = true;
  }

  toggle?.addEventListener('click', () => {
    if (Checkers.Overlay.isOpen(panel)) { Checkers.Overlay.close(); return; }
    Checkers.Overlay.open(panel, {
      modal: mobile.matches, trigger: toggle, initial: input, dismissSurface: backdrop,
      onOpen: () => {
        panel.inert = false;
        panel.setAttribute('aria-hidden', 'false');
        root.classList.add('is-open');
        toggle.setAttribute('aria-expanded', 'true');
        if (mobile.matches) panel.setAttribute('aria-modal', 'true');
        updateViewport();
        resizeInput();
        log.scrollTop = log.scrollHeight;
      },
      onClose: hide
    });
  });
  close?.addEventListener('click', () => { if (Checkers.Overlay.isOpen(panel)) Checkers.Overlay.close(); });
  backdrop?.addEventListener('click', () => { if (Checkers.Overlay.isOpen(panel)) Checkers.Overlay.close(); });
  mobile.addEventListener('change', () => { if (Checkers.Overlay.isOpen(panel)) Checkers.Overlay.close(); });
  input?.addEventListener('input', resizeInput);
  input?.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      send();
    }
  });
  window.visualViewport?.addEventListener('resize', updateViewport);
  window.visualViewport?.addEventListener('scroll', updateViewport);
  window.addEventListener('resize', updateViewport);
  updateViewport();

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

  /** Sends one question with bounded user-only context; credentials stay on the server. */
  async function send() {
    const message = (input?.value || '').trim();
    if (!message || sending) return;
    setSending(true);
    const language = Checkers.locale();
    const recentQuestions = history.filter((item) => item.role === 'user')
      .slice(-4).map((item) => item.text.slice(0, 500));
    input.value = '';
    resizeInput();
    append('user', message);
    input.focus({ preventScroll: true });
    try {
      const res = await Checkers.api('/api/assistant/chat', {
        method: 'POST',
        signal: AbortSignal.timeout(15000),
        body: JSON.stringify({
          message,
          locale: language,
          recentQuestions
        })
      });
      if (Checkers.locale() === language) append('bot', res.answer);
    } catch (e) {
      if (Checkers.locale() === language) append('bot', Checkers.apiErrorMessage(e));
    } finally {
      setSending(false);
      if (Checkers.Overlay.isOpen(panel) && panel.contains(document.activeElement)) {
        input?.focus({ preventScroll: true });
      }
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
