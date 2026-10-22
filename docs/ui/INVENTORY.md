# UI Inventory — UniResearch

> Tài liệu kiểm kê toàn bộ màn hình, URL, vai trò, template Thymeleaf và thành phần UI.
> Sinh từ mã nguồn hiện tại (`src/main/java/.../controller`, `src/main/resources/templates`).
> Cập nhật lần cuối: UI-8.

---

## 1. Vai trò & tài khoản demo

| Vai trò (`UserRole`) | Tài khoản demo | Mật khẩu | Phạm vi chính |
|---|---|---|---|
| `ADMIN` | `admin` | `123456` | Quản trị toàn hệ thống: tài khoản, vai trò & phân quyền, bộ môn, hồ sơ GV/SV, thông báo nhà trường |
| `TRUONG_KHOA` | `truongkhoa` | `123456` | Duyệt đề tài, phân công GVHD, phản biện, hội đồng, công bố kết quả, quản lý đợt đăng ký |
| `GIANG_VIEN` | `gv01` | `123456` | Đề xuất đề tài, duyệt đăng ký nhóm, phản biện, chấm điểm hội đồng, xem báo cáo |
| `SINH_VIEN` | `sv01` | `123456` | Nhóm, đăng ký đề tài, nộp báo cáo, xem kết quả |

Quyền chi tiết (`hasAuthority`) được khai báo trong `SecurityConfig` và kiểm tra lại ở từng controller bằng `@PreAuthorize`.

---

## 2. Bản đồ màn hình theo vai trò

### 2.1. Công khai

| Màn hình | URL | Template | Ghi chú |
|---|---|---|---|
| Đăng nhập | `GET /login` | `login.html` | Layout riêng (không sidebar), 2 cột showcase + form, hiện/ẩn mật khẩu |
| Trang lỗi 403 | — | `error/403.html` | Dùng chung `fragments/error.html` |
| Trang lỗi 404 | — | `error/404.html` | Dùng chung `fragments/error.html` |
| Trang lỗi 500 | — | `error/500.html` | Dùng chung `fragments/error.html` |

### 2.2. Mọi vai trò đã đăng nhập

| Màn hình | URL | Template | Thành phần chính |
|---|---|---|---|
| Tổng quan (dashboard) | `GET /` | `home.html` | Hero, quick-grid 6 chức năng, lộ trình 6 bước, link-list, card hỗ trợ |
| Danh sách đề tài công bố | `GET /topics` | `topics/list.html` | `.box` + `.table-scroll` (bảng → thẻ ở ≤700px) |
| Thông báo chính thức | `GET /announcements` | `announcements/list.html` | Danh sách `.article` |
| Hồ sơ cá nhân | `GET /account/profile` | `account/profile.html` | `.detail-list` / bảng 2 cột |
| Đổi mật khẩu | `GET /account/password` · `POST /account/password` | `account/password.html` | Form 3 trường + `.field-error` |

### 2.3. Trưởng khoa (`TRUONG_KHOA`)

| Màn hình | URL | Template |
|---|---|---|
| Quản lý đợt đăng ký | `GET /periods` | `periods/list.html` |
| Tạo / sửa đợt | `GET /periods/new` · `GET /periods/{id}/edit` | `periods/form.html` |
| Duyệt & công bố đề tài | `GET /head/topics` | `head/topics/list.html` |
| Phân công / đổi GVHD | `GET /head/topics/{id}/supervisors` | `head/topics/supervisors.html` |
| Phân công phản biện | `GET /head/reviewers` | `head/reviewers/list.html` |
| Quản lý hội đồng | `GET /head/councils` | `head/councils/list.html` |
| Chi tiết hội đồng | `GET /head/councils/{id}` | `head/councils/detail.html` |
| Phân công GV chấm | `GET /head/councils/{id}/topics/{councilTopicId}/assignments` | `head/councils/assignments.html` |
| Công bố kết quả | `GET /head/results` | `head/results/list.html` |
| Nhật ký email kết quả | `GET /head/emails` | `head/emails/list.html` |
| Thông báo của khoa | `GET /head/announcements` | `announcements/manage.html` |
| Sửa thông báo khoa | `GET /head/announcements/{id}/edit` | `announcements/form.html` |

### 2.4. Giảng viên (`GIANG_VIEN`)

