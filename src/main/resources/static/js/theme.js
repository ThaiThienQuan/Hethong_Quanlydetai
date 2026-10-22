/* theme.js — chống FOUC cho UniResearch
   Nạp đồng bộ trong <head> TRƯỚC CSS. Lưu lựa chọn theo người dùng vào localStorage.
*/
(function () {
  'use strict';
  var KEY = 'uniresearch-theme';
  var root = document.documentElement;
  var mql = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  function readPref() {
    try { return localStorage.getItem(KEY) || 'system'; } catch (_) { return 'system'; }
  }

  /* Đồng bộ icon nút đổi theme: sun khi sáng, moon khi tối
     (áp dụng cho nút trên topbar và nút ở trang đăng nhập) */
  function syncThemeIcons(isDark) {
    var triggers = document.querySelectorAll('.theme-trigger, .auth-theme-toggle');
    triggers.forEach(function (trigger) {
      var sun = trigger.querySelector('.ico-sun');
      var moon = trigger.querySelector('.ico-moon');
      if (sun) sun.toggleAttribute('hidden', isDark);
      if (moon) moon.toggleAttribute('hidden', !isDark);
    });
  }

  function apply(pref) {
    var t = (pref === 'system' || !pref)
      ? (mql && mql.matches ? 'dark' : 'light')
      : pref;
    root.setAttribute('data-theme', t);
    root.setAttribute('data-theme-pref', pref || 'system');
    syncThemeIcons(t === 'dark');
    syncThemeColor(t);
  }

  /* Đọc màu thanh trình duyệt từ token --theme-color trong theme.css (không hardcode ở JS) */
  function syncThemeColor(theme) {
    var meta = document.querySelector('meta[name="theme-color"]');
    if (!meta) return;
    var color = '';
    try {
      color = window.getComputedStyle(root).getPropertyValue('--theme-color').trim();
    } catch (_) { color = ''; }
    if (color) meta.setAttribute('content', color);
    else if (meta.getAttribute('content')) meta.setAttribute('content', '');
  }

  apply(readPref());

  /* Chắc chắn theme-color được cập nhật sau khi CSS đã nạp xong */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      syncThemeIcons(root.getAttribute('data-theme') === 'dark');
      syncThemeColor(root.getAttribute('data-theme'));
    });
  }
  window.addEventListener('load', function () {
    syncThemeIcons(root.getAttribute('data-theme') === 'dark');
    syncThemeColor(root.getAttribute('data-theme'));
  });

  if (mql && mql.addEventListener) {
    mql.addEventListener('change', function () {
      if (readPref() === 'system') apply('system');
    });
  }

  window.setTheme = function (pref) {
    try { localStorage.setItem(KEY, pref); } catch (_) {}
    apply(pref);
  };

  window.cycleTheme = function () {
    var pref = readPref();
    var next = pref === 'light' ? 'dark' : pref === 'dark' ? 'system' : 'light';
    window.setTheme(next);
    return next;
  };
})();
