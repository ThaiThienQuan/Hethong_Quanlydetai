# UniResearch — Hướng dẫn giao diện (UI Guide)

> Chủ đề **Aurora Academy**: học thuật, đáng tin cậy, hiện đại, có chút tương lai nhưng không hào nhoáng.
> Toàn bộ giao diện dùng **CSS thuần + JavaScript thuần + SVG nội tuyến**. Không Bootstrap, không Tailwind, không jQuery, không icon font, không CDN.

---

## 1. Triết lý thiết kế

| Nguyên tắc | Thể hiện |
|---|---|
| Học thuật, đáng tin cậy | Nền sáng trung tính `#F5F7FF`, chữ tương phản cao, bo góc vừa phải, không hiệu ứng màu rực rỡ |
| Tinh tế, tương lai | Gradient chàm → xanh lơ (`--grad-brand`), quầng sáng mờ trong sidebar, card có viền + bóng đổ rất nhẹ |
| Nhất quán | **Mọi màu đều đi qua design token** trong `theme.css` — đổi theme chỉ cần đổi biến |
| Tôn trọng người dùng | Dark mode theo hệ thống, không FOUC, focus ring rõ, `skip-link`, `aria-*` đầy đủ, tôn trọng `prefers-reduced-motion` |

---

## 2. Cấu trúc tệp

```
src/main/resources/
├─ static/
│  ├─ css/
│  │  ├─ theme.css        ← TOÀN BỘ design token (sáng + tối)
│  │  ├─ layout.css       ← Khung app: grid, sidebar, topbar, vùng nội dung
│  │  ├─ components.css   ← Thư viện thành phần dùng chung
│  │  └─ pages.css        ← Riêng từng nhóm trang (hero, auth, style guide)
│  └─ js/
│     ├─ theme.js         ← Nạp đồng bộ: set theme trước khi vẽ (chống FOUC)
│     └─ ui.js            ← Hành vi UI (sidebar, menu, modal, bảng→thẻ…)
└─ templates/
   ├─ fragments/
   │  ├─ layout.html      ← Khung duy nhất: head(pageTitle) + nav
   │  ├─ nav.html         ← Fragment ủy quyền → layout::nav
   │  ├─ icons.html       ← Sprite SVG
   │  └─ error.html       ← Fragment errorPage(...)
   ├─ ui/style-guide.html ← Trang Style Guide (chỉ profile "demo", ADMIN)
   └─ …                  ← 50 template nghiệp vụ
```

> `style.css` cũ đã được **xoá** — không template nào tham chiếu tới nữa.

---

## 3. Design token

Token nằm trong `:root` và `:root[data-theme="dark"]` của `theme.css`. **Đây là nơi duy nhất được phép chứa mã màu cứng.**

### 3.1. Bảng màu

| Nhóm | Token | Sáng | Tối |
|---|---|---|---|
| Nền | `--bg` | `#F5F7FF` | `#0B1020` |
| | `--surface` | `#FFFFFF` | `#121936` |
| | `--surface-2` | `#EEF1FB` | `#1A2347` |
| | `--surface-3` | `#E3E8F8` | `#232E58` |
| | `--border` | `#D9DEF0` | `#2A3560` |
| | `--border-strong` | `#B7BFDC` | `#3D4A7A` |
| Chữ | `--text` | `#151A33` | `#E8ECFF` |
| | `--text-strong` | `#0A0F23` | `#FFFFFF` |
| | `--text-muted` | `#5A6285` | `#A3ACD1` |
| Chàm (chính) | `--primary` | `#4F46E5` | `#8B93FF` |
| | `--primary-hover` | `#4338CA` | `#A5ABFF` |
| | `--primary-soft` | `#EAEAFE` | `#232C5E` |
| | `--on-primary` | `#FFFFFF` | `#0B1020` |
| Cyan (phụ) | `--accent` | `#0E7490` | `#22D3EE` |
| | `--accent-soft` | `#CFFAFE` | `#0F3D4A` |
| | `--on-accent` | `#A5F3FC` | `#A5F3FC` |
| Amber (nhấn) | `--highlight` | `#B45309` | `#FBBF24` |
| | `--highlight-soft` | `#FEF3C7` | `#422F0A` |
| Thành công | `--success` | `#15803D` | `#4ADE80` |
| | `--success-bg` / `--success-border` | `#DCFCE7` / `#86EFAC` | `#06261B` / `#16603F` |
| Cảnh báo | `--warning` | `#B45309` | `#FBBF24` |
| | `--warning-bg` / `--warning-border` | `#FEF3C7` / `#FCD34D` | `#2D1D04` / `#7C5410` |
| Nguy hiểm | `--danger` | `#B91C1C` | `#F87171` |
| | `--danger-bg` / `--danger-border` | `#FEE2E2` / `#FCA5A5` | `#2E0F12` / `#7C2C2F` |
| Thông tin | `--info` | `#1D4ED8` | `#60A5FA` |
| | `--info-bg` / `--info-border` | `#DBEAFE` / `#93C5FD` | `#0C1F3D` / `#1F4A8A` |
| Trung tính | `--muted-bg` / `--muted-border` | `#EEF1FB` / `#C7CEE4` | `#1A2347` / `#2A3560` |
| Thanh trình duyệt | `--theme-color` | `#F5F7FF` | `#0B1020` |

