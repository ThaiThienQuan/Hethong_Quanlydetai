# PROMPT: Triển khai từ đầu Hệ thống quản lý đề tài sinh viên (khoa CNTT)

> Dùng cho AI lập trình (Claude Code, Codex, Cursor...) chạy trong VS Code, tại thư mục gốc của repo
> `dynkayn/Student-Project-Management-System`, nhánh `main`.

---

## 0. NGUYÊN TẮC CHUNG

- Bạn là kỹ sư Java/Spring Boot senior. Nhiệm vụ: **triển khai lại toàn bộ chức năng từ đầu, từng giai đoạn, theo đúng đặc tả ở mục 2**, trên nền công nghệ hiện có của dự án (mục 1).
- "Từ đầu" nghĩa là: thiết kế lại schema, entity, service, controller, giao diện cho đúng và nhất quán. **Không** nghĩa là đổi công nghệ. Giữ bộ khung dự án (`pom.xml`, Maven Wrapper, package `blog.hethong_quanlydetai`, cấu hình kết nối MySQL). Mã cũ chỉ được tái sử dụng khi bạn đã đọc và xác nhận nó đúng đặc tả; nếu không đúng thì viết lại.
- **Không tự ý** đổi framework, thay database, hay thêm thư viện trùng chức năng với thư viện đã có. Chỉ thêm công cụ khi có lý do cụ thể, cần thiết, và phải ghi lý do vào báo cáo.
- Làm theo các giai đoạn ở mục 9. Hết mỗi giai đoạn phải build và test xanh trước khi sang giai đoạn tiếp theo.
- Mọi quy tắc nghiệp vụ phải được kiểm tra ở **backend (service layer)**. Giao diện chỉ ẩn/hiện chức năng, không thay thế kiểm tra quyền và kiểm tra dữ liệu.
- Giao diện và thông báo **tiếng Việt**. Không commit/push nếu tôi chưa yêu cầu.

## 0.1 RUBRIC CHẤM ĐIỂM CỦA GIẢNG VIÊN (ƯU TIÊN CAO NHẤT, THIẾU LÀ MẤT ĐIỂM)

Thứ tự ưu tiên khi mâu thuẫn: **rubric > đề bài (mục 2) > các phần còn lại của prompt**. Mỗi dòng dưới đây phải có **bằng chứng** (test, màn hình chạy thật, hoặc tên lớp/phương thức) và được ghi vào `docs/RUBRIC_MAPPING.md` ở GĐ 12.

| Mục rubric | Yêu cầu | Nơi thực hiện trong prompt | Bằng chứng phải có |
|---|---|---|---|
| 3 – Kiến trúc (1,0đ) | Cấu trúc chuẩn Spring MVC | 7.1 | Cây package `controller/service/repository/entity/dto`; controller mỏng |
| 3 | Áp dụng **≥ 3 kỹ thuật Spring** (Data JPA, Security, Validation, REST/Thymeleaf, DI/IoC...) | 7.1, 7.2, 7.10 | Bảng "kỹ thuật ↔ lớp/phương thức cụ thể" (dùng luôn cho chương 2 báo cáo) |
| 3 | Bảo mật: **không lưu mật khẩu plain-text**; phân quyền chi tiết theo từng vai trò | 3, 5.1 | Cột mật khẩu trong DB **và cả dữ liệu mẫu** đều là BCrypt; ma trận quyền + kiểm tra quyền sở hữu |
| 4 – Chức năng (2,0đ) | **CRUD đầy đủ**: đề tài, GV, SV, đợt đăng ký, bộ môn, thông báo; login/logout | 5.1, 5.1b, 5.2, 5.3, 5.9 | Mỗi đối tượng có đủ Tạo – Xem/tìm – Sửa – Xóa trên giao diện (xóa an toàn khi có ràng buộc) |
| 4 | Đợt đăng ký 2 giai đoạn (GĐ1 duyệt đề tài, GĐ2 mới cho SV đăng ký) | 5.2, 5.3 | Test khung thời gian |
| 4 | Ràng buộc nhóm; **đúng 1 nhóm trưởng** nộp báo cáo; SV đã vào nhóm A thì **không vào nhóm B** | 5.4, 5.5 | Test + xử lý đua tranh. ⚠ Rubric ghi "3–5 SV", đề bài ghi "tối đa 3" → xem A9 |
| 4 | Hội đồng 3–5 GV, điểm TB cộng, GV **không chấm đề tài mình hướng dẫn** | 5.6, 5.7, 5.8 | Test từng quy tắc |
| 4 | Công bố kết quả và **gửi email cho từng SV trong nhóm** | 5.8, 7.10 | Test Mockito `JavaMailSender` + `email_logs` |
| 4 | **Tra cứu điểm theo đợt đăng ký**; SV chỉ xem điểm đợt mình đã đăng ký, **không xem đợt khác** | 5.8, 4 (`group_members.period_id`) | Test đoán URL/`periodId` đợt khác → 403/404 |

**Nhắc lại bài học để tránh mất điểm:** (1) mật khẩu mẫu `{noop}`/plain-text trong SQL là lỗi nghiêm trọng nhất ở mục 3; (2) dữ liệu mẫu có mốc thời gian cố định sẽ hết hạn đúng ngày chấm; (3) báo cáo mẫu chỉ có đường dẫn mà không có tệp thật sẽ lỗi khi bấm tải; (4) chức năng có trong mã nhưng không có màn hình thì giảng viên coi như không có.

## 1. CÔNG CỤ VÀ CÔNG NGHỆ TRIỂN KHAI

| # | Hạng mục | Quy định |
|---|----------|----------|
| 1 | Môi trường phát triển | VS Code |
| 2 | Ngôn ngữ backend | Java 25 (theo cấu hình dự án hiện tại) |
| 3 | Framework backend | Spring Boot |
| 4 | Giao diện | Thymeleaf, HTML, CSS, JavaScript; tiếp tục dùng thư viện hiện có nếu phù hợp |
| 5 | Bảo mật | Spring Security |
| 6 | Truy cập dữ liệu | Spring Data JPA / Hibernate |
| 7 | Cơ sở dữ liệu | MySQL |
| 8 | Dependency và build | Maven hoặc Maven Wrapper hiện có (`./mvnw`) |
| 9 | Quản lý mã nguồn | Git và GitHub |
| 10 | Quản lý CSDL | MySQL Workbench |
| 11 | Kiểm thử | JUnit, Spring Boot Test; Postman khi phù hợp |
| 12 | Kiểm tra giao diện | Google Chrome DevTools |
| 13 | Gửi email khi công bố kết quả | `spring-boot-starter-mail` (`JavaMailSender`), SMTP cấu hình bằng biến môi trường. **Đây là thư viện duy nhất được phép thêm** (rubric bắt buộc gửi email) |

### 1.1 BƯỚC BẮT BUỘC TRƯỚC KHI VIẾT CODE: xác minh công nghệ với mã nguồn hiện tại

Chạy và đọc thật, rồi lập bảng "dự kiến ↔ thực tế ↔ kết luận" trong báo cáo:

1. `pom.xml`: Spring Boot parent (hiện là 4.1.1), `java.version` (phải là **25**), danh sách starter đang có (data-jpa, security, validation, webmvc, thymeleaf, devtools, mysql-connector-j, lombok, các starter test; thường **chưa có** `spring-boot-starter-mail`, sẽ thêm ở GĐ 9). Nếu `java.version` khác 25 thì **dừng và hỏi tôi**, không tự đổi.
2. `./mvnw -v` và `java -version`: xác nhận JDK dùng để build là 25.
3. `application.properties`: kết nối MySQL, `ddl-auto`, `open-in-view`, mức log.
4. `src/main/resources`: có `templates/` (Thymeleaf), `static/css/`; kiểm tra có `static/js/` hay thư viện JS/CSS nào đang dùng không.
5. `src/test`: các test hiện có chạy được bằng `./mvnw test` hay không.
6. Có file nào của IDE/Workbench/Postman trong repo không (`.vscode/`, `*.mwb`, `*.postman_collection.json`).

