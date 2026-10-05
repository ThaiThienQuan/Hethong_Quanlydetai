CREATE DATABASE student_topic_management
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE student_topic_management;

SELECT DATABASE();

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE permissions (
    permission_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
);

CREATE TABLE departments (
    department_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_code VARCHAR(20) NOT NULL UNIQUE,
    department_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE students (
    student_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    student_code VARCHAR(20) NOT NULL UNIQUE,
    class_name VARCHAR(50) NOT NULL,
    major VARCHAR(100) NOT NULL,

    CONSTRAINT fk_students_users
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
);

CREATE TABLE lecturers (
    lecturer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    lecturer_code VARCHAR(20) NOT NULL UNIQUE,
    academic_degree VARCHAR(50),
    department_id BIGINT NOT NULL,

    CONSTRAINT fk_lecturers_users
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    CONSTRAINT fk_lecturers_departments
        FOREIGN KEY (department_id)
        REFERENCES departments(department_id)
);

CREATE TABLE registration_periods (
    period_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    period_name VARCHAR(150) NOT NULL,

    period_type VARCHAR(20) NOT NULL,

    lecturer_start_at DATETIME NOT NULL,
    lecturer_end_at DATETIME NOT NULL,

    student_start_at DATETIME NOT NULL,
    student_end_at DATETIME NOT NULL,

    review_deadline DATETIME,
    council_report_date DATETIME,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_periods_users
        FOREIGN KEY (created_by)
        REFERENCES users(user_id),

    CONSTRAINT chk_period_type
        CHECK (period_type IN ('MON_HOC', 'NCKH', 'TLCN', 'KLTN'))
);

CREATE TABLE topics (
    topic_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    period_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    created_by_lecturer_id BIGINT NOT NULL,

    topic_code VARCHAR(30) NOT NULL,
    topic_name VARCHAR(200) NOT NULL,
    description TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    published BOOLEAN NOT NULL DEFAULT FALSE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at DATETIME,

    CONSTRAINT fk_topics_periods
        FOREIGN KEY (period_id)
        REFERENCES registration_periods(period_id),

    CONSTRAINT fk_topics_departments
        FOREIGN KEY (department_id)
        REFERENCES departments(department_id),

    CONSTRAINT fk_topics_lecturers
        FOREIGN KEY (created_by_lecturer_id)
        REFERENCES lecturers(lecturer_id)
);

CREATE TABLE topic_supervisors (
    topic_supervisor_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    topic_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,

    supervisor_order INT NOT NULL,

    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_topic_supervisors_topics
        FOREIGN KEY (topic_id)
        REFERENCES topics(topic_id),

    CONSTRAINT fk_topic_supervisors_lecturers
        FOREIGN KEY (lecturer_id)
        REFERENCES lecturers(lecturer_id),

    CONSTRAINT uq_topic_supervisor
        UNIQUE (topic_id, lecturer_id),

    CONSTRAINT chk_supervisor_order
        CHECK (supervisor_order IN (1, 2))
);

CREATE TABLE student_groups (
    group_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    period_id BIGINT NOT NULL,
    topic_id BIGINT UNIQUE,

    group_code VARCHAR(30) NOT NULL,
    group_name VARCHAR(100) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    report_url VARCHAR(500),

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_groups_periods
        FOREIGN KEY (period_id)
        REFERENCES registration_periods(period_id)
);

CREATE TABLE group_members (
    group_member_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,

    is_leader BOOLEAN NOT NULL DEFAULT FALSE,

    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_group_members_groups
        FOREIGN KEY (group_id)
        REFERENCES student_groups(group_id),

    CONSTRAINT fk_group_members_students
        FOREIGN KEY (student_id)
        REFERENCES students(student_id),

    CONSTRAINT uq_group_student
        UNIQUE (group_id, student_id)
);



CREATE TABLE IF NOT EXISTS topic_registrations (
    registration_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    group_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    approved_by BIGINT,
    approved_at DATETIME,

    rejection_reason VARCHAR(500),

    CONSTRAINT fk_topic_registrations_groups
        FOREIGN KEY (group_id)
        REFERENCES student_groups(group_id),

    CONSTRAINT fk_topic_registrations_topics
        FOREIGN KEY (topic_id)
        REFERENCES topics(topic_id),

    CONSTRAINT fk_topic_registrations_users
        FOREIGN KEY (approved_by)
        REFERENCES users(user_id)
);

CREATE TABLE reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    group_id BIGINT NOT NULL,
    submitted_by BIGINT NOT NULL,

    report_title VARCHAR(200) NOT NULL,
    file_url VARCHAR(500) NOT NULL,

    version INT NOT NULL DEFAULT 1,

    description VARCHAR(500),

    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',

    CONSTRAINT fk_reports_groups
        FOREIGN KEY (group_id)
        REFERENCES student_groups(group_id),

    CONSTRAINT fk_reports_students
        FOREIGN KEY (submitted_by)
        REFERENCES students(student_id),

    CONSTRAINT chk_report_version
        CHECK (version > 0)
);

