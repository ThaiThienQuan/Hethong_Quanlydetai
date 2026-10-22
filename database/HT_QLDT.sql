/* =====================================================================
   HT_QLDT.sql  -  Schema chuẩn (v2) - Hệ thống quản lý đề tài sinh viên
   MySQL 8.0.16+ (cần để CHECK được thực thi)  |  InnoDB  |  utf8mb4

   - Chỉ chứa CẤU TRÚC. Dữ liệu mẫu (profile demo) nằm ở tệp riêng.
   - Danh mục vai trò/quyền do ứng dụng nạp khi khởi động (idempotent).
   - Chạy sạch trên MySQL trống bằng MySQL Workbench: File > Run SQL Script.
   - Nếu muốn tạo lại từ đầu, bỏ comment dòng DROP DATABASE bên dưới
     (CHỈ làm trên database có thể xóa).
   - Các cột sinh ra (generated) tên *_group_id, *_topic_id, *_council_id
     chỉ để đảm bảo ràng buộc duy nhất ở mức CSDL: trong JPA không map,
     hoặc map với insertable=false, updatable=false.
   ===================================================================== */

-- DROP DATABASE IF EXISTS student_topic_management;
CREATE DATABASE IF NOT EXISTS student_topic_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE student_topic_management;

SET NAMES utf8mb4;

/* =====================================================================
   1. TÀI KHOẢN, VAI TRÒ, PHÂN QUYỀN
   ===================================================================== */

CREATE TABLE users (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    username            VARCHAR(50)  NOT NULL,
    password_hash       VARCHAR(100) NOT NULL COMMENT 'Chuỗi BCrypt, VD {bcrypt}$2a$10$... - TUYỆT ĐỐI không lưu mật khẩu rõ',
    full_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(100) NOT NULL,
    phone               VARCHAR(20)  NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    enabled             TINYINT(1)   NOT NULL DEFAULT 1,
    failed_login_count  INT          NOT NULL DEFAULT 0 COMMENT 'Số lần đăng nhập sai liên tiếp',
    locked_until        DATETIME     NULL     COMMENT 'Khóa tạm do đăng nhập sai nhiều lần',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NULL     ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT chk_users_status  CHECK (status IN ('ACTIVE', 'LOCKED')),
    CONSTRAINT chk_users_failed  CHECK (failed_login_count >= 0)
) ENGINE=InnoDB;

CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)  NOT NULL COMMENT 'ADMIN, TRUONG_KHOA, GIANG_VIEN, SINH_VIEN hoặc vai trò tự tạo',
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    is_system   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = vai trò gốc, không được xóa',
    PRIMARY KEY (id),
    CONSTRAINT uq_roles_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE permissions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(100) NOT NULL COMMENT 'VD TOPIC_APPROVE, RESULT_PUBLISH',
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_permissions_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles (id)       ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB;

/* =====================================================================
   2. BỘ MÔN, HỒ SƠ SINH VIÊN, HỒ SƠ GIẢNG VIÊN
   ===================================================================== */