**Quy tắc áp dụng:**
- Bộ công nghệ trên là cố định, **ngoại lệ duy nhất đã được duyệt: `spring-boot-starter-mail`** (xem 7.10). Không thêm: Lombok thứ hai, ORM khác, thư viện template khác, SPA framework, thư viện UI/CSS lớn (Bootstrap, Tailwind...) nếu CSS thuần đã đủ, thư viện upload/validation/date trùng chức năng với Spring/JDK.
- JavaScript: chỉ JS thuần (vanilla), nhỏ gọn, đặt ở `static/js/`. Chỉ thêm thư viện JS khi không thể làm bằng JS thuần, và phải nêu lý do.
- Không thêm tệp cấu hình phụ thuộc riêng một IDE, trừ khi là khuyến nghị tùy chọn không ảnh hưởng build.
- **MySQL Workbench:** script schema phải chạy sạch khi mở trong Workbench (`Run SQL Script`) trên một MySQL trống; đặt tên ràng buộc rõ ràng để Workbench vẽ được sơ đồ EER (Reverse Engineer). Ghi hướng dẫn ngắn trong README.
- **Postman / REST:** ứng dụng chủ yếu render server-side. Tuy nhiên rubric nêu "REST/Thymeleaf", nên làm **một vài endpoint `@RestController` nhỏ có mục đích thật** (không làm cho có): `GET /api/students/available?periodId=&q=` (gợi ý SV chưa có nhóm khi lập nhóm, phục vụ JS), `GET /api/my/results?periodId=` (tra cứu kết quả theo đợt), cùng các endpoint REST đang có (ví dụ `controller/api/SecurityApiController`). Chúng gọi **cùng service và cùng kiểm tra quyền** với trang web, trả JSON qua DTO. Tạo collection Postman trong `docs/postman/` và hướng dẫn đăng nhập/CSRF khi gọi.
- **Chrome DevTools:** dùng để kiểm tra giao diện theo mục 8.6 (không cần cài gì vào dự án).

## 2. ĐẶC TẢ NGHIỆP VỤ (nguồn chuẩn, từ đề bài)

Hệ thống quản lý đề tài môn học / nghiên cứu khoa học (NCKH) / tiểu luận chuyên ngành (TLCN) / khóa luận tốt nghiệp (KLTN), tự động hóa toàn bộ quy trình trong khoa CNTT: tạo đợt đăng ký → GV và SV đăng ký đề tài → phê duyệt đề tài → phân công GVHD, GVPB, GV chấm điểm → Khoa CNTT công bố kết quả.

1. **Đợt đăng ký** theo từng giai đoạn: giai đoạn 1 xây dựng và công bố danh sách đề tài; giai đoạn 2 các nhóm SV đăng ký thực hiện một đề tài cụ thể trong danh sách đã công bố. Trong mỗi đợt, hệ thống chỉ cho đăng ký đúng thời gian quy định. **Trưởng khoa** tạo đợt với: tên đợt, loại (môn học/NCKH/TLCN/KLTN), thời gian bắt đầu–kết thúc cho GV đăng ký, thời gian bắt đầu–kết thúc cho SV đăng ký, ngày hạn chót GVPB nộp điểm về Khoa (chỉ thiết lập khi loại là TLCN hoặc KLTN), ngày báo cáo hội đồng (chỉ thiết lập khi loại là KLTN).
2. **Đề tài** quản lý theo từng bộ môn; mỗi đề tài thuộc một bộ môn cụ thể, có nội dung rõ ràng, được hướng dẫn bởi **ít nhất 1 và tối đa 2 GV**.
3. **Nhóm SV:** tối đa 3 thành viên, có một nhóm trưởng; mỗi SV chỉ tham gia **duy nhất một nhóm** trong suốt quá trình thực hiện đề tài; mỗi nhóm chỉ đăng ký **duy nhất một đề tài** từ danh sách đã công bố; sau khi đăng ký và được chấp thuận, nhóm thực hiện theo sự hướng dẫn của GV được phân công; **nhóm trưởng** nộp báo cáo đề tài.
4. **Phản biện:** sau khi hoàn thành đề tài, nhóm tham gia phản biện theo kế hoạch. Hệ thống quản lý việc lập các **hội đồng phản biện**, mỗi hội đồng 3–5 GV gồm **một chủ tịch và một thư ký**. Chủ tịch hội đồng chịu trách nhiệm tổng hợp các đánh giá và điểm của các thành viên để đưa ra kết quả phản biện cuối cùng. **Điểm cuối = trung bình cộng các điểm thành phần.** Một GV có thể chấm nhiều đề tài khác nhau, nhưng **không được chấm đề tài mình đang hướng dẫn**.
5. **Công bố:** sau khi phản biện và chấm điểm hoàn tất, kết quả đánh giá được công bố trên hệ thống; SV xem được điểm và kết quả đánh giá của đề tài mình tham gia.
6. **Ngoài đề tài:** quản lý tài khoản người dùng; đăng tải thông báo chính thức của nhà trường và khoa; các giải pháp **phân quyền và bảo mật dữ liệu**.

### 2.1 Các giả định (đề bài chưa nói rõ). Dùng mặc định dưới đây, làm cho dễ đổi, và liệt kê ở mục "Cần tôi xác nhận" trong báo cáo cuối

| Mã | Điểm chưa rõ | Mặc định chọn |
|----|--------------|---------------|
| A1 | Ai phê duyệt đề tài | Trưởng khoa (`TOPIC_APPROVE`). Quyền gán theo vai trò nên đổi được bằng màn hình Phân quyền, không phải sửa code |
| A2 | Ai công bố kết quả ("Khoa CNTT công bố") | Trưởng khoa (`RESULT_PUBLISH`); Chủ tịch hội đồng chỉ tổng hợp điểm |
| A3 | Ai lập hội đồng, phân công GVPB/GV chấm | **Trưởng khoa** (`COUNCIL_MANAGE`, `ASSIGNMENT_MANAGE`) lập hội đồng và phân công GVPB/GV chấm, đúng câu "Trưởng Khoa phân công GVHD, GVPB, GV chấm điểm" của đề bài. Chủ tịch hội đồng chỉ **tổng hợp** điểm/nhận xét của hội đồng mình |
| A4 | Thang điểm, làm tròn, ngưỡng đạt | Thang 0–10, làm tròn `HALF_UP` 2 chữ số, ĐẠT khi điểm cuối ≥ 5,00 (đặt thành hằng số cấu hình) |
| A5 | GVHD thứ hai có phải cùng bộ môn không | Không bắt buộc cùng bộ môn |
| A6 | Hội đồng áp dụng cho loại đợt nào | Mọi loại đợt; "hạn chót GVPB" chỉ có với TLCN/KLTN, "ngày báo cáo hội đồng" chỉ có với KLTN |
| A7 | Nộp báo cáo | Tải tệp lên (PDF/DOC/DOCX, tối đa 20 MB), có số phiên bản tăng dần; chỉ nhóm trưởng nộp được, và chỉ khi nhóm đã được GV chấp thuận |
| A8 | Đề tài bị từ chối | GV được sửa và gửi duyệt lại (`REJECTED` → `PENDING`), có lý do từ chối |
| A9 | **Kích thước nhóm** (đề bài: tối đa 3; ảnh rubric ghi "1 nhóm chỉ từ 3-5 SV") | Mặc định theo đề bài: `app.group.min-size=1`, `app.group.max-size=3`, đọc từ cấu hình, **không đặt CHECK cứng trong DB**, test viết theo cấu hình. **Phải hỏi giảng viên**; nếu chốt 3–5 thì chỉ đổi cấu hình |
| A10 | Ai chấp thuận đăng ký của nhóm | **Chỉ GVHD của đề tài** (cả hai GVHD) chấp thuận/từ chối được (`REGISTRATION_CONFIRM`). Trưởng khoa **không** có quyền này |
| A11 | Ai phân công GVHD | GV chọn GVHD thứ hai khi đề xuất; **Trưởng khoa có thể thêm/đổi GVHD** (`SUPERVISOR_ASSIGN`), luôn giữ 1–2 GVHD, lưu `assigned_by`, ghi nhật ký |
| A12 | Gửi email khi công bố | Mỗi SV trong nhóm nhận **một email riêng**; gửi sau khi công bố commit; lỗi gửi không làm hỏng công bố; ghi `email_logs`, có nút gửi lại |
| A13 | "Tra cứu điểm theo đợt đăng ký" | SV chỉ xem kết quả đã công bố của các đợt mà SV đó **có nhóm tham gia**; đợt khác bị từ chối |
| A14 | Số vai trò tài khoản | **4 vai trò** khớp đề bài: `ADMIN`, `TRUONG_KHOA`, `GIANG_VIEN`, `SINH_VIEN`. "Chủ tịch hội đồng" là quan hệ theo từng hội đồng (`council_members.role`), không phải vai trò tài khoản |

