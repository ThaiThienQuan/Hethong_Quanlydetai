# TỔNG QUAN LUỒNG DỮ LIỆU & QUY TRÌNH NGHIỆP VỤ
## Hệ thống Quản lý Đề tài Đại học — Khoa CNTT

Tài liệu này gộp **hai** phần mà người đọc thường tách rời nhưng thực tế là một:
1. **Luồng dữ liệu** (data flow) — dữ liệu đi từ đâu, qua bước nào, nằm ở bảng nào, ai được quyền chạm vào.
2. **Quy trình nghiệp vụ** (business workflow) — trạng thái mỗi đối tượng nghiệp vụ đi qua những trạng thái nào, ai làm gì, quy tắc chặn là gì.

Mọi nội dung dưới đây được đối chiếu trực tiếp với mã nguồn hiện tại
(`src/main/java/vn/edu/demo/topicmanagement`), schema `database/HT_QLDT.sql`
và dữ liệu demo `database/HT_QLDT_02_du_lieu_demo_day_du.sql`.

---

## 0. Tổng quan hệ thống

| Hạng mục | Chi tiết |
|---|---|
| Framework | Spring Boot 4.x (Web MVC + Thymeleaf, không phải SPA) |
| CSDL | MySQL 8.0.16+, InnoDB, `utf8mb4`, tên DB `student_topic_management` |
| ORM | JPA/Hibernate, `ddl-auto=validate` (schema do SQL quản lý, app không tự tạo bảng) |
| Bảo mật | Spring Security + session, CSRF bật, `@PreAuthorize("hasAuthority('...')")` |
| Lưu trữ tệp | `app.storage.dir` (mặc định `./storage/reports`), **ngoài** `static/` |
| Múi giờ | `Asia/Ho_Chi_Minh` |
| Cấu hình nghiệp vụ | `application.properties` → `AppProperties` |

Kiến trúc phân tầng:

```
Controller (34 lớp, theo vai trò: admin/ head/ lecturer/ student)
        ↓
Service (24 lớp — nơi nằm TOÀN BỘ quy tắc nghiệp vụ)
        ↓
Repository (Spring Data JPA)
        ↓
Entity (33 lớp)  →  MySQL (30 bảng + 2 view)
```

> **Điểm quan trọng:** Các ràng buộc nghiệp vụ được chia làm 3 tầng:
> - **DB (CHECK/UNIQUE/FK)** — ràng buộc *luôn đúng*, không thể vượt qua.
> - **Service (Java)** — ràng buộc *phụ thuộc ngữ cảnh* (vd: GVHD không chấm đề tài mình hướng dẫn, đã quá hạn thì Admin vẫn được nhập điểm).
> - **UI (Thymeleaf)** — chỉ ẩn/hiện nút cho hợp lý, **không phải** lớp bảo vệ.

---

## 1. Mô hình dữ liệu tổng thể

### 1.1. Bản đồ quan hệ (rút gọn)

```
users ──┬── user_roles ──── roles ──── role_permissions ──── permissions
        ├── students ──┐
        └── lecturers ─┤
                       │
departments ───────────┤
                       ▼
registration_periods  (ĐỢT — trục thời gian + trục nghiệp vụ của toàn hệ thống)
    │
    ├── topics ──┬── topic_supervisors (1–2 GVHD)
    │            └── topic_registrations ──── student_groups ──── group_members
    │                                          │
    │                                          └── reports (nhiều phiên bản)
    ├── councils ──┬── council_members (3–5 GV: 1 CH, 1 TK)
    │              ├── council_topics
    │              └── review_assignments ──── evaluations
    │                                           │
    │                                           └── review_results
    │                                                    │
    │                                                    └── email_logs (mỗi SV 1 dòng)
    └── notifications ──── notification_target_roles
audit_logs (ghi lại mọi thao tác nhạy cảm)
```

### 1.2. Bảng theo nhóm nghiệp vụ

| Nhóm | Bảng | Ý nghĩa |
|---|---|---|
| **Tài khoản & phân quyền** | `users`, `roles`, `permissions`, `user_roles`, `role_permissions` | RBAC 5 bảng |
| **Danh mục** | `departments` | Bộ môn |
| **Hồ sơ chuyên viên** | `students`, `lecturers` | 1–1 với `users` qua `user_id` |
| **Đợt đăng ký** | `registration_periods` | Trung tâm nghiệp vụ |
| **Đề tài** | `topics`, `topic_supervisors` | GVHD 1–2 người |
| **Nhóm & đăng ký** | `student_groups`, `group_members`, `topic_registrations` | SV đăng ký qua nhóm |
| **Báo cáo** | `reports` | Nhiều phiên bản, tệp ngoài DB |
| **Phản biện** | `councils`, `council_members`, `council_topics`, `review_assignments`, `evaluations` | Hội đồng + chấm điểm |
| **Kết quả** | `review_results` | 1 đề tài ↔ 1 kết quả |
| **Thông báo & nhật ký** | `notifications`, `notification_target_roles`, `audit_logs`, `email_logs` | Vận hành & đối chiếu |

### 1.3. Hai cơ chế "sinh khóa" bằng GENERATED COLUMN

MySQL không có `partial index`, nên dự án dùng **cột sinh tự động (STORED)** để ép ràng buộc "duy nhất" có điều kiện:

```248:263:database/HT_QLDT.sql
    active_group_id  BIGINT GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING', 'APPROVED') THEN group_id ELSE NULL END) STORED,
    approved_topic_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'APPROVED' THEN topic_id ELSE NULL END) STORED,
    PRIMARY KEY (id),
    ...
    CONSTRAINT uq_registrations_one_active_per_group UNIQUE (active_group_id),
    CONSTRAINT uq_registrations_one_approved_per_topic UNIQUE (approved_topic_id),
```

