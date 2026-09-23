window.Checkers = window.Checkers || {};

Checkers.t = function (key, ...args) {
  const dict = window.i18n || {};
  let text = dict[key] != null ? dict[key] : key;
  args.forEach((arg, i) => {
    text = String(text).replace('{' + i + '}', arg == null ? '' : arg);
  });
  return text;
};

Checkers.locale = function () {
  return (window.i18n && window.i18n._locale) || 'en';
};

Checkers.setLocaleMessages = function (messages) {
  window.i18n = messages || {};
  document.documentElement.lang = Checkers.locale();
};
