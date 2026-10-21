/* TOPICHUB UI enhancements (no backend or external dependencies) */
(function () {
  'use strict';
  var root = document.documentElement;
  try { if (localStorage.getItem('topichub-theme') === 'dark') root.setAttribute('data-theme', 'dark'); } catch (_) {}
  var body = document.body;
  var sidebarToggle = document.querySelector('[data-sidebar-toggle]');
  var sidebarCloses = document.querySelectorAll('[data-sidebar-close]');
  function closeSidebar() {
    body.classList.remove('sidebar-open');
    if (sidebarToggle) sidebarToggle.setAttribute('aria-expanded', 'false');
  }
  if (sidebarToggle) sidebarToggle.addEventListener('click', function () {
    var isOpen = body.classList.toggle('sidebar-open');
    sidebarToggle.setAttribute('aria-expanded', String(isOpen));
  });
  sidebarCloses.forEach(function (button) { button.addEventListener('click', closeSidebar); });
  document.addEventListener('keydown', function (event) { if (event.key === 'Escape') closeSidebar(); });
  document.querySelectorAll('.sidebar-link').forEach(function (link) {
    link.addEventListener('click', closeSidebar);
  });
  var themeToggle = document.querySelector('[data-theme-toggle]');
  if (themeToggle) themeToggle.addEventListener('click', function () {
    var theme = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    if (theme === 'light') root.removeAttribute('data-theme');
    else root.setAttribute('data-theme', 'dark');
    try { localStorage.setItem('topichub-theme', theme); } catch (_) {}
  });
  var path = location.pathname.replace(/\/$/, '') || '/';
  var links = Array.from(document.querySelectorAll('.sidebar-link[href]'));
  var active = null, best = -1;
  links.forEach(function (link) {
    var url = new URL(link.href, location.href).pathname.replace(/\/$/, '') || '/';
    if ((path === url || (url !== '/' && path.startsWith(url + '/'))) && url.length > best) {
      active = link; best = url.length;
    }
  });
  if (active) { active.classList.add('active'); active.setAttribute('aria-current', 'page'); }
  var headline = document.querySelector('.box>h2, .workspace-intro h2, .hero h1');
  var crumb = document.getElementById('current-page-crumb');
  var title = document.getElementById('current-page-title');
  var pageName = headline && headline.textContent.trim().replace(/\s+/g, ' ') || 'Tổng quan';
  if (path === '/') pageName = 'Tổng quan';
  if (pageName.length > 62) pageName = pageName.slice(0, 59) + '…';
  if (crumb) crumb.textContent = pageName;
  if (title) title.textContent = pageName;
  /* Search only tables with enough data; dynamically generated control never submits a form. */
  var searchSvg = '<svg class="ui-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></svg>';
  document.querySelectorAll('.table-scroll').forEach(function (scroller) {
    var table = scroller.querySelector('table');
    if (!table) return;
    var rows = Array.from(table.querySelectorAll('tbody > tr'));
    if (rows.length < 5) return;
    var bar = document.createElement('div');
    bar.className = 'table-toolbar';
    var count = document.createElement('span'); count.className = 'table-count';
    var label = document.createElement('label');
    label.className = 'table-search-wrap';
    label.setAttribute('aria-label', 'Tìm kiếm trong bảng');
    label.innerHTML = searchSvg;
    var input = document.createElement('input');
    input.type = 'search'; input.placeholder = 'Tìm nhanh trong bảng…';
    input.setAttribute('aria-label', 'Tìm nhanh trong bảng');
    label.appendChild(input);
    bar.appendChild(count); bar.appendChild(label);
    scroller.parentNode.insertBefore(bar, scroller);
    function update() {
      var q = input.value.toLocaleLowerCase('vi-VN').trim(), visible = 0;
      rows.forEach(function (row) {
        var match = !q || row.textContent.toLocaleLowerCase('vi-VN').includes(q);
        row.hidden = !match;
        if (match) visible++;
      });
      count.textContent = visible + ' / ' + rows.length + ' dòng';
    }
    input.addEventListener('input', update);
    update();
  });
})();