- `active_group_id` → một nhóm chỉ có **1 đăng ký đang hiệu lực** (PENDING hoặc APPROVED).
- `approved_topic_id` → một đề tài chỉ có **1 nhóm được chấp thuận**.

Tương tự:

| Bảng | Cột sinh | Ràng buộc |
|---|---|---|
| `group_members` | `leader_group_id` | 1 nhóm chỉ có 1 trưởng (`uq_group_members_one_leader`) |
| `council_members` | `chairman_council_id` | 1 hội đồng chỉ có 1 chủ tịch |
| `council_members` | `secretary_council_id` | 1 hội đồng chỉ có 1 thư ký |

Cột này **không map trong JPA** (insertable=false, updatable=false) — nó thuộc về tầng DB.

### 1.4. FK kép — ràng buộc liên bảng mạnh

```344:359:database/HT_QLDT.sql
CREATE TABLE review_assignments (
    ...
    CONSTRAINT fk_assignments_member      FOREIGN KEY (council_id, lecturer_id) REFERENCES council_members (council_id, lecturer_id),
    CONSTRAINT fk_assignments_council_topic FOREIGN KEY (council_id, topic_id)  REFERENCES council_topics (council_id, topic_id),
```

FK kép `(council_id, lecturer_id)` → đảm bảo **người chấm phải là thành viên của đúng hội đồng đó**.
FK kép `(council_id, topic_id)` → đảm bảo **đề tài phải thuộc đúng hội đồng đó**.
Việc này chỉ làm được vì `council_members` và `council_topics` đều có UNIQUE trên cặp cột tương ứng.

Tương tự, `group_members` có FK kép `(group_id, period_id)` → `student_groups (id, period_id)`, đảm bảo thành viên và nhóm cùng thuộc một đợt.

### 1.5. View hỗ trợ

```475:497:database/HT_QLDT.sql
CREATE VIEW v_council_validity AS
SELECT c.id AS council_id,
       COUNT(m.id) AS member_count,
       ...
       (COUNT(m.id) BETWEEN 3 AND 5
        AND COALESCE(SUM(m.role = 'CHAIRMAN'), 0) = 1
        AND COALESCE(SUM(m.role = 'SECRETARY'), 0) = 1) AS is_valid
FROM councils c
LEFT JOIN council_members m ON m.council_id = c.id
GROUP BY c.id;
```

- `v_council_validity` — đánh giá hội đồng có hợp lệ không (3–5 GV, đúng 1 CH, đúng 1 TK).
- `v_topic_score_summary` — đối chiếu điểm trung bình từ `evaluations` với `review_results`.

---

## 2. Luồng dữ liệu — chi tiết theo đối tượng

### 2.1. Luồng của ĐỢT ĐĂNG KÝ (`registration_periods`)

Đợt là trục xương sống: mọi đề tài, nhóm, hội đồng đều thuộc một đợt. Đợt có **2 giai đoạn tuần tự** + 2 mốc phụ.

```
   lecturer_start_at ──── lecturer_end_at ──── student_start_at ──── student_end_at
   (GV đề xuất)             (hết GĐ1)            (SV lập nhóm/đăng ký)   (hết GĐ2)
                                                                              │
                                        review_deadline ──────────────────┘   (chỉ TLCN/KLTN)
                                                │
                                        council_report_date ────────────────┘   (chỉ KLTN)
```

Ràng buộc DB:

| Constraint | Ý nghĩa |
|---|---|
| `chk_period_lecturer_time` | `lecturer_start_at < lecturer_end_at` |
| `chk_period_student_time` | `student_start_at < student_end_at` |
| `chk_period_two_phases` | `student_start_at >= lecturer_end_at` (2 giai đoạn không chồng lấn) |
| `chk_period_review_type` | `review_deadline` chỉ có ở TLCN/KLTN |
| `chk_period_council_type` | `council_report_date` chỉ có ở KLTN |
| `chk_period_review_after_student` | `review_deadline > student_end_at` |
| `chk_period_council_after_review` | `council_report_date > review_deadline` |

**Luồng trạng thái đợt:**

```
DRAFT ──publish──► PUBLISHED ──close──► CLOSED
  │                    │
  │                    └── (đã có đề tài thì không sửa/xóa được)
  └── xóa được (chỉ khi chưa có đề tài)
```

Điểm kiểm tra thời gian (service layer):

```java
// RegistrationPeriodService
requireLecturerPhaseOpen(period)  // phải PUBLISHED + trong [lecturer_start_at, lecturer_end_at]
requireStudentPhaseOpen(period)   // phải PUBLISHED + trong [student_start_at, student_end_at]
```

Ngoài ra có `checkRules(form)` trả về `Map<String,String>` để hiển thị lỗi ngay trên form — pattern này được dùng lại ở `TopicService.checkRules`.

### 2.2. Luồng của ĐỀ TÀI (`topics`)

**Luồng trạng thái:**

```
        GV đề xuất                        Trưởng khoa
           │                                   │
           ▼                                   ▼
     ┌───────────┐  approve   ┌───────────┐  publish   ┌──────────────┐
     │  PENDING  │────────────►│ APPROVED  │────────────►│  APPROVED    │
     └─────┬─────┘             └───────────┘             │  + published=1│
           │ reject (bắt buộc có lý do)                   └──────────────┘
           ▼
     ┌───────────┐
     │ REJECTED  │──► GV sửa & nộp lại ──► PENDING
     └───────────┘
```

Cột `published` là cờ boolean tách khỏi `status`:

```162:184:database/HT_QLDT.sql
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reject_reason          VARCHAR(500) NULL,
    approved_by            BIGINT       NULL,
    approved_at            DATETIME     NULL,
    published              TINYINT(1)   NOT NULL DEFAULT 0,
    ...
    CONSTRAINT chk_topics_published   CHECK (published = 0 OR status = 'APPROVED')
```

