CREATE DATABASE IF NOT EXISTS student_topic_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE student_topic_management;

CREATE TABLE roles (
    role_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
);

CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE,
    phone VARCHAR(30),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

CREATE TABLE registration_periods (
    period_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(50) NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    status VARCHAR(30) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE topics (
    topic_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    field VARCHAR(150),
    category VARCHAR(100),
    lecturer_id BIGINT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_topics_lecturer FOREIGN KEY (lecturer_id) REFERENCES users(user_id),
    CONSTRAINT fk_topics_approver FOREIGN KEY (approved_by) REFERENCES users(user_id)
);

CREATE TABLE topic_registrations (
    registration_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    registration_status VARCHAR(30) DEFAULT 'WAITING_CONFIRMATION',
    lecturer_confirmation_status VARCHAR(30) DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at DATETIME,
    CONSTRAINT fk_reg_topic FOREIGN KEY (topic_id) REFERENCES topics(topic_id) ON DELETE CASCADE,
    CONSTRAINT fk_reg_student FOREIGN KEY (student_id) REFERENCES users(user_id)
);

CREATE TABLE submissions (
    submission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    submitted_at DATETIME NOT NULL,
    status VARCHAR(30) DEFAULT 'SUBMITTED',
    notes TEXT,
    CONSTRAINT fk_submissions_registration FOREIGN KEY (registration_id) REFERENCES topic_registrations(registration_id) ON DELETE CASCADE
);

CREATE TABLE review_assignments (
    assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    topic_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    assigned_by BIGINT NOT NULL,
    assignment_status VARCHAR(30) DEFAULT 'ASSIGNED',
    assigned_at DATETIME NOT NULL,
    CONSTRAINT fk_assignment_topic FOREIGN KEY (topic_id) REFERENCES topics(topic_id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(user_id),
    CONSTRAINT fk_assignment_assigner FOREIGN KEY (assigned_by) REFERENCES users(user_id)
);

CREATE TABLE evaluations (
    evaluation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    score DECIMAL(5,2),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evaluation_assignment FOREIGN KEY (assignment_id) REFERENCES review_assignments(assignment_id) ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(user_id)
);

CREATE TABLE results (
    result_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_id BIGINT NOT NULL,
    average_score DECIMAL(5,2),
    final_result VARCHAR(30) DEFAULT 'PENDING',
    published_at DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_results_registration FOREIGN KEY (registration_id) REFERENCES topic_registrations(registration_id) ON DELETE CASCADE
);

CREATE TABLE notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    sender_id BIGINT NOT NULL,
    target_role_id BIGINT,
    target_user_id BIGINT,
    notification_type VARCHAR(50) DEFAULT 'SYSTEM',
    is_read TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
    CONSTRAINT fk_notification_role FOREIGN KEY (target_role_id) REFERENCES roles(role_id),
    CONSTRAINT fk_notification_user FOREIGN KEY (target_user_id) REFERENCES users(user_id)
);

CREATE TABLE councils (
    council_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    chairman_id BIGINT,
    secretary_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_council_chairman FOREIGN KEY (chairman_id) REFERENCES users(user_id),
    CONSTRAINT fk_council_secretary FOREIGN KEY (secretary_id) REFERENCES users(user_id)
);

CREATE TABLE council_members (
    council_member_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    council_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    CONSTRAINT fk_council_member_council FOREIGN KEY (council_id) REFERENCES councils(council_id) ON DELETE CASCADE,
    CONSTRAINT fk_council_member_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

INSERT INTO roles (name, description) VALUES
('ADMIN', 'Quản trị hệ thống'),
('TRUONG_KHOA', 'Trưởng khoa'),
('CHU_TICH_HOI_DONG', 'Chủ tịch hội đồng'),
('GIANG_VIEN', 'Giảng viên'),
('SINH_VIEN', 'Sinh viên');

INSERT INTO permissions (code, name, description) VALUES
('USER_MANAGE', 'Quản lý người dùng', 'Tạo, sửa, khóa tài khoản'),
('ROLE_MANAGE', 'Quản lý vai trò', 'Quản lý danh sách vai trò'),
('PERMISSION_MANAGE', 'Quản lý quyền', 'Gán quyền cho vai trò'),
('TOPIC_VIEW', 'Xem đề tài', 'Xem danh sách đề tài'),
('TOPIC_CREATE', 'Tạo đề tài', 'Đề xuất đề tài mới'),
('TOPIC_APPROVE', 'Duyệt đề tài', 'Phê duyệt đề tài'),
('REGISTRATION_CREATE', 'Đăng ký đề tài', 'Sinh viên đăng ký đề tài'),
('REGISTRATION_CONFIRM', 'Xác nhận đăng ký', 'Giảng viên xác nhận sinh viên'),
('SUBMISSION_CREATE', 'Nộp bài', 'Sinh viên nộp đề tài'),
('ASSIGNMENT_MANAGE', 'Phân công chấm', 'Chủ tịch phân công giảng viên chấm'),
('EVALUATION_CREATE', 'Nhập điểm', 'Giảng viên chấm điểm'),
('RESULT_CALCULATE', 'Tính điểm trung bình', 'Tổng hợp điểm đánh giá'),
('RESULT_PUBLISH', 'Công bố kết quả', 'Công bố điểm cho sinh viên'),
('RESULT_VIEW', 'Xem kết quả đã công bố', 'Sinh viên xem điểm đã công bố của nhóm mình'),
('PERIOD_MANAGE', 'Quản lý đợt đăng ký', 'Trưởng khoa tạo và công bố đợt đăng ký'),
('COUNCIL_MANAGE', 'Quản lý hội đồng', 'Chủ tịch hội đồng quản lý hội đồng phản biện'),
('NOTICE_VIEW', 'Xem thông báo', 'Xem thông báo hệ thống'),
('NOTICE_MANAGE', 'Quản lý thông báo', 'Gửi và quản lý thông báo');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON (
     (r.name = 'ADMIN' AND p.code IN ('USER_MANAGE','ROLE_MANAGE','PERMISSION_MANAGE','TOPIC_VIEW','TOPIC_CREATE','TOPIC_APPROVE','REGISTRATION_CREATE','REGISTRATION_CONFIRM','SUBMISSION_CREATE','ASSIGNMENT_MANAGE','EVALUATION_CREATE','RESULT_CALCULATE','RESULT_PUBLISH','RESULT_VIEW','PERIOD_MANAGE','COUNCIL_MANAGE','NOTICE_VIEW','NOTICE_MANAGE'))
 OR (r.name = 'TRUONG_KHOA' AND p.code IN ('TOPIC_VIEW','PERIOD_MANAGE','NOTICE_VIEW','NOTICE_MANAGE'))
 OR (r.name = 'CHU_TICH_HOI_DONG' AND p.code IN ('TOPIC_VIEW','TOPIC_APPROVE','ASSIGNMENT_MANAGE','RESULT_CALCULATE','RESULT_PUBLISH','COUNCIL_MANAGE','NOTICE_VIEW','NOTICE_MANAGE'))
 OR (r.name = 'GIANG_VIEN' AND p.code IN ('TOPIC_VIEW','TOPIC_CREATE','REGISTRATION_CONFIRM','EVALUATION_CREATE','NOTICE_VIEW'))
 OR (r.name = 'SINH_VIEN' AND p.code IN ('TOPIC_VIEW','REGISTRATION_CREATE','SUBMISSION_CREATE','RESULT_VIEW','NOTICE_VIEW'))
);

CREATE INDEX idx_topics_lecturer ON topics(lecturer_id);
CREATE INDEX idx_topic_reg_student ON topic_registrations(student_id);
CREATE INDEX idx_review_assignments_reviewer ON review_assignments(reviewer_id);
CREATE INDEX idx_notifications_user ON notifications(target_user_id);
CREATE INDEX idx_notifications_role ON notifications(target_role_id);
