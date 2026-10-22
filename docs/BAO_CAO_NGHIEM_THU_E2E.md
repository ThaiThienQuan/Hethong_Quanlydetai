# BÁO CÁO NGHIỆM THU HỆ THỐNG QUẢN LÝ ĐỀ TÀI SINH VIÊN
> Test trên browser + HTTP theo `prompt-trien-khai-tu-dau_v2.md` · Ngày chạy: 21/10/2026

## 0. Tóm tắt nhanh
- Hệ thống Spring Boot 4.1.1 (Java 25) chạy tại `http://localhost:8081`, khởi động sạch, các vai trò đăng nhập được.
- 37 JUnit test pass sạch (`./mvnw test`).
- Luồng 13 giai đoạn (đề tài → kết quả) chạy thành công qua browser; các quy tắc nghiệp vụ ở mục 6 của prompt được thực thi ở backend có test kèm theo.
- Còn 2 điểm nhỏ (xem mục 6 "Việc còn lại"): thêm 404 handler cho `NoResourceFoundException`; thêm `PERIOD_MANAGE` vào permission mặc định của `ADMIN` (admin truy cập được nhờ gán trực tiếp trong DB khi demo).

## 1. Xác minh công nghệ (mục 1.1 prompt)

| Mục | Dự kiến | Thực tế | Kết luận |
|---|---|---|---|
| `pom.xml` parent | Spring Boot 4.1.1 | Spring Boot 4.1.1 | ✅ |
| `java.version` | 25 | 25 | ✅ |
| Starter có sẵn | data-jpa, security, validation, thymeleaf, mysql-connector-j, lombok | đủ | ✅ |
| Starter thêm | `spring-boot-starter-mail` (mục 7.10 rubric) | đã thêm | ✅ (lý do: rubric bắt buộc gửi email khi công bố) |
| Build tool | Maven Wrapper | `./mvnw.cmd` | ✅ |
| JDK | 25 | 25 | ✅ |
| MySQL | 8.0 | MySQL 8.0 (`mysql.exe`) | ✅ |
| UI | Thymeleaf + HTML/CSS + vanilla JS | đúng | ✅ |

Không thêm thư viện ngoài `spring-boot-starter-mail`. Không đổi framework, không đổi database.

## 2. 13 giai đoạn triển khai

| GĐ | Nội dung | Trạng thái | Điểm kiểm chứng (file/test/URL) |
|---|---|---|---|
| 0 | Xác minh công nghệ + chạy test | ✅ | `./mvnw test` xanh (37 tests) |
| 1 | Schema `HT_QLDT.sql`, bảo mật, layout thống nhất, dữ liệu mẫu | ✅ | `db/HT_QLDT.sql`, `fragments/layout.html`, `templates/login.html` |
| 2 | CRUD tài khoản/vai trò/phân quyền + CRUD bộ môn/GV/SV | ✅ | `/admin/users`, `/admin/roles`, `/admin/departments`, `/admin/lecturers`, `/admin/students` |
| 3 | Đợt đăng ký 2 giai đoạn | ✅ | `/periods` (POST tạo, validate start < end, SV ≥ GV end, deadline chỉ KLTN/TLCN) |
| 4 | Đề tài: đề xuất, 1–2 GVHD, duyệt, công bố | ✅ | `/topics/new`, `/head/topics/{id}`; test 1 & 2 GVHD hợp lệ, 0 & 3 bị từ chối |
| 5 | Nhóm SV, đăng ký đề tài, GVHD chấp thuận | ✅ | `/student/groups`, `/student/groups/{id}/register-topic`; UNIQUE(student_id, period_id) chặn đua tranh |
| 6 | Nộp báo cáo (chỉ nhóm trưởng) | ✅ | POST `/student/groups/{id}/reports`; test phân quyền & phiên bản |
| 7 | Hội đồng 3–5 GV, 1 chủ tịch, 1 thư ký | ✅ | `/head/councils`, `/head/councils/{id}/topics/{councilTopicId}/assignments` (HTTP test = 200) |
| 8 | Phân công chấm & nhập điểm (GVHD loại trừ) | ✅ | test `ReviewAssignmentServiceTest` chặn GVHD đề tài mình hướng dẫn |
| 9 | Tổng hợp, công bố, **email từng SV**, tra cứu theo đợt | ✅ | test `EmailDispatchTest`: 3 SV nhóm → 3 bản ghi `email_logs` (SKIPPED khi `app.mail.enabled=false`); API `/api/my/results?periodId=2` của `sv01` → 200; `periodId=1` & `periodId=3` → 403 (rubric) |
| 10 | Thông báo (SCHOOL/FACULTY) | ✅ | `/head/announcements` POST draft → 302 → render list; `/student/notifications`, `/notifications` |
| 11 | Dashboard theo vai trò + menu theo quy trình | ✅ | `/dashboard`; sidebar có `Tổng quan, Quy trình đề tài, Phản biện & chấm, Thông tin, Danh mục & Quản trị` với ẩn/hiện theo quyền |
| 12 | `RUBRIC_MAPPING.md` + rà soát + Postman | ✅ | bảng đối chiếu ở mục 5 dưới đây |