→ DB đảm bảo **không thể công bố một đề tài chưa được duyệt**.

**Luồng GVHD (`topic_supervisors`):**

- `supervisor_order` ∈ {1, 2} → tối đa 2 GVHD, thứ tự xác định GVHD chính.
- GV đề xuất tự động là GVHD chính (`order = 1`).
- Trưởng khoa có thể bổ sung GVHD thứ hai qua `SUPERVISOR_ASSIGN` — nhưng **bị chặn** nếu đề tài đã có phân công phản biện (`"Đề tài đã có phân công phản biện nên không thể đổi GVHD."`).

### 2.3. Luồng của NHÓM & ĐĂNG KÝ

**`student_groups`** (nhóm sinh viên):

```
DRAFT ──đăng ký đề tài──► REGISTERED ──GV duyệt──► APPROVED ──nộp báo cáo──► SUBMITTED ──có kết quả──► COMPLETED
```

Ràng buộc DB:
- `uq_groups_topic UNIQUE (topic_id)` → 1 nhóm ↔ 1 đề tài.
- `uq_groups_code UNIQUE (group_code)`.

**`group_members`** — quy tắc then chốt:

| Quy tắc | Nơi kiểm tra |
|---|---|
| SV chỉ ở **1 nhóm / 1 đợt** | DB: `uq_group_members_student_period UNIQUE (student_id, period_id)` |
| 1 nhóm có **đúng 1 trưởng** | DB: `uq_group_members_one_leader` (qua `leader_group_id`) |
| Nhóm **tối đa 3 thành viên** | Service: `MAX_GROUP_SIZE = 3` (đọc từ `app.group.max-size`) |
| Nhóm **tối thiểu 1** | Service: `app.group.min-size` |

> Nhờ UNIQUE `(student_id, period_id)`, một SV có thể tham gia nhóm ở **nhiều đợt khác nhau** → tra cứu kết quả theo từng đợt. Đây là cơ sở của màn hình "Kết quả của tôi" cho sinh viên.

**`topic_registrations`** — luồng đăng ký:

```
   Nhóm trưởng đăng ký
          │
          ▼
     ┌─────────┐  GV duyệt   ┌─────────┐
     │ PENDING │─────────────►│ APPROVED│──► group.topic_id được gán
     └────┬────┘              └─────────┘
          │ GV từ chối (bắt buộc lý do, ≤500 ký tự)
          ▼
     ┌───────────┐
     │ REJECTED  │──► nhóm được đăng ký lại (tạo bản ghi mới)
     └───────────┘
     ┌────────────┐
     │ CANCELLED  │ (nhóm trưởng tự hủy khi còn PENDING)
     └────────────┘
```

Điểm kiểm tra ở service (`StudentPortalService`):

```java
// Chỉ nhóm trưởng mới được đăng ký
if (!gm.isLeader()) throw new IllegalStateException("Chỉ nhóm trưởng mới được đăng ký đề tài.");
// Đề tài phải đã công bố + đợt đang mở cho SV
if (!topic.isPublished() || period.getStatus() != PeriodStatus.PUBLISHED) ...
// Đề tài phải thuộc đúng đợt của nhóm
if (!period.getId().equals(gm.getGroup().getPeriod().getId())) ...
// Chỉ GVHD của đề tài (hoặc Trưởng khoa) mới được chấp thuận
if (!registrationRepository.existsPendingForSupervisor(reg.getId(), username)) ...
```

> Lưu ý: `getJoinableGroups()` chỉ hiện nhóm `DRAFT` mà **chưa có** đăng ký `PENDING`/`APPROVED` và còn **chỗ trống** (< max-size).

### 2.4. Luồng của BÁO CÁO (`reports`)

```
Nhóm trưởng nộp báo cáo (v1)  ──►  reports: version=1, status=SUBMITTED
        │
        └──► nộp lại (v2)  ──►  reports: version=2, status=SUBMITTED
                                    reports: version=1, status=SUPERSEDED   (giữ lại lịch sử)
```

Ràng buộc DB: `uq_reports_group_version UNIQUE (group_id, version)` → mỗi phiên bản là một dòng riêng.

**Bảo mật tệp:**
- Chỉ nhóm trưởng nộp: `if (!gm.isLeader())` → `"Chỉ nhóm trưởng mới được nộp báo cáo."`
- Chỉ nộp sau khi đăng ký được duyệt: `"Chỉ nộp báo cáo sau khi đăng ký đề tài được duyệt."`
- Định dạng: `pdf`, `doc`, `docx` (service), dung lượng 10 MB (service) / 20 MB (multipart config).
- Tên file lưu = **UUID ngẫu nhiên** + đuôi file; tên gốc được làm sạch và lưu vào `file_name`.
- Thư mục lưu **nằm ngoài** `static/` → không tải trực tiếp được bằng URL; phải qua controller kiểm tra quyền.
- Kiểm tra đường dẫn: `if (!target.startsWith(reportDirectory))` → chống path traversal.

**Quyền tải báo cáo:**
- Sinh viên: chỉ thành viên trong nhóm (`"Chỉ thành viên trong nhóm mới được tải báo cáo."` → 403).
- Giảng viên: chỉ người được phân công (`"Bạn không được phân công xem báo cáo này."` → 403).

### 2.5. Luồng của HỘI ĐỒNG & CHẤM ĐIỂM

**`councils`:**

```
DRAFT ──activate──► READY ──(có kết quả & công bố xong)──► FINISHED
```

Điều kiện `activate()` (service):

```java
// Hội đồng phải có từ MIN_MEMBERS đến MAX_MEMBERS giảng viên
// Hội đồng phải có đúng một chủ tịch và một thư ký
// Hãy xếp ít nhất một đề tài vào hội đồng
```