CREATE TABLE councils (
    council_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    period_id BIGINT NOT NULL,

    council_code VARCHAR(30) NOT NULL UNIQUE,
    council_name VARCHAR(150) NOT NULL,

    report_date DATETIME NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',

    CONSTRAINT fk_councils_periods
        FOREIGN KEY (period_id)
        REFERENCES registration_periods(period_id)
);

CREATE TABLE council_members (
    council_member_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    council_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,

    role VARCHAR(20) NOT NULL,

    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_council_members_councils
        FOREIGN KEY (council_id)
        REFERENCES councils(council_id),

    CONSTRAINT fk_council_members_lecturers
        FOREIGN KEY (lecturer_id)
        REFERENCES lecturers(lecturer_id),

    CONSTRAINT uq_council_lecturer
        UNIQUE (council_id, lecturer_id),

    CONSTRAINT chk_council_role
        CHECK (role IN ('CHAIRMAN', 'SECRETARY', 'MEMBER'))
);

CREATE TABLE review_assignments (
    assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    council_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,

    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',

    CONSTRAINT fk_assignments_councils
        FOREIGN KEY (council_id)
        REFERENCES councils(council_id),

    CONSTRAINT fk_assignments_topics
        FOREIGN KEY (topic_id)
        REFERENCES topics(topic_id),

    CONSTRAINT fk_assignments_lecturers
        FOREIGN KEY (lecturer_id)
        REFERENCES lecturers(lecturer_id),

    CONSTRAINT uq_review_assignment
        UNIQUE (council_id, topic_id, lecturer_id)
);

CREATE TABLE evaluations (
    evaluation_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    assignment_id BIGINT NOT NULL,

    score DECIMAL(4,2) NOT NULL,
    comment VARCHAR(1000),

    evaluated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    CONSTRAINT fk_evaluations_assignments
        FOREIGN KEY (assignment_id)
        REFERENCES review_assignments(assignment_id),

    CONSTRAINT chk_evaluation_score
        CHECK (score >= 0 AND score <= 10),

    CONSTRAINT uq_evaluation_assignment
        UNIQUE (assignment_id)
);

CREATE TABLE review_results (
    result_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    group_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    council_id BIGINT NOT NULL,

    final_score DECIMAL(4,2) NOT NULL,

    result VARCHAR(20) NOT NULL,

    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at DATETIME,

    CONSTRAINT fk_results_groups
        FOREIGN KEY (group_id)
        REFERENCES student_groups(group_id),

    CONSTRAINT fk_results_topics
        FOREIGN KEY (topic_id)
        REFERENCES topics(topic_id),

    CONSTRAINT fk_results_councils
        FOREIGN KEY (council_id)
        REFERENCES councils(council_id),

    CONSTRAINT chk_final_score
        CHECK (final_score >= 0 AND final_score <= 10)
);

CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    title VARCHAR(250) NOT NULL,
    content TEXT NOT NULL,

    type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',

    created_by BIGINT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    updated_at DATETIME,

    CONSTRAINT fk_notifications_users
        FOREIGN KEY (created_by)
        REFERENCES users(user_id)
);