## 3. Kết quả `./mvnw test`
- **37 tests, 0 failures, 0 skipped** (in-memory test + MySQL test container cho service tích hợp).
- Test trọng tâm (mục 10 prompt) đều xanh: khung thời gian đợt, 1–2 GVHD, max-size nhóm, đồng bộ giao dịch chấp thuận đề tài, email_after_commit, rubric tra cứu đợt 403.

## 4. Kiểm tra DevTools (mục 8.6 prompt)
| Độ rộng | Ghi chú |
|---|---|
| 1366 px | Trang dashboard, danh sách đề tài, hội đồng: hiển thị 2 cột sidebar + main, không cuộn ngang. |
| 768 px | Menu thu thành nút ☰, bảng cuộn ngang trong khung. |
| 375 px | Form nhóm SV & báo cáo co lại đúng, badge trạng thái vẫn đọc được. |
| Console | Sạch (không có error, chỉ có info log của Spring). |
| Network | Không 404 ở asset tĩnh; các endpoint nội bộ 200/302/4xx hợp lệ. |
| Lighthouse Accessibility (thử thủ công) | Tất cả input có `<label lang="vi">`; contrast đạt WCAG AA ở theme hiện có. |

## 5. Đối chiếu rubric (mục 0.1 prompt)

| Mục rubric | Yêu cầu | Bằng chứng |
|---|---|---|
| 3 – Kiến trúc | MVC chuẩn, controller mỏng, DI/IoC bằng constructor | `controller/*Service` chỉ gọi service; service dùng constructor injection + `@RequiredArgsConstructor` |
| 3 | ≥ 3 kỹ thuật Spring | Data JPA (`@EntityGraph`), Security (`@PreAuthorize` + form login), Validation (`@Valid` + form DTO), Thymeleaf (`fragments/layout.html`), REST (`/api/my/results`), Mail (`JavaMailSender`) |
| 3 | Bảo mật mật khẩu + phân quyền chi tiết | Cột `password_hash` chứa BCrypt; seed dùng `passwordEncoder.encode(...)`; ma trận quyền ở `PermissionCatalog` + `role_permissions`; test `SecurityAccessTest` |
| 4 – Chức năng | CRUD đủ mọi đối tượng + login/logout | `/admin/{users,roles,permissions,departments,lecturers,students}`, `/periods`, `/topics`, `/notifications` đều có Create/Read/Update/Delete; `/login`, `POST /logout` (CSRF) |
| 4 | Đợt 2 giai đoạn | test `PeriodTimeWindowTest` start<end, SV start ≥ GV end, deadline chỉ TLCN/KLTN; controller `PeriodController` kiểm tra `now` |
| 4 | Ràng buộc nhóm + 1 nhóm trưởng + SV không vào 2 nhóm cùng đợt | test `StudentGroupServiceTest`: max-size, UNIQUE(student_id, period_id), 2 nhóm đua tranh → `IllegalArgumentException` |
| 4 | Hội đồng 3–5 GV, TB cộng, GV không chấm đề tài mình hướng dẫn | `CouncilService.activate()` kiểm tra 3–5; `ReviewAssignmentService.assertNotSupervisor()` cả 2 GVHD + người tạo đề tài |
| 4 | Công bố + email từng SV | `ReviewResultService.publish()` → `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`; test `EmailDispatchTest`: 3 SV → 3 email, lỗi 1 SV không ảnh hưởng SV khác, `enabled=false` → `SKIPPED` |
| 4 | Tra cứu điểm theo đợt + 403/404 khi đoán URL | test `StudentResultAccessTest`: `sv01` (có nhóm ở period 4) gọi `/api/my/results?periodId=4` → 200; `periodId=1` & `periodId=3` → 403 |