**`council_members`:** 3–5 GV, đúng 1 `CHAIRMAN`, đúng 1 `SECRETARY`, còn lại `MEMBER`. GV bị khóa tài khoản thì không thêm được (`"Tài khoản giảng viên đang bị khóa."`).

**`council_topics`:** chỉ xếp đề tài thỏa 4 điều kiện:

| Điều kiện | Thông báo lỗi |
|---|---|
| Đăng ký của đề tài đã `APPROVED` | "Chỉ xếp đề tài của đăng ký đã được duyệt." |
| Đề tài thuộc đúng đợt của hội đồng | "Đề tài phải thuộc đúng đợt của hội đồng." |
| Đề tài chưa thuộc hội đồng khác | "Đề tài này đã thuộc một hội đồng khác." |
| Đề tài đã có báo cáo được nộp | "Đề tài chưa có báo cáo được nộp - không đưa vào phản biện." |

DB cũng chặn: `uq_council_topics_topic UNIQUE (topic_id)` → 1 đề tài chỉ ở 1 hội đồng.

**`review_assignments`** (phân công chấm) — 2 ràng buộc:

1. **DB (FK kép):** người chấm phải là thành viên hội đồng; đề tài phải thuộc hội đồng đó.
2. **Service:** người chấm **không được là GVHD** của đề tài đó.

Danh sách ứng viên được lọc sẵn ở UI: `getCoSupervisorCandidates` / danh sách chọn người chấm — **không hiển thị GVHD của đề tài**.

**`evaluations`** (điểm thành phần):

- 1 phân công ↔ 1 điểm: `uq_evaluations_assignment UNIQUE (assignment_id)`.
- Điểm `DECIMAL(4,2)` ∈ [0, 10] (DB CHECK + service).
- Nhận xét **bắt buộc** (`comment VARCHAR(1000) NOT NULL`, service kiểm tra blank).
- Trạng thái: `DRAFT` → `SUBMITTED`. Điểm được tính trung bình chỉ lấy dòng `SUBMITTED`.

**Luồng nhập điểm (`submitScore`):**

```
GV được phân công
   │  1. Kiểm tra phân công thuộc về mình          → 403 nếu không
   │  2. Kiểm tra hội đồng status == READY         → "Hội đồng chưa sẵn sàng chấm."
   │  3. Kiểm tra không phải GVHD của đề tài        → "GVHD không được chấm đề tài mình hướng dẫn."
   │  4. Kiểm tra hạn nộp điểm
   │     - quá hạn & không phải ADMIN → chặn "Đã quá hạn nộp điểm phản biện."
   │     - quá hạn & là ADMIN        → cho phép (ghi nhận ngoại lệ)
   │  5. Kiểm tra điểm 0–10, nhận xét không rỗng
   ▼
Điểm lưu vào evaluations, assignment.status = 'COMPLETED'
```

**Luồng tổng hợp (`finalizeResult`):**

```
Chủ tịch hội đồng (hoặc ADMIN)
   │  1. Hội đồng != DRAFT và != FINISHED
   │  2. Phải có phân công chấm
   │  3. Số điểm SUBMITTED phải BẰNG số phân công
   │     → nếu thiếu: "Chưa đủ điểm: còn N ..."
   │  4. Không GVHD nào chấm đề tài của mình
   │  5. Tìm được nhóm đang thực hiện đề tài
   │  6. Kết quả đã publish → không tổng hợp lại
   ▼
final_score = TỔNG điểm / SỐ người chấm  (làm tròn 2 chữ số, HALF_UP)
result = final_score >= app.passing-score (5.00) ? 'PASSED' : 'FAILED'
```

> Công thức xác nhận trên dữ liệu demo: t15 = (7,50+8,00)/2 = **7,75**; t16 = (4,50+4,00+5,00)/3 = **4,50**; t17 = (8,00+8,50+8,50)/3 = **8,33**.

**Luồng công bố (`publishResultByHead`):**

```
Trưởng khoa (hoặc ADMIN)   ← "Chỉ Trưởng khoa được công bố kết quả." (403)
   │  1. Kiểm tra chưa publish → "Kết quả này đã được công bố." nếu có
   │  2. Set published=1, published_at, published_by
   ▼
Gửi email cho TỪNG sinh viên trong nhóm
   │  → 1 dòng email_logs cho mỗi (result_id, student_id)
   │  → gửi lỗi KHÔNG làm hỏng thao tác công bố
   ▼
SV xem kết quả ở màn hình "Kết quả của tôi" (chỉ nhóm của mình, đợt của mình)
```

### 2.6. Luồng EMAIL (`email_logs`)

Mỗi cặp **(kết quả, sinh viên)** đúng 1 dòng: `uq_email_logs_result_student UNIQUE (result_id, student_id)`. Gửi lại → **cập nhật** dòng cũ, không tạo dòng mới.

| Trạng thái | Ý nghĩa |
|---|---|
| `SENT` | Gửi thành công |
| `FAILED` | Lỗi SMTP (giữ `error_message`, tăng `attempt_count`) |
| `SKIPPED` | `app.mail.enabled=false` → chỉ ghi log, không gửi thật |

Nút "Gửi lại" (`EmailService.resend`) dùng cho dòng `FAILED`/`SKIPPED`.

### 2.7. Luồng THÔNG BÁO (`notifications`)

```
DRAFT ──publish──► PUBLISHED ──retract──► RETRACTED
```

- Không có dòng trong `notification_target_roles` → gửi cho **tất cả vai trò**.
- Có dòng → chỉ gửi cho các vai trò được chỉ định.
- Loại: `SCHOOL` (nhà trường) / `FACULTY` (khoa).

### 2.8. Nhật ký kiểm toán (`audit_logs`)