### 3.2. Gradient

| Token | Giá trị |
|---|---|
| `--grad-brand` | `linear-gradient(135deg, #4F46E5 0%, #6366F1 45%, #06B6D4 100%)` |
| `--grad-aurora` | `linear-gradient(135deg, #6366F1 0%, #8B5CF6 50%, #06B6D4 100%)` (tối: `#A78BFA` / `#22D3EE`) |
| `--grad-soft` | nền chàm→cyan rất nhạt |

### 3.3. Hình dạng & bóng

- Bo góc: `--radius-xs 6px` · `--radius-sm 8px` · `--radius-md 12px` · `--radius-lg 16px` · `--radius-xl 22px` · `--radius-pill 999px`
- Bóng: `--shadow-1` (thẻ) · `--shadow-2` (hover) · `--shadow-3` (modal) · `--shadow-glow` (nút chính)

### 3.4. Kích thước & khoảng cách

- `--topbar-height 64px` · `--sidebar-width 260px` · `--sidebar-width-collapsed 76px` · `--container-max 1440px`
- Thang khoảng cách bội 4: `--space-1` … `--space-12` (4 → 48px)

### 3.5. Font

- `--font-sans`: Inter → Be Vietnam Pro → system-ui → …
- `--font-mono`: JetBrains Mono → ui-monospace → …

> Font tải qua `@font-face` cục bộ / hệ thống — không dùng CDN.

---

## 4. Chủ đề sáng / tối

### 4.1. Cơ chế

1. `theme.js` được nạp **đồng bộ** (không `defer`) ngay trong `<head>`, trước khi trình duyệt vẽ khung đầu tiên.
2. Script đọc `localStorage['uniresearch-theme']` (`light` | `dark` | `system`, mặc định `system`).
3. Nếu `system`, dùng `matchMedia('(prefers-color-scheme: dark)')` để chọn.
4. Đặt `document.documentElement.dataset.theme = 'light'|'dark'` và `dataset.themePref = 'light'|'dark'|'system'`.
5. `theme.css` khai báo hai khối `:root` và `:root[data-theme="dark"]` → đổi theme = đổi biến, **không reload trang, không FOUC**.
6. Màu thanh trình duyệt cũng lấy từ token `--theme-color` trong `theme.css`; `theme.js` đọc biến này rồi ghi vào `<meta name="theme-color">` → **không có mã màu cứng nào trong HTML/JS**.

### 4.2. Đổi theme từ giao diện

Nút trên topbar (`[data-theme-menu-toggle]`) mở menu 3 lựa chọn (`[data-theme-set]`), `ui.js` gọi `window.setTheme(pref)`.

### 4.3. Thêm theme mới (ví dụ "high contrast")

```css
:root[data-theme="contrast"] { --bg: #000; --surface: #111; --text: #fff; /* … */ }
```

```js
// theme.js – thêm vào hàm resolve()
if (pref === 'contrast') return 'contrast';
```

Chỉ cần khai báo token, **không sửa component nào**.

---

## 5. Bố cục (layout)

### 5.1. Grid ứng dụng

