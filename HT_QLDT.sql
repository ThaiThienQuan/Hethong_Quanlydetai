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
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
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
        CHECK (period_type IN ('COURSE', 'RESEARCH', 'TLCN', 'KLTN'))
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

    group_code VARCHAR(30) NOT NULL,
    group_name VARCHAR(100) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

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

CREATE TABLE topic_registrations (
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

    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,

    type VARCHAR(20) NOT NULL,

    created_by BIGINT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    CONSTRAINT fk_notifications_users
        FOREIGN KEY (created_by)
        REFERENCES users(user_id)
);

SHOW TABLES;