Ghi lại thao tác nhạy cảm: `PERIOD_PUBLISH`, `SUPERVISOR_ASSIGN`, `RESULT_CALCULATE`, `RESULT_PUBLISH`, …
Cấu trúc: `user_id`, `action`, `entity`, `entity_id`, `detail`, `created_at`.

---

## 3. Ma trận quyền (RBAC)

### 3.1. Cấu trúc

- **Vai trò** (`roles`): 4 vai trò gốc `is_system=1` (không xóa được) + vai trò tự tạo.
- **Quyền** (`permissions`): 22 quyền nghiệp vụ.
- **Gán quyền**: `role_permissions` — Admin có thể chỉnh trên màn hình "Phân quyền".
- Danh mục được nạp **idempotent** khi khởi động: quyền đã tồn tại thì giữ, **không ghi đè** phân quyền đã chỉnh.

### 3.2. Quyền mặc định theo vai trò

| Vai trò | Quyền được gán |
|---|---|
| **ADMIN** | USER_MANAGE, ROLE_MANAGE, PERMISSION_MANAGE, DEPARTMENT_MANAGE, PROFILE_MANAGE, TOPIC_VIEW, TOPIC_APPROVE, SUPERVISOR_ASSIGN, REGISTRATION_CONFIRM, REPORT_VIEW, COUNCIL_MANAGE, ASSIGNMENT_MANAGE, RESULT_CALCULATE, RESULT_PUBLISH, NOTICE_VIEW, NOTICE_MANAGE |
| **TRUONG_KHOA** | DEPARTMENT_MANAGE, PROFILE_MANAGE, PERIOD_MANAGE, TOPIC_VIEW, TOPIC_APPROVE, SUPERVISOR_ASSIGN, REGISTRATION_CONFIRM, REPORT_VIEW, COUNCIL_MANAGE, ASSIGNMENT_MANAGE, RESULT_PUBLISH, NOTICE_VIEW, NOTICE_MANAGE |
| **GIANG_VIEN** | TOPIC_VIEW, TOPIC_CREATE, REGISTRATION_CONFIRM, REPORT_VIEW, EVALUATION_CREATE, RESULT_CALCULATE, NOTICE_VIEW |
| **SINH_VIEN** | TOPIC_VIEW, REGISTRATION_CREATE, SUBMISSION_CREATE, REPORT_VIEW, RESULT_VIEW, NOTICE_VIEW |

> **Điểm dễ nhầm:** `RESULT_CALCULATE` được gán cho GIANG_VIEN, nhưng service chặn — chỉ **chủ tịch hội đồng** (quan hệ `council_members.role`) mới tổng hợp được kết quả của hội đồng đó.
>
> **"Chủ tịch hội đồng" KHÔNG phải vai trò tài khoản** — nó là thuộc tính theo từng hội đồng, nên một GV có thể là chủ tịch hội đồng A và chỉ là ủy viên ở hội đồng B.

### 3.3. Bảo vệ ở tầng HTTP (`SecurityConfig`)

```java
.requestMatchers("/admin/users/**").hasAuthority("USER_MANAGE")
.requestMatchers("/roles/**").hasAuthority("ROLE_MANAGE")
.requestMatchers("/periods/**").hasAuthority("PERIOD_MANAGE")
.requestMatchers("/head/topics/*/approve", ...).hasAuthority("TOPIC_APPROVE")
.requestMatchers("/head/councils/**").hasAuthority("COUNCIL_MANAGE")
.requestMatchers("/head/results/**", "/head/emails/**").hasAuthority("RESULT_PUBLISH")
.requestMatchers("/lecturer/topics/new", ...).hasAuthority("TOPIC_CREATE")
.requestMatchers("/student/groups/**", "/student/registrations/**").hasAuthority("REGISTRATION_CREATE")
.requestMatchers("/student/reports/**").hasAuthority("SUBMISSION_CREATE")
.requestMatchers("/student/results/**").hasAuthority("RESULT_VIEW")
.requestMatchers("/api/my/results/**").hasAuthority("RESULT_VIEW")
```

- `/login`, `/css/**`, `/js/**`, `/images/**`, `/error/**`, `/support/**` → `permitAll()`.
- `CSRF` bật cho mọi form POST; đăng xuất bằng POST.
- Controller ghi chú rõ: `hasAuthority` khớp bảng `role_permissions`, **không** dùng `hasRole` cho quyền nghiệp vụ.

### 3.4. URL theo vai trò

| Prefix | Vai trò | Màn hình chính |
|---|---|---|
| `/admin/**` | Admin | Người dùng, vai trò, phân quyền, bộ môn, hồ sơ GV/SV, thông báo |
| `/head/**` | Trưởng khoa | Đợt, duyệt đề tài, GVHD, hội đồng, người chấm, kết quả, email |
| `/lecturer/**` | Giảng viên | Đề tài của tôi, duyệt đăng ký, xem báo cáo, nhập điểm, công việc hội đồng |
| `/student/**` | Sinh viên | Nhóm, đăng ký, nộp báo cáo, kết quả của tôi |

---

## 4. Quy trình nghiệp vụ — 9 giai đoạn (kịch bản demo)

Dữ liệu demo được bố trí để **đi trọn 9 giai đoạn** mà không phải sửa dữ liệu giữa lúc trình bày.

### GĐ1 — GV đề xuất đề tài (đợt P1, đang mở cho GV)

| # | Ai | Hành động | Kết quả trong DB |
|---|---|---|---|
| 1 | `gv01` | Tạo đề tài mới | `topics.status = PENDING` |
| 2 | `truongkhoa` | Duyệt t1, t2, t5 | `status = APPROVED`, `approved_by`, `approved_at` |
| 3 | `truongkhoa` | Từ chối đề tài (bắt buộc lý do) | `status = REJECTED`, `reject_reason` |
| 4 | `truongkhoa` | Công bố danh sách đề tài (t3 đã duyệt sẵn) | `published = 1` |