```css
body.app-page {
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  grid-template-rows: var(--topbar-height) minmax(0, 1fr);
  grid-template-areas: "sidebar topbar" "sidebar main";
}
.app-frame { display: contents; }   /* fragment nav tham gia trực tiếp vào grid */
```

> **Quan trọng:** dùng `minmax(0, 1fr)` chứ không phải `1fr`. Track `1fr` có min-width `auto`, nên nội dung rộng (bảng, topbar) sẽ **nới rộng cột** và làm trang tràn ngang trên điện thoại. Kèm `min-width: 0` cho `.app-topbar` và `main`.

### 5.2. Vùng

| Vùng | Selector | Ghi chú |
|---|---|---|
| Sidebar | `.app-sidebar` | Gradient chàm đậm, nhóm menu `.sidebar-group`, mục `.sidebar-link` |
| Topbar | `.app-topbar` | Sticky, nền blur, breadcrumb `#current-page-title` (JS điền tự) |
| Nội dung | `.app-main` | `max-width: var(--container-max)`, tự căn giữa |
| Container | `.box` / `.card` | `.box` giữ tương thích template cũ; `.card` là thẻ mới |

### 5.3. Hành vi sidebar

| Bề rộng | Hành vi |
|---|---|
| `< 1024px` | Sidebar là **ngăn kéo** (off-canvas). Nút ☰ mở, overlay `.sidebar-backdrop`, bấm ra ngoài / Esc đóng. |
| `≥ 1024px` | Sidebar cố định, có nút **thu gọn** (`.is-collapsed` trên `<body>`), trạng thái lưu ở `localStorage['uniresearch-sidebar-collapsed']`. |

---

## 6. Thành phần (component)

### 6.1. Nút

```html
<!-- Nút chính: gradient thương hiệu + quầng sáng + hiệu ứng ánh sáng quét khi hover -->
<button class="btn btn-primary">
  <svg class="ui-icon" aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>
  Nút chính
</button>
<a class="btn btn-secondary" href="#">Nút phụ</a>
<button class="btn">Mặc định</button>
<button class="btn btn-outline">Nút viền</button>
<button class="btn btn-ghost">Phụ mờ</button>
<button class="btn btn-success">Thành công</button>
<button class="btn btn-warning">Cảnh báo</button>
<button class="btn btn-danger">Nguy hiểm</button>
<button class="icon-btn" aria-label="Sửa"><svg class="ui-icon">…</svg></button>
```

**Biến thể "mềm"** — dùng cho các nút nhỏ trong ô thao tác của bảng. Nhẹ mắt hơn
hẳn so với nút đặc, nhưng vẫn phân biệt được mức độ nguy hiểm:

```html
<button class="btn btn-soft-primary btn-sm">Sửa</button>
<button class="btn btn-soft-success btn-sm">Duyệt</button>
<button class="btn btn-soft-warning btn-sm">Khóa</button>
<button class="btn btn-soft-danger btn-sm">Xóa</button>
```

- Chiều cao tối thiểu **44px** (`.btn-sm` 34px, `.btn-lg` 48px) → đạt chuẩn chạm 44px.
- `white-space: nowrap` để không vỡ chữ.
- Tương thích ngược: `.btn.primary` / `.btn.secondary` cũng hoạt động.
- **Quy ước chọn màu theo ý nghĩa hành động** (không chọn theo thứ tự hiển thị):
  - Xác nhận / tạo mới → `btn-primary`
  - Hành động phụ (lọc, đặt lại, gán vai trò) → `btn-secondary`
  - Phê duyệt / mở / gửi lại → `btn-soft-success`
  - Khoá / tạm dừng / đóng đợt → `btn-soft-warning`
  - Xoá / từ chối / huỷ / gỡ → `btn-soft-danger` (trong bảng) hoặc `btn-danger` (nút độc lập)
- Nút chưa gán class vẫn được style sẵn qua `:where(button:not([class]))` → không bao giờ
  rơi về nút xám mặc định của trình duyệt. Dùng `:where()` để độ ưu tiên bằng 0 nên
  các rule chuyên biệt (`.theme-menu-list button`, `.user-menu-list button`) vẫn thắng.

### 6.2. Biểu mẫu

```html
<div class="form-row">
  <label for="x">Nhãn <span class="required">*</span></label>
  <input id="x" type="text" placeholder="Gợi ý">
  <span class="form-hint">Gợi ý bổ sung</span>
  <div class="field-error">
    <svg class="ui-icon">…</svg> Thông báo lỗi cụ thể.
  </div>
</div>
```