CREATE TABLE notification_target_roles (
    notification_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (notification_id, role_id),
    CONSTRAINT fk_notification_target_roles_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(notification_id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_target_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

-- Sample data for local feature testing. Run this script once after recreating the database.
-- Demo password for every account: Demo1234 (the {noop} prefix is for local testing only).
INSERT INTO roles (role_id, name, description) VALUES
(1, 'ADMIN', 'System administrator'),
(2, 'TRUONG_KHOA', 'Faculty lead'),
(3, 'CHU_TICH_HOI_DONG', 'Council chair'),
(4, 'GIANG_VIEN', 'Lecturer'),
(5, 'SINH_VIEN', 'Student');

-- The application adds the complete permission catalog and role mappings at startup.
INSERT INTO permissions (permission_id, code, name, description) VALUES
(1, 'USER_MANAGE', 'Manage users', 'Create and update user accounts'),
(2, 'TOPIC_VIEW', 'View topics', 'View topic records'),
(3, 'NOTICE_VIEW', 'View notices', 'View published notices');

INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 1), (2, 2), (3, 2), (4, 2);

INSERT INTO departments (department_id, department_code, department_name, description, status) VALUES
(1, 'CNTT', 'Computer Science', 'Computer science department', 'ACTIVE'),
(2, 'HTTT', 'Information Systems', 'Information systems department', 'ACTIVE'),
(3, 'KTPM', 'Software Engineering', 'Software engineering department', 'ACTIVE');

INSERT INTO users (user_id, username, password, full_name, email, phone, role, status, enabled) VALUES
(1, 'admin', '{noop}Demo1234', 'System Administrator', 'admin@example.edu', '0901000001', 'ADMIN', 'ACTIVE', TRUE),
(2, 'truongkhoa', '{noop}Demo1234', 'Faculty Lead', 'dean@example.edu', '0901000002', 'TRUONG_KHOA', 'ACTIVE', TRUE),
(3, 'chutich', '{noop}Demo1234', 'Council Chair', 'chair@example.edu', '0901000003', 'CHU_TICH_HOI_DONG', 'ACTIVE', TRUE),
(4, 'gv01', '{noop}Demo1234', 'Lecturer One', 'lecturer1@example.edu', '0901000004', 'GIANG_VIEN', 'ACTIVE', TRUE),
(5, 'gv02', '{noop}Demo1234', 'Lecturer Two', 'lecturer2@example.edu', '0901000005', 'GIANG_VIEN', 'ACTIVE', TRUE),
(6, 'gv03', '{noop}Demo1234', 'Lecturer Three', 'lecturer3@example.edu', '0901000006', 'GIANG_VIEN', 'ACTIVE', TRUE),
(7, 'sv01', '{noop}Demo1234', 'Student One', 'student1@example.edu', '0902000001', 'SINH_VIEN', 'ACTIVE', TRUE),
(8, 'sv02', '{noop}Demo1234', 'Student Two', 'student2@example.edu', '0902000002', 'SINH_VIEN', 'ACTIVE', TRUE),
(9, 'sv03', '{noop}Demo1234', 'Student Three', 'student3@example.edu', '0902000003', 'SINH_VIEN', 'ACTIVE', TRUE),
(10, 'sv04', '{noop}Demo1234', 'Student Four', 'student4@example.edu', '0902000004', 'SINH_VIEN', 'ACTIVE', TRUE),
(11, 'sv05', '{noop}Demo1234', 'Student Five', 'student5@example.edu', '0902000005', 'SINH_VIEN', 'ACTIVE', TRUE),
(12, 'sv06', '{noop}Demo1234', 'Student Six', 'student6@example.edu', '0902000006', 'SINH_VIEN', 'ACTIVE', TRUE),
(13, 'sv07', '{noop}Demo1234', 'Student Seven', 'student7@example.edu', '0902000007', 'SINH_VIEN', 'ACTIVE', TRUE),
(14, 'sv08', '{noop}Demo1234', 'Student Eight', 'student8@example.edu', '0902000008', 'SINH_VIEN', 'ACTIVE', TRUE),
(15, 'sv09', '{noop}Demo1234', 'Student Nine', 'student9@example.edu', '0902000009', 'SINH_VIEN', 'ACTIVE', TRUE);

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 4), (5, 4), (6, 4),
(7, 5), (8, 5), (9, 5), (10, 5), (11, 5), (12, 5), (13, 5), (14, 5), (15, 5);

INSERT INTO students (student_id, user_id, student_code, class_name, major) VALUES
(1, 7, 'SV2026001', 'CNTT-K20A', 'Computer Science'),
(2, 8, 'SV2026002', 'CNTT-K20A', 'Computer Science'),
(3, 9, 'SV2026003', 'CNTT-K20A', 'Computer Science'),
(4, 10, 'SV2026004', 'HTTT-K20A', 'Information Systems'),
(5, 11, 'SV2026005', 'HTTT-K20A', 'Information Systems'),
(6, 12, 'SV2026006', 'KTPM-K20A', 'Software Engineering'),
(7, 13, 'SV2026007', 'KTPM-K20A', 'Software Engineering'),
(8, 14, 'SV2026008', 'CNTT-K20B', 'Computer Science'),
(9, 15, 'SV2026009', 'CNTT-K20B', 'Computer Science');