## 3. VAI TRÒ VÀ QUYỀN

Vai trò tài khoản (4, khớp đề bài, xem A14): `ADMIN`, `TRUONG_KHOA`, `GIANG_VIEN`, `SINH_VIEN`. Quyền là chuỗi, kiểm tra bằng `@PreAuthorize("hasAuthority('...')")`. Quyền khởi tạo cho từng vai trò (chỉnh được trong màn hình Phân quyền):

| Quyền | ADMIN | TRUONG_KHOA | GIANG_VIEN | SINH_VIEN |
|-------|:---:|:---:|:---:|:---:|
| USER_MANAGE, ROLE_MANAGE, PERMISSION_MANAGE | ✔ | | | |
| DEPARTMENT_MANAGE, PROFILE_MANAGE (CRUD bộ môn, hồ sơ GV, hồ sơ SV) | ✔ | ✔ | | |
| PERIOD_MANAGE (tạo, sửa, xóa nháp, công bố, đóng đợt) | ✔ | ✔ | | |
| TOPIC_VIEW (xem đề tài) | ✔ | ✔ | ✔ | ✔ |
| TOPIC_CREATE (đề xuất, sửa, xóa đề tài của mình) | | | ✔ | |
| TOPIC_APPROVE (duyệt/từ chối đề tài, công bố danh sách) | ✔ | ✔ | | |
| SUPERVISOR_ASSIGN (phân công/điều chỉnh GVHD) | ✔ | ✔ | | |
| REGISTRATION_CREATE (lập nhóm, đăng ký đề tài) | | | | ✔ |
| REGISTRATION_CONFIRM (chấp thuận nhóm) | | | ✔ (chỉ GVHD đề tài đó) | |
| SUBMISSION_CREATE (nộp báo cáo) | | | | ✔ (chỉ nhóm trưởng) |
| REPORT_VIEW (xem/tải báo cáo) | ✔ | ✔ | ✔ (đề tài mình hướng dẫn/chấm/hội đồng của mình) | ✔ (nhóm mình) |
| COUNCIL_MANAGE (lập hội đồng) | ✔ | ✔ | | |
| ASSIGNMENT_MANAGE (phân công GVPB/GV chấm) | ✔ | ✔ | | |
| EVALUATION_CREATE (nhập điểm) | | | ✔ (được phân công, không phải GVHD) | |
| RESULT_CALCULATE (tổng hợp điểm) | ✔ | | ✔ (**chỉ khi là chủ tịch của đúng hội đồng đó**) | |
| RESULT_PUBLISH (công bố kết quả, kích hoạt gửi email) | ✔ | ✔ | | |
| RESULT_VIEW (xem kết quả đã công bố) | | | | ✔ (nhóm mình, đợt mình tham gia) |
| NOTICE_VIEW | ✔ | ✔ | ✔ | ✔ |
| NOTICE_MANAGE (CRUD thông báo) | ✔ | ✔ | | |

Lưu ý: "chủ tịch của hội đồng X" là **quan hệ theo từng hội đồng**. Một GV có thể là chủ tịch của hội đồng này và thành viên của hội đồng khác. Mọi thao tác của chủ tịch phải kiểm tra bằng `council_members.role = CHAIRMAN` của **đúng hội đồng**, không chỉ theo quyền.

## 4. MÔ HÌNH DỮ LIỆU (thiết kế lại; một script nguồn chuẩn `HT_QLDT.sql`)

Tên bảng/cột snake_case, khóa ngoại và ràng buộc **đặt tên rõ ràng**. Script chạy sạch trên MySQL mới, có `DROP/CREATE DATABASE` an toàn hoặc `IF NOT EXISTS`, charset `utf8mb4`.

- `users` (username UNIQUE, `password_hash` VARCHAR(100) chứa chuỗi BCrypt, full_name, email UNIQUE, phone, status ACTIVE/LOCKED, enabled, created_at), `roles`, `permissions`, `user_roles`, `role_permissions`.
- `departments` (bộ môn: code UNIQUE, name, status).
- `students` (user_id UNIQUE, student_code UNIQUE, class_name, major), `lecturers` (user_id UNIQUE, lecturer_code UNIQUE, academic_degree, department_id).
- `registration_periods`: name, `period_type` CHECK IN (`MON_HOC`,`NCKH`,`TLCN`,`KLTN`), `lecturer_start_at/lecturer_end_at`, `student_start_at/student_end_at`, `review_deadline` NULL, `council_report_date` NULL, status (`DRAFT`,`PUBLISHED`,`CLOSED`), created_by. CHECK: start < end; student_start ≥ lecturer_end (hai giai đoạn riêng biệt); `review_deadline` chỉ khác NULL khi TLCN/KLTN; `council_report_date` chỉ khác NULL khi KLTN.
- `topics`: period_id, department_id, created_by_lecturer_id, topic_code UNIQUE, topic_name, description (bắt buộc, độ dài tối thiểu), status (`PENDING`,`APPROVED`,`REJECTED`), reject_reason, approved_by, approved_at, published (BOOLEAN), created_at.
- `topic_supervisors`: topic_id, lecturer_id, assigned_by NULL (người phân công, A11), supervisor_order CHECK IN (1,2), UNIQUE (topic_id, lecturer_id), UNIQUE (topic_id, supervisor_order). Ràng buộc mức service: mỗi đề tài có 1–2 GVHD.
- `student_groups`: period_id, **UNIQUE (id, period_id)** (làm đích cho FK kép của `group_members`), group_code UNIQUE, group_name, `topic_id` NULL UNIQUE (đề tài nhóm đang thực hiện, set khi được chấp thuận), status (`DRAFT`,`REGISTERED`,`APPROVED`,`SUBMITTED`,`COMPLETED`), created_at.
- `group_members`: group_id, **period_id** (sao chép từ nhóm), student_id, is_leader, **UNIQUE (student_id, period_id)** (mỗi SV một nhóm duy nhất **trong một đợt**), UNIQUE (group_id, student_id), FK kép (group_id, period_id) → `student_groups(id, period_id)`. **Lý do:** nếu để `UNIQUE (student_id)` toàn cục thì SV đã học đợt TLCN sẽ không bao giờ lập nhóm được ở đợt KLTN, và yêu cầu "tra cứu điểm theo đợt" trở nên vô nghĩa. Số thành viên tối đa/tối thiểu của nhóm đọc từ cấu hình (A9), không viết CHECK cứng. Mỗi nhóm đúng 1 nhóm trưởng (kiểm tra ở service, có thể thêm chỉ mục/cột sinh ra để đảm bảo ở DB).
- `topic_registrations`: group_id, topic_id, status (`PENDING`,`APPROVED`,`REJECTED`,`CANCELLED`), registered_at, approved_by, approved_at, rejection_reason. Mỗi nhóm tối đa một đăng ký hoạt động (`PENDING`/`APPROVED`); mỗi đề tài tối đa một đăng ký `APPROVED`.
- `reports`: group_id, submitted_by (SV nhóm trưởng), report_title, file_name, stored_path, file_size, version (> 0), description, submitted_at, status. UNIQUE (group_id, version).
- `councils`: period_id, council_code UNIQUE, council_name, report_date NULL, status (`DRAFT`,`READY`,`FINISHED`).
- `council_members`: council_id, lecturer_id, role CHECK IN (`CHAIRMAN`,`SECRETARY`,`MEMBER`), UNIQUE (council_id, lecturer_id).
- `council_topics` (đề tài do hội đồng phản biện): council_id, topic_id UNIQUE.
- `review_assignments`: council_id, topic_id, lecturer_id, assigned_by, assigned_at, status (`ASSIGNED`,`COMPLETED`), UNIQUE (council_id, topic_id, lecturer_id).
- `evaluations`: assignment_id UNIQUE, score DECIMAL(4,2) CHECK 0–10, comment, evaluated_at, status.
- `review_results`: group_id, topic_id UNIQUE, council_id, final_score CHECK 0–10, result (`PASSED`,`FAILED`), calculated_by, calculated_at, published, published_at, published_by.
- `notifications`: title, content, `type` (`SCHOOL`,`FACULTY`), created_by, status (`DRAFT`,`PUBLISHED`), created_at, published_at, updated_at; `notification_target_roles` (notification_id, role_id) để giới hạn người xem theo vai trò.
- `audit_logs`: user_id, action, entity, entity_id, detail, created_at (ghi các hành động quan trọng: tạo/duyệt đề tài, chấp thuận đăng ký, phân công, nhập điểm, tổng hợp, công bố, đổi quyền, khóa tài khoản).
- `email_logs`: id, result_id (→ `review_results`), student_id, to_email, subject, status (`SENT`,`FAILED`,`SKIPPED`), error_message NULL, attempt_count, created_at, sent_at NULL. Index (result_id), (student_id). Dùng cho nhật ký gửi email và nút "Gửi lại".

