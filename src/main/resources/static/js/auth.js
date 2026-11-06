document.addEventListener('DOMContentLoaded', () => {
  const form = document.querySelector('[data-auth-form]');
  if (!form) return;
  const mode = form.dataset.authForm;
  const status = form.querySelector('[data-form-status]');
  const buttons = form.querySelectorAll('button');
  let busy = false;
  function setBusy(value) {
    busy = value; form.setAttribute('aria-busy', String(value));
    buttons.forEach(button => { button.disabled = value; });
  }
  function clearErrors() {
    form.querySelectorAll('.is-error').forEach(input => { input.classList.remove('is-error'); input.removeAttribute('aria-invalid'); });
    form.querySelectorAll('[data-field-error]').forEach(error => { error.textContent = ''; });
    status.textContent = '';
  }
  function fieldError(name, message) {
    const input = form.elements.namedItem(name);
    const error = form.querySelector('[data-field-error="' + name + '"]');
    input?.classList.add('is-error'); input?.setAttribute('aria-invalid', 'true');
    if (error) error.textContent = message;
  }
  function sanitizeNext(next) {
    if (!next || !next.startsWith('/') || next.startsWith('//') || next.includes('://') || next.includes('\\')) return '/';
    return next;
  }
  function finish(body) {
    Checkers.Auth.save(body);
    const next = sanitizeNext(new URLSearchParams(location.search).get('next'));
    location.href = next.includes('lang=') ? next : next + (next.includes('?') ? '&' : '?') + 'lang=' + Checkers.locale();
  }
  function report(error) {
    const message = Checkers.apiErrorMessage(error);
    const field = { USERNAME_TAKEN: 'username', EMAIL_TAKEN: 'email', INVALID_CREDENTIALS: 'password' }[error.errorCode];
    if (field) { fieldError(field, message); form.elements.namedItem(field)?.focus(); }
    else { status.textContent = message; }
  }
  form.addEventListener('submit', async event => {
    event.preventDefault(); if (busy) return; clearErrors();
    for (const input of form.querySelectorAll('[required]')) {
      if (!input.value.trim()) {
        const key = input.name === 'password' ? 'passwordRequired' : input.name === 'email' ? 'emailRequired' : 'usernameRequired';
        fieldError(input.name, Checkers.t('validation.' + key));
      } else if (mode === 'register') {
        if (input.name === 'username' && (input.value.length < 3 || input.value.length > 50)) fieldError(input.name, Checkers.t('redesign.validation.username'));
        if (input.name === 'password' && input.value.length < 8) fieldError(input.name, Checkers.t('redesign.validation.password'));
        if (input.name === 'email' && input.validity.typeMismatch) fieldError(input.name, Checkers.t('redesign.validation.email'));
      }
    }
    const invalid = form.querySelector('.is-error');
    if (invalid) { invalid.focus(); return; }
    setBusy(true);
    try {
      const values = new FormData(form);
      const data = mode === 'login' ? { usernameOrEmail: values.get('usernameOrEmail'), password: values.get('password') }
        : { username: values.get('username'), email: values.get('email'), password: values.get('password') };
      finish(await Checkers.api('/api/auth/' + mode, { method: 'POST', body: JSON.stringify(data) }));
    } catch (error) { report(error); }
    finally { setBusy(false); }
  });
  form.querySelector('[data-guest-login]')?.addEventListener('click', async () => {
    if (busy) return; clearErrors(); setBusy(true);
    try { finish(await Checkers.api('/api/auth/guest', { method: 'POST' })); }
    catch (error) { report(error); }
    finally { setBusy(false); }
  });
});