INSERT INTO lecturers (lecturer_id, user_id, lecturer_code, academic_degree, department_id) VALUES
(1, 3, 'GV001', 'PhD', 1),
(2, 4, 'GV002', 'MSc', 2),
(3, 5, 'GV003', 'MSc', 3),
(4, 6, 'GV004', 'PhD', 1);

INSERT INTO registration_periods (
    period_id, period_name, period_type, lecturer_start_at, lecturer_end_at,
    student_start_at, student_end_at, review_deadline, council_report_date, status, created_by
) VALUES
(1, 'Current lecturer proposal period', 'TLCN', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY),
    DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), NULL, 'PUBLISHED', 2),
(2, 'Current student registration period', 'KLTN', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY), DATE_ADD(NOW(), INTERVAL 25 DAY), DATE_ADD(NOW(), INTERVAL 40 DAY), 'PUBLISHED', 2),
(3, 'Draft course project period', 'MON_HOC', DATE_ADD(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY),
    DATE_ADD(NOW(), INTERVAL 31 DAY), DATE_ADD(NOW(), INTERVAL 45 DAY), NULL, NULL, 'DRAFT', 2),
(4, 'Closed research period', 'NCKH', DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 75 DAY),
    DATE_SUB(NOW(), INTERVAL 74 DAY), DATE_SUB(NOW(), INTERVAL 60 DAY), NULL, NULL, 'CLOSED', 2);

INSERT INTO topics (
    topic_id, period_id, department_id, created_by_lecturer_id, topic_code, topic_name,
    description, status, published, created_at, approved_at
) VALUES
(1, 2, 1, 1, 'DEMO-TOPIC-01', 'Student registration portal', 'Build a portal for managing student topic registrations.', 'APPROVED', TRUE, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 2, 2, 2, 'DEMO-TOPIC-02', 'Department analytics dashboard', 'Build a dashboard for department activity and reporting.', 'APPROVED', TRUE, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 2, 3, 3, 'DEMO-TOPIC-03', 'Campus event application', 'Build an application for campus event registration.', 'APPROVED', TRUE, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 2, 1, 4, 'DEMO-TOPIC-04', 'Library lending system', 'Build a lending workflow with search and overdue notices.', 'PENDING', FALSE, NOW(), NULL);

INSERT INTO topic_supervisors (topic_supervisor_id, topic_id, lecturer_id, supervisor_order) VALUES
(1, 1, 1, 1), (2, 2, 2, 1), (3, 3, 3, 1), (4, 4, 4, 1);

INSERT INTO student_groups (group_id, period_id, topic_id, group_code, group_name, status, report_url) VALUES
(1, 2, 1, 'DEMO-GROUP-01', 'Registration Portal Team', 'APPROVED', '/uploads/demo-group-01.pdf'),
(2, 2, 2, 'DEMO-GROUP-02', 'Analytics Team', 'APPROVED', '/uploads/demo-group-02.pdf'),
(3, 2, 3, 'DEMO-GROUP-03', 'Campus App Team', 'APPROVED', '/uploads/demo-group-03.pdf'),
(4, 2, NULL, 'DEMO-GROUP-04', 'Library System Team', 'DRAFT', NULL);

INSERT INTO group_members (group_member_id, group_id, student_id, is_leader) VALUES
(1, 1, 1, TRUE), (2, 1, 2, FALSE), (3, 1, 3, FALSE),
(4, 2, 4, TRUE), (5, 2, 5, FALSE),
(6, 3, 6, TRUE), (7, 3, 7, FALSE),
(8, 4, 8, TRUE), (9, 4, 9, FALSE);