- Nhãn luôn hiển thị — **không** dùng placeholder thay nhãn.
- Font ô nhập **16px** để iOS không tự phóng to.
- Focus ring: `box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary) 16%, transparent)`.
- `.form-grid` 2 cột từ 768px; `.form-actions` gồm nút + đường kẻ trên.

### 6.3. Bảng

```html
<div class="table-scroll" role="region" tabindex="0" aria-label="Bảng dữ liệu">
  <table>
    <thead><tr><th>#</th><th>Tên</th><th>Trạng thái</th></tr></thead>
    <tbody>
      <tr><td>1</td><td>…</td><td><span class="badge st-OPEN">OPEN</span></td></tr>
    </tbody>
  </table>
</div>
```

- `.table-scroll` cuộn ngang an toàn, có `tabindex` để bàn phím cuộn được.
- **≤ 700px:** `ui.js` gắn `data-label` cho từng `<td>` từ `<th>` tương ứng, CSS biến bảng thành **thẻ xếp dọc** có nhãn cột.
- Bảng rộng (>6 cột): thêm class `.table-wide` (min-width 1000px) để cuộn ngang thay vì bóp cột.

### 6.4. Huy hiệu trạng thái

| Class | Dùng cho |
|---|---|
| `.st-DRAFT`, `.st-INACTIVE`, `.st-sys` | Trung tính (xám) |
| `.st-OPEN`, `.st-PUBLISHED`, `.st-APPROVED`, `.st-ACTIVE`, `.st-SCORED` | Thành công (xanh) |
| `.st-CLOSED`, `.st-REJECTED`, `.st-OVERDUE` | Nguy hiểm (đỏ) |
| `.st-PENDING` | Cảnh báo (amber) |
| `.st-RUNNING` | Thông tin (xanh dương) |
| `.st-HIGHLIGHT` | Nổi bật (highlight) |

### 6.5. Flash message

`.ok` / `.error` / `.flash` — có icon mặc định (`✓` / `⚠`), tự ẩn sau 6 giây và có nút đóng (do `ui.js` gắn). Dùng `data-auto-hide="0"` để không tự ẩn.

### 6.6. Modal xác nhận

`ui.js` chặn submit của form có `onsubmit="return confirm(...)"`, hiển thị modal `role="alertdialog"` (bắt tiêu điểm, đóng bằng Esc, khoá cuộn nền) thay cho `confirm()` gốc.

Thêm thông điệp riêng:

```html
<form … data-confirm-title="Xoá đợt" data-confirm-message="Bạn có chắc?">
```

### 6.7. Icon

- Mọi icon là **SVG nội tuyến** với `class="ui-icon"` → `display:inline-block; flex-shrink:0; vertical-align:middle`.
- Icon dùng `stroke="currentColor"` nên tự đổi màu theo ngữ cảnh.
- Sprite dùng chung nằm ở `fragments/icons.html` (`#i-book`, `#i-users`, …).

---

## 7. Responsive

| Mốc | Hành vi |
|---|---|
| **≤ 480px** (375–412px) | Sidebar thành ngăn kéo; `.form-grid` 1 cột; bảng → thẻ; `.roadmap` 1 cột; `.quick-grid` 1 cột; modal thành **bottom sheet** (bo góc dưới, `env(safe-area-inset-bottom)`) |
| **≤ 600px** | `.toolbar` xuống dòng, mỗi trường 100%; `.filter-bar` 1 cột; `.workflow-steps` 2 cột |
| **≤ 700px** | Bảng → thẻ; `.table-scroll table { min-width: 0 }` |
| **≤ 768px** (tablet) | `.form-grid` 2 cột; `.detail-list` 1 cột → 2 cột từ 640px |
| **≤ 900px** | Hero ẩn phần minh họa; trang đăng nhập chuyển 1 cột, ẩn panel showcase |
| **≤ 1023px** | Sidebar off-canvas; grid 1 cột; `.dashboard-panels` 1 cột |
| **≥ 1024px** (laptop) | Sidebar cố định + thu gọn được; `.split-layout` 2 cột; `.roadmap` 6 cột; `.workflow-steps` 4 cột |
| **≥ 1440px** | Nội dung giới hạn `--container-max: 1440px`, căn giữa |