Tự tạo dữ liệu mẫu (profile `demo`, mục 7.9) trong tệp riêng; **không** nhét dữ liệu mẫu lẫn vào schema chuẩn.

## 5. CHỨC NĂNG CHI TIẾT THEO MODULE

Mỗi module gồm: mục đích, người dùng, màn hình, thao tác, quy tắc kiểm tra (lỗi trả về thông báo tiếng Việt rõ ràng qua `IllegalArgumentException`/validation), trạng thái.

### 5.1 Xác thực, tài khoản, vai trò, phân quyền, bảo mật
- Đăng nhập bằng form; **đăng xuất bằng `POST /logout` có CSRF**, nút "Đăng xuất" luôn hiện ở topbar (rubric chấm login/logout). Tài khoản `LOCKED`/không `enabled` không đăng nhập được, thông báo chung chung ("Sai tài khoản hoặc mật khẩu hoặc tài khoản bị khóa") để không lộ thông tin.
- **Quản lý tài khoản** (`USER_MANAGE`): danh sách có tìm kiếm (username, họ tên, email), lọc theo vai trò/trạng thái, phân trang; tạo/sửa; gán vai trò; khóa/mở khóa; đặt lại mật khẩu. Khi tạo tài khoản SV/GV phải tạo kèm hồ sơ `students`/`lecturers` (mã SV/GV duy nhất, bộ môn của GV). Không cho admin tự khóa/hạ quyền chính mình; không xóa vai trò cuối cùng của admin cuối cùng.
- **Tài khoản cá nhân:** xem hồ sơ, đổi mật khẩu (nhập mật khẩu cũ, mật khẩu mới ≥ 8 ký tự, có chữ và số).
- **Vai trò & quyền:** danh sách/tạo/sửa vai trò; gán/bỏ quyền cho vai trò qua màn hình Phân quyền; vai trò hệ thống (4 vai trò gốc) không xóa được. Danh mục quyền nạp khi khởi động theo bảng ở mục 3 (idempotent, không ghi đè phân quyền đã chỉnh).
- **Bảo mật dữ liệu:** mật khẩu băm bằng `DelegatingPasswordEncoder` (BCrypt), **tuyệt đối không** lưu mật khẩu dạng rõ hoặc `{noop}` ở bất kỳ nơi nào, **kể cả profile demo, script SQL mẫu, seeder, log, tài liệu** (rubric chấm riêng mục này: dữ liệu mẫu phải chứa chuỗi BCrypt; mật khẩu demo chỉ ghi trong README); CSRF bật cho mọi form POST; session cố định khi đăng nhập; header bảo mật mặc định của Spring Security; giới hạn đăng nhập sai liên tiếp (khóa tạm theo tài khoản, ví dụ 5 lần/15 phút) bằng cơ chế tự viết nhỏ, không thêm thư viện; không log mật khẩu/token; tham số hóa mọi truy vấn; thoát (escape) mặc định của Thymeleaf, không dùng `th:utext` với dữ liệu người dùng; kiểm tra **quyền sở hữu** trên từng thao tác (SV chỉ thao tác nhóm của mình, GV chỉ thao tác đề tài/phân công của mình); mọi endpoint ghi dữ liệu đều có `@PreAuthorize`; trang lỗi 403/404/500 thân thiện, không lộ stack trace.

### 5.1b CRUD danh mục: bộ môn, hồ sơ giảng viên, hồ sơ sinh viên (rubric yêu cầu CRUD đủ, không chỉ tạo tài khoản)
- **Bộ môn** (`DEPARTMENT_MANAGE`): danh sách có tìm kiếm + phân trang; tạo (mã duy nhất, tên), sửa, xóa. Không xóa bộ môn đã có GV hoặc đề tài: chuyển `INACTIVE` (bộ môn `INACTIVE` không hiện ở form mới).
- **Hồ sơ GV** (`PROFILE_MANAGE`): danh sách lọc theo bộ môn, tìm theo mã/tên; xem chi tiết (đề tài hướng dẫn, hội đồng tham gia); tạo (cùng tài khoản, **tái dùng service ở 5.1**, không nhân đôi logic), sửa (họ tên, học vị, bộ môn, email, SĐT); xóa chỉ khi chưa được tham chiếu, ngược lại khóa tài khoản và báo lý do.
- **Hồ sơ SV** (`PROFILE_MANAGE`): tương tự; lọc theo lớp/ngành; xem các đợt đã tham gia; xóa chỉ khi chưa thuộc nhóm nào.
- Mọi thao tác xóa có hộp xác nhận; vi phạm ràng buộc trả thông báo tiếng Việt, không lộ lỗi SQL.

### 5.2 Đợt đăng ký (Trưởng khoa)
- **Màn hình:** danh sách đợt (lọc theo loại/trạng thái, hiển thị giai đoạn hiện tại: *Chưa mở / GV đăng ký / SV đăng ký / Đã đóng*), form tạo/sửa, chi tiết đợt.
- **Form:** tên đợt; loại đợt; hai khoảng thời gian (GV, SV); hạn chót GVPB nộp điểm và ngày báo cáo hội đồng **chỉ hiện** khi chọn loại tương ứng (JS ẩn/hiện; **backend vẫn kiểm tra** và xóa/từ chối giá trị thừa).
- **Quy tắc:** bắt đầu < kết thúc; giai đoạn SV bắt đầu sau khi giai đoạn GV kết thúc; hạn chót GVPB phải sau ngày kết thúc đăng ký của SV; ngày báo cáo hội đồng phải sau hạn chót GVPB (nếu có). Chỉ sửa/xóa đợt khi chưa có đề tài; đợt `PUBLISHED` chỉ được sửa mốc thời gian, không đổi loại. Công bố đợt: `DRAFT` → `PUBLISHED`; đóng đợt: → `CLOSED` (sau đó mọi thao tác đăng ký bị chặn).
- **Hệ thống chỉ mở đăng ký đúng thời gian quy định** (kiểm tra ở service mỗi lần GV đề xuất, SV lập nhóm/đăng ký), dựa trên giờ máy chủ.

### 5.3 Đề tài (GV đề xuất, Trưởng khoa duyệt)
- **GV** (trong khung thời gian GV của đợt `PUBLISHED`): đề xuất đề tài gồm mã, tên, mô tả/nội dung rõ ràng, bộ môn (mặc định bộ môn của GV, không được chọn bộ môn khác), tối đa 1 GVHD thứ hai (A5). Đề tài tạo ra ở `PENDING`. GV sửa đề tài `PENDING`/`REJECTED` của mình; gửi lại sau khi bị từ chối (A8). Không sửa được đề tài đã `APPROVED` nếu đã có đăng ký.
- **Trưởng khoa:** xem hàng đợi đề tài chờ duyệt (lọc theo bộ môn/đợt/trạng thái), duyệt hoặc từ chối (bắt buộc nhập lý do khi từ chối). Lưu người duyệt và thời điểm.
- **Công bố danh sách đề tài (giai đoạn 1):** đề tài `APPROVED` được công bố để SV thấy ở giai đoạn 2. Chốt một cách duy nhất: đề tài `APPROVED` và `published = true`; Trưởng khoa bấm "Công bố danh sách" theo đợt (hoặc tự động khi đợt chuyển sang giai đoạn SV). Dùng nhất quán ở danh sách cho SV và ở kiểm tra khi đăng ký.
- **Quản lý theo bộ môn:** danh sách đề tài nhóm/lọc theo bộ môn, hiển thị các GVHD, trạng thái, nhóm đang thực hiện (nếu có).
- **Xóa đề tài** (đủ CRUD): GV xóa đề tài của mình khi `PENDING`/`REJECTED` và chưa có đăng ký; Trưởng khoa/admin xóa khi chưa có đăng ký nào.
- **Trưởng khoa phân công/điều chỉnh GVHD** (`SUPERVISOR_ASSIGN`, đúng đề bài; A11): thêm hoặc thay GVHD ở trang chi tiết đề tài. Vẫn phải 1–2 GVHD, không trùng; không thêm làm GVHD một GV đang được phân công chấm đề tài đó; lưu `assigned_by`, ghi `audit_logs`.
- **Quy tắc:** mã đề tài duy nhất; 1 ≤ số GVHD ≤ 2; GVHD không trùng nhau; đề tài thuộc đúng một bộ môn và một đợt.