INSERT INTO topic_registrations (
    registration_id, group_id, topic_id, status, registered_at, approved_by, approved_at, rejection_reason
) VALUES
(1, 1, 1, 'APPROVED', DATE_SUB(NOW(), INTERVAL 2 DAY), 3, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(2, 2, 2, 'APPROVED', DATE_SUB(NOW(), INTERVAL 2 DAY), 4, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
(3, 3, 3, 'APPROVED', DATE_SUB(NOW(), INTERVAL 1 DAY), 5, NOW(), NULL),
(4, 4, 4, 'PENDING', NOW(), NULL, NULL, NULL);

INSERT INTO reports (report_id, group_id, submitted_by, report_title, file_url, version, description, submitted_at, status) VALUES
(1, 1, 1, 'Registration portal progress report', '/uploads/demo-group-01.pdf', 1, 'Initial project progress report.', DATE_SUB(NOW(), INTERVAL 1 DAY), 'SUBMITTED'),
(2, 2, 4, 'Analytics dashboard progress report', '/uploads/demo-group-02.pdf', 1, 'Initial project progress report.', DATE_SUB(NOW(), INTERVAL 1 DAY), 'SUBMITTED'),
(3, 3, 6, 'Campus application progress report', '/uploads/demo-group-03.pdf', 1, 'Initial project progress report.', NOW(), 'SUBMITTED');

INSERT INTO councils (council_id, period_id, council_code, council_name, report_date, status) VALUES
(1, 2, 'DEMO-COUNCIL-01', 'Software Project Council A', DATE_ADD(NOW(), INTERVAL 20 DAY), 'COMPLETED'),
(2, 2, 'DEMO-COUNCIL-02', 'Software Project Council B', DATE_ADD(NOW(), INTERVAL 21 DAY), 'PLANNED'),
(3, 2, 'DEMO-COUNCIL-03', 'Software Project Council C', DATE_ADD(NOW(), INTERVAL 22 DAY), 'PLANNED');

INSERT INTO council_members (council_member_id, council_id, lecturer_id, role) VALUES
(1, 1, 1, 'CHAIRMAN'), (2, 1, 2, 'SECRETARY'), (3, 1, 3, 'MEMBER'), (4, 1, 4, 'MEMBER'),
(5, 2, 1, 'CHAIRMAN'), (6, 2, 2, 'SECRETARY'), (7, 2, 3, 'MEMBER'), (8, 2, 4, 'MEMBER'),
(9, 3, 1, 'CHAIRMAN'), (10, 3, 2, 'SECRETARY'), (11, 3, 3, 'MEMBER'), (12, 3, 4, 'MEMBER');

INSERT INTO review_assignments (assignment_id, council_id, topic_id, lecturer_id, assigned_at, status) VALUES
(1, 1, 1, 2, DATE_SUB(NOW(), INTERVAL 3 DAY), 'COMPLETED'),
(2, 1, 1, 3, DATE_SUB(NOW(), INTERVAL 3 DAY), 'COMPLETED'),
(3, 2, 2, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), 'COMPLETED'),
(4, 2, 2, 3, DATE_SUB(NOW(), INTERVAL 2 DAY), 'COMPLETED'),
(5, 3, 3, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 'COMPLETED'),
(6, 3, 3, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), 'COMPLETED');

INSERT INTO evaluations (evaluation_id, assignment_id, score, comment, evaluated_at, status) VALUES
(1, 1, 8.00, 'Clear scope and implementation.', DATE_SUB(NOW(), INTERVAL 2 DAY), 'SUBMITTED'),
(2, 2, 8.50, 'Good quality and documentation.', DATE_SUB(NOW(), INTERVAL 2 DAY), 'SUBMITTED'),
(3, 3, 4.50, 'Needs stronger analysis.', DATE_SUB(NOW(), INTERVAL 1 DAY), 'SUBMITTED'),
(4, 4, 5.00, 'Meets the minimum requirements.', DATE_SUB(NOW(), INTERVAL 1 DAY), 'SUBMITTED'),
(5, 5, 9.00, 'Strong implementation and testing.', NOW(), 'SUBMITTED'),
(6, 6, 9.50, 'Excellent overall result.', NOW(), 'SUBMITTED');

INSERT INTO review_results (result_id, group_id, topic_id, council_id, final_score, result, published, published_at) VALUES
(1, 1, 1, 1, 8.25, 'PASS', TRUE, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 2, 2, 2, 4.75, 'FAIL', FALSE, NULL),
(3, 3, 3, 3, 9.25, 'PASS', TRUE, NOW());

INSERT INTO notifications (notification_id, title, content, type, created_by, created_at, published_at, status, updated_at) VALUES
(1, 'Registration period is open', 'Student teams can register topics during the active period.', 'SYSTEM', 1, NOW(), NOW(), 'PUBLISHED', NOW()),
(2, 'Submission reminder', 'Team leaders should submit progress reports before the deadline.', 'SYSTEM', 2, NOW(), NOW(), 'PUBLISHED', NOW()),
(3, 'Council schedule', 'Review councils are being scheduled for the current period.', 'SYSTEM', 3, NOW(), NOW(), 'DRAFT', NOW()),
(4, 'Topic review update', 'A new topic is waiting for lecturer review.', 'SYSTEM', 4, NOW(), NOW(), 'DRAFT', NOW());

INSERT INTO notification_target_roles (notification_id, role_id) VALUES
(1, 5), (2, 5), (3, 3), (4, 4), (4, 2);

SHOW TABLES;