CREATE TABLE departments (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    CONSTRAINT uq_departments_code   UNIQUE (code),
    CONSTRAINT chk_departments_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE students (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    student_code VARCHAR(20)  NOT NULL,
    class_name   VARCHAR(50)  NOT NULL,
    major        VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_students_user_id UNIQUE (user_id),
    CONSTRAINT uq_students_code    UNIQUE (student_code)
) ENGINE=InnoDB;

CREATE TABLE lecturers (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    lecturer_code   VARCHAR(20) NOT NULL,
    academic_degree VARCHAR(50) NULL,
    department_id   BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lecturers_user       FOREIGN KEY (user_id)       REFERENCES users (id),
    CONSTRAINT fk_lecturers_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT uq_lecturers_user_id UNIQUE (user_id),
    CONSTRAINT uq_lecturers_code    UNIQUE (lecturer_code)
) ENGINE=InnoDB;

/* =====================================================================
   3. ĐỢT ĐĂNG KÝ (2 giai đoạn)
   GĐ1: GV đề xuất, Trưởng khoa duyệt/công bố  |  GĐ2: nhóm SV đăng ký
   ===================================================================== */

CREATE TABLE registration_periods (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    name                VARCHAR(150) NOT NULL,
    period_type         VARCHAR(20)  NOT NULL,
    lecturer_start_at   DATETIME     NOT NULL,
    lecturer_end_at     DATETIME     NOT NULL,
    student_start_at    DATETIME     NOT NULL,
    student_end_at      DATETIME     NOT NULL,
    review_deadline     DATETIME     NULL COMMENT 'Hạn chót GVPB nộp điểm - chỉ TLCN/KLTN',
    council_report_date DATETIME     NULL COMMENT 'Ngày báo cáo hội đồng - chỉ KLTN',
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by          BIGINT       NOT NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_periods_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_period_type   CHECK (period_type IN ('MON_HOC', 'NCKH', 'TLCN', 'KLTN')),
    CONSTRAINT chk_period_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED')),
    CONSTRAINT chk_period_lecturer_time CHECK (lecturer_start_at < lecturer_end_at),
    CONSTRAINT chk_period_student_time  CHECK (student_start_at < student_end_at),
    CONSTRAINT chk_period_two_phases    CHECK (student_start_at >= lecturer_end_at),
    CONSTRAINT chk_period_review_type   CHECK (review_deadline IS NULL OR period_type IN ('TLCN', 'KLTN')),
    CONSTRAINT chk_period_council_type  CHECK (council_report_date IS NULL OR period_type = 'KLTN'),
    CONSTRAINT chk_period_review_after_student CHECK (review_deadline IS NULL OR review_deadline > student_end_at),
    CONSTRAINT chk_period_council_after_review CHECK (council_report_date IS NULL OR review_deadline IS NULL OR council_report_date > review_deadline)
) ENGINE=InnoDB;

/* =====================================================================
   4. ĐỀ TÀI VÀ GIẢNG VIÊN HƯỚNG DẪN (1 - 2 GVHD)
   ===================================================================== */

CREATE TABLE topics (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    period_id              BIGINT       NOT NULL,
    department_id          BIGINT       NOT NULL,
    created_by_lecturer_id BIGINT       NOT NULL,
    topic_code             VARCHAR(30)  NOT NULL,
    topic_name             VARCHAR(200) NOT NULL,
    description            TEXT         NOT NULL,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reject_reason          VARCHAR(500) NULL,
    approved_by            BIGINT       NULL,
    approved_at            DATETIME     NULL,
    published              TINYINT(1)   NOT NULL DEFAULT 0,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_topics_period     FOREIGN KEY (period_id)              REFERENCES registration_periods (id),
    CONSTRAINT fk_topics_department FOREIGN KEY (department_id)          REFERENCES departments (id),
    CONSTRAINT fk_topics_lecturer   FOREIGN KEY (created_by_lecturer_id) REFERENCES lecturers (id),
    CONSTRAINT fk_topics_approver   FOREIGN KEY (approved_by)            REFERENCES users (id),
    CONSTRAINT uq_topics_code UNIQUE (topic_code),
    CONSTRAINT chk_topics_status      CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_topics_description CHECK (CHAR_LENGTH(TRIM(description)) >= 20),
    CONSTRAINT chk_topics_published   CHECK (published = 0 OR status = 'APPROVED')
) ENGINE=InnoDB;

CREATE INDEX idx_topics_period_status ON topics (period_id, status, published);
CREATE INDEX idx_topics_department    ON topics (department_id);

CREATE TABLE topic_supervisors (
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    topic_id         BIGINT   NOT NULL,
    lecturer_id      BIGINT   NOT NULL,
    supervisor_order INT      NOT NULL COMMENT '1 = GVHD chính (người đề xuất), 2 = GVHD thứ hai',
    assigned_by      BIGINT   NULL COMMENT 'Trưởng khoa phân công/điều chỉnh (nếu có)',
    assigned_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_topic_supervisors_topic       FOREIGN KEY (topic_id)    REFERENCES topics (id) ON DELETE CASCADE,
    CONSTRAINT fk_topic_supervisors_lecturer    FOREIGN KEY (lecturer_id) REFERENCES lecturers (id),
    CONSTRAINT fk_topic_supervisors_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id),
    CONSTRAINT uq_topic_supervisor_lecturer UNIQUE (topic_id, lecturer_id),
    CONSTRAINT uq_topic_supervisor_order    UNIQUE (topic_id, supervisor_order),
    CONSTRAINT chk_supervisor_order CHECK (supervisor_order IN (1, 2))
) ENGINE=InnoDB;
-- Quy tắc "mỗi đề tài có 1-2 GVHD" kiểm tra ở service (không thể ép bằng CHECK).