### 5.4 Nhóm sinh viên và đăng ký đề tài
- **Lập nhóm** (SV, trong khung thời gian SV của đợt): SV tạo nhóm và tự thành **nhóm trưởng**, chọn thêm tối đa (`app.group.max-size` − 1) SV khác (tìm theo mã/tên; chỉ hiện SV **chưa thuộc nhóm nào trong đợt này**). Tối đa `app.group.max-size` thành viên (mặc định 3, xem A9); không trùng; **nhóm trưởng cũng phải chưa thuộc nhóm khác**; mỗi SV chỉ một nhóm trong cùng một đợt: **đã vào nhóm A thì không được vào nhóm B** (ràng buộc `UNIQUE(student_id, period_id)` + kiểm tra service, xử lý đua tranh bằng bắt `DataIntegrityViolationException`). Cho phép nhóm trưởng thêm/bớt thành viên khi nhóm chưa đăng ký đề tài.
- **Đăng ký đề tài** (chỉ nhóm trưởng): chọn đề tài đã công bố **cùng đợt** với nhóm; mỗi nhóm chỉ một đăng ký hoạt động; không đăng ký đề tài đã có nhóm được chấp thuận; có thể hủy đăng ký khi còn `PENDING`.
- **GVHD chấp thuận/từ chối** (GVHD của đề tài đó, cả hai GVHD đều có quyền, A10): chấp thuận → đăng ký `APPROVED`, gán `student_groups.topic_id`, nhóm `APPROVED`, tự **từ chối các đăng ký `PENDING` khác** của cùng đề tài, trong **một transaction**; từ chối → bắt buộc lý do, nhóm quay về trạng thái có thể đăng ký đề tài khác.
- **Màn hình:** SV xem nhóm của mình, thành viên, trạng thái đăng ký; **GVHD** xem hàng đợi xác nhận (chỉ đề tài mình làm GVHD) và các nhóm đang hướng dẫn.

### 5.5 Nộp báo cáo (nhóm trưởng)
- Chỉ nhóm trưởng, chỉ khi nhóm đã `APPROVED`. Tải tệp (A7): kiểm tra phần mở rộng và dung lượng, **sanitize tên tệp**, lưu vào thư mục cấu hình `app.storage.dir` (ngoài thư mục `static`), tên lưu ngẫu nhiên; số phiên bản tăng dần; tiêu đề, mô tả.
- Xem/tải về có kiểm tra quyền: SV trong nhóm, GVHD của đề tài, thành viên hội đồng phụ trách đề tài, Trưởng khoa, admin. Không truy cập trực tiếp tệp bằng URL tĩnh.
- Nhóm chuyển `SUBMITTED` khi có báo cáo đầu tiên. Danh sách báo cáo lọc theo đợt/đề tài/nhóm.

### 5.6 Hội đồng phản biện
- **Trưởng khoa** (`COUNCIL_MANAGE`) lập hội đồng theo đợt: mã, tên, ngày báo cáo (với KLTN lấy theo ngày báo cáo hội đồng của đợt), chọn **3–5 GV**, đúng **1 chủ tịch** và **1 thư ký**; chọn các đề tài (đã `SUBMITTED`) đưa vào phản biện.
- **Hợp lệ để dùng:** chỉ khi đủ 3–5 GV, đúng 1 chủ tịch, đúng 1 thư ký thì mới chuyển `READY`. Hội đồng `READY` không cho xóa thành viên làm tụt dưới 3 hoặc làm mất chủ tịch/thư ký; không xóa hội đồng đã có phân công/điểm.
- Một GV được tham gia nhiều hội đồng; không trùng GV trong cùng hội đồng.
- **Màn hình:** danh sách, tạo/sửa, chi tiết (thành viên, đề tài, phân công, tiến độ chấm).

### 5.7 Phân công chấm (GVPB / GV chấm) và nhập điểm
- **Phân công** (Trưởng khoa hoặc admin, đúng đề bài; A3): với mỗi đề tài của hội đồng, chọn các thành viên hội đồng làm người chấm (GVPB). Điều kiện: hội đồng `READY`; người chấm **thuộc hội đồng**; hội đồng và đề tài **cùng đợt**; đề tài đã có báo cáo; **người chấm không phải GVHD của đề tài** (kiểm tra cả hai GVHD và người tạo đề tài); không trùng phân công; mỗi đề tài có ít nhất 1 người chấm. Không xóa phân công đã có điểm.
- **Nhập điểm** (GV được phân công): chỉ thấy/chấm đề tài **được giao**; điểm 0–10 (tối đa 2 chữ số thập phân), nhận xét bắt buộc; mỗi phân công **một** bản chấm (cho sửa đến hạn chót, có ghi nhật ký); **trước hạn chót GVPB** của đợt (nếu có, A6): quá hạn thì từ chối (admin được phép, có ghi nhật ký). Hiển thị rõ hạn chót và số ngày còn lại trên màn hình chấm.
- **Màn hình:** GV thấy danh sách "đề tài cần chấm" (chưa chấm/đã chấm/quá hạn).

### 5.8 Tổng hợp và công bố kết quả
- **Tổng hợp** (Chủ tịch của hội đồng đó, hoặc admin): chỉ khi **mọi** người chấm được phân công đã nộp điểm. Điểm cuối = trung bình cộng điểm thành phần, làm tròn theo A4; kết quả ĐẠT/KHÔNG ĐẠT theo ngưỡng cấu hình. Cho xem bảng điểm từng thành viên + nhận xét trước khi chốt. Chưa công bố thì được tổng hợp lại nếu điểm có thay đổi; đã công bố thì **khóa**.
- **Công bố** (Trưởng khoa theo A2, hoặc admin): chuyển kết quả sang `published`, ghi người/thời điểm; từ lúc này SV mới thấy. Có thể công bố từng đề tài hoặc cả hội đồng.
- **SV xem kết quả:** chỉ kết quả **đã công bố** của nhóm mình (điểm cuối, kết quả, nhận xét tổng hợp); không thấy điểm thành phần của từng GV nếu không được cấp quyền (mặc định ẩn).
- **Tra cứu theo đợt (rubric):** trang "Kết quả của tôi" có bộ chọn đợt; danh sách đợt **chỉ gồm các đợt mà SV có nhóm tham gia** (lấy từ `group_members.period_id` theo `student_id` của người đăng nhập, **không tin tham số từ client**). Truy cập `periodId` của đợt SV không tham gia, hoặc kết quả chưa công bố, hoặc của nhóm khác → 403/404, kể cả khi đoán URL hay gọi `GET /api/my/results?periodId=`.
- **Gửi email khi công bố (rubric):** khi Trưởng khoa công bố, hệ thống gửi **email riêng cho từng SV trong nhóm** (kể cả thành viên không phải nhóm trưởng) gồm: họ tên, tên đợt, tên đề tài, điểm cuối, kết quả ĐẠT/KHÔNG ĐẠT, nhận xét tổng hợp, đường dẫn đăng nhập. Chi tiết kỹ thuật ở 7.10. Công bố từng đề tài hoặc cả hội đồng đều gửi cho đúng các SV liên quan, mỗi kết quả chỉ gửi một lần (trừ khi bấm "Gửi lại").

