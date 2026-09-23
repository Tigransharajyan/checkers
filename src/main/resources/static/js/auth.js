document.addEventListener('DOMContentLoaded', () => {
  const form = document.querySelector('[data-auth-form]');
  if (!form) return;

  const mode = form.getAttribute('data-auth-form');

  function clearErrors() {
    form.querySelectorAll('.is-error').forEach((el) => el.classList.remove('is-error'));
    form.querySelectorAll('[data-field-error]').forEach((el) => { el.textContent = ''; });
  }

  function showFieldError(name, message) {
    const input = form.querySelector('[name="' + name + '"]');
    const err = form.querySelector('[data-field-error="' + name + '"]');
    if (input) input.classList.add('is-error');
    if (err) err.textContent = message || Checkers.t('common.error');
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();
    const fd = new FormData(form);

    // client-side required checks for red borders
    let valid = true;
    form.querySelectorAll('[required]').forEach((input) => {
      if (!String(input.value || '').trim()) {
        showFieldError(input.name, Checkers.t('common.error'));
        valid = false;
      }
    });
    if (!valid) return;

    try {
      let body;
      if (mode === 'login') {
        body = await Checkers.api('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({
            usernameOrEmail: fd.get('usernameOrEmail'),
            password: fd.get('password')
          })
        });
      } else {
        body = await Checkers.api('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({
            username: fd.get('username'),
            email: fd.get('email'),
            password: fd.get('password')
          })
        });
      }
      Checkers.Auth.save(body);
      const params = new URLSearchParams(location.search);
      const next = params.get('next') || '/';
      location.href = next.includes('lang=') ? next : (next + (next.includes('?') ? '&' : '?') + 'lang=' + Checkers.locale());
    } catch (err) {
      const msg = Checkers.apiErrorMessage(err);
      if (mode === 'login') {
        showFieldError('password', msg);
      } else {
        showFieldError('username', msg);
      }
      Checkers.toast(msg, 'error');
    }
  });

  document.querySelector('[data-guest-login]')?.addEventListener('click', async () => {
    try {
      const body = await Checkers.api('/api/auth/guest', { method: 'POST' });
      Checkers.Auth.save(body);
      location.href = '/?lang=' + Checkers.locale();
    } catch (err) {
      Checkers.toast(Checkers.apiErrorMessage(err), 'error');
    }
  });
});