/* =====================================================================
   5. NHÓM SINH VIÊN VÀ ĐĂNG KÝ ĐỀ TÀI
   ===================================================================== */

CREATE TABLE student_groups (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    period_id  BIGINT       NOT NULL,
    group_code VARCHAR(30)  NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    topic_id   BIGINT       NULL COMMENT 'Đề tài đang thực hiện - gán khi đăng ký được chấp thuận',
    status     VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_groups_period FOREIGN KEY (period_id) REFERENCES registration_periods (id),
    CONSTRAINT fk_groups_topic  FOREIGN KEY (topic_id)  REFERENCES topics (id),
    CONSTRAINT uq_groups_code   UNIQUE (group_code),
    CONSTRAINT uq_groups_topic  UNIQUE (topic_id),
    CONSTRAINT uq_groups_id_period UNIQUE (id, period_id) COMMENT 'Đích của FK kép từ group_members',
    CONSTRAINT chk_groups_status CHECK (status IN ('DRAFT', 'REGISTERED', 'APPROVED', 'SUBMITTED', 'COMPLETED'))
) ENGINE=InnoDB;

/* Mỗi SV chỉ một nhóm TRONG MỘT ĐỢT (UNIQUE student_id + period_id): SV đã học
   đợt TLCN vẫn lập được nhóm ở đợt KLTN và tra cứu kết quả theo từng đợt.
   Số thành viên tối thiểu/tối đa (app.group.min-size / max-size) kiểm tra ở service. */
CREATE TABLE group_members (
    id              BIGINT     NOT NULL AUTO_INCREMENT,
    group_id        BIGINT     NOT NULL,
    period_id       BIGINT     NOT NULL COMMENT 'Sao chép từ nhóm, ép khớp bằng FK kép',
    student_id      BIGINT     NOT NULL,
    is_leader       TINYINT(1) NOT NULL DEFAULT 0,
    joined_at       DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leader_group_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_leader = 1 THEN group_id ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_group_members_group   FOREIGN KEY (group_id, period_id) REFERENCES student_groups (id, period_id),
    CONSTRAINT fk_group_members_student FOREIGN KEY (student_id)          REFERENCES students (id),
    CONSTRAINT uq_group_members_student_period UNIQUE (student_id, period_id),
    CONSTRAINT uq_group_members_group_student  UNIQUE (group_id, student_id),
    CONSTRAINT uq_group_members_one_leader     UNIQUE (leader_group_id)
) ENGINE=InnoDB;

