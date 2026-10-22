/* UniResearch – ui.js (thuần JS, không phụ thuộc thư viện)
   - Ngăn kéo sidebar (điện thoại) + thu gọn sidebar (desktop), ghi nhớ lựa chọn
   - Menu người dùng & menu chuyển theme (Sáng / Tối / Theo hệ thống)
   - Tự ẩn & đóng được flash message
   - Hộp xác nhận thay thế confirm() — bắt tiêu điểm, đóng bằng Esc
   - Hiện/ẩn mật khẩu
   - Bảng dạng thẻ trên điện thoại (gắn data-label tự động)
   - Gắn aria-current cho mục sidebar đang xem
*/
(function () {
  'use strict';

  var doc = document;
  var root = doc.documentElement;
  var body = doc.body;

  /* ------------------------------------------------------------------
     1. SIDEBAR: ngăn kéo (mobile) + thu gọn (desktop)
  ------------------------------------------------------------------ */
  var sidebar = doc.getElementById('app-sidebar');
  var toggleBtn = doc.querySelector('[data-sidebar-toggle]');
  var collapseBtn = doc.querySelector('[data-sidebar-collapse]');
  var COLLAPSE_KEY = 'uniresearch-sidebar-collapsed';

  function closeDrawer() {
    body.classList.remove('sidebar-open');
    if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'false');
  }

  if (toggleBtn) {
    toggleBtn.addEventListener('click', function () {
      var open = body.classList.toggle('sidebar-open');
      toggleBtn.setAttribute('aria-expanded', String(open));
      if (open && sidebar) {
        var first = sidebar.querySelector('.sidebar-link');
        if (first) first.focus();
      }
    });
  }

  doc.querySelectorAll('[data-sidebar-close]').forEach(function (el) {
    el.addEventListener('click', closeDrawer);
  });

  // Đóng ngăn kéo khi bấm link menu (chỉ trên mobile)
  doc.querySelectorAll('.sidebar-link').forEach(function (link) {
    link.addEventListener('click', function () {
      if (window.matchMedia('(max-width: 1023px)').matches) closeDrawer();
    });
  });

  // Thu gọn sidebar (desktop) — ghi nhớ lựa chọn
  function applyCollapsed(collapsed) {
    body.classList.toggle('is-collapsed', collapsed);
    if (collapseBtn) collapseBtn.setAttribute('aria-expanded', String(!collapsed));
    try { localStorage.setItem(COLLAPSE_KEY, collapsed ? '1' : '0'); } catch (_) {}
  }
  if (collapseBtn) {
    collapseBtn.addEventListener('click', function () {
      applyCollapsed(!body.classList.contains('is-collapsed'));
    });
  }
  try {
    if (localStorage.getItem(COLLAPSE_KEY) === '1' && window.matchMedia('(min-width: 1024px)').matches) {
      applyCollapsed(true);
    }
  } catch (_) {}

  /* ------------------------------------------------------------------
     2. MENU THU GỌN (user menu / theme menu)
  ------------------------------------------------------------------ */
  function setupDropdown(toggleSel, menuSel) {
    var t = doc.querySelector(toggleSel);
    var m = doc.querySelector(menuSel);
    if (!t || !m) return;
    t.addEventListener('click', function (e) {
      e.stopPropagation();
      var open = m.classList.toggle('open');
      t.setAttribute('aria-expanded', String(open));
    });
    m.addEventListener('click', function (e) { e.stopPropagation(); });
    return { t: t, m: m };
  }
  var userMenu = setupDropdown('[data-user-menu-toggle]', '[data-user-menu]');
  var themeMenu = setupDropdown('[data-theme-menu-toggle]', '[data-theme-menu]');

  doc.addEventListener('click', function () {
    [userMenu, themeMenu].forEach(function (mm) {
      if (!mm) return;
      mm.m.classList.remove('open');
      mm.t.setAttribute('aria-expanded', 'false');
    });
  });

  /* ------------------------------------------------------------------
     3. THEME: Sáng / Tối / Theo hệ thống
  ------------------------------------------------------------------ */
  function currentPref() {
    return root.getAttribute('data-theme-pref') || 'system';
  }
  function syncThemeUI() {
    var pref = currentPref();
    doc.querySelectorAll('[data-theme-set]').forEach(function (b) {
      b.setAttribute('aria-checked', String(b.getAttribute('data-theme-set') === pref));
    });
    /* Icon sun/moon do theme.js quản lý (nằm trong <head>, chạy trước) */
  }
  doc.querySelectorAll('[data-theme-set]').forEach(function (b) {
    b.addEventListener('click', function () {
      var pref = b.getAttribute('data-theme-set');
      if (window.setTheme) window.setTheme(pref);
      syncThemeUI();
      if (themeMenu) {
        themeMenu.m.classList.remove('open');
        themeMenu.t.setAttribute('aria-expanded', 'false');
      }
    });
  });
  syncThemeUI();

  /* ------------------------------------------------------------------
     4. ĐƯỜNG DẪN → TIÊU ĐỀ TOPBAR + aria-current
  ------------------------------------------------------------------ */
  var path = location.pathname.replace(/\/+$/, '') || '/';
  var links = Array.prototype.slice.call(doc.querySelectorAll('.sidebar-link[href]'));
  var active = null, best = -1;
  links.forEach(function (link) {
    var url;
    try { url = new URL(link.href, location.href).pathname.replace(/\/+$/, '') || '/'; }
    catch (_) { return; }
    if ((path === url || (url !== '/' && path.indexOf(url + '/') === 0)) && url.length > best) {
      active = link; best = url.length;
    }
  });
  if (active) {
    active.setAttribute('aria-current', 'page');
    active.classList.add('is-active');
  }

  var headline = doc.querySelector('[data-page-title], .page-title-block h1, .box > h2:first-child, .hero h1');
  var crumb = doc.getElementById('current-page-crumb');
  var title = doc.getElementById('current-page-title');
  var pageName = headline ? headline.textContent.trim().replace(/\s+/g, ' ') : 'Tổng quan';
  if (path === '/') pageName = 'Tổng quan';
  if (pageName.length > 62) pageName = pageName.slice(0, 59) + '…';
  if (crumb) crumb.textContent = pageName;
  if (title) title.textContent = pageName;

  /* ------------------------------------------------------------------
     5. FLASH: tự ẩn + nút đóng
  ------------------------------------------------------------------ */
  doc.querySelectorAll('.flash').forEach(function (f) {
    if (!f.getAttribute('role')) f.setAttribute('role', 'status');
    var close = f.querySelector('.close');
    if (!close) {
      close = doc.createElement('button');
      close.type = 'button';
      close.className = 'close';
      close.setAttribute('aria-label', 'Đóng thông báo');
      close.innerHTML = '&times;';
      f.appendChild(close);
    }
    close.addEventListener('click', function () { f.remove(); });
    var ms = parseInt(f.getAttribute('data-auto-hide') || '6000', 10);
    if (ms > 0) setTimeout(function () { f.remove(); }, ms);
  });

  /* ------------------------------------------------------------------
     6. HỘP XÁC NHẬN (thay confirm())
     - Bắt tiêu điểm, đóng bằng Esc, khoá cuộn nền
  ------------------------------------------------------------------ */
  var confirmBox = null;
  var lastFocus = null;

  function buildConfirmBox() {
    var back = doc.createElement('div');
    back.className = 'modal-backdrop';
    back.setAttribute('role', 'presentation');
    back.innerHTML =
      '<div class="modal" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-text">' +
        '<h2 id="confirm-title">Xác nhận</h2>' +
        '<p id="confirm-text"></p>' +
        '<div class="modal-actions">' +
          '<button type="button" class="btn" data-confirm-cancel>Hủy bỏ</button>' +
          '<button type="button" class="btn btn-danger" data-confirm-ok>Đồng ý</button>' +
        '</div>' +
      '</div>';
    doc.body.appendChild(back);
    back.addEventListener('click', function (e) {
      if (e.target === back) closeConfirm(false);
    });
    back.querySelector('[data-confirm-cancel]').addEventListener('click', function () { closeConfirm(false); });
    back.querySelector('[data-confirm-ok]').addEventListener('click', function () { closeConfirm(true); });
    return back;
  }

  function openConfirm(message, titleText) {
    if (!confirmBox) confirmBox = buildConfirmBox();
    confirmBox.querySelector('#confirm-text').textContent = message || 'Bạn có chắc chắn không?';
    if (titleText) confirmBox.querySelector('#confirm-title').textContent = titleText;
    lastFocus = doc.activeElement;
    confirmBox.classList.add('open');
    var ok = confirmBox.querySelector('[data-confirm-ok]');
    if (ok) ok.focus();
  }

  function closeConfirm(result) {
    if (!confirmBox) return;
    confirmBox.classList.remove('open');
    if (lastFocus && lastFocus.focus) lastFocus.focus();
    lastFocus = null;
    if (result) {
      var form = pendingForm;
      pendingForm = null;
      if (form) form.submit();
    }
  }

  var pendingForm = null;
  doc.addEventListener('submit', function (e) {
    var form = e.target;
    if (!form.getAttribute('onsubmit')) return;
    var msg = form.getAttribute('data-confirm-message') ||
              form.getAttribute('data-confirm') ||
              (form.getAttribute('onsubmit').match(/confirm\((['"])(.*?)\1/) || [])[2] ||
              'Bạn có chắc chắn muốn thực hiện thao tác này?';
    e.preventDefault();
    pendingForm = form;
    openConfirm(msg, form.getAttribute('data-confirm-title') || 'Xác nhận thao tác');
  });

  doc.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      if (confirmBox && confirmBox.classList.contains('open')) { closeConfirm(false); return; }
      // Modal dùng chung (quên mật khẩu) - đóng trước, rồi mới thu gọn menu.
      if (openModal) { closeOpenModal(); return; }
      closeDrawer();
      if (userMenu) { userMenu.m.classList.remove('open'); userMenu.t.setAttribute('aria-expanded', 'false'); }
      if (themeMenu) { themeMenu.m.classList.remove('open'); themeMenu.t.setAttribute('aria-expanded', 'false'); }
    }
  });

  /* ------------------------------------------------------------------
     7. HIỆN / ẨN MẬT KHẨU
  ------------------------------------------------------------------ */
  doc.querySelectorAll('[data-toggle-password]').forEach(function (btn) {
    var input = doc.getElementById(btn.getAttribute('data-toggle-password'));
    if (!input) return;
    btn.addEventListener('click', function () {
      var show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
      btn.setAttribute('aria-pressed', String(show));
      var icon = btn.querySelector('svg');
      if (icon) {
        var sun = icon.querySelector('.ico-eye');
        var moon = icon.querySelector('.ico-eye-off');
        if (sun) sun.toggleAttribute('hidden', show);
        if (moon) moon.toggleAttribute('hidden', !show);
      }
      input.focus();
    });
  });

  /* ------------------------------------------------------------------
     8. BẢNG DẠNG THẺ TRÊN ĐIỆN THOẠI
     Gắn data-label từ <th> để CSS hiển thị nhãn cột trong mỗi thẻ.
  ------------------------------------------------------------------ */
  doc.querySelectorAll('table').forEach(function (table) {
    var headCells = table.querySelectorAll('thead th');
    if (!headCells.length) return;
    table.querySelectorAll('tbody > tr').forEach(function (row) {
      var cells = row.querySelectorAll('td');
      cells.forEach(function (cell, i) {
        var head = headCells[i];
        if (head && !cell.getAttribute('data-label')) {
          cell.setAttribute('data-label', head.textContent.trim());
        }
      });
    });
  });

  /* ------------------------------------------------------------------
     9. MODAL DÙNG CHUNG (quên mật khẩu → liên hệ Admin)
     Mở bằng [data-modal-open="id"], đóng bằng [data-modal-close] hoặc Esc.
  ------------------------------------------------------------------ */
  var openModal = null;   // backdrop đang mở
  var modalLastFocus = null;

  function closeOpenModal() {
    if (!openModal) return;
    // Chỉ bỏ class "open" - .modal-backdrop mặc định đã có visibility:hidden
    // nên vẫn chuyển động mượt và không bị Tab tới.
    openModal.classList.remove('open');
    if (modalLastFocus && modalLastFocus.focus) modalLastFocus.focus();
    modalLastFocus = null;
    openModal = null;
  }

  doc.querySelectorAll('[data-modal-open]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var modal = doc.getElementById(btn.getAttribute('data-modal-open'));
      if (!modal) return;
      closeOpenModal();
      modalLastFocus = btn;
      modal.classList.add('open');
      openModal = modal;
      // Bắt tiêu điểm vào phần tử đầu tiên trong modal (nút đóng) để hỗ trợ bàn phím.
      var focusable = modal.querySelector('a[href], button:not([disabled]), input, [tabindex]:not([tabindex="-1"])');
      if (focusable) focusable.focus();
    });
  });

  doc.querySelectorAll('[data-modal-close]').forEach(function (btn) {
    btn.addEventListener('click', closeOpenModal);
  });

  // Bấm ra ngoài vùng modal (nền tối) thì đóng.
  doc.querySelectorAll('.modal-backdrop').forEach(function (back) {
    back.addEventListener('click', function (e) {
      // Chỉ đóng đúng backdrop đang mở (hộp xác nhận của ui.js dùng chung class này).
      if (e.target === back && back === openModal) closeOpenModal();
    });
  });

  /* ------------------------------------------------------------------
     10. CHỐNG BẤM ĐÚP KHI GỬI FORM
  ------------------------------------------------------------------ */
  doc.querySelectorAll('form').forEach(function (form) {
    form.addEventListener('submit', function () {
      var btn = form.querySelector('button[type="submit"]');
      if (btn) {
        setTimeout(function () {
          btn.disabled = true;
          btn.classList.add('is-loading');
        }, 0);
        // Khôi phục nếu trình duyệt chặn điều hướng (back/forward cache)
        setTimeout(function () {
          btn.disabled = false;
          btn.classList.remove('is-loading');
        }, 8000);
      }
    });
  });
})();