| Màn hình | URL | Template |
|---|---|---|
| Đề tài của tôi | `GET /lecturer/topics` | `lecturer/topics/list.html` |
| Đăng ký đề tài mới | `GET /lecturer/topics/new` | `lecturer/topics/form.html` |
| Nhiệm vụ phản biện | `GET /lecturer/reviews` | `lecturer/reviews/list.html` |
| Duyệt đăng ký nhóm | `GET /lecturer/registrations` | `lecturer/registrations/list.html` |
| Báo cáo nhóm được hướng dẫn | `GET /lecturer/reports` | `lecturer/reports/list.html` |
| Chấm điểm hội đồng | `GET /lecturer/council-work` | `lecturer/councils/work.html` |

### 2.5. Sinh viên (`SINH_VIEN`)

| Màn hình | URL | Template |
|---|---|---|
| Nhóm của tôi | `GET /student/groups` | `student/groups/list.html` |
| Đăng ký đề tài | `GET /student/registrations` | `student/registrations/list.html` |
| Nộp báo cáo | `GET /student/reports` | `student/reports/page.html` |
| Kết quả của tôi | `GET /student/results` | `student/results/list.html` |

### 2.6. Quản trị viên (`ADMIN`)

| Màn hình | URL | Template |
|---|---|---|
| Quản lý tài khoản | `GET /admin/users` | `admin/users/list.html` |
| Tạo / sửa tài khoản | `GET /admin/users/new` · `GET /admin/users/{id}/edit` | `admin/users/form.html` |
| Quản lý vai trò | `GET /roles` | `admin/roles/list.html` |
| Tạo / sửa vai trò | `GET /roles/new` · `GET /roles/{id}/edit` | `admin/roles/form.html` |
| Phân quyền cho vai trò | `GET /roles/{id}/permissions` | `admin/roles/permissions.html` |
| Quản lý bộ môn | `GET /admin/departments` | `admin/departments/list.html` |
| Thêm / sửa bộ môn | `GET /admin/departments/new` · `GET /admin/departments/{id}/edit` | `admin/departments/form.html` |
| Hồ sơ giảng viên | `GET /admin/profiles/lecturers` | `admin/profiles/lecturers/list.html` |
| Chi tiết hồ sơ GV | `GET /admin/profiles/lecturers/{id}` | `admin/profiles/lecturers/detail.html` |
| Sửa hồ sơ GV | `GET /admin/profiles/lecturers/{id}/edit` | `admin/profiles/lecturers/form.html` |
| Hồ sơ sinh viên | `GET /admin/profiles/students` | `admin/profiles/students/list.html` |
| Chi tiết hồ sơ SV | `GET /admin/profiles/students/{id}` | `admin/profiles/students/detail.html` |
| Sửa hồ sơ SV | `GET /admin/profiles/students/{id}/edit` | `admin/profiles/students/form.html` |
| Thông báo nhà trường | `GET /admin/announcements` | `announcements/manage.html` |
| Sửa thông báo | `GET /admin/announcements/{id}/edit` | `announcements/form.html` |

> Trưởng khoa cũng được vào `/admin/departments/**` và `/admin/profiles/**` (`hasAnyRole('ADMIN','TRUONG_KHOA')`).

### 2.7. Trang thiết kế (chỉ profile `demo`)

| Màn hình | URL | Template | Điều kiện |
|---|---|---|---|
| Style Guide | `GET /ui/style-guide` | `ui/style-guide.html` | `@Profile("demo")` + `hasRole('ADMIN')` |

---

## 3. Khung trang dùng chung

| File | Vai trò |
|---|---|
| `fragments/layout.html` | Khung duy nhất. Fragment `head(pageTitle)` (tiêu đề, meta, 4 file CSS, `theme.js` nạp đồng bộ) và fragment `nav` (sidebar + topbar). |
| `fragments/nav.html` | Fragment ủy quyền: `th:fragment="nav"` → `th:replace="~{fragments/layout :: nav}"`. Giữ tương thích cho template gọi `~{fragments/nav :: nav}`. |
| `fragments/error.html` | Fragment `errorPage(code, title, message, hint)` dùng cho 403/404/500. |

Mọi trang nghiệp vụ có cùng khuôn:

```html
<body class="app-page">
<a class="skip-link" href="#main-content">Bỏ qua đến nội dung chính</a>
<div th:replace="~{fragments/nav :: nav}"></div>
<main id="main-content" class="app-main"> … </main>
</body>
```

> **Lưu ý Thymeleaf:** khi truyền biến động vào fragment phải dùng `~{fragments/layout :: head(${pageTitle})}` (không đặt trong dấu nháy), nếu không `<title>` sẽ hiển thị nguyên chuỗi `${pageTitle}`.

---

## 4. Hệ thống style

### 4.1. File

| File | Nội dung |
|---|---|
| `static/css/theme.css` | **Toàn bộ design token.** Biến `:root` (sáng) và `:root[data-theme="dark"]`. Gradient, bóng đổ, bo góc, font, màu trạng thái. |
| `static/css/layout.css` | Khung ứng dụng (CSS Grid 2 vùng), sidebar, topbar, vùng nội dung, phân trang trang, in ấn. |
| `static/css/components.css` | Thư viện thành phần: card, box, nút, biểu mẫu, bảng, badge, chip, stat, progress, score, empty-state, modal, detail-list, article, split-layout, utility. |
| `static/css/pages.css` | Phần riêng từng nhóm trang: dashboard, quick-grid, trang đăng nhập (auth), perm-grid, style guide. |
| `static/js/theme.js` | Nạp **đồng bộ** trong `<head>`: đọc `localStorage`, đặt `data-theme` / `data-theme-pref` trước khi vẽ → **không FOUC**. Cũng cấp `window.setTheme(pref)`. |
| `static/js/ui.js` | Ngăn kéo sidebar, thu gọn sidebar, menu user/theme, tự ẩn flash, hộp xác nhận thay `confirm()`, hiện/ẩn mật khẩu, bảng → thẻ ở mobile (`data-label`), chống bấm đúp submit. |

Không dùng Bootstrap, Tailwind, jQuery, icon font hay CDN nào.

### 4.2. Design token chính

| Nhóm | Token | Sáng | Tối |
|---|---|---|---|
| Nền | `--bg` / `--surface` / `--surface-2` / `--surface-3` | `#F5F7FF` / `#FFFFFF` / `#EEF1FB` / `#E3E8F8` | `#070B1C` / `#0B1020` / `#1A2347` / `#232E58` |
| Chữ | `--text` / `--text-strong` / `--text-muted` | `#151A33` / `#0A0F23` / `#5A6285` | `#E8ECFF` / `#FFFFFF` / `#A3ACD1` |
| Thương hiệu | `--primary` / `--primary-hover` / `--primary-soft` | `#4F46E5` / `#4338CA` / `#EAEAFE` | `#8B93FF` / `#A5ABFF` / `#232C5E` |
| Nhấn | `--accent` (cyan) | `#0E7490` | `#22D3EE` |
| Nhấn | `--highlight` (amber) | `#B45309` | `#FBBF24` |
| Trạng thái | `--success` / `--warning` / `--danger` / `--info` | `#15803D` / `#B45309` / `#B91C1C` / `#1D4ED8` | `#4ADE80` / `#FBBF24` / `#F87171` / `#60A5FA` |
| Gradient | `--grad-brand` | `linear-gradient(135deg, #4F46E5, #6366F1 45%, #06B6D4)` | như cũ |
| Gradient | `--grad-aurora` | `linear-gradient(135deg, #6366F1, #8B5CF6, #06B6D4)` | `… #A78BFA … #22D3EE` |

Mọi mã màu cứng trong `layout.css` / `components.css` / `pages.css` đã được thay bằng `var(--token)`.

### 4.3. Chủ đề

- Lựa chọn: **Sáng / Tối / Theo hệ thống** (menu trên topbar).
- Lưu ở `localStorage` (`uniresearch-theme`), mặc định `system` → theo `prefers-color-scheme`.
- `theme.js` đặt `data-theme="light|dark"` trên `<html>` và `data-theme-pref` để khôi phục đúng lựa chọn.

---

## 5. Thành phần UI (component)