### GĐ2 — SV lập nhóm & đăng ký (đợt P2, đang mở cho SV)

| # | Ai | Hành động | Kết quả |
|---|---|---|---|
| 5 | `sv06` | Lập nhóm (g3 có sẵn) + thêm tối đa 3 thành viên | `student_groups.status = DRAFT` |
| 6 | Nhóm trưởng | Đăng ký đề tài đã công bố | `topic_registrations.status = PENDING` |
| 7 | `gv03` (GVHD t7) | Chấp thuận đăng ký của g2 | `status = APPROVED`, `groups.topic_id` được gán, `groups.status = REGISTERED` |
| 8 | Nhóm g4 | Đăng ký lại sau khi bị từ chối | Bản ghi mới `PENDING` |

### GĐ3 — Nộp báo cáo

| # | Ai | Hành động | Kết quả |
|---|---|---|---|
| 9 | `sv01` (trưởng g1) | Nộp báo cáo | `reports` version 1 `SUBMITTED`, `groups.status = SUBMITTED` |
| 10 | `sv02` (thành viên) | Cố nộp báo cáo | **403** — chỉ nhóm trưởng được nộp |

### GĐ4 — Lập hội đồng phản biện (đợt P3)

| # | Ai | Hành động | Kết quả |
|---|---|---|---|
| 11 | `truongkhoa` | Xếp t14 vào hội đồng | `council_topics` |
| 12 | `truongkhoa` | Thêm thành viên cho c3 (đang có 2 GV) | `council_members` → c3 hợp lệ |

### GĐ5 — Phân công người chấm

| # | Ai | Hành động | Kết quả |
|---|---|---|---|
| 13 | `truongkhoa` | Phân công cho t11, t12, t13 | `review_assignments` (GVHD của đề tài **không xuất hiện** trong danh sách chọn) |

### GĐ6 — GV nhập điểm

| # | Ai | Hành động | Kết quả |
|---|---|---|---|
| 14 | `gv06` | Nhập điểm cho KLTN-26-101 (phần **còn dở** trong dữ liệu demo) | `evaluations` mới, assignment 3 → `COMPLETED` |
| 15 | `gv01`, `gv02` | Xem phân công của mình (t12) | Bảng điểm |
| 16 | GV **không** phải chủ tịch | Thử tổng hợp | **403** — chỉ chủ tịch hội đồng được tổng hợp |

### GĐ7 — Chủ tịch tổng hợp

| # | Ai | Hành động | Kết quả |
|---|---|---|---|
| 17 | `gv02` (chủ tịch HD-A) | Tổng hợp KLTN-26-102 (đủ điểm) | `review_results.final_score = trung bình`, `published = 0` |
| 18 | `gv02` | Tổng hợp KLTN-26-101 khi chưa đủ điểm | **Bị chặn**: "Chưa đủ điểm: còn N ..." |

### GĐ8 — Trưởng khoa công bố kết quả

| # | Ai | Hành động | Kết quả |
|---|---|---|---|
| 19 | `truongkhoa` | Công bố KLTN-26-103 (đã tổng hợp 9,00, chưa publish) | `published = 1`, `published_at`, `published_by` + gửi email từng SV |
| 20 | `truongkhoa` | Xem nhật ký email | 1 dòng `FAILED` của sv05 để demo nút "Gửi lại" |

### GĐ9 — SV xem kết quả

| # | Ai | Hành động | Kết quả |
|---|---|---|---|
| 21 | `sv01` (có nhóm ở đợt 2 và đợt 4) | Xem kết quả, lọc theo đợt | Chỉ thấy kết quả của nhóm mình, theo từng đợt |
| 22 | `sv01` | Chọn đợt không tham gia (đợt 3) | **403** |
| 23 | `sv07` | Xem kết quả | Không thấy kết quả nhóm khác |

### Các quy tắc nghiệp vụ nên thử trực tiếp khi phản biện

| # | Thao tác | Kết quả mong đợi |
|---|---|---|
| a | Thêm người thứ 4 vào nhóm | Từ chối (tối đa 3) |
| b | Cho SV lập/tham gia nhóm thứ 2 trong cùng đợt | Từ chối |
| c | Phân công GVHD chấm đề tài mình hướng dẫn | Từ chối |
| d | Gán 3 GVHD cho một đề tài | Từ chối (tối đa 2) |
| e | Xếp cùng 1 đề tài vào 2 hội đồng | Từ chối |
| f | Nhập điểm ngoài khoảng 0–10 | Từ chối |
| g | Công bố kết quả 2 lần | Từ chối |
| h | SV xem kết quả nhóm khác | **403** |

---

## 5. Tài khoản & dữ liệu demo

### 5.1. Tài khoản (mật khẩu chung `123456`)

| Nhóm | Tài khoản | Ghi chú |
|---|---|---|
| Admin | `admin` | Quản trị hệ thống |
| Trưởng khoa | `truongkhoa` | Trần Minh Quân |
| Giảng viên | `gv01`…`gv08` | GV001–GV008, thuộc 3 bộ môn (KHMT, HTTT, KTPM) |
| Sinh viên | `sv01`…`sv24` | **sv24 bị khóa** (status LOCKED, enabled=0) → thử đăng nhập bị chặn |

Mọi email dùng đuôi `@demo.local` (Mailpit/Mailtrap), không phải email thật.

### 5.2. 5 đợt demo — mỗi đợt ở một giai đoạn khác nhau