### 5.9 Thông báo chính thức
- Trưởng khoa/admin (`NOTICE_MANAGE`) soạn thông báo: tiêu đề, nội dung, **loại: Nhà trường / Khoa**, đối tượng nhận (chọn vai trò hoặc tất cả), trạng thái `DRAFT`/`PUBLISHED`. Sửa/xóa khi `DRAFT`; thông báo đã đăng chỉ được thu hồi (ẩn), không xóa hẳn.
- Người dùng chỉ thấy thông báo `PUBLISHED` dành cho vai trò của mình; danh sách có lọc theo loại và phân trang; thông báo mới nhất hiển thị ở Tổng quan.

### 5.10 Tổng quan (dashboard) theo vai trò
- **Mọi vai trò:** thông báo mới; "việc cần làm của tôi" (xem bảng dưới); thanh **tiến trình quy trình** của đợt hiện hành (Tạo đợt → Đề xuất & duyệt đề tài → SV đăng ký → Thực hiện & nộp báo cáo → Phản biện & chấm → Công bố).
- Trưởng khoa/admin: số liệu tổng (người dùng, SV, GV, đề tài theo trạng thái, nhóm, báo cáo, kết quả đã công bố), đợt đang mở.
- Bảng "việc cần làm":

| Vai trò | Việc cần làm hiển thị |
|---------|----------------------|
| Trưởng khoa | Đề tài chờ duyệt; đề tài chưa đủ GVHD; đề tài chưa phân công chấm; kết quả chờ công bố; hội đồng chưa hợp lệ; email gửi lỗi |
| GV là chủ tịch của một hội đồng | Đề tài đủ điểm chờ tổng hợp (ngoài các việc của Giảng viên) |
| Giảng viên | Đăng ký chờ xác nhận; đề tài cần chấm; hạn chót sắp tới |
| Sinh viên | Trạng thái nhóm/đăng ký; báo cáo đã nộp; kết quả đã công bố |

## 6. QUY TẮC TOÀN VẸN DỮ LIỆU TỔNG HỢP (để rà lại sau mỗi giai đoạn)

1. Mỗi SV ≤ 1 nhóm **trong mỗi đợt**; mỗi nhóm có số SV trong khoảng `min-size`..`max-size` (mặc định 1–3, A9), đúng 1 nhóm trưởng.
2. Mỗi nhóm ≤ 1 đăng ký hoạt động; mỗi đề tài ≤ 1 nhóm được chấp thuận.
3. Mỗi đề tài có 1–2 GVHD, không trùng; đề tài thuộc đúng 1 bộ môn.
4. Hội đồng dùng được khi có 3–5 GV, 1 chủ tịch, 1 thư ký.
5. Người chấm ∈ hội đồng và ∉ GVHD của đề tài; mỗi phân công ≤ 1 điểm.
6. Chỉ tổng hợp khi đủ điểm; điểm cuối = trung bình cộng; đã công bố thì khóa.
7. Hạn chót GVPB chỉ có ở TLCN/KLTN; ngày báo cáo hội đồng chỉ có ở KLTN.
8. Mọi thao tác theo giai đoạn chỉ hợp lệ trong khung thời gian của đợt `PUBLISHED`.
9. Mật khẩu ở mọi nơi (DB, seed, log) đều là BCrypt; không có plain-text.
10. SV chỉ tra cứu được kết quả đã công bố của đợt mình tham gia.
11. Công bố kết quả → mỗi SV của nhóm có đúng một bản ghi `email_logs` cho kết quả đó.

## 7. YÊU CẦU KỸ THUẬT

7.1 **Kiến trúc:** `controller` (xử lý web, nhận form, gọi service) → `service` (nghiệp vụ, `@Transactional`) → `repository` (Spring Data JPA). Controller không chứa nghiệp vụ. Dùng DTO/form object cho dữ liệu nhập, không bind trực tiếp vào entity (tránh mass assignment). **Dependency Injection/IoC:** chỉ dùng constructor injection (các field `final`, `@RequiredArgsConstructor` hoặc constructor tường minh), không `@Autowired` trên field; cấu hình đọc qua `@ConfigurationProperties`. Ghi lại trong `docs/RUBRIC_MAPPING.md` mỗi kỹ thuật Spring đang dùng (Data JPA, Security, Validation, Thymeleaf, DI/IoC, REST, Mail) kèm ít nhất một lớp/phương thức minh họa.
7.2 **Validation:** Bean Validation trên form + kiểm tra nghiệp vụ ở service; hiển thị lỗi cạnh từng trường; giữ lại dữ liệu đã nhập khi lỗi.
7.3 **Hiệu năng:** tránh N+1 (dùng `@EntityGraph`/fetch join), phân trang cho mọi danh sách dài, `open-in-view=false`.
7.4 **Cấu hình:** `application.properties` đọc `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` từ biến môi trường; `ddl-auto=none` hoặc `validate` (schema do `HT_QLDT.sql` quản lý); mức log `INFO`/`WARN` (không tắt hẳn log); `app.storage.dir`, `app.passing-score`, `app.group.min-size`, `app.group.max-size`, `app.security.*`, `app.mail.from`, `app.mail.enabled` và `MAIL_HOST`/`MAIL_PORT`/`MAIL_USERNAME`/`MAIL_PASSWORD` (từ biến môi trường) đều khai báo rõ; **không** lưu mật khẩu/secret thật trong repo.
7.5 **Mã nguồn:** đặt tên nhất quán, không sao chép-dán logic, hằng số trạng thái dùng `enum`/hằng chung thay cho chuỗi rải rác; Lombok chỉ dùng như hiện có.
7.6 **Múi giờ/định dạng:** thời gian lưu và hiển thị theo `Asia/Ho_Chi_Minh`, định dạng `dd/MM/yyyy HH:mm`; điểm hiển thị dạng `8,17`.
7.7 **Nhật ký:** ghi `audit_logs` cho các hành động quan trọng ở mục 4.
7.8 **Xử lý lỗi:** `@ControllerAdvice` bắt `IllegalArgumentException`/validation/`AccessDenied`/`DataIntegrityViolation` và trả thông báo tiếng Việt thân thiện qua flash message.
7.9 **Dữ liệu mẫu (profile `demo`):** 1 admin, 1 trưởng khoa, ≥ 8 GV (trong đó 2 người được chỉ định làm chủ tịch hội đồng, vẫn là vai trò `GIANG_VIEN`), ≥ 6 GV thuộc ≥ 3 bộ môn, ≥ 12 SV; 1 đợt TLCN và 1 đợt KLTN ở các giai đoạn khác nhau; vài đề tài ở đủ trạng thái; nhóm/đăng ký/báo cáo/hội đồng/điểm đủ để thấy **mọi trạng thái** trên giao diện. Mật khẩu demo chỉ ghi trong README, **trong DB/seed luôn là chuỗi BCrypt** (sinh bằng `PasswordEncoder` trong seeder hoặc băm sẵn). **Mọi mốc thời gian của đợt tính tương đối theo lúc khởi động** (ví dụ `now − 2 ngày`) để đợt luôn đúng giai đoạn khi demo; **tệp báo cáo mẫu phải được tạo thật** trong `app.storage.dir` khi seed; có ≥ 1 SV tham gia **2 đợt khác nhau** để thử tra cứu theo đợt; email SV demo trỏ về hộp thư thử (Mailpit/Mailtrap), không dùng email thật.