| Nhóm | Class | Ghi chú |
|---|---|---|
| Khung | `.box`, `.box.narrow/.wide/.xwide/.formbox`, `.card`, `.card-header/.card-title/.card-sub` | `.box` giữ tương thích với template cũ |
| Tiêu đề trang | `.page-title-block`, `.page-eyebrow` | |
| Nút | `.btn` + `.btn-primary/.btn-secondary/.btn-ghost/.btn-danger/.btn-success`, `.btn-sm/.btn-lg/.btn-block`, `.icon-btn` | Tương thích thêm `.btn.primary` / `.btn.secondary` |
| Biểu mẫu | `.form-grid`, `.form-row`, `.form-group`, `.form-hint`, `.field-error`, `.required`, `.form-actions` | |
| Bảng | `.table-scroll`, `.table-wide`, `.pagination`, `.page-link` | ≤700px: bảng → thẻ, nhãn cột lấy từ `<th>` (JS gắn `data-label`) |
| Huy hiệu | `.badge` + `.st-DRAFT/.st-OPEN/.st-CLOSED/.st-PENDING/.st-RUNNING/.st-sys/.st-HIGHLIGHT`, `.chip` | |
| Số liệu | `.stat-grid`, `.stat-card` (+`.is-accent/.is-success/.is-warning/.is-danger`), `.stat-label/.stat-value/.stat-hint` | |
| Tiến trình | `.progress`, `.progress-label` | |
| Điểm | `.score-badge` (+`.is-pass/.is-fail`) | |
| Rỗng | `.empty-state` | |
| Modal | `.modal-backdrop`, `.modal`, `.modal-actions` | `ui.js` dựng hộp xác nhận thay `confirm()` |
| Khác | `.detail-list/.detail-card`, `.article`, `.split-layout`, `.roadmap`, `.perm-grid/.perm-card`, `.system-warn` | |
| Icon | `.ui-icon` (`display:inline-block; flex-shrink:0; vertical-align:middle`) | Áp dụng cho mọi SVG nội tuyến |
| Utility | `.u-mt-4`, `.sr-only` | |

---

## 6. Quy tắc bảo trì (bắt buộc khi sửa giao diện)

1. **Không đổi nghiệp vụ.** Mọi thay đổi chỉ nằm trong `templates/`, `static/css/`, `static/js/`.
2. **Không đổi** tên tham số form, `th:field` / `th:object` / `th:action`, CSRF, URL, entity, repository, service, schema DB.
3. **Mọi màu phải đi qua `var(--token)`** trong `theme.css`. Không hardcode `#rrggbb` trong file khác `theme.css`.
4. **Không dùng `style="..."` trong template.** Dùng class; nếu cần class mới thì thêm vào `components.css`.
5. **Mọi icon là SVG nội tuyến** với class `.ui-icon`, dùng `currentColor`.
6. **Mọi trang phải có** `.skip-link` → `#main-content`, `<main id="main-content" class="app-main">`.
7. **Tiêu đề trang** đặt qua `~{fragments/layout :: head('…')}`; nếu tiêu đề động dùng `${pageTitle}` **không** đặt trong dấu nháy.
8. **Bảng phải bọc trong `.table-scroll`** để cuộn ngang an toàn; cột dùng `th` để tự sinh nhãn ở mobile.

---

## 7. Công cụ kiểm tra

| Công cụ | Cách dùng | Mục đích |
|---|---|---|
| `tools/check_pages.ps1` | `.\tools\check_pages.ps1 <user> <pass>` | Đăng nhập thật rồi GET hết các URL, in mã HTTP + `<title>`. |
| `tools/audit_ui.py` | `py -3 tools/audit_ui.py` | So class dùng trong template với class định nghĩa trong CSS; liệt kê `style=` inline và màu hardcode. |
| `tools/tokenize_css.py` | `py -3 tools/tokenize_css.py` | Quét lại màu hardcode còn sót trong `layout/components/pages.css`. |
| `tools/shots/shoot.js` | `node tools/shots/shoot.js docs/ui/after <user> <pass> [admin] [adminPass]` | Chụp màn hình 2 theme × 4 bề rộng (375/768/1366/1920) + kiểm tra tràn ngang & console error. Màn hình admin-only (`/roles`, `/admin/users`, `/ui/style-guide`) được chụp bằng browser context thứ hai, đăng nhập `admin`. |
| `tools/shots/overflow.js` | `node tools/shots/overflow.js /path` | Chỉ ra phần tử nào tràn ngang ở 375px. |

Ảnh chụp nằm ở `docs/ui/after/<slug>-<theme>-<width>.png`.
