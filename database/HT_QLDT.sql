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

/* =====================================================================
   HT_QLDT_02_du_lieu_mau.sql  -  DỮ LIỆU MẪU ĐỂ TEST/DEMO (chỉ dùng demo!)

   Thứ tự chạy:  HT_QLDT.sql  ->  HT_QLDT_01_danh_muc_quyen.sql  ->  file này.
   Sau đó chạy:   python tao_bao_cao_mau.py <app.storage.dir>   (tạo tệp PDF thật).

   * MẬT KHẨU: mọi tài khoản dùng chung   123456
     (trong DB chỉ lưu chuỗi BCrypt, KHÔNG có mật khẩu rõ).
   * THỜI GIAN: mọi mốc tính tương đối theo thời điểm chạy script (@now).
     Muốn đợt luôn đúng giai đoạn khi demo -> reset + chạy lại file này ngay trước buổi demo.
   * Email SV/GV dùng đuôi @demo.local (trỏ vào Mailpit/Mailtrap, không phải email thật).
   * Chạy lần 2: bỏ comment khối "RESET" bên dưới (XÓA toàn bộ dữ liệu nghiệp vụ, giữ vai trò/quyền).
   ===================================================================== */

/* ---------- RESET (tùy chọn) - bỏ comment nếu muốn nạp lại từ đầu ----------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE email_logs;          TRUNCATE TABLE audit_logs;
TRUNCATE TABLE notification_target_roles; TRUNCATE TABLE notifications;
TRUNCATE TABLE review_results;      TRUNCATE TABLE evaluations;
TRUNCATE TABLE review_assignments;  TRUNCATE TABLE council_topics;
TRUNCATE TABLE council_members;     TRUNCATE TABLE councils;
TRUNCATE TABLE reports;             TRUNCATE TABLE topic_registrations;
TRUNCATE TABLE group_members;       TRUNCATE TABLE student_groups;
TRUNCATE TABLE topic_supervisors;   TRUNCATE TABLE topics;
TRUNCATE TABLE registration_periods;
TRUNCATE TABLE students;            TRUNCATE TABLE lecturers;
TRUNCATE TABLE departments;         TRUNCATE TABLE user_roles;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;
------------------------------------------------------------------------- */

SET @now = NOW();
-- Chuỗi "@pw" chi la PLACEHOLDER. SQL khong the sinh BCrypt, va
-- DelegatingPasswordEncoder (Spring Security 7) bat buoc phai co prefix "{bcrypt}".
-- Buoc 1: Import file SQL nay (cac user se bi FAIL dang nhap — do co tinh).
-- Buoc 2: Chay lop ResetPasswords (xem scripts/README.md hoac go lenh ben duoi)
--         de sinh hash that cho tat ca tai khoan.
-- Classpath goi y (Java 17+):
--   javac -cp "spring-security-crypto-6.5.11.jar;commons-logging-1.3.6.jar;mysql-connector-j-9.7.0.jar" \
--         -d tools/out tools/ResetPasswords.java
--   java  -cp "spring-security-crypto-6.5.11.jar;commons-logging-1.3.6.jar;mysql-connector-j-9.7.0.jar;tools/out" \
--         ResetPasswords 123456
-- Sau buoc nay, dang nhap bang "admin/123456" (hoac mat khau ban dat).
SET @pw  = '{bcrypt}$2a$10$toBeReplacedByResetPasswordsClassHashHashHashHashHashHashHashHashHashHashHashHashHash';

/* =====================================================================
   1. BỘ MÔN (id 4 không có dữ liệu nào tham chiếu -> dùng để thử XÓA bộ môn)
   ===================================================================== */
INSERT INTO departments (id, code, name, description, status) VALUES
(1, 'KHMT', 'Khoa học máy tính',     'Trí tuệ nhân tạo, dữ liệu, thuật toán', 'ACTIVE'),
(2, 'HTTT', 'Hệ thống thông tin',    'Phân tích thiết kế, ERP, cơ sở dữ liệu', 'ACTIVE'),
(3, 'KTPM', 'Kỹ thuật phần mềm',     'Quy trình phần mềm, kiểm thử, DevOps', 'ACTIVE'),
(4, 'MMT',  'Mạng máy tính',         'Bộ môn thử nghiệm: chưa có GV/đề tài', 'ACTIVE');

/* =====================================================================
   2. TÀI KHOẢN  (1 admin, 1 trưởng khoa, 8 GV, 20 SV + 1 SV bị khóa)
   ===================================================================== */
