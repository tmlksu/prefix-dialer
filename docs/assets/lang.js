/* 言語切り替え。?lang=en / localStorage / ブラウザ設定 の順で決める。 */
(function () {
  var KEY = 'prefixdialer-lang';
  function detect() {
    var q = new URLSearchParams(location.search).get('lang');
    if (q === 'ja' || q === 'en') return q;
    try {
      var saved = localStorage.getItem(KEY);
      if (saved === 'ja' || saved === 'en') return saved;
    } catch (e) { /* localStorage が使えない環境では無視 */ }
    return (navigator.language || 'ja').toLowerCase().indexOf('ja') === 0 ? 'ja' : 'en';
  }
  function apply(lang) {
    var el = document.documentElement;
    el.classList.remove('lang-ja', 'lang-en');
    el.classList.add('lang-' + lang);
    el.setAttribute('lang', lang);
    var buttons = document.querySelectorAll('.lang-switch button');
    for (var i = 0; i < buttons.length; i++) {
      buttons[i].setAttribute('aria-pressed', String(buttons[i].dataset.lang === lang));
    }
  }
  window.__setLang = function (lang) {
    try { localStorage.setItem(KEY, lang); } catch (e) { /* 保存できなくても切り替えは行う */ }
    apply(lang);
  };
  apply(detect());
  document.addEventListener('DOMContentLoaded', function () {
    apply(document.documentElement.getAttribute('lang') || 'ja');
    var sw = document.querySelector('.lang-switch');
    if (!sw) return;
    sw.addEventListener('click', function (ev) {
      var b = ev.target.closest('button[data-lang]');
      if (b) window.__setLang(b.dataset.lang);
    });
  });
})();