**Nguyên tắc chống tràn ngang:** mọi track grid dùng `minmax(0, 1fr)`; mọi vùng con có `min-width: 0`; bảng luôn trong `.table-scroll`; văn bản dài dùng `overflow-wrap: anywhere`.

---

## 8. Khả năng tiếp cận (a11y)

- `skip-link` → `#main-content` ở đầu mỗi trang.
- Tiêu đề `<h1>`/`<h2>` theo thứ bậc; `main` có `id="main-content"`.
- Nhãn form luôn gắn với control (`for`/`id`); lỗi dùng `.field-error` kèm icon.
- Focus ring nhìn thấy trên mọi control; `:focus-visible` cho phần tử điều hướng.
- Menu dùng `aria-expanded`, `aria-haspopup`, `role="menu"/"menuitem"/"menuitemradio"`; mục sidebar đang xem có `aria-current="page"` (JS tự gắn).
- Modal: `role="alertdialog"`, `aria-modal="true"`, đóng bằng Esc, trả focus về nút đã bấm.
- Bảng cuộn ngang có `role="region"` + `tabindex="0"` + `aria-label`.
- Flash message có `role="status"`.
- `prefers-reduced-motion: reduce` → tắt animation/transition.
- `forced-colors: active` → ép viền rõ cho khung, thẻ, badge, control.
- Tương phản chữ thường ≥ 4.5:1 ở **cả hai** theme.

---

## 9. Hiệu năng

- Không thư viện JS/CSS ngoài; tổng CSS ~ 4 file, JS ~ 2 file.
- `theme.js` đồng bộ, `ui.js` đặt sau (defer) — không chặn render.
- Icon SVG nội tuyến ⇒ không request mạng, không FOUT.
- `prefers-reduced-motion` tôn trọng.
- `will-change`/`filter` chỉ dùng khi cần (nút hover, topbar blur).

---

## 10. Trang Style Guide

- URL: `GET /ui/style-guide`
- Bật bằng profile `demo`: `--spring.profiles.active=demo`
- Chỉ `ADMIN` được truy cập (`@Profile("demo")` + `@PreAuthorize("hasRole('ADMIN')")` + `requestMatchers("/ui/**")`)
- Trang này **không thuộc nghiệp vụ**, không đụng entity/service/database.
- Trang tự hiển thị giá trị thật của từng token (đọc từ `theme.css` bằng `getComputedStyle`), nên **không có mã màu cứng trong HTML** — đổi theme là số hiển thị tự đổi theo.

Chạy thử:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

---

## 11. Kiểm tra trước khi commit

```bash
# 1. Test tự động (phải xanh 100%)
./mvnw test

# 2. Quét class thiếu / inline style / màu hardcode
#    Kết quả mong đợi: 0 class thiếu, 0 inline style, 0 hex/rgb ngoài theme.css
py -3 tools/audit_ui.py
py -3 tools/tokenize_css.py

# 3. Kiểm tra HTTP + <title> cho từng vai trò
.\tools\check_pages.ps1 truongkhoa 123456
.\tools\check_pages.ps1 admin 123456
.\tools\check_pages.ps1 gv01 123456
.\tools\check_pages.ps1 sv01 123456

# 4. Chụp màn hình 2 theme × 4 bề rộng + kiểm tra tràn ngang
#    Tham số: <outDir> [tài khoản chính] [mật khẩu] [tài khoản admin] [mật khẩu admin]
node tools/shots/shoot.js docs/ui/after truongkhoa 123456 admin 123456
```

> **Ghi chú:** `/roles`, `/admin/users` và `/ui/style-guide` chỉ `ADMIN` mới vào được
> (`USER_MANAGE` / `ROLE_MANAGE` trong `SecurityConfig`, `hasRole('ADMIN')` cho Style Guide).
> Script tự mở browser context thứ hai để đăng nhập `admin` và chụp 3 màn hình này —
> **không** chỉnh phân quyền cho tài khoản trưởng khoa.

> **Không được** thay đổi entity, repository, service, schema DB, tên tham số form, `th:field` / `th:object` / `th:action`, token CSRF, URL hay cấu hình phân quyền hiện có.