| Đợt | Loại | Trạng thái | Giai đoạn nghiệp vụ |
|---|---|---|---|
| P1 | MON_HOC | PUBLISHED | GĐ1 — GV đề xuất đề tài (đang mở) |
| P2 | TLCN | PUBLISHED | GĐ2 — SV lập nhóm & đăng ký (đang mở) |
| P3 | KLTN | PUBLISHED | GĐ3–GĐ6 — đã nộp báo cáo, đang phản biện & chấm điểm |
| P4 | TLCN | CLOSED | GĐ9 — đã công bố kết quả, tra cứu lịch sử |
| P5 | NCKH | DRAFT | Thử tạo / sửa / xóa đợt |

Mọi mốc thời gian tính tương đối theo `@now = NOW()` → luôn đúng giai đoạn.

### 5.3. Kịch bản dữ liệu cố ý "chưa hoàn chỉnh"

| Đối tượng | Trạng thái | Dùng để demo |
|---|---|---|
| Topic t1, t2, t5 | PENDING | Duyệt / từ chối |
| Topic t3 | APPROVED, chưa publish | Công bố danh sách đề tài |
| Topic t4 | REJECTED kèm lý do | Xem lý do từ chối |
| Group g2, g5, g6 | REGISTERED | Chờ GVHD chấp thuận |
| Group g4 | Đăng ký bị REJECTED | Đăng ký lại |
| Hội đồng c3 | DRAFT, chỉ 2 GV | Xếp đề tài sẽ bị chặn → phải bổ sung thành viên |
| Topic t14 | Chưa vào hội đồng nào | Chức năng "Xếp đề tài vào hội đồng" |
| Assignment 3 (gv06 chấm t11) | ASSIGNED, chưa có điểm | "Chưa đủ điểm" → rồi nhập điểm cho đủ |
| Kết quả t13 | Đã tổng hợp 9,00, **chưa** publish | Công bố kết quả |
| email_logs sv05 | FAILED (timeout SMTP) | Nút "Gửi lại" |
| Bộ môn MMT (id 4) | Không có GV/đề tài tham chiếu | Thử xóa bộ môn |

### 5.4. Cách nạp lại dữ liệu demo

```powershell
# Nạp vào database TRỐNG
powershell -File scripts\reset-demo-data.ps1 -SqlFile HT_QLDT_02_du_lieu_demo_day_du.sql
```

Thứ tự chuẩn: `HT_QLDT.sql` (schema) → `HT_QLDT_01_danh_muc_quyen.sql` (danh mục quyền) → `HT_QLDT_02_du_lieu_demo_day_du.sql` (dữ liệu) → `tao_bao_cao_demo_day_du.py` (tệp PDF thật) → `scripts\reset-passwords.ps1` (sinh BCrypt thật).

> ⚠ **KHÔNG nạp chung** `HT_QLDT.sql` (đã chứa sẵn dữ liệu mẫu) với bản demo kịch bản — cả hai dùng chung tên đăng nhập `admin`/`truongkhoa`/`gv01…`/`sv01…` nên sẽ trùng khóa UNIQUE. Chọn **một** trong hai.

---

## 6. Bảng ràng buộc nghiệp vụ tổng hợp

| # | Quy tắc | Tầng kiểm tra | Thông báo lỗi |
|---|---|---|---|
| 1 | Nhóm tối đa 3 thành viên | Service (`app.group.max-size`) | "Nhóm đã đủ tối đa 3 thành viên." |
| 2 | SV chỉ 1 nhóm / 1 đợt | **DB UNIQUE** | "Bạn đã tham gia nhóm trong đợt này." |
| 3 | 1 nhóm có 1 trưởng | **DB UNIQUE** (generated col) | — |
| 4 | 1 nhóm có 1 đăng ký hiệu lực | **DB UNIQUE** (generated col) | — |
| 5 | 1 đề tài có 1 nhóm được duyệt | **DB UNIQUE** (generated col) | — |
| 6 | Chỉ nhóm trưởng đăng ký / nộp báo cáo / hủy đăng ký | Service | "Chỉ nhóm trưởng mới được đăng ký đề tài." |
| 7 | Đề tài tối đa 2 GVHD | **DB CHECK** (`supervisor_order`) | "Hai giảng viên hướng dẫn phải là hai người khác nhau." |
| 8 | Không đổi GVHD khi đã có phân công phản biện | Service | "Đề tài đã có phân công phản biện nên không thể đổi GVHD." |
| 9 | Người chấm phải là thành viên hội đồng | **DB FK kép** | — |
| 10 | Người chấm không được là GVHD của đề tài | Service | "GVHD không được chấm đề tài mình hướng dẫn." |
| 11 | Hội đồng 3–5 GV, đúng 1 CH + 1 TK | Service (+ view `v_council_validity`) | "Hội đồng phải có từ 3 đến 5 giảng viên..." |
| 12 | 1 đề tài chỉ ở 1 hội đồng | **DB UNIQUE** | "Đề tài này đã thuộc một hội đồng khác." |
| 13 | Điểm ∈ [0, 10] | **DB CHECK** + Service | "Điểm phải nằm trong khoảng 0 đến 10." |
| 14 | Nhận xét bắt buộc | **DB NOT NULL** + Service | "Vui lòng nhập nhận xét (bắt buộc)." |
| 15 | Phải đủ điểm mới tổng hợp | Service | "Chưa đủ điểm: còn N ..." |
| 16 | Chỉ chủ tịch hội đồng / ADMIN tổng hợp | Service | "Chỉ Trưởng khoa được công bố kết quả." |
| 17 | Chỉ Trưởng khoa / ADMIN công bố | Service | "Chỉ Trưởng khoa được công bố kết quả." |
| 18 | Không công bố 2 lần | Service + DB CHECK | "Kết quả này đã được công bố." |
| 19 | SV chỉ xem kết quả nhóm mình, đợt mình tham gia | Service (403) | — |
| 20 | Tệp báo cáo: pdf/doc/docx, ≤10 MB | Service | "Chỉ nhận tệp PDF, DOC hoặc DOCX." |
| 21 | Không đăng nhập sai 5 lần → khóa 15 phút | Service + `users` | "Tài khoản tạm thời bị khóa..." |
| 22 | Đợt có đề tài thì không sửa/xóa | Service | "Đợt đã có đề tài nên không được sửa hoặc xóa." |
| 23 | Xóa bộ môn đang có tham chiếu | Service | — |