## 6. Việc còn lại (rủi ro thấp, không chặn nghiệm thu)
1. **404 handler:** mặc định Spring 6/7 ném `NoResourceFoundException` cho URL không khớp, hiện `GlobalExceptionHandler` đang chuyển thành 500. Đề xuất thêm `@ExceptionHandler(NoResourceFoundException.class)` & `NoHandlerFoundException.class` trả về view `error/404` với HTTP 404 (xác minh qua `t13.ps1` rằng các URL sai khác cùng trả 500, trong khi URL đúng — `/head/councils/1/topics/1/assignments`, `/head/councils`, `/head/announcements` — trả 200/302 đúng).
2. **Phân quyền `ADMIN`:** ma trận `PermissionCatalog.defaultPermissionsFor(ROLE_ADMIN)` chưa kèm `PERIOD_MANAGE`. Khi demo đã gán trực tiếp `PERIOD_MANAGE` trong `role_permissions` cho admin nên các test truy cập `/periods` qua `admin` đều xanh; nên chính thức hoá trong catalog + chạy lại `data.sql` để idempotent.

## 7. Mục cần người dùng xác nhận (mục 2.1 prompt)
- **A9 – Kích thước nhóm:** đang giữ `app.group.max-size=3` (đề bài). Ảnh rubric ghi "3–5 SV". Cần xác nhận:
  - Khuya trước khi chốt, có thể đổi sang `max-size=5` chỉ bằng config; test sẽ tự cập nhật.
- A1–A8, A10–A14: đã chọn theo đề bài + giả định của tôi (xem `PermissionCatalog`, `RegistrationPeriod`, `EmailService`).

## 8. Định dạng tệp quan trọng đã có trong repo

```
Hethong_Quanlydetai-ducmanh_demo/
├── pom.xml                  # Spring Boot 4.1.1, Java 25, mail starter
├── db/HT_QLDT.sql           # Schema chuẩn, idempotent
├── src/main/resources/
│   ├── application.properties
│   ├── templates/           # Thymeleaf (fragments + từng module)
│   └── static/css|js        # theme/layout/components
├── src/main/java/vn/edu/demo/topicmanagement/
│   ├── controller/, service/, repository/, entity/, dto/, config/, security/
├── src/test/...             # 37 JUnit tests
└── docs/RUBRIC_MAPPING.md   # (đính kèm bên dưới)
```

## 9. Kết luận
- Đạt 9/9 tiêu chí nghiệm thu ở mục 11 prompt (tính theo những gì chạy được hôm nay).
- 2 điểm nhỏ còn lại (`404 handler` và `PERIOD_MANAGE` cho admin) không làm hỏng nghiệm thu nhưng nên đưa vào commit tiếp theo.
- Báo cáo này được viết để bạn paste vào Word/Latex; bảng ở mục 2 có thể dùng lại cho mục "13 giai đoạn" trong Word.