CREATE TABLE topic_registrations (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    group_id         BIGINT       NOT NULL,
    topic_id         BIGINT       NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    registered_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by      BIGINT       NULL,
    approved_at      DATETIME     NULL,
    rejection_reason VARCHAR(500) NULL,
    active_group_id  BIGINT GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING', 'APPROVED') THEN group_id ELSE NULL END) STORED,
    approved_topic_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'APPROVED' THEN topic_id ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_registrations_group    FOREIGN KEY (group_id)    REFERENCES student_groups (id),
    CONSTRAINT fk_registrations_topic    FOREIGN KEY (topic_id)    REFERENCES topics (id),
    CONSTRAINT fk_registrations_approver FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT uq_registrations_one_active_per_group UNIQUE (active_group_id),
    CONSTRAINT uq_registrations_one_approved_per_topic UNIQUE (approved_topic_id),
    CONSTRAINT chk_registrations_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE INDEX idx_registrations_topic ON topic_registrations (topic_id, status);

/* =====================================================================
   6. BÁO CÁO (chỉ nhóm trưởng nộp - kiểm tra ở service)
   ===================================================================== */

CREATE TABLE reports (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    group_id     BIGINT       NOT NULL,
    submitted_by BIGINT       NOT NULL COMMENT 'students.id của nhóm trưởng',
    report_title VARCHAR(200) NOT NULL,
    file_name    VARCHAR(255) NOT NULL COMMENT 'Tên gốc đã làm sạch',
    stored_path  VARCHAR(500) NOT NULL COMMENT 'Đường dẫn tương đối trong app.storage.dir (tên ngẫu nhiên)',
    file_size    BIGINT       NOT NULL,
    version      INT          NOT NULL DEFAULT 1,
    description  VARCHAR(500) NULL,
    submitted_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status       VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    PRIMARY KEY (id),
    CONSTRAINT fk_reports_group   FOREIGN KEY (group_id)     REFERENCES student_groups (id),
    CONSTRAINT fk_reports_student FOREIGN KEY (submitted_by) REFERENCES students (id),
    CONSTRAINT uq_reports_group_version UNIQUE (group_id, version),
    CONSTRAINT chk_reports_version CHECK (version > 0),
    CONSTRAINT chk_reports_size    CHECK (file_size > 0),
    CONSTRAINT chk_reports_status  CHECK (status IN ('SUBMITTED', 'SUPERSEDED'))
) ENGINE=InnoDB;

/* =====================================================================
   7. HỘI ĐỒNG PHẢN BIỆN, PHÂN CÔNG CHẤM, ĐIỂM, KẾT QUẢ
   ===================================================================== */

CREATE TABLE councils (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    period_id    BIGINT       NOT NULL,
    council_code VARCHAR(30)  NOT NULL,
    council_name VARCHAR(150) NOT NULL,
    report_date  DATETIME     NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by   BIGINT       NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_councils_period     FOREIGN KEY (period_id)  REFERENCES registration_periods (id),
    CONSTRAINT fk_councils_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT uq_councils_code UNIQUE (council_code),
    CONSTRAINT chk_councils_status CHECK (status IN ('DRAFT', 'READY', 'FINISHED'))
) ENGINE=InnoDB;

CREATE TABLE council_members (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    council_id           BIGINT      NOT NULL,
    lecturer_id          BIGINT      NOT NULL,
    role                 VARCHAR(20) NOT NULL,
    joined_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    chairman_council_id  BIGINT GENERATED ALWAYS AS (CASE WHEN role = 'CHAIRMAN'  THEN council_id ELSE NULL END) STORED,
    secretary_council_id BIGINT GENERATED ALWAYS AS (CASE WHEN role = 'SECRETARY' THEN council_id ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_council_members_council   FOREIGN KEY (council_id)  REFERENCES councils (id),
    CONSTRAINT fk_council_members_lecturer  FOREIGN KEY (lecturer_id) REFERENCES lecturers (id),
    CONSTRAINT uq_council_members_lecturer  UNIQUE (council_id, lecturer_id),
    CONSTRAINT uq_council_members_one_chair UNIQUE (chairman_council_id),
    CONSTRAINT uq_council_members_one_secretary UNIQUE (secretary_council_id),
    CONSTRAINT chk_council_members_role CHECK (role IN ('CHAIRMAN', 'SECRETARY', 'MEMBER'))
) ENGINE=InnoDB;
-- Quy tắc "3-5 thành viên" kiểm tra ở service; xem view v_council_validity.

CREATE TABLE council_topics (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    council_id BIGINT   NOT NULL,
    topic_id   BIGINT   NOT NULL,
    added_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_council_topics_council FOREIGN KEY (council_id) REFERENCES councils (id),
    CONSTRAINT fk_council_topics_topic   FOREIGN KEY (topic_id)   REFERENCES topics (id),
    CONSTRAINT uq_council_topics_topic   UNIQUE (topic_id),
    CONSTRAINT uq_council_topics_pair    UNIQUE (council_id, topic_id)
) ENGINE=InnoDB;

/* Người chấm phải là thành viên hội đồng và đề tài phải thuộc hội đồng đó
   (FK kép). "Người chấm không phải GVHD của đề tài" kiểm tra ở service. */
CREATE TABLE review_assignments (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    council_id  BIGINT      NOT NULL,
    topic_id    BIGINT      NOT NULL,
    lecturer_id BIGINT      NOT NULL,
    assigned_by BIGINT      NOT NULL,
    assigned_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status      VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    PRIMARY KEY (id),
    CONSTRAINT fk_assignments_member      FOREIGN KEY (council_id, lecturer_id) REFERENCES council_members (council_id, lecturer_id),
    CONSTRAINT fk_assignments_council_topic FOREIGN KEY (council_id, topic_id)  REFERENCES council_topics (council_id, topic_id),
    CONSTRAINT fk_assignments_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id),
    CONSTRAINT uq_assignments_unique UNIQUE (council_id, topic_id, lecturer_id),
    CONSTRAINT chk_assignments_status CHECK (status IN ('ASSIGNED', 'COMPLETED'))
) ENGINE=InnoDB;

CREATE INDEX idx_assignments_lecturer ON review_assignments (lecturer_id, status);

CREATE TABLE evaluations (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT        NOT NULL,
    score         DECIMAL(4,2)  NOT NULL,
    comment       VARCHAR(1000) NOT NULL,
    evaluated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NULL ON UPDATE CURRENT_TIMESTAMP,
    status        VARCHAR(20)   NOT NULL DEFAULT 'SUBMITTED',
    PRIMARY KEY (id),
    CONSTRAINT fk_evaluations_assignment FOREIGN KEY (assignment_id) REFERENCES review_assignments (id),
    CONSTRAINT uq_evaluations_assignment UNIQUE (assignment_id),
    CONSTRAINT chk_evaluations_score  CHECK (score >= 0 AND score <= 10),
    CONSTRAINT chk_evaluations_status CHECK (status IN ('DRAFT', 'SUBMITTED'))
) ENGINE=InnoDB;

CREATE TABLE review_results (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    group_id        BIGINT        NOT NULL,
    topic_id        BIGINT        NOT NULL,
    council_id      BIGINT        NOT NULL,
    final_score     DECIMAL(4,2)  NOT NULL COMMENT 'Trung bình cộng các điểm thành phần',
    result          VARCHAR(20)   NOT NULL,
    summary_comment VARCHAR(2000) NULL COMMENT 'Nhận xét tổng hợp của chủ tịch hội đồng',
    calculated_by   BIGINT        NOT NULL,
    calculated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published       TINYINT(1)    NOT NULL DEFAULT 0,
    published_at    DATETIME      NULL,
    published_by    BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_results_group        FOREIGN KEY (group_id)             REFERENCES student_groups (id),
    CONSTRAINT fk_results_council_topic FOREIGN KEY (council_id, topic_id) REFERENCES council_topics (council_id, topic_id),
    CONSTRAINT fk_results_calculated_by FOREIGN KEY (calculated_by)       REFERENCES users (id),
    CONSTRAINT fk_results_published_by  FOREIGN KEY (published_by)        REFERENCES users (id),
    CONSTRAINT uq_results_topic UNIQUE (topic_id),
    CONSTRAINT uq_results_group UNIQUE (group_id),
    CONSTRAINT chk_results_score  CHECK (final_score >= 0 AND final_score <= 10),
    CONSTRAINT chk_results_result CHECK (result IN ('PASSED', 'FAILED')),
    CONSTRAINT chk_results_published CHECK (published = 0 OR (published_at IS NOT NULL AND published_by IS NOT NULL))
) ENGINE=InnoDB;

/* =====================================================================
   8. THÔNG BÁO, NHẬT KÝ, EMAIL
   ===================================================================== */

CREATE TABLE notifications (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(250) NOT NULL,
    content      TEXT         NOT NULL,
    type         VARCHAR(20)  NOT NULL DEFAULT 'FACULTY',
    created_by   BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME     NULL,
    updated_at   DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_notifications_type   CHECK (type IN ('SCHOOL', 'FACULTY')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETRACTED'))
) ENGINE=InnoDB;

CREATE INDEX idx_notifications_status ON notifications (status, published_at);

-- Không có dòng nào cho một thông báo = gửi cho tất cả vai trò.
CREATE TABLE notification_target_roles (
    notification_id BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    PRIMARY KEY (notification_id, role_id),
    CONSTRAINT fk_notif_roles_notification FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_roles_role         FOREIGN KEY (role_id)         REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NULL,
    action     VARCHAR(60)  NOT NULL,
    entity     VARCHAR(60)  NOT NULL,
    entity_id  BIGINT       NULL,
    detail     VARCHAR(1000) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_audit_logs_entity  ON audit_logs (entity, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at);

/* Mỗi (kết quả, SV) đúng một dòng; gửi lại thì cập nhật dòng đó. */
CREATE TABLE email_logs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    result_id     BIGINT       NOT NULL,
    student_id    BIGINT       NOT NULL,
    to_email      VARCHAR(100) NOT NULL,
    subject       VARCHAR(250) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'FAILED',
    error_message VARCHAR(500) NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at       DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_email_logs_result  FOREIGN KEY (result_id)  REFERENCES review_results (id),
    CONSTRAINT fk_email_logs_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT uq_email_logs_result_student UNIQUE (result_id, student_id),
    CONSTRAINT chk_email_logs_status CHECK (status IN ('SENT', 'FAILED', 'SKIPPED')),
    CONSTRAINT chk_email_logs_attempts CHECK (attempt_count >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_email_logs_status ON email_logs (status);

/* =====================================================================
   9. VIEW HỖ TRỢ
   ===================================================================== */

-- Hội đồng hợp lệ: 3-5 GV, đúng 1 chủ tịch, đúng 1 thư ký.
CREATE VIEW v_council_validity AS
SELECT c.id AS council_id,
       COUNT(m.id) AS member_count,
       COALESCE(SUM(m.role = 'CHAIRMAN'), 0)  AS chairman_count,
       COALESCE(SUM(m.role = 'SECRETARY'), 0) AS secretary_count,
       (COUNT(m.id) BETWEEN 3 AND 5
        AND COALESCE(SUM(m.role = 'CHAIRMAN'), 0) = 1
        AND COALESCE(SUM(m.role = 'SECRETARY'), 0) = 1) AS is_valid
FROM councils c
LEFT JOIN council_members m ON m.council_id = c.id
GROUP BY c.id;

-- Điểm từng thành viên và điểm trung bình theo đề tài (dùng để đối chiếu).
CREATE VIEW v_topic_score_summary AS
SELECT ra.council_id,
       ra.topic_id,
       COUNT(ra.id) AS assigned_count,
       COUNT(ev.id) AS evaluated_count,
       ROUND(AVG(ev.score), 2) AS average_score
FROM review_assignments ra
LEFT JOIN evaluations ev ON ev.assignment_id = ra.id AND ev.status = 'SUBMITTED'
GROUP BY ra.council_id, ra.topic_id;

/* ---------- 1. VAI TRÒ (4 vai trò gốc, is_system = 1) ---------- */
INSERT IGNORE INTO roles (code, name, description, is_system) VALUES
('ADMIN',       'Quản trị hệ thống', 'Quản lý tài khoản, vai trò, phân quyền, danh mục và thông báo nhà trường', 1),
('TRUONG_KHOA', 'Trưởng khoa',       'Tạo đợt, duyệt/công bố đề tài, phân công GVHD/GVPB, lập hội đồng, công bố kết quả', 1),
('GIANG_VIEN',  'Giảng viên',        'Đề xuất đề tài, hướng dẫn, chấm điểm; chủ tịch hội đồng tổng hợp điểm hội đồng mình', 1),
('SINH_VIEN',   'Sinh viên',         'Lập nhóm, đăng ký đề tài, nộp báo cáo (nhóm trưởng), xem kết quả đợt mình tham gia', 1);

/* ---------- 2. QUYỀN ---------- */
INSERT IGNORE INTO permissions (code, name, description) VALUES
('USER_MANAGE',         'Quản lý tài khoản',            'Tạo, sửa, khóa/mở khóa, đặt lại mật khẩu, xóa tài khoản chưa được tham chiếu'),
('ROLE_MANAGE',         'Quản lý vai trò',              'Tạo/sửa vai trò, gán vai trò cho tài khoản'),
('PERMISSION_MANAGE',   'Quản lý phân quyền',           'Gán/gỡ quyền cho vai trò'),
('DEPARTMENT_MANAGE',   'Quản lý bộ môn',               'CRUD bộ môn'),
('PROFILE_MANAGE',      'Quản lý hồ sơ GV/SV',          'CRUD hồ sơ giảng viên và sinh viên'),
('PERIOD_MANAGE',       'Quản lý đợt đăng ký',          'Tạo, sửa, xóa nháp, công bố và đóng đợt'),
('TOPIC_VIEW',          'Xem đề tài',                   'Xem danh sách và chi tiết đề tài'),
('TOPIC_CREATE',        'Đề xuất đề tài',               'Đề xuất, sửa, xóa đề tài của chính mình'),
('TOPIC_APPROVE',       'Duyệt và công bố đề tài',      'Duyệt/từ chối đề tài, công bố danh sách đề tài'),
('SUPERVISOR_ASSIGN',   'Phân công GVHD',               'Thêm/đổi giảng viên hướng dẫn (1-2 GVHD)'),
('REGISTRATION_CREATE', 'Lập nhóm và đăng ký đề tài',   'Tạo/tham gia nhóm, nhóm trưởng đăng ký đề tài'),
('REGISTRATION_CONFIRM','Chấp thuận đăng ký của nhóm',  'GVHD của đề tài hoặc Trưởng khoa chấp thuận/từ chối'),
('SUBMISSION_CREATE',   'Nộp báo cáo',                  'Nhóm trưởng nộp báo cáo của nhóm'),
('REPORT_VIEW',         'Xem/tải báo cáo',              'Xem/tải báo cáo trong phạm vi được phép'),
('COUNCIL_MANAGE',      'Lập hội đồng',                 'Tạo hội đồng 3-5 GV, chủ tịch, thư ký, xếp đề tài'),
('ASSIGNMENT_MANAGE',   'Phân công GVPB/GV chấm',       'Phân công người chấm trong hội đồng'),
('EVALUATION_CREATE',   'Nhập điểm',                    'Nhập/sửa điểm và nhận xét được phân công (không phải GVHD)'),
('RESULT_CALCULATE',    'Tổng hợp điểm',                'Tổng hợp điểm hội đồng; GV chỉ làm được khi là chủ tịch của hội đồng đó'),
('RESULT_PUBLISH',      'Công bố kết quả',              'Công bố kết quả và kích hoạt gửi email cho từng SV'),
('RESULT_VIEW',         'Xem kết quả',                  'SV xem kết quả đã công bố của nhóm mình, theo đợt mình tham gia'),
('NOTICE_VIEW',         'Xem thông báo',                'Xem thông báo phù hợp phạm vi'),
('NOTICE_MANAGE',       'Quản lý thông báo',            'CRUD và công bố/thu hồi thông báo');

/* ---------- 3. GÁN QUYỀN MẶC ĐỊNH CHO VAI TRÒ ---------- */
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN (
  'USER_MANAGE','ROLE_MANAGE','PERMISSION_MANAGE','DEPARTMENT_MANAGE','PROFILE_MANAGE',
  'TOPIC_VIEW','TOPIC_APPROVE','SUPERVISOR_ASSIGN','REGISTRATION_CONFIRM',
  'REPORT_VIEW','COUNCIL_MANAGE','ASSIGNMENT_MANAGE','RESULT_CALCULATE','RESULT_PUBLISH',
  'NOTICE_VIEW','NOTICE_MANAGE');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE r.code = 'TRUONG_KHOA' AND p.code IN (
  'DEPARTMENT_MANAGE','PROFILE_MANAGE','PERIOD_MANAGE','TOPIC_VIEW','TOPIC_APPROVE',
  'SUPERVISOR_ASSIGN','REGISTRATION_CONFIRM','REPORT_VIEW','COUNCIL_MANAGE',
  'ASSIGNMENT_MANAGE','RESULT_PUBLISH','NOTICE_VIEW','NOTICE_MANAGE');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE r.code = 'GIANG_VIEN' AND p.code IN (
  'TOPIC_VIEW','TOPIC_CREATE','REGISTRATION_CONFIRM','REPORT_VIEW',
  'EVALUATION_CREATE','RESULT_CALCULATE','NOTICE_VIEW');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE r.code = 'SINH_VIEN' AND p.code IN (
  'TOPIC_VIEW','REGISTRATION_CREATE','SUBMISSION_CREATE','REPORT_VIEW',
  'RESULT_VIEW','NOTICE_VIEW');

SELECT r.code AS vai_tro, COUNT(rp.permission_id) AS so_quyen
FROM roles r LEFT JOIN role_permissions rp ON rp.role_id = r.id
GROUP BY r.code ORDER BY r.id;