---

## 7. Sơ đồ luồng tổng thể (toàn cảnh)

```
                    ┌───────────────┐
                    │  TRƯỞNG KHOA  │  Tạo/đóng ĐỢT
                    └───────┬───────┘
                            │
        ┌───────────────────┴────────────────────┐
        │                                        │
   [GĐ1] GV đề xuất đề tài            [GĐ2] SV lập nhóm
        │                                        │
   topics: PENDING ──duyệt──► APPROVED          ├── groups: DRAFT
        │                          │              ├── groups: REGISTERED
        │                          ▼              │
        │                   publish=1              ├── topic_registrations: PENDING
        │                          │              │        │
        │                          │              │   GV duyệt ──► APPROVED
        │                          │              │        │      (groups.status=APPROVED)
        │                          │              │        │
        │                          └──────┬───────┘        │
        │                                 │                │
        │                            [GĐ3] Nộp báo cáo     │
        │                                 │                │
        │                            reports (v1, v2...)   │
        │                            groups: SUBMITTED     │
        │                                 │                │
        │                            [GĐ4] Lập hội đồng   │
        │                            councils: DRAFT→READY │
        │                            council_members 3-5   │
        │                            council_topics       │
        │                                 │                │
        │                            [GĐ5] Phân công chấm │
        │                            review_assignments    │
        │                                 │                │
        │                            [GĐ6] GV nhập điểm    │
        │                            evaluations (0-10)    │
        │                                 │                │
        │                            [GĐ7] CH tổng hợp    │
        │                            review_results       │
        │                            final_score = TB     │
        │                                 │                │
        │                            [GĐ8] Công bố        │
        │                            published=1           │
        │                            email_logs (1/SV)    │
        │                                 │                │
        └─────────────────────────────────┴────────────────┘
                                          │
                                   [GĐ9] SV xem kết quả
                                   (/student/results)
```

---

## 8. Câu hỏi thường gặp khi đọc mã nguồn

**Q: Tại sao có cả `status` lẫn `published` ở `topics`?**
Vì `status` mô tả vòng đời **duyệt** (PENDING/APPROVED/REJECTED), còn `published` mô tả vòng đời **công bố cho SV**. Một đề tài đã APPROVED nhưng chưa `published` thì SV chưa đăng ký được. DB chặn `published=1` khi `status != APPROVED`.

**Q: Vì sao một SV có thể ở nhiều nhóm?**
Vì UNIQUE là `(student_id, period_id)` — theo **đợt**, không theo nhóm. Đây là chủ ý để SV tra cứu được kết quả của từng đợt đã học.

**Q: `RESULT_CALCULATE` cho GIANG_VIEN nhưng sao GV không tổng hợp được?**
Quyền cho phép **vào màn hình**, service mới là nơi phân quyền chi tiết: chỉ chủ tịch hội đồng của hội đồng chứa đề tài mới tổng hợp được.

**Q: Cột `*_group_id` / `*_topic_id` / `*_council_id` trong DB là gì?**
Đó là cột GENERATED ... STORED dùng để tạo UNIQUE có điều kiện (MySQL không có partial index). Trong JPA chúng được đặt `insertable=false, updatable=false`.

**Q: Vì sao `app.mail.enabled=false` mà demo vẫn thấy email?**
Khi tắt mail, `EmailService` vẫn ghi một dòng `email_logs` với status `SKIPPED` → vẫn hiển thị đầy đủ luồng gửi email, chỉ không gửi thật.

**Q: Điểm cuối cùng tính thế nào?**
Trung bình cộng các điểm thành phần `SUBMITTED`, làm tròn 2 chữ số (HALF_UP). `result = PASSED` nếu `final_score >= app.passing-score` (mặc định 5.00).

---

## 9. Bản đồ file quan trọng

| Đường dẫn | Vai trò |
|---|---|
| `database/HT_QLDT.sql` | Schema chuẩn: 30 bảng, 2 view, 22 quyền, 4 vai trò gốc |
| `database/HT_QLDT_02_du_lieu_demo_day_du.sql` | Dữ liệu demo kịch bản đầy đủ + hướng dẫn nạp + kịch bản trình bày |
| `database/tao_bao_cao_demo_day_du.py` | Tạo tệp PDF báo cáo mẫu thật |
| `src/main/resources/application.properties` | Cấu hình + tham số nghiệp vụ |
| `config/SecurityConfig.java` | Luật bảo vệ URL theo quyền |
| `config/PermissionCatalog.java` | Danh mục 22 quyền + tên hiển thị |
| `service/StudentPortalService.java` | Luồng nhóm + đăng ký (SV) |
| `service/TopicService.java` | Luồng đề tài (đề xuất/duyệt/công bố/GVHD) |
| `service/CouncilService.java` | Luồng hội đồng + xếp đề tài |
| `service/CouncilScoringService.java` | Luồng chấm điểm + tổng hợp + công bố |
| `service/StudentReportService.java` | Luồng nộp/tải báo cáo + bảo mật tệp |
| `service/RegistrationPeriodService.java` | Luồng đợt + kiểm tra khung thời gian |
| `service/EmailService.java` | Gửi email kết quả + log |
| `docs/bao-cao-chuc-nang-quan-ly-de-tai.md` | Báo cáo chức năng (bản yêu cầu) |
| `BAO_CAO_NGHIEM_THU_E2E.md` | Báo cáo nghiệm thu E2E |