INSERT INTO users (id, username, password_hash, full_name, email, phone, status, enabled, created_at) VALUES
( 1, 'admin',      @pw, 'Quản trị hệ thống', 'admin@demo.local',      '0900000001', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
( 2, 'truongkhoa', @pw, 'Trần Minh Quân',    'truongkhoa@demo.local', '0900000002', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
( 3, 'gv01', @pw, 'Nguyễn Văn An',     'gv01@demo.local', '0911000001', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
( 4, 'gv02', @pw, 'Lê Thị Bình',       'gv02@demo.local', '0911000002', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
( 5, 'gv03', @pw, 'Phạm Quốc Cường',   'gv03@demo.local', '0911000003', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
( 6, 'gv04', @pw, 'Hoàng Thu Dung',    'gv04@demo.local', '0911000004', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
( 7, 'gv05', @pw, 'Võ Minh Đức',       'gv05@demo.local', '0911000005', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
( 8, 'gv06', @pw, 'Đặng Thị Phương',   'gv06@demo.local', '0911000006', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
( 9, 'gv07', @pw, 'Bùi Hữu Giang',     'gv07@demo.local', '0911000007', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(10, 'gv08', @pw, 'Ngô Thanh Hà',      'gv08@demo.local', '0911000008', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(11, 'sv01', @pw, 'Nguyễn Hoài Nam',   'sv01@demo.local', '0922000001', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(12, 'sv02', @pw, 'Trần Thu Hà',       'sv02@demo.local', '0922000002', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(13, 'sv03', @pw, 'Lê Quang Huy',      'sv03@demo.local', '0922000003', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(14, 'sv04', @pw, 'Phạm Ngọc Anh',     'sv04@demo.local', '0922000004', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(15, 'sv05', @pw, 'Vũ Minh Tuấn',      'sv05@demo.local', '0922000005', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(16, 'sv06', @pw, 'Đỗ Thị Lan',        'sv06@demo.local', '0922000006', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(17, 'sv07', @pw, 'Bùi Văn Khoa',      'sv07@demo.local', '0922000007', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(18, 'sv08', @pw, 'Hoàng Gia Bảo',     'sv08@demo.local', '0922000008', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(19, 'sv09', @pw, 'Phan Thanh Tùng',   'sv09@demo.local', '0922000009', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(20, 'sv10', @pw, 'Đặng Mỹ Linh',      'sv10@demo.local', '0922000010', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(21, 'sv11', @pw, 'Ngô Đức Thịnh',     'sv11@demo.local', '0922000011', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(22, 'sv12', @pw, 'Dương Khánh Vy',    'sv12@demo.local', '0922000012', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(23, 'sv13', @pw, 'Lý Hữu Phước',      'sv13@demo.local', '0922000013', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(24, 'sv14', @pw, 'Trương Bảo Châu',   'sv14@demo.local', '0922000014', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(25, 'sv15', @pw, 'Mai Anh Dũng',      'sv15@demo.local', '0922000015', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(26, 'sv16', @pw, 'Cao Thị Hồng',      'sv16@demo.local', '0922000016', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(27, 'sv17', @pw, 'Đinh Công Danh',    'sv17@demo.local', '0922000017', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(28, 'sv18', @pw, 'Tạ Minh Khôi',      'sv18@demo.local', '0922000018', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(29, 'sv19', @pw, 'Huỳnh Ngọc Trâm',   'sv19@demo.local', '0922000019', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(30, 'sv20', @pw, 'Lâm Quốc Việt',     'sv20@demo.local', '0922000020', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -200 DAY)),
(31, 'sv21', @pw, 'Nguyễn Văn Khóa',   'sv21@demo.local', '0922000021', 'LOCKED', 0, DATE_ADD(@now, INTERVAL -200 DAY));  -- tài khoản bị khóa: thử đăng nhập phải bị chặn

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = CASE
    WHEN u.username = 'admin'       THEN 'ADMIN'
    WHEN u.username = 'truongkhoa'  THEN 'TRUONG_KHOA'
    WHEN u.username LIKE 'gv%'      THEN 'GIANG_VIEN'
    ELSE 'SINH_VIEN' END;

/* =====================================================================
   3. HỒ SƠ GIẢNG VIÊN (id GV n = n, user = n + 2) VÀ SINH VIÊN (id SV n = n, user = n + 10)
   ===================================================================== */
INSERT INTO lecturers (id, user_id, lecturer_code, academic_degree, department_id) VALUES
(1, 3,  'GV001', 'TS',     1),
(2, 4,  'GV002', 'PGS.TS', 1),
(3, 5,  'GV003', 'TS',     2),
(4, 6,  'GV004', 'ThS',    2),
(5, 7,  'GV005', 'ThS',    3),
(6, 8,  'GV006', 'TS',     3),
(7, 9,  'GV007', 'ThS',    1),
(8, 10, 'GV008', 'PGS.TS', 2);

INSERT INTO students (id, user_id, student_code, class_name, major) VALUES
( 1, 11, 'SV22001', 'DHKTPM18A', 'Kỹ thuật phần mềm'),
( 2, 12, 'SV22002', 'DHKTPM18A', 'Kỹ thuật phần mềm'),
( 3, 13, 'SV22003', 'DHKTPM18A', 'Kỹ thuật phần mềm'),
( 4, 14, 'SV22004', 'DHHTTT18A', 'Hệ thống thông tin'),
( 5, 15, 'SV22005', 'DHHTTT18A', 'Hệ thống thông tin'),
( 6, 16, 'SV22006', 'DHKHMT18A', 'Khoa học máy tính'),
( 7, 17, 'SV22007', 'DHKHMT18A', 'Khoa học máy tính'),
( 8, 18, 'SV22008', 'DHKHMT18A', 'Khoa học máy tính'),
( 9, 19, 'SV22009', 'DHHTTT18B', 'Hệ thống thông tin'),
(10, 20, 'SV22010', 'DHHTTT18B', 'Hệ thống thông tin'),
(11, 21, 'SV22011', 'DHKTPM18B', 'Kỹ thuật phần mềm'),
(12, 22, 'SV22012', 'DHKTPM18B', 'Kỹ thuật phần mềm'),
(13, 23, 'SV22013', 'DHKTPM18B', 'Kỹ thuật phần mềm'),
(14, 24, 'SV22014', 'DHKHMT18B', 'Khoa học máy tính'),
(15, 25, 'SV22015', 'DHKHMT18B', 'Khoa học máy tính'),
(16, 26, 'SV22016', 'DHKTPM18C', 'Kỹ thuật phần mềm'),
(17, 27, 'SV22017', 'DHKTPM18C', 'Kỹ thuật phần mềm'),
(18, 28, 'SV22018', 'DHKHMT18C', 'Khoa học máy tính'),
(19, 29, 'SV22019', 'DHHTTT18C', 'Hệ thống thông tin'),
(20, 30, 'SV22020', 'DHHTTT18C', 'Hệ thống thông tin'),
(21, 31, 'SV22021', 'DHKHMT18C', 'Khoa học máy tính');

/* =====================================================================
   4. ĐỢT ĐĂNG KÝ - mỗi đợt ở một giai đoạn khác nhau để test từng chức năng
      P1 TLCN  : đang GĐ1 (GV đề xuất)            -> test đề xuất/duyệt/công bố đề tài
      P2 KLTN  : đang GĐ2 (SV đăng ký)            -> test lập nhóm, đăng ký, duyệt, nộp báo cáo
      P3 KLTN  : đã hết đăng ký, đang phản biện   -> test hội đồng, chấm điểm, tổng hợp, công bố + email
      P4 TLCN  : đã hoàn tất, kết quả đã công bố  -> test tra cứu kết quả theo đợt
      P5 MON_HOC: nháp, chưa có đề tài            -> test sửa/xóa đợt
   ===================================================================== */
INSERT INTO registration_periods
 (id, name, period_type, lecturer_start_at, lecturer_end_at, student_start_at, student_end_at,
  review_deadline, council_report_date, status, created_by, created_at) VALUES
(1, 'TLCN HK1 2026-2027 - GĐ1 (GV đề xuất đề tài)', 'TLCN',
    DATE_ADD(@now, INTERVAL -3 DAY),  DATE_ADD(@now, INTERVAL 7 DAY),
    DATE_ADD(@now, INTERVAL 8 DAY),   DATE_ADD(@now, INTERVAL 21 DAY),
    DATE_ADD(@now, INTERVAL 35 DAY),  NULL, 'PUBLISHED', 2, DATE_ADD(@now, INTERVAL -5 DAY)),
(2, 'KLTN HK1 2026-2027 - GĐ2 (SV đăng ký đề tài)', 'KLTN',
    DATE_ADD(@now, INTERVAL -30 DAY), DATE_ADD(@now, INTERVAL -16 DAY),
    DATE_ADD(@now, INTERVAL -2 DAY),  DATE_ADD(@now, INTERVAL 12 DAY),
    DATE_ADD(@now, INTERVAL 25 DAY),  DATE_ADD(@now, INTERVAL 35 DAY), 'PUBLISHED', 2, DATE_ADD(@now, INTERVAL -35 DAY)),
(3, 'KLTN HK2 2025-2026 - Phản biện và chấm điểm', 'KLTN',
    DATE_ADD(@now, INTERVAL -80 DAY), DATE_ADD(@now, INTERVAL -66 DAY),
    DATE_ADD(@now, INTERVAL -65 DAY), DATE_ADD(@now, INTERVAL -50 DAY),
    DATE_ADD(@now, INTERVAL 6 DAY),   DATE_ADD(@now, INTERVAL 12 DAY), 'PUBLISHED', 2, DATE_ADD(@now, INTERVAL -85 DAY)),
(4, 'TLCN HK2 2025-2026 - Đã hoàn tất', 'TLCN',
    DATE_ADD(@now, INTERVAL -150 DAY), DATE_ADD(@now, INTERVAL -135 DAY),
    DATE_ADD(@now, INTERVAL -134 DAY), DATE_ADD(@now, INTERVAL -120 DAY),
    DATE_ADD(@now, INTERVAL -90 DAY),  NULL, 'CLOSED', 2, DATE_ADD(@now, INTERVAL -155 DAY)),
(5, 'Đồ án môn học HK2 2026-2027 - Bản nháp', 'MON_HOC',
    DATE_ADD(@now, INTERVAL 20 DAY),  DATE_ADD(@now, INTERVAL 30 DAY),
    DATE_ADD(@now, INTERVAL 31 DAY),  DATE_ADD(@now, INTERVAL 45 DAY),
    NULL, NULL, 'DRAFT', 2, @now);

/* =====================================================================
   5. ĐỀ TÀI + GVHD
   ===================================================================== */
INSERT INTO topics
 (id, period_id, department_id, created_by_lecturer_id, topic_code, topic_name, description,
  status, reject_reason, approved_by, approved_at, published, created_at) VALUES
-- P1 (GĐ1): đủ trạng thái để test duyệt / từ chối / công bố
( 1, 1, 1, 1, 'TLCN-26-001', 'Hệ thống gợi ý học phần cho sinh viên',
     'Xây dựng ứng dụng web gợi ý học phần dựa trên kết quả học tập và sở thích của sinh viên.',
     'PENDING',  NULL, NULL, NULL, 0, DATE_ADD(@now, INTERVAL -2 DAY)),
( 2, 1, 2, 3, 'TLCN-26-002', 'Quản lý kho vật tư theo mô hình ERP thu nhỏ',
     'Phân tích thiết kế và hiện thực module kho, nhập xuất, báo cáo tồn kho cho doanh nghiệp nhỏ.',
     'PENDING',  NULL, NULL, NULL, 0, DATE_ADD(@now, INTERVAL -2 DAY)),
( 3, 1, 3, 5, 'TLCN-26-003', 'Công cụ kiểm thử tự động cho API REST',
     'Xây dựng công cụ sinh và chạy bộ kiểm thử tự động từ đặc tả OpenAPI.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -1 DAY), 0, DATE_ADD(@now, INTERVAL -3 DAY)),
( 4, 1, 1, 7, 'TLCN-26-004', 'Nhận dạng cảm xúc từ văn bản tiếng Việt',
     'Huấn luyện mô hình phân loại cảm xúc cho bình luận tiếng Việt trên mạng xã hội.',
     'REJECTED', 'Phạm vi quá rộng, cần thu hẹp bộ dữ liệu và nêu rõ tiêu chí đánh giá.', 2, DATE_ADD(@now, INTERVAL -1 DAY), 0, DATE_ADD(@now, INTERVAL -3 DAY)),
( 5, 1, 2, 8, 'TLCN-26-005', 'Cổng thông tin đặt lịch phòng học',
     'Xây dựng hệ thống đặt và duyệt lịch sử dụng phòng học, phòng lab theo khoa.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -1 DAY), 1, DATE_ADD(@now, INTERVAL -3 DAY)),
-- P2 (GĐ2): đề tài đã công bố cho SV đăng ký
( 6, 2, 1, 1, 'KLTN-26-001', 'Nền tảng học trực tuyến có chấm bài tự động',
     'Xây dựng nền tảng quản lý khóa học, nộp bài và chấm bài lập trình tự động.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -20 DAY), 1, DATE_ADD(@now, INTERVAL -28 DAY)),
( 7, 2, 2, 3, 'KLTN-26-002', 'Hệ thống quản lý bệnh án điện tử cho phòng khám',
     'Thiết kế và hiện thực hệ thống hồ sơ bệnh án điện tử, phân quyền bác sĩ và lễ tân.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -20 DAY), 1, DATE_ADD(@now, INTERVAL -28 DAY)),
( 8, 2, 3, 5, 'KLTN-26-003', 'Ứng dụng quản lý dự án phần mềm theo Scrum',
     'Xây dựng công cụ quản lý backlog, sprint và biểu đồ burndown cho nhóm phát triển.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -20 DAY), 1, DATE_ADD(@now, INTERVAL -28 DAY)),
( 9, 2, 1, 7, 'KLTN-26-004', 'Phân tích dữ liệu giao thông đô thị bằng học máy',
     'Thu thập, xử lý dữ liệu giao thông và dự báo tình trạng ùn tắc theo khung giờ.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -20 DAY), 1, DATE_ADD(@now, INTERVAL -28 DAY)),
-- P3 (phản biện)
(11, 3, 1, 1, 'KLTN-25-101', 'Hệ thống nhận diện biển số xe tại bãi đỗ',
     'Nhận diện biển số xe từ camera và quản lý lượt vào ra bãi đỗ xe của trường.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -75 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
(12, 3, 2, 3, 'KLTN-25-102', 'Website thương mại điện tử đa gian hàng',
     'Xây dựng sàn thương mại điện tử cho phép nhiều gian hàng, giỏ hàng và thanh toán giả lập.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -75 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
(13, 3, 3, 5, 'KLTN-25-103', 'Hệ thống CI/CD tự động cho đồ án sinh viên',
     'Thiết lập quy trình build, test, triển khai tự động và giám sát cho các đồ án của khoa.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -75 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
(14, 3, 1, 7, 'KLTN-25-104', 'Chatbot hỗ trợ tư vấn tuyển sinh',
     'Xây dựng chatbot trả lời câu hỏi tuyển sinh dựa trên cơ sở tri thức của khoa.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -75 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
-- P4 (đã hoàn tất)
(15, 4, 1, 2, 'TLCN-25-201', 'Ứng dụng quản lý chi tiêu cá nhân',
     'Xây dựng ứng dụng web ghi nhận thu chi, thống kê theo danh mục và cảnh báo ngân sách.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -145 DAY), 1, DATE_ADD(@now, INTERVAL -148 DAY)),
(16, 4, 2, 4, 'TLCN-25-202', 'Hệ thống quản lý thư viện khoa',
     'Xây dựng hệ thống mượn trả sách, tìm kiếm tài liệu và thông báo quá hạn.',
     'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -145 DAY), 1, DATE_ADD(@now, INTERVAL -148 DAY));

INSERT INTO topic_supervisors (topic_id, lecturer_id, supervisor_order, assigned_by, assigned_at) VALUES
( 1, 1, 1, NULL, DATE_ADD(@now, INTERVAL -2 DAY)),
( 2, 3, 1, NULL, DATE_ADD(@now, INTERVAL -2 DAY)),
( 2, 4, 2, NULL, DATE_ADD(@now, INTERVAL -2 DAY)),   -- đề tài có 2 GVHD
( 3, 5, 1, NULL, DATE_ADD(@now, INTERVAL -3 DAY)),
( 4, 7, 1, NULL, DATE_ADD(@now, INTERVAL -3 DAY)),
( 5, 8, 1, NULL, DATE_ADD(@now, INTERVAL -3 DAY)),
( 6, 1, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY)),
( 6, 2, 2, 2,    DATE_ADD(@now, INTERVAL -22 DAY)),  -- Trưởng khoa bổ sung GVHD thứ hai
( 7, 3, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY)),
( 8, 5, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY)),
( 9, 7, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY)),
(11, 1, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(12, 3, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(13, 5, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(13, 6, 2, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(14, 7, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(15, 2, 1, NULL, DATE_ADD(@now, INTERVAL -148 DAY)),
(16, 4, 1, NULL, DATE_ADD(@now, INTERVAL -148 DAY));

/* =====================================================================
   6. NHÓM SINH VIÊN VÀ THÀNH VIÊN
      sv01-sv03 và sv04-sv05 tham gia CẢ 2 đợt (P4 đã xong, P2 đang đăng ký)
      -> test "tra cứu điểm theo đợt" và "UNIQUE(student, period)".
      sv18, sv19, sv20 chưa có nhóm ở P2 -> test tạo nhóm/tham gia nhóm.
   ===================================================================== */
INSERT INTO student_groups (id, period_id, group_code, group_name, topic_id, status, created_at) VALUES
(1, 2, 'KLTN26-01', 'Nhóm EduCode',     6,    'APPROVED',  DATE_ADD(@now, INTERVAL -3 DAY)),
(2, 2, 'KLTN26-02', 'Nhóm MediRecord',  NULL, 'REGISTERED', DATE_ADD(@now, INTERVAL -2 DAY)),
(3, 2, 'KLTN26-03', 'Nhóm Scrumboard',  NULL, 'DRAFT',     DATE_ADD(@now, INTERVAL -1 DAY)),
(4, 3, 'KLTN25-01', 'Nhóm PlateVision', 11,   'SUBMITTED', DATE_ADD(@now, INTERVAL -64 DAY)),
(5, 3, 'KLTN25-02', 'Nhóm MultiShop',   12,   'SUBMITTED', DATE_ADD(@now, INTERVAL -64 DAY)),
(6, 3, 'KLTN25-03', 'Nhóm AutoPipeline',13,   'SUBMITTED', DATE_ADD(@now, INTERVAL -64 DAY)),
(7, 3, 'KLTN25-04', 'Nhóm AdmissionBot',14,   'SUBMITTED', DATE_ADD(@now, INTERVAL -64 DAY)),
(8, 4, 'TLCN25-01', 'Nhóm MoneyTrack',  15,   'COMPLETED', DATE_ADD(@now, INTERVAL -133 DAY)),
(9, 4, 'TLCN25-02', 'Nhóm LibSys',      16,   'COMPLETED', DATE_ADD(@now, INTERVAL -133 DAY));

INSERT INTO group_members (group_id, period_id, student_id, is_leader, joined_at) VALUES
(1, 2,  1, 1, DATE_ADD(@now, INTERVAL -3 DAY)), (1, 2,  2, 0, DATE_ADD(@now, INTERVAL -3 DAY)), (1, 2,  3, 0, DATE_ADD(@now, INTERVAL -3 DAY)),
(2, 2,  4, 1, DATE_ADD(@now, INTERVAL -2 DAY)), (2, 2,  5, 0, DATE_ADD(@now, INTERVAL -2 DAY)),
(3, 2, 16, 1, DATE_ADD(@now, INTERVAL -1 DAY)), (3, 2, 17, 0, DATE_ADD(@now, INTERVAL -1 DAY)),
(4, 3,  6, 1, DATE_ADD(@now, INTERVAL -64 DAY)), (4, 3,  7, 0, DATE_ADD(@now, INTERVAL -64 DAY)), (4, 3,  8, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
(5, 3,  9, 1, DATE_ADD(@now, INTERVAL -64 DAY)), (5, 3, 10, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
(6, 3, 11, 1, DATE_ADD(@now, INTERVAL -64 DAY)), (6, 3, 12, 0, DATE_ADD(@now, INTERVAL -64 DAY)), (6, 3, 13, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
(7, 3, 14, 1, DATE_ADD(@now, INTERVAL -64 DAY)), (7, 3, 15, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
(8, 4,  1, 1, DATE_ADD(@now, INTERVAL -133 DAY)), (8, 4,  2, 0, DATE_ADD(@now, INTERVAL -133 DAY)), (8, 4,  3, 0, DATE_ADD(@now, INTERVAL -133 DAY)),
(9, 4,  4, 1, DATE_ADD(@now, INTERVAL -133 DAY)), (9, 4,  5, 0, DATE_ADD(@now, INTERVAL -133 DAY));

/* ---------- Đăng ký đề tài ---------- */
INSERT INTO topic_registrations
 (group_id, topic_id, status, registered_at, approved_by, approved_at, rejection_reason) VALUES
(1, 6,  'APPROVED', DATE_ADD(@now, INTERVAL -2 DAY),   4, DATE_ADD(@now, INTERVAL -1 DAY),   NULL),   -- gv02 (GVHD thứ hai của đề tài 6) duyệt
(2, 7,  'PENDING',  DATE_ADD(@now, INTERVAL -1 DAY),   NULL, NULL, NULL),                              -- chờ GVHD/Trưởng khoa duyệt
(3, 8,  'REJECTED', DATE_ADD(@now, INTERVAL -1 DAY),   2, DATE_ADD(@now, INTERVAL -1 DAY),   'Nhóm chưa nêu rõ phạm vi, vui lòng đăng ký lại sau khi trao đổi với GVHD.'),
(4, 11, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY),  2, DATE_ADD(@now, INTERVAL -61 DAY),  NULL),
(5, 12, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY),  2, DATE_ADD(@now, INTERVAL -61 DAY),  NULL),
(6, 13, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY),  2, DATE_ADD(@now, INTERVAL -61 DAY),  NULL),
(7, 14, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY),  2, DATE_ADD(@now, INTERVAL -61 DAY),  NULL),
(8, 15, 'APPROVED', DATE_ADD(@now, INTERVAL -132 DAY), 2, DATE_ADD(@now, INTERVAL -131 DAY), NULL),
(9, 16, 'APPROVED', DATE_ADD(@now, INTERVAL -132 DAY), 2, DATE_ADD(@now, INTERVAL -131 DAY), NULL);

/* ---------- Báo cáo (tệp thật tạo bởi tao_bao_cao_mau.py, đường dẫn tương đối app.storage.dir) ---------- */
INSERT INTO reports
 (id, group_id, submitted_by, report_title, file_name, stored_path, file_size, version, description, submitted_at, status) VALUES
(1, 4,  6, 'Báo cáo KLTN - Nhận diện biển số xe (bản 1)', 'bao_cao_nhom4_v1.pdf', 'demo/KLTN-25-101_v1.pdf', 614, 1, 'Bản nộp đầu tiên.',        DATE_ADD(@now, INTERVAL -45 DAY), 'SUPERSEDED'),
(2, 4,  6, 'Báo cáo KLTN - Nhận diện biển số xe (bản 2)', 'bao_cao_nhom4_v2.pdf', 'demo/KLTN-25-101_v2.pdf', 614, 2, 'Bản chỉnh sửa theo góp ý.', DATE_ADD(@now, INTERVAL -30 DAY), 'SUBMITTED'),
(3, 5,  9, 'Báo cáo KLTN - Website thương mại điện tử',   'bao_cao_nhom5.pdf',    'demo/KLTN-25-102_v1.pdf', 614, 1, NULL,                       DATE_ADD(@now, INTERVAL -30 DAY), 'SUBMITTED'),
(4, 6, 11, 'Báo cáo KLTN - Hệ thống CI/CD',                'bao_cao_nhom6.pdf',    'demo/KLTN-25-103_v1.pdf', 614, 1, NULL,                       DATE_ADD(@now, INTERVAL -30 DAY), 'SUBMITTED'),
(5, 7, 14, 'Báo cáo KLTN - Chatbot tư vấn tuyển sinh',     'bao_cao_nhom7.pdf',    'demo/KLTN-25-104_v1.pdf', 614, 1, NULL,                       DATE_ADD(@now, INTERVAL -29 DAY), 'SUBMITTED'),
(6, 8,  1, 'Báo cáo TLCN - Quản lý chi tiêu cá nhân',      'bao_cao_nhom8.pdf',    'demo/TLCN-25-201_v1.pdf', 614, 1, NULL,                       DATE_ADD(@now, INTERVAL -100 DAY), 'SUBMITTED'),
(7, 9,  4, 'Báo cáo TLCN - Quản lý thư viện khoa',         'bao_cao_nhom9.pdf',    'demo/TLCN-25-202_v1.pdf', 614, 1, NULL,                       DATE_ADD(@now, INTERVAL -100 DAY), 'SUBMITTED');

/* =====================================================================
   7. HỘI ĐỒNG
      C1 (P3): 5 GV, 2 đề tài - T11 còn thiếu điểm của gv06, T12 đủ điểm chờ chủ tịch tổng hợp
      C2 (P3): 3 GV, đề tài 13 đã tổng hợp, CHƯA công bố  -> Trưởng khoa công bố + gửi email
      C3 (P4): 3 GV, 2 đề tài, đã công bố                -> dữ liệu lịch sử
      C4 (P3): chỉ 2 GV  -> hội đồng KHÔNG hợp lệ, thử thêm thành viên
      Đề tài 14 chưa xếp hội đồng nào -> thử xếp đề tài vào hội đồng
   ===================================================================== */
INSERT INTO councils (id, period_id, council_code, council_name, report_date, status, created_by, created_at) VALUES
(1, 3, 'HD-KLTN-25-A', 'Hội đồng KLTN 2025-2026 - A', DATE_ADD(@now, INTERVAL 12 DAY), 'READY',    2, DATE_ADD(@now, INTERVAL -20 DAY)),
(2, 3, 'HD-KLTN-25-B', 'Hội đồng KLTN 2025-2026 - B', DATE_ADD(@now, INTERVAL 13 DAY), 'READY',    2, DATE_ADD(@now, INTERVAL -20 DAY)),
(3, 4, 'HD-TLCN-25-C', 'Hội đồng TLCN 2025-2026 - C', DATE_ADD(@now, INTERVAL -95 DAY),'FINISHED', 2, DATE_ADD(@now, INTERVAL -110 DAY)),
(4, 3, 'HD-KLTN-25-D', 'Hội đồng KLTN 2025-2026 - D (chưa đủ thành viên)', NULL,        'DRAFT',    2, DATE_ADD(@now, INTERVAL -2 DAY));

INSERT INTO council_members (council_id, lecturer_id, role, joined_at) VALUES
(1, 2, 'CHAIRMAN',  DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv02 là chủ tịch hội đồng A
(1, 4, 'SECRETARY', DATE_ADD(@now, INTERVAL -20 DAY)),
(1, 6, 'MEMBER',    DATE_ADD(@now, INTERVAL -20 DAY)),
(1, 1, 'MEMBER',    DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv01 là GVHD đề tài 11 -> KHÔNG được chấm đề tài 11
(1, 3, 'MEMBER',    DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv03 là GVHD đề tài 12 -> KHÔNG được chấm đề tài 12
(2, 8, 'CHAIRMAN',  DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv08 là chủ tịch hội đồng B
(2, 7, 'SECRETARY', DATE_ADD(@now, INTERVAL -20 DAY)),
(2, 1, 'MEMBER',    DATE_ADD(@now, INTERVAL -20 DAY)),
(3, 2, 'CHAIRMAN',  DATE_ADD(@now, INTERVAL -110 DAY)),
(3, 3, 'SECRETARY', DATE_ADD(@now, INTERVAL -110 DAY)),
(3, 5, 'MEMBER',    DATE_ADD(@now, INTERVAL -110 DAY)),
(4, 7, 'CHAIRMAN',  DATE_ADD(@now, INTERVAL -2 DAY)),
(4, 8, 'SECRETARY', DATE_ADD(@now, INTERVAL -2 DAY));

INSERT INTO council_topics (council_id, topic_id, added_at) VALUES
(1, 11, DATE_ADD(@now, INTERVAL -19 DAY)),
(1, 12, DATE_ADD(@now, INTERVAL -19 DAY)),
(2, 13, DATE_ADD(@now, INTERVAL -19 DAY)),
(3, 15, DATE_ADD(@now, INTERVAL -109 DAY)),
(3, 16, DATE_ADD(@now, INTERVAL -109 DAY));

/* ---------- Phân công chấm (không ai chấm đề tài mình hướng dẫn) ---------- */
INSERT INTO review_assignments (id, council_id, topic_id, lecturer_id, assigned_by, assigned_at, status) VALUES
( 1, 1, 11, 2, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 2, 1, 11, 4, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 3, 1, 11, 6, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'ASSIGNED'),    -- gv06 CHƯA nhập điểm: đăng nhập gv06 để thử nhập
( 4, 1, 12, 2, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 5, 1, 12, 4, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 6, 1, 12, 6, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 7, 2, 13, 8, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 8, 2, 13, 7, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 9, 2, 13, 1, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
(10, 3, 15, 3, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(11, 3, 15, 5, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(12, 3, 16, 2, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(13, 3, 16, 3, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(14, 3, 16, 5, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED');

INSERT INTO evaluations (assignment_id, score, comment, evaluated_at, status) VALUES
( 1, 8.50, 'Hệ thống hoàn chỉnh, độ chính xác nhận diện tốt, báo cáo trình bày rõ ràng.', DATE_ADD(@now, INTERVAL -5 DAY), 'SUBMITTED'),
( 2, 8.00, 'Đạt yêu cầu, cần bổ sung thêm kịch bản kiểm thử trong điều kiện thiếu sáng.',  DATE_ADD(@now, INTERVAL -4 DAY), 'SUBMITTED'),
( 4, 7.00, 'Chức năng đầy đủ nhưng phần bảo mật thanh toán còn sơ sài.',                    DATE_ADD(@now, INTERVAL -5 DAY), 'SUBMITTED'),
( 5, 7.50, 'Giao diện tốt, mã nguồn có cấu trúc, cần thêm kiểm thử tự động.',              DATE_ADD(@now, INTERVAL -4 DAY), 'SUBMITTED'),
( 6, 8.00, 'Sản phẩm hoàn thiện, demo ổn định.',                                            DATE_ADD(@now, INTERVAL -4 DAY), 'SUBMITTED'),
( 7, 9.00, 'Quy trình CI/CD đầy đủ, tài liệu rõ ràng.',                                     DATE_ADD(@now, INTERVAL -6 DAY), 'SUBMITTED'),
( 8, 8.50, 'Triển khai tốt, cần mô tả thêm phần giám sát.',                                 DATE_ADD(@now, INTERVAL -6 DAY), 'SUBMITTED'),
( 9, 9.50, 'Xuất sắc, vượt yêu cầu đề ra.',                                                 DATE_ADD(@now, INTERVAL -6 DAY), 'SUBMITTED'),
(10, 7.50, 'Đạt yêu cầu, giao diện cần cải thiện.',                                         DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(11, 8.00, 'Hoàn thành tốt các chức năng chính.',                                           DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(12, 4.50, 'Thiếu nhiều chức năng cốt lõi, báo cáo chưa đạt.',                              DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(13, 4.00, 'Chưa đạt yêu cầu tối thiểu về kiểm thử và tài liệu.',                           DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(14, 5.00, 'Ở mức tối thiểu.',                                                              DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED');

/* ---------- Kết quả: đề tài 13 đã tổng hợp nhưng CHƯA công bố; đề tài 15, 16 đã công bố ---------- */
INSERT INTO review_results
 (id, group_id, topic_id, council_id, final_score, result, summary_comment, calculated_by, calculated_at, published, published_at, published_by) VALUES
(1, 6, 13, 2, 9.00, 'PASSED', 'Hội đồng đánh giá cao tính thực tiễn và mức độ hoàn thiện của hệ thống.', 10, DATE_ADD(@now, INTERVAL -1 DAY),   0, NULL, NULL),
(2, 8, 15, 3, 7.75, 'PASSED', 'Đạt yêu cầu, cần hoàn thiện thêm giao diện.',                              4,  DATE_ADD(@now, INTERVAL -92 DAY),  1, DATE_ADD(@now, INTERVAL -90 DAY), 2),
(3, 9, 16, 3, 4.50, 'FAILED', 'Chưa đạt: thiếu chức năng cốt lõi và tài liệu kiểm thử.',                   4,  DATE_ADD(@now, INTERVAL -92 DAY),  1, DATE_ADD(@now, INTERVAL -90 DAY), 2);

/* ---------- Nhật ký email của 2 kết quả đã công bố (1 email FAILED để thử "Gửi lại") ---------- */
INSERT INTO email_logs (result_id, student_id, to_email, subject, status, error_message, attempt_count, created_at, sent_at) VALUES
(2, 1, 'sv01@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY)),
(2, 2, 'sv02@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY)),
(2, 3, 'sv03@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY)),
(3, 4, 'sv04@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY)),
(3, 5, 'sv05@demo.local', 'Kết quả TLCN HK2 2025-2026', 'FAILED', 'Không kết nối được máy chủ SMTP (timeout).', 3, DATE_ADD(@now, INTERVAL -90 DAY), NULL);

/* =====================================================================
   8. THÔNG BÁO (không có dòng target = gửi tất cả vai trò)
   ===================================================================== */
INSERT INTO notifications (id, title, content, type, created_by, status, created_at, published_at) VALUES
(1, 'Lịch làm việc của trường dịp nghỉ lễ', 'Nhà trường thông báo lịch nghỉ lễ và lịch làm việc của các phòng ban. Vui lòng theo dõi để sắp xếp công việc.', 'SCHOOL',  1, 'PUBLISHED', DATE_ADD(@now, INTERVAL -6 DAY), DATE_ADD(@now, INTERVAL -6 DAY)),
(2, 'Mở đăng ký đề tài KLTN HK1 2026-2027',  'Sinh viên lập nhóm và đăng ký đề tài đã công bố trong giai đoạn quy định. Nhóm trưởng là người đăng ký và nộp báo cáo.', 'FACULTY', 2, 'PUBLISHED', DATE_ADD(@now, INTERVAL -2 DAY), DATE_ADD(@now, INTERVAL -2 DAY)),
(3, 'Hạn chót nộp điểm phản biện',            'Giảng viên được phân công chấm vui lòng nhập điểm và nhận xét trước hạn chót của đợt.', 'FACULTY', 2, 'PUBLISHED', DATE_ADD(@now, INTERVAL -1 DAY), DATE_ADD(@now, INTERVAL -1 DAY)),
(4, 'Kế hoạch bảo vệ hội đồng (bản nháp)',   'Lịch bảo vệ dự kiến của các hội đồng sẽ được công bố sau khi hoàn tất lập hội đồng.', 'FACULTY', 2, 'DRAFT', @now, NULL),
(5, 'Thông báo đã thu hồi',                  'Nội dung thông báo này đã được thu hồi do cập nhật lịch.', 'SCHOOL', 1, 'RETRACTED', DATE_ADD(@now, INTERVAL -10 DAY), DATE_ADD(@now, INTERVAL -10 DAY));

INSERT INTO notification_target_roles (notification_id, role_id)
SELECT 2, id FROM roles WHERE code = 'SINH_VIEN'
UNION ALL SELECT 3, id FROM roles WHERE code = 'GIANG_VIEN'
UNION ALL SELECT 4, id FROM roles WHERE code = 'GIANG_VIEN';

/* =====================================================================
   9. NHẬT KÝ HỆ THỐNG
   ===================================================================== */
INSERT INTO audit_logs (user_id, action, entity, entity_id, detail, created_at) VALUES
(2, 'PERIOD_PUBLISH',  'registration_periods', 2,  'Công bố đợt KLTN HK1 2026-2027',               DATE_ADD(@now, INTERVAL -34 DAY)),
(2, 'SUPERVISOR_ASSIGN','topics',              6,  'Thêm GVHD thứ hai (GV002) cho đề tài KLTN-26-001', DATE_ADD(@now, INTERVAL -22 DAY)),
(2, 'RESULT_PUBLISH',  'review_results',       2,  'Công bố kết quả đề tài TLCN-25-201',            DATE_ADD(@now, INTERVAL -90 DAY));

/* =====================================================================
   10. KIỂM TRA NHANH SAU KHI NẠP
   ===================================================================== */
SELECT 'users' AS bang, COUNT(*) AS so_dong FROM users
UNION ALL SELECT 'registration_periods', COUNT(*) FROM registration_periods
UNION ALL SELECT 'topics', COUNT(*) FROM topics
UNION ALL SELECT 'student_groups', COUNT(*) FROM student_groups
UNION ALL SELECT 'review_results', COUNT(*) FROM review_results;

SELECT council_id, member_count, chairman_count, secretary_count, is_valid FROM v_council_validity;