7.10 **Gửi email (rubric bắt buộc):**
- Thêm `spring-boot-starter-mail` vào `pom.xml` (thư viện duy nhất được thêm; ghi lý do vào báo cáo).
- Cấu hình `spring.mail.*` từ biến môi trường `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`; `app.mail.from`; `app.mail.enabled` (`false` → không gửi thật, chỉ ghi log và `email_logs` trạng thái `SKIPPED`). **Không** để secret thật trong repo. README hướng dẫn dùng Mailpit/MailHog/Mailtrap cục bộ hoặc Gmail App Password.
- `EmailService` gửi qua `JavaMailSender`, UTF-8, nội dung tiếng Việt; **mỗi SV một email** (không CC để lộ địa chỉ); không đưa điểm thành phần của từng GV vào email.
- Gửi **sau khi transaction công bố commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)` kết hợp `@Async`, bật `@EnableAsync`, hoặc cơ chế tương đương). Lỗi gửi **không** làm hỏng việc công bố; ghi `email_logs` `SENT`/`FAILED`/`SKIPPED` kèm lý do.
- Trang "Nhật ký email" cho Trưởng khoa: lọc theo đợt/trạng thái, nút "Gửi lại" cho email `FAILED` (có xác nhận); không gửi trùng email đã `SENT`.
- Test bằng Mockito (`JavaMailSender` mock): công bố kết quả nhóm 3 SV → đúng 3 email tới đúng 3 địa chỉ; một SV lỗi/thiếu email thì các SV khác vẫn nhận; chưa công bố thì không gửi; `enabled=false` thì `SKIPPED`.

7.11 **Tài liệu đối chiếu rubric:** tạo `docs/RUBRIC_MAPPING.md` gồm (a) bảng ở mục 0.1 với đường dẫn mã/test/ảnh màn hình thật cho từng dòng; (b) bảng kỹ thuật Spring đã dùng; (c) danh sách chức năng CRUD kèm URL màn hình.

## 8. BỐ CỤC GIAO DIỆN (làm cho dễ nhìn, nhất quán)

### 8.1 Hiện trạng cần khắc phục (đã khảo sát, hãy xác nhận lại)
- Có **3 kiểu khung trang khác nhau** (`app` + `sidebar` ở dashboard; `admin-layout` ở topics/registrations/evaluations/councils/reports; `page-shell` ở nhóm trang tài khoản/vai trò/phân quyền/thông báo/đợt đăng ký), và `login` riêng.
- Menu bên trái **bị chép lặp ở từng trang và khác nhau** (ví dụ trang Đề tài chỉ có 4 liên kết cố định, không theo quyền), không dùng `th:fragment` nào.
- CSS tách thành nhiều tệp theo từng nhóm trang (`theme.css`, `dashboard.css`, `admin-module.css`, `security-module.css`, `registration-periods.css`, `registration-period-form.css`) với kiểu trùng lặp; chưa có thư mục `static/js`.

### 8.2 Khung trang thống nhất
- Một layout duy nhất bằng Thymeleaf fragments: `templates/fragments/layout.html` (head, sidebar theo quyền, topbar, vùng thông báo flash, phân trang, badge trạng thái, hộp xác nhận). Mọi trang (trừ đăng nhập và trang lỗi) dùng chung layout này; xóa mã khung trùng lặp.
- Gộp CSS còn: `theme.css` (biến màu, chữ, khoảng cách), `layout.css` (khung, sidebar, topbar, lưới), `components.css` (bảng, form, nút, badge, card, thông báo, phân trang). Tên lớp CSS nhất quán, mỗi trang không tự định nghĩa lại thành phần chung.

### 8.3 Menu điều hướng nhóm theo quy trình (chỉ hiện mục người dùng có quyền)
1. **Tổng quan**
2. **Quy trình đề tài:** Đợt đăng ký · Đề tài · Nhóm & đăng ký · Báo cáo
3. **Phản biện & chấm điểm:** Hội đồng · Phân công chấm · Nhập điểm · Tổng hợp & công bố · Kết quả của tôi
4. **Thông tin:** Thông báo
5. **Danh mục & Quản trị:** Bộ môn · Giảng viên · Sinh viên · Tài khoản · Vai trò · Phân quyền · Nhật ký email
Mục đang xem được tô sáng; trên màn hình nhỏ menu thu thành nút ☰.

### 8.4 Mẫu trang chuẩn (mọi trang danh sách/biểu mẫu theo cùng một khung)
- **Đầu trang:** đường dẫn (breadcrumb), tiêu đề, một dòng mô tả ngắn, **một nút hành động chính** ở bên phải.
- **Thanh lọc/tìm kiếm** phía trên bảng; bảng có tiêu đề cột cố định, hàng xen kẽ, cột thao tác gọn bên phải, **phân trang** (10/20/50 dòng).
- **Badge trạng thái có màu thống nhất:** nháp/chờ (xám–vàng), đã duyệt/đạt (xanh), từ chối/quá hạn (đỏ), đang diễn ra (xanh dương).
- **Trạng thái rỗng** có câu hướng dẫn ("Chưa có đề tài. Bấm Đề xuất đề tài để bắt đầu").
- **Biểu mẫu:** nhóm trường theo khối có tiêu đề, nhãn rõ ràng, chú thích dưới trường, lỗi hiện ngay cạnh trường, nút Lưu/Hủy cố định cuối biểu mẫu.
- Thao tác nguy hiểm (xóa, từ chối, công bố, khóa tài khoản) có **hộp xác nhận**; thông báo thành công/lỗi dạng flash ở đầu trang, tự biến mất.
- Trang chi tiết có **thanh tiến trình trạng thái** (ví dụ đề tài: Chờ duyệt → Đã duyệt → Có nhóm thực hiện → Đã nộp → Đã chấm → Đã công bố).

### 8.5 Trang Tổng quan
Đầu trang: lời chào + vai trò; dải **tiến trình quy trình** của đợt hiện hành; khối **"Việc cần làm"** theo vai trò (mục 5.10) ở vị trí nổi bật; bên dưới là thẻ số liệu, bảng đợt đang mở, thông báo mới. Không đặt quá nhiều khối ngang hàng; ưu tiên một cột chính + một cột phụ.

### 8.6 Chất lượng giao diện (kiểm tra bằng Chrome DevTools)
- Bố cục tốt ở **1366 px, 768 px, 375 px** (không cuộn ngang trang; bảng rộng cuộn ngang trong khung riêng).
- Không có lỗi/cảnh báo ở **Console** và không có request 404 ở **Network** khi duyệt từng trang.
- Có nhãn cho input, tương phản chữ đủ, điều hướng được bằng bàn phím, `:focus-visible` rõ ràng, `lang="vi"`; thử Lighthouse (Accessibility) và ghi kết quả vào báo cáo.
- JavaScript thuần, chỉ cho: ẩn/hiện trường theo loại đợt, hộp xác nhận, tìm kiếm nhanh trong danh sách SV khi lập nhóm, menu ☰, tự ẩn flash.

## 9. KẾ HOẠCH TRIỂN KHAI THEO GIAI ĐOẠN

| GĐ | Nội dung | Kết quả kiểm tra |
|----|----------|------------------|
| 0 | Xác minh công nghệ (mục 1.1), chạy `./mvnw test`, đọc `docs/` và mã hiện có, lập bảng "dự kiến ↔ thực tế" | Báo cáo xác minh |
| 1 | Nền tảng: schema `HT_QLDT.sql` mới, `application.properties`, bảo mật, layout/fragments + CSS hợp nhất, trang lỗi, dữ liệu mẫu demo, README | Khởi động được trên MySQL trống; đăng nhập các vai trò; khung trang thống nhất |
| 2 | Tài khoản, vai trò, phân quyền, đổi mật khẩu, nhật ký, **CRUD bộ môn/GV/SV (5.1b)** | Test quyền theo vai trò; test xóa bị chặn khi còn ràng buộc; quét `users` không có plain-text |
| 3 | Đợt đăng ký | Test khung thời gian, điều kiện loại đợt |
| 4 | Đề tài (đề xuất, 1–2 GVHD, duyệt, công bố, lọc theo bộ môn) | Test GVHD, trạng thái, quyền |
| 5 | Nhóm và đăng ký đề tài, GV chấp thuận | Test ≤ 3 SV, 1 nhóm/SV, 1 đề tài/nhóm, đua tranh |
| 6 | Nộp báo cáo | Test nhóm trưởng, tệp, phiên bản, quyền tải |
| 7 | Hội đồng phản biện | Test 3–5 GV, 1 chủ tịch, 1 thư ký |
| 8 | Phân công chấm, nhập điểm, hạn chót | Test không chấm đề tài mình hướng dẫn, quá hạn |
| 9 | Tổng hợp và công bố, **gửi email từng SV**, SV tra cứu kết quả theo đợt | Test trung bình cộng, khóa sau công bố, đúng số email, đợt khác bị 403/404 |
| 10 | Thông báo chính thức | Test loại, đối tượng nhận |
| 11 | Dashboard theo vai trò, hoàn thiện bố cục | DevTools: 3 độ rộng, Console sạch, Lighthouse |
| 12 | Rà soát toàn diện, cập nhật `docs/`, **`RUBRIC_MAPPING.md`**, Postman collection, README | Toàn bộ nghiệm thu mục 11 |

Mỗi giai đoạn: viết test → code → chạy `./mvnw test` → kiểm tra giao diện → mới sang giai đoạn tiếp. Mỗi giai đoạn một commit riêng nếu tôi cho phép commit.

## 10. KIỂM THỬ

- **JUnit + Spring Boot Test** (Mockito cho service; `@DataJpaTest`/`@SpringBootTest` với cơ sở dữ liệu test phù hợp cho truy vấn và ràng buộc; `spring-security-test` với `@WithMockUser(authorities=...)` cho phân quyền). Mỗi quy tắc ở mục 5–6 phải có tối thiểu **một ca thành công và một ca bị từ chối**. Trọng tâm:
  - Đợt: sai thứ tự thời gian bị từ chối; hạn chót GVPB với MON_HOC/NCKH bị từ chối; ngày báo cáo hội đồng với loại khác KLTN bị từ chối; thao tác ngoài khung thời gian bị chặn.
  - Đề tài: 1 và 2 GVHD hợp lệ, 0 và 3 GVHD bị từ chối, trùng GVHD bị từ chối; mã trùng bị từ chối.
  - Nhóm/đăng ký: nhóm đủ `max-size` SV hợp lệ, vượt `max-size` bị từ chối (đổi cấu hình sang 5 thì test vẫn đúng); SV đã vào nhóm A ở đợt X không vào được nhóm B cùng đợt nhưng **vẫn lập/tham gia được nhóm ở đợt Y**; SV (kể cả nhóm trưởng) đã thuộc nhóm khác bị từ chối; chỉ nhóm trưởng đăng ký; nhóm đăng ký thứ hai bị từ chối; đề tài đã có nhóm được chấp thuận thì nhóm khác bị từ chối; chấp thuận gán `topic_id` và từ chối các đăng ký còn lại; chỉ GVHD (cả hai) được xác nhận; hai request đồng thời không tạo hai nhóm cho một SV.
  - Báo cáo: chỉ nhóm trưởng; chưa được chấp thuận thì không nộp; tệp sai định dạng/quá lớn bị từ chối; người ngoài quyền không tải được.
  - Hội đồng/phân công/chấm: thiếu/thừa thành viên, thiếu chủ tịch/thư ký bị từ chối; người chấm không thuộc hội đồng bị từ chối; GVHD (cả GVHD thứ hai) bị từ chối khi chấm đề tài mình hướng dẫn; người không phải Trưởng khoa/admin bị từ chối phân công; GV không phải chủ tịch của đúng hội đồng đó bị từ chối tổng hợp; điểm ngoài 0–10 bị từ chối; chấm trùng bị từ chối; quá hạn chót bị từ chối.
  - Kết quả: thiếu điểm một người chấm thì không tổng hợp; điểm cuối bằng trung bình cộng (ví dụ 8,0 · 7,5 · 9,0 → 8,17); đã công bố thì khóa; SV chỉ thấy kết quả đã công bố của nhóm mình, không thấy của nhóm khác.
  - Tra cứu theo đợt: SV có nhóm ở 2 đợt thấy đúng kết quả từng đợt; gọi `periodId` đợt không tham gia → 403/404 (cả trang web lẫn `/api/my/results`).
  - Email: xem 7.10 (đúng số email, đúng người nhận, lỗi một người không chặn người khác, không gửi khi chưa công bố, gửi lại không trùng).
  - CRUD: mỗi đối tượng (đề tài, GV, SV, đợt, bộ môn, thông báo) có test tạo/sửa/xóa thành công và xóa bị chặn khi còn ràng buộc; Trưởng khoa thêm/đổi GVHD giữ đúng 1–2 GVHD.
  - Mật khẩu: test quét bảng `users` sau khi seed demo, mọi `password_hash` bắt đầu bằng `{bcrypt}` hoặc `$2`, không có `{noop}`.
  - Bảo mật: người chưa đăng nhập bị chuyển về `/login`; mỗi vai trò chỉ gọi được endpoint phù hợp; POST thiếu CSRF bị từ chối; tài khoản khóa không đăng nhập được; khóa tạm sau nhiều lần sai.
  - Schema: ứng dụng khởi động và ánh xạ đủ mọi entity trên schema từ `HT_QLDT.sql` (phát hiện lệch cột).
- **Postman:** collection cho các endpoint REST hiện có (mục 1.1).
- **Kiểm thử luồng đầy đủ bằng tay** theo mục 11, có ghi lại các bước.

## 11. TIÊU CHÍ NGHIỆM THU

1. Công nghệ đúng bảng ở mục 1; có báo cáo xác minh; không đổi framework/database, không thêm thư viện trùng chức năng.
2. Ứng dụng khởi động trên MySQL trống nạp `HT_QLDT.sql`; không lỗi "Unknown column"/`NOT NULL`; mở được trong MySQL Workbench và vẽ được sơ đồ EER.
3. Đi hết luồng bằng tài khoản demo: tạo đợt → GV đề xuất → duyệt và công bố danh sách → SV lập nhóm và đăng ký → GVHD chấp thuận → nhóm trưởng nộp báo cáo → lập hội đồng và phân công chấm → chấm điểm → tổng hợp → công bố (hệ thống gửi email cho từng SV trong nhóm) → SV tra cứu kết quả theo đợt; đăng thông báo nhà trường/khoa.
4. Mọi quy tắc ở mục 2 và mục 6 được thực thi ở backend và có test chứng minh; `./mvnw test` xanh; `./mvnw -DskipTests package` thành công.
5. Không còn endpoint ghi dữ liệu nào thiếu `@PreAuthorize` và kiểm tra quyền sở hữu; không có mật khẩu/secret thật trong repo.
6. Giao diện dùng một layout duy nhất, menu theo quy trình và theo quyền, mẫu trang nhất quán; đạt các kiểm tra mục 8.6 trong Chrome DevTools.
7. `README.md` (tiếng Việt) và `docs/` được cập nhật: yêu cầu môi trường (JDK 25, MySQL), cách nạp script bằng Workbench, biến môi trường, tài khoản demo, cách chạy ứng dụng và test, sơ đồ luồng và bảng quyền.
8. Có `docs/RUBRIC_MAPPING.md`; mỗi dòng rubric ở mục 0.1 đều có bằng chứng; không có dòng nào để trống.
9. Mọi mật khẩu trong DB sau khi nạp dữ liệu mẫu đều là BCrypt; đủ CRUD cho đề tài, GV, SV, đợt, bộ môn, thông báo; đăng nhập/đăng xuất hoạt động.

## 12. RÀNG BUỘC KHI THỰC HIỆN

- Không chuyển đổi framework, không thay database, không thêm thư viện trùng chức năng; thay đổi công nghệ nào cũng phải hỏi tôi trước.
- Không xóa tài liệu/mã mà chưa nêu lý do; không `git push`, không `git reset --hard`, không ghi đè lịch sử.
- Nếu gặp mâu thuẫn giữa đề bài và mã hiện có, **ưu tiên đề bài**, và ghi lại trong báo cáo.
- Nếu một yêu cầu mơ hồ, dùng giả định ở mục 2.1, làm cho dễ đổi, và đưa vào mục "Cần tôi xác nhận".

## 13. ĐỊNH DẠNG BÁO CÁO CUỐI CÙNG (tiếng Việt, ngắn gọn)

1. Bảng xác minh công nghệ (dự kiến ↔ thực tế ↔ kết luận) và các thư viện đã thêm (nếu có, kèm lý do).
2. Bảng 13 giai đoạn: trạng thái, các tệp chính đã thêm/sửa/xóa.
3. Kết quả `./mvnw test` (số test, pass/fail), kết quả kiểm tra DevTools (3 độ rộng, Console, Lighthouse).
4. Mục **"Cần tôi xác nhận"**: các giả định A1–A14 (đặc biệt **A9 kích thước nhóm 3 hay 3–5**) và điểm mơ hồ khác, kèm phương án đã chọn tạm thời.
5. Bảng đối chiếu rubric (mục 0.1): từng dòng ↔ trạng thái ↔ bằng chứng.
6. Rủi ro và việc còn lại.
