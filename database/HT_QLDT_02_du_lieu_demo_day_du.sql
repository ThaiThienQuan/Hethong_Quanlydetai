/* =====================================================================
   HT_QLDT_02_du_lieu_demo_day_du.sql
   DỮ LIỆU DEMO "KỊCH BẢN ĐẦY ĐỦ" - Dùng để demo/trình bày trước khi báo cáo

   Bản này KHÁC bản demo gốc: bố trí lại theo đúng vòng đời đề tài để có thể
   đi trọn 9 giai đoạn nghiệp vụ mà không phải sửa dữ liệu giữa lúc trình bày.

   ---------------------------------------------------------------------
   CÁCH NẠP (bắt buộc nạp vào database TRỐNG)
   ---------------------------------------------------------------------
     1. Xóa database cũ (nếu có):        DROP DATABASE student_topic_management;
     2. Nạp schema:                       database\HT_QLDT.sql
     3. Nạp dữ liệu demo kịch bản này:    database\HT_QLDT_02_du_lieu_demo_day_du.sql
     4. Tạo tệp báo cáo mẫu (PDF thật):   python database\tao_bao_cao_demo_day_du.py .\storage\reports
     5. Sinh mật khẩu BCrypt cho các tài khoản:
            powershell -File scripts\reset-passwords.ps1 -Password 123456

     Hoặc chạy một lệnh:
            powershell -File scripts\reset-demo-data.ps1 -SqlFile HT_QLDT_02_du_lieu_demo_day_du.sql

   ⚠ KHÔNG nạp chung với bản demo gốc (HT_QLDT.sql đã chứa sẵn dữ liệu mẫu):
     cả hai dùng chung tên đăng nhập admin/truongkhoa/gv01../sv01.. nên sẽ trùng
     khóa UNIQUE. Chọn MỘT trong hai bản.

   ---------------------------------------------------------------------
   MẬT KHẨU
   ---------------------------------------------------------------------
     Mọi tài khoản dùng chung: 123456
     Trong DB chỉ lưu chuỗi BCrypt (không có mật khẩu rõ).
     Bước 6 ở trên là BẮT BUỘC - nếu chưa chạy thì đăng nhập sẽ bị từ chối.

   ---------------------------------------------------------------------
   THỜI GIAN
   ---------------------------------------------------------------------
     Mọi mốc tính tương đối theo lúc chạy file (@now = NOW()), nên đợt luôn
     đúng giai đoạn tương ứng. Muốn demo lại từ đầu: xóa database rồi nạp lại.

   ---------------------------------------------------------------------
   EMAIL
   ---------------------------------------------------------------------
     Địa chỉ dùng đuôi @demo.local (Mailpit/Mailtrap), không phải email thật.
     app.mail.enabled=false -> email ghi trạng thái SKIPPED, không gửi thật.

   ---------------------------------------------------------------------
   BẢN ĐỒ KỊCH BẢN - DEMO THEO THỨ TỰ (xem bảng chi tiết ở cuối file)
   ---------------------------------------------------------------------
     P1  Đồ án môn học  : đang mở GĐ1  -> GV đề xuất / Trưởng khoa duyệt, công bố
     P2  TLCN           : đang mở GĐ2  -> SV lập nhóm, đăng ký, GVHD chấp thuận, nộp báo cáo
     P3  KLTN           : đã nộp báo cáo-> lập hội đồng, phân công chấm, nhập điểm, tổng hợp, công bố
     P4  TLCN (cũ)     : đã xong      -> SV tra cứu kết quả theo đợt (lịch sử)
     P5  NCKH (nháp)    : DRAFT         -> tạo / sửa / xóa đợt
   ===================================================================== */

USE student_topic_management;
SET NAMES utf8mb4;

-- Chuỗi "@pw" là PLACEHOLDER. SQL không sinh được BCrypt, và
-- DelegatingPasswordEncoder bắt buộc phải có prefix "{bcrypt}".
-- Bước 6 ở trên sẽ thay bằng hash thật cho mọi tài khoản.
SET @pw = '{bcrypt}$2a$10$toBeReplacedByResetPasswordsClassHashHashHashHashHashHashHashHashHashHashHashHashHash';

SET @now = NOW();

/* =====================================================================
   RESET (tùy chọn) - bỏ comment để nạp lại từ đầu mà không cần DROP DB
   ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE email_logs;               TRUNCATE TABLE audit_logs;
TRUNCATE TABLE notification_target_roles;TRUNCATE TABLE notifications;
TRUNCATE TABLE review_results;           TRUNCATE TABLE evaluations;
TRUNCATE TABLE review_assignments;       TRUNCATE TABLE council_topics;
TRUNCATE TABLE council_members;          TRUNCATE TABLE councils;
TRUNCATE TABLE reports;                  TRUNCATE TABLE topic_registrations;
TRUNCATE TABLE group_members;            TRUNCATE TABLE student_groups;
TRUNCATE TABLE topic_supervisors;        TRUNCATE TABLE topics;
TRUNCATE TABLE registration_periods;
TRUNCATE TABLE students;                 TRUNCATE TABLE lecturers;
TRUNCATE TABLE departments;              TRUNCATE TABLE user_roles;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;
------------------------------------------------------------------------- */


/* =====================================================================
   1. BỘ MÔN
   id 4 (MMT) cố ý KHÔNG có GV/đề tài nào tham chiếu -> dùng để thử XÓA bộ môn
   ===================================================================== */
INSERT INTO departments (id, code, name, description, status) VALUES
(1, 'KHMT', 'Khoa học máy tính',  'Trí tuệ nhân tạo, dữ liệu, thuật toán',        'ACTIVE'),
(2, 'HTTT', 'Hệ thống thông tin', 'Phân tích thiết kế, ERP, cơ sở dữ liệu',     'ACTIVE'),
(3, 'KTPM', 'Kỹ thuật phần mềm',  'Quy trình phần mềm, kiểm thử, DevOps',        'ACTIVE'),
(4, 'MMT',  'Mạng máy tính',      'Bộ môn thử nghiệm: chưa có GV/đề tài',      'ACTIVE');


/* =====================================================================
   2. TÀI KHOẢN
   1 admin + 1 trưởng khoa + 8 giảng viên (3 bộ môn) + 24 sinh viên
   (sv24 bị khóa -> thử đăng nhập phải bị chặn)
   ===================================================================== */
INSERT INTO users (id, username, password_hash, full_name, email, phone, status, enabled, created_at) VALUES
( 1, 'admin',      @pw, 'Quản trị hệ thống',   'admin@demo.local',      '0900000001', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
( 2, 'truongkhoa', @pw, 'Trần Minh Quân',      'truongkhoa@demo.local', '0900000002', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
-- 8 giảng viên: gv01, gv02 (KHMT) | gv03, gv04, gv08 (HTTT) | gv05, gv06 (KTPM)
( 3, 'gv01', @pw, 'Nguyễn Văn An',   'gv01@demo.local', '0911000001', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
( 4, 'gv02', @pw, 'Lê Thị Bình',     'gv02@demo.local', '0911000002', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
( 5, 'gv03', @pw, 'Phạm Quốc Cường', 'gv03@demo.local', '0911000003', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
( 6, 'gv04', @pw, 'Hoàng Thu Dung',  'gv04@demo.local', '0911000004', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
( 7, 'gv05', @pw, 'Võ Minh Đức',     'gv05@demo.local', '0911000005', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
( 8, 'gv06', @pw, 'Đặng Thị Phương', 'gv06@demo.local', '0911000006', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
( 9, 'gv07', @pw, 'Bùi Hữu Giang',   'gv07@demo.local', '0911000007', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(10, 'gv08', @pw, 'Ngô Thanh Hà',    'gv08@demo.local', '0911000008', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
-- 24 sinh viên
(11, 'sv01', @pw, 'Nguyễn Hoài Nam',  'sv01@demo.local', '0922000001', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(12, 'sv02', @pw, 'Trần Thu Hà',      'sv02@demo.local', '0922000002', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(13, 'sv03', @pw, 'Lê Quang Huy',     'sv03@demo.local', '0922000003', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(14, 'sv04', @pw, 'Phạm Ngọc Anh',    'sv04@demo.local', '0922000004', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(15, 'sv05', @pw, 'Vũ Minh Tuấn',     'sv05@demo.local', '0922000005', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(16, 'sv06', @pw, 'Đỗ Thị Lan',       'sv06@demo.local', '0922000006', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(17, 'sv07', @pw, 'Bùi Văn Khoa',     'sv07@demo.local', '0922000007', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(18, 'sv08', @pw, 'Hoàng Gia Bảo',    'sv08@demo.local', '0922000008', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(19, 'sv09', @pw, 'Phan Thanh Tùng',  'sv09@demo.local', '0922000009', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(20, 'sv10', @pw, 'Đặng Mỹ Linh',     'sv10@demo.local', '0922000010', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(21, 'sv11', @pw, 'Ngô Đức Thịnh',    'sv11@demo.local', '0922000011', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(22, 'sv12', @pw, 'Dương Khánh Vy',   'sv12@demo.local', '0922000012', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(23, 'sv13', @pw, 'Lý Hữu Phước',     'sv13@demo.local', '0922000013', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(24, 'sv14', @pw, 'Trương Bảo Châu',  'sv14@demo.local', '0922000014', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(25, 'sv15', @pw, 'Mai Anh Dũng',     'sv15@demo.local', '0922000015', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(26, 'sv16', @pw, 'Cao Thị Hồng',     'sv16@demo.local', '0922000016', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(27, 'sv17', @pw, 'Đinh Công Danh',   'sv17@demo.local', '0922000017', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(28, 'sv18', @pw, 'Tạ Minh Khôi',     'sv18@demo.local', '0922000018', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(29, 'sv19', @pw, 'Huỳnh Ngọc Trâm',  'sv19@demo.local', '0922000019', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(30, 'sv20', @pw, 'Lâm Quốc Việt',    'sv20@demo.local', '0922000020', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(31, 'sv21', @pw, 'Phan Thị Hồng Vân','sv21@demo.local', '0922000021', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(32, 'sv22', @pw, 'Đỗ Trung Kiên',    'sv22@demo.local', '0922000022', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(33, 'sv23', @pw, 'Lý Gia Bảo',       'sv23@demo.local', '0922000023', 'ACTIVE', 1, DATE_ADD(@now, INTERVAL -220 DAY)),
(34, 'sv24', @pw, 'Trịnh Thế Vinh',   'sv24@demo.local', '0922000024', 'LOCKED', 0, DATE_ADD(@now, INTERVAL -220 DAY));

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = CASE
    WHEN u.username = 'admin'      THEN 'ADMIN'
    WHEN u.username = 'truongkhoa' THEN 'TRUONG_KHOA'
    WHEN u.username LIKE 'gv%'     THEN 'GIANG_VIEN'
    ELSE 'SINH_VIEN'
END;


/* =====================================================================
   3. HỒ SƠ GIẢNG VIÊN (id = số thứ tự, user = id + 2) VÀ SINH VIÊN (user = id + 10)
   ===================================================================== */
INSERT INTO lecturers (id, user_id, lecturer_code, academic_degree, department_id) VALUES
(1,  3, 'GV001', 'TS',     1),   -- gv01  KHMT
(2,  4, 'GV002', 'PGS.TS', 1),   -- gv02  KHMT
(3,  5, 'GV003', 'TS',     2),   -- gv03  HTTT
(4,  6, 'GV004', 'ThS',    2),   -- gv04  HTTT
(5,  7, 'GV005', 'ThS',    3),   -- gv05  KTPM
(6,  8, 'GV006', 'TS',     3),   -- gv06  KTPM
(7,  9, 'GV007', 'ThS',    1),   -- gv07  KHMT
(8, 10, 'GV008', 'PGS.TS', 2);   -- gv08  HTTT

INSERT INTO students (id, user_id, student_code, class_name, major) VALUES
( 1, 11, 'SV23001', 'DHKTPM18A', 'Kỹ thuật phần mềm'),
( 2, 12, 'SV23002', 'DHKTPM18A', 'Kỹ thuật phần mềm'),
( 3, 13, 'SV23003', 'DHKTPM18A', 'Kỹ thuật phần mềm'),
( 4, 14, 'SV23004', 'DHHTTT18A', 'Hệ thống thông tin'),
( 5, 15, 'SV23005', 'DHHTTT18A', 'Hệ thống thông tin'),
( 6, 16, 'SV23006', 'DHHTTT18A', 'Hệ thống thông tin'),
( 7, 17, 'SV23007', 'DHKHMT18A', 'Khoa học máy tính'),
( 8, 18, 'SV23008', 'DHKHMT18A', 'Khoa học máy tính'),
( 9, 19, 'SV23009', 'DHKHMT18A', 'Khoa học máy tính'),
(10, 20, 'SV23010', 'DHKHMT18A', 'Khoa học máy tính'),
(11, 21, 'SV23011', 'DHKTPM18B', 'Kỹ thuật phần mềm'),
(12, 22, 'SV23012', 'DHKTPM18B', 'Kỹ thuật phần mềm'),
(13, 23, 'SV23013', 'DHKTPM18B', 'Kỹ thuật phần mềm'),
(14, 24, 'SV23014', 'DHKTPM18B', 'Kỹ thuật phần mềm'),
(15, 25, 'SV23015', 'DHKTPM18B', 'Kỹ thuật phần mềm'),
(16, 26, 'SV23016', 'DHHTTT18B', 'Hệ thống thông tin'),
(17, 27, 'SV23017', 'DHHTTT18B', 'Hệ thống thông tin'),
(18, 28, 'SV23018', 'DHHTTT18B', 'Hệ thống thông tin'),
(19, 29, 'SV23019', 'DHHTTT18B', 'Hệ thống thông tin'),
(20, 30, 'SV23020', 'DHKHMT18B', 'Khoa học máy tính'),
(21, 31, 'SV23021', 'DHKHMT18B', 'Khoa học máy tính'),
(22, 32, 'SV23022', 'DHKHMT18B', 'Khoa học máy tính'),
(23, 33, 'SV23023', 'DHKTPM18C', 'Kỹ thuật phần mềm'),
(24, 34, 'SV23024', 'DHKTPM18C', 'Kỹ thuật phần mềm');


/* =====================================================================
   4. ĐỢT ĐĂNG KÝ - mỗi đợt ở MỘT giai đoạn khác nhau

   Ràng buộc schema đã tôn trọng:
     · student_start_at >= lecturer_end_at          (2 giai đoạn tuần tự)
     · review_deadline chỉ có ở TLCN/KLTN và phải > student_end_at
     · council_report_date chỉ có ở KLTN và phải > review_deadline
   ===================================================================== */
INSERT INTO registration_periods
 (id, name, period_type, lecturer_start_at, lecturer_end_at, student_start_at, student_end_at,
  review_deadline, council_report_date, status, created_by, created_at) VALUES
-- P1: đang mở GIAI ĐOẠN 1 (giảng viên đề xuất đề tài). Đồ án môn học -> không có hạn GVPB.
(1, 'Đồ án môn học HK1 2026-2027 - GĐ1: GV đề xuất đề tài', 'MON_HOC',
    DATE_ADD(@now, INTERVAL  -3 DAY), DATE_ADD(@now, INTERVAL  10 DAY),
    DATE_ADD(@now, INTERVAL  11 DAY), DATE_ADD(@now, INTERVAL  25 DAY),
    NULL, NULL, 'PUBLISHED', 2, DATE_ADD(@now, INTERVAL -6 DAY)),
-- P2: đang mở GIAI ĐOẠN 2 (nhóm sinh viên đăng ký đề tài đã công bố).
(2, 'TLCN HK1 2026-2027 - GĐ2: SV lập nhóm và đăng ký đề tài', 'TLCN',
    DATE_ADD(@now, INTERVAL -25 DAY), DATE_ADD(@now, INTERVAL -12 DAY),
    DATE_ADD(@now, INTERVAL -11 DAY), DATE_ADD(@now, INTERVAL   9 DAY),
    DATE_ADD(@now, INTERVAL  26 DAY), NULL, 'PUBLISHED', 2, DATE_ADD(@now, INTERVAL -30 DAY)),
-- P3: đã qua đăng ký, đã nộp báo cáo -> đang PHẢN BIỆN VÀ CHẤM ĐIỂM. Còn hạn GVPB.
(3, 'KLTN HK1 2026-2027 - Đã nộp báo cáo, đang phản biện và chấm điểm', 'KLTN',
    DATE_ADD(@now, INTERVAL -70 DAY), DATE_ADD(@now, INTERVAL -57 DAY),
    DATE_ADD(@now, INTERVAL -56 DAY), DATE_ADD(@now, INTERVAL -42 DAY),
    DATE_ADD(@now, INTERVAL   8 DAY), DATE_ADD(@now, INTERVAL 18 DAY), 'PUBLISHED', 2, DATE_ADD(@now, INTERVAL -75 DAY)),
-- P4: đã hoàn tất, kết quả ĐÃ CÔNG BỐ -> sinh viên tra cứu lịch sử. Đợt đã đóng.
(4, 'TLCN HK2 2025-2026 - Đã công bố kết quả', 'TLCN',
    DATE_ADD(@now, INTERVAL -140 DAY), DATE_ADD(@now, INTERVAL -127 DAY),
    DATE_ADD(@now, INTERVAL -126 DAY), DATE_ADD(@now, INTERVAL -112 DAY),
    DATE_ADD(@now, INTERVAL  -90 DAY), NULL, 'CLOSED', 2, DATE_ADD(@now, INTERVAL -145 DAY)),
-- P5: bản nháp -> thử TẠO / SỬA / XÓA đợt.
(5, 'NCKH HK1 2026-2027 - Bản nháp', 'NCKH',
    DATE_ADD(@now, INTERVAL  30 DAY), DATE_ADD(@now, INTERVAL  40 DAY),
    DATE_ADD(@now, INTERVAL  41 DAY), DATE_ADD(@now, INTERVAL  55 DAY),
    NULL, NULL, 'DRAFT', 2, DATE_ADD(@now, INTERVAL -1 DAY));


/* =====================================================================
   5. ĐỀ TÀI

   P1 (đang GĐ1) — đủ trạng thái để demo duyệt / từ chối / công bố:
        t1, t2, t5  PENDING   -> Trưởng khoa duyệt hoặc từ chối
        t3          APPROVED, CHƯA công bố -> bấm "Công bố danh sách đề tài"
        t4          REJECTED (kèm lý do)

   P2 (đang GĐ2) — đề tài đã công bố cho sinh viên đăng ký:
        t6..t10     APPROVED + published = 1

   P3 (đang chấm) — t11, t12, t13 đã vào hội đồng; t14 CHƯA vào hội đồng
   P4 (đã xong)  — t15, t16, t17 đã có kết quả công bố
   ===================================================================== */
INSERT INTO topics
 (id, period_id, department_id, created_by_lecturer_id, topic_code, topic_name, description,
  status, reject_reason, approved_by, approved_at, published, created_at) VALUES
-- ---------- P1: đề xuất của giảng viên (giai đoạn 1) ----------
( 1, 1, 1, 1, 'DA-MH-26-001', 'Ứng dụng quản lý lịch học cá nhân',
    'Xây dựng ứng dụng web quản lý lịch học, lịch thi và nhắc việc trước hạn cho sinh viên.',
    'PENDING',  NULL, NULL, NULL, 0, DATE_ADD(@now, INTERVAL -2 DAY)),
( 2, 1, 2, 3, 'DA-MH-26-002', 'Hệ thống quản lý kho thiết bị văn phòng',
    'Theo dõi nhập xuất thiết bị, bảo trì định kỳ và cấp phát cho phòng ban trong khoa.',
    'PENDING',  NULL, NULL, NULL, 0, DATE_ADD(@now, INTERVAL -2 DAY)),
( 3, 1, 3, 5, 'DA-MH-26-003', 'Công cụ kiểm thử hiệu năng tự động',
    'Sinh kịch bản tải và đo độ trễ của REST API, so sánh hiệu năng giữa các phiên bản.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -1 DAY), 0, DATE_ADD(@now, INTERVAL -3 DAY)),
( 4, 1, 1, 7, 'DA-MH-26-004', 'Mô hình dự báo giá cổ phiếu Việt Nam',
    'Áp dụng học máy và dữ liệu thị trường để dự báo biến động giá cổ phiếu niêm yết.',
    'REJECTED', 'Phạm vi quá rộng và nguồn dữ liệu chưa mô tả rõ, đề nghị thu hẹp phạm vi.', 2, DATE_ADD(@now, INTERVAL -1 DAY), 0, DATE_ADD(@now, INTERVAL -3 DAY)),
( 5, 1, 2, 4, 'DA-MH-26-005', 'Cổng thông tin đăng ký phòng thí nghiệm',
    'Cho phép sinh viên đặt lịch sử dụng máy tính và thiết bị đo lường của phòng thí nghiệm.',
    'PENDING',  NULL, NULL, NULL, 0, DATE_ADD(@now, INTERVAL -1 DAY)),
-- ---------- P2: danh sách đề tài đã công bố cho sinh viên đăng ký ----------
( 6, 2, 1, 1, 'TLCN-26-001', 'Nền tảng học trực tuyến có chấm bài tự động',
    'Quản lý khóa học, nộp bài lập trình và chấm điểm tự động theo bộ kiểm thử.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -20 DAY), 1, DATE_ADD(@now, INTERVAL -28 DAY)),
( 7, 2, 2, 3, 'TLCN-26-002', 'Hồ sơ bệnh án điện tử cho phòng khám',
    'Thiết kế và hiện thực hồ sơ bệnh án điện tử kèm phân quyền bác sĩ, lễ tân và bệnh nhân.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -20 DAY), 1, DATE_ADD(@now, INTERVAL -28 DAY)),
( 8, 2, 3, 5, 'TLCN-26-003', 'Quản lý dự án phần mềm theo Scrum',
    'Công cụ quản lý backlog, sprint và vẽ biểu đồ burndown cho nhóm phát triển.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -20 DAY), 1, DATE_ADD(@now, INTERVAL -28 DAY)),
( 9, 2, 1, 7, 'TLCN-26-004', 'Phân tích dữ liệu giao thông đô thị bằng học máy',
    'Thu thập và xử lý dữ liệu giao thông, dự báo tình trạng ùn tắc theo khung giờ.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -20 DAY), 1, DATE_ADD(@now, INTERVAL -28 DAY)),
(10, 2, 2, 4, 'TLCN-26-005', 'Hệ thống quản lý thư viện trực tuyến',
    'Tra cứu, mượn trả và đặt trước sách, tài liệu số trong thư viện của trường.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -20 DAY), 1, DATE_ADD(@now, INTERVAL -28 DAY)),
-- ---------- P3: đã nộp báo cáo, đang phản biện ----------
(11, 3, 1, 1, 'KLTN-26-101', 'Nhận diện biển số xe tại bãi đỗ ô tô',
    'Nhận diện biển số xe từ camera và quản lý lượt vào ra bãi đỗ của trường.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -75 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
(12, 3, 2, 3, 'KLTN-26-102', 'Sàn thương mại điện tử đa gian hàng',
    'Xây dựng sàn bán hàng cho phép nhiều gian hàng, giỏ hàng và thanh toán giả lập.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -75 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
(13, 3, 3, 5, 'KLTN-26-103', 'CI/CD tự động cho đồ án sinh viên',
    'Thiết lập quy trình build, test, triển khai tự động và giám sát cho đồ án của khoa.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -75 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
(14, 3, 1, 7, 'KLTN-26-104', 'Chatbot tư vấn tuyển sinh 24/7',
    'Xây dựng chatbot trả lời câu hỏi tuyển sinh dựa trên cơ sở tri thức của khoa.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -75 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
-- ---------- P4: đã công bố kết quả ----------
(15, 4, 1, 2, 'TLCN-25-201', 'Ứng dụng theo dõi chi tiêu cá nhân',
    'Ghi nhận thu chi, thống kê theo danh mục và cảnh báo vượt ngân sách hằng tháng.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -145 DAY), 1, DATE_ADD(@now, INTERVAL -148 DAY)),
(16, 4, 2, 4, 'TLCN-25-202', 'Quản lý thư viện khoa',
    'Mượn trả sách, tìm kiếm tài liệu và gửi thông báo quá hạn tự động.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -145 DAY), 1, DATE_ADD(@now, INTERVAL -148 DAY)),
(17, 4, 3, 6, 'TLCN-25-203', 'Trợ lý quản lý nhóm thực hiện đồ án',
    'Công cụ phân công nhiệm vụ, theo dõi tiến độ và hợp nhất báo cáo của nhóm đồ án.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -145 DAY), 1, DATE_ADD(@now, INTERVAL -148 DAY));

/* ---------- GV hướng dẫn: mỗi đề tài có 1 hoặc 2 GVHD ----------
   t6, t11, t13 có 2 GVHD (người thứ hai do Trưởng khoa bổ sung, assigned_by = 2). */
INSERT INTO topic_supervisors (topic_id, lecturer_id, supervisor_order, assigned_by, assigned_at) VALUES
( 1, 1, 1, NULL, DATE_ADD(@now, INTERVAL  -2 DAY)),
( 2, 3, 1, NULL, DATE_ADD(@now, INTERVAL  -2 DAY)),
( 3, 5, 1, NULL, DATE_ADD(@now, INTERVAL  -3 DAY)),
( 4, 7, 1, NULL, DATE_ADD(@now, INTERVAL  -3 DAY)),
( 5, 4, 1, NULL, DATE_ADD(@now, INTERVAL  -1 DAY)),
( 6, 1, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY)),
( 6, 2, 2, 2,    DATE_ADD(@now, INTERVAL -22 DAY)),   -- Trưởng khoa bổ sung GVHD thứ hai
( 7, 3, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY)),
( 8, 5, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY)),
( 9, 7, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY)),
(10, 4, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY)),
(11, 1, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(11, 2, 2, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(12, 3, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(13, 5, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(13, 6, 2, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(14, 7, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(15, 2, 1, NULL, DATE_ADD(@now, INTERVAL -148 DAY)),
(16, 4, 1, NULL, DATE_ADD(@now, INTERVAL -148 DAY)),
(17, 6, 1, NULL, DATE_ADD(@now, INTERVAL -148 DAY));


/* =====================================================================
   6. NHÓM SINH VIÊN VÀ THÀNH VIÊN  (tối đa 3 người, đúng 1 nhóm trưởng)

   P2 - đang đăng ký:
     g1 (t6) APPROVED    -> đã được GVHD chấp thuận, sẵn sàng nộp báo cáo
     g2 (t7) REGISTERED  -> nhóm trưởng đã đăng ký, CHỜ GVHD chấp thuận
     g3 (—)  DRAFT       -> mới lập nhóm, CHƯA đăng ký đề tài
     g4 (t8) DRAFT       -> đăng ký t8 bị TỪ CHỐI, được đăng ký lại
     g5 (t9) REGISTERED  -> chờ GVHD chấp thuận
     g6 (t10) REGISTERED -> nhóm 1 thành viên (app.group.min-size = 1), chờ duyệt

   P3 - đã nộp báo cáo:
     g7 (t11) SUBMITTED, g8 (t12) SUBMITTED, g9 (t13) SUBMITTED
     g10 (t14) APPROVED  -> chưa nộp báo cáo, chưa xếp hội đồng

   P4 - đã hoàn tất: g11 (t15), g12 (t16), g13 (t17)  -> status COMPLETED

   ⚠ SV THAM GIA 2 ĐỢT (để demo "tra cứu kết quả theo đợt"):
       sv01, sv02, sv03 : g1 (đợt 2) + g11 (đợt 4)
       sv04, sv05       : g2 (đợt 2) + g12 (đợt 4)
       sv18, sv19       : g9 (đợt 3) + g13 (đợt 4)
     Ràng buộc UNIQUE(student_id, period_id) cho phép SV ở nhiều đợt khác nhau.
   ===================================================================== */
INSERT INTO student_groups (id, period_id, group_code, group_name, topic_id, status, created_at) VALUES
( 1, 2, 'TLCN26-01', 'Nhóm EduCode',     6,  'APPROVED',   DATE_ADD(@now, INTERVAL  -3 DAY)),
( 2, 2, 'TLCN26-02', 'Nhóm MediRecord',  7,  'REGISTERED', DATE_ADD(@now, INTERVAL  -2 DAY)),
( 3, 2, 'TLCN26-03', 'Nhóm Môn Học Mới', NULL,'DRAFT',      DATE_ADD(@now, INTERVAL  -1 DAY)),
( 4, 2, 'TLCN26-04', 'Nhóm Scrumboard',  8,  'DRAFT',      DATE_ADD(@now, INTERVAL  -2 DAY)),
( 5, 2, 'TLCN26-05', 'Nhóm TrafficAI',   9,  'REGISTERED', DATE_ADD(@now, INTERVAL  -1 DAY)),
( 6, 2, 'TLCN26-06', 'Nhóm LibOnline',   10, 'REGISTERED', DATE_ADD(@now, INTERVAL  -1 DAY)),
( 7, 3, 'KLTN26-01', 'Nhóm PlateVision', 11, 'SUBMITTED',  DATE_ADD(@now, INTERVAL -64 DAY)),
( 8, 3, 'KLTN26-02', 'Nhóm MultiShop',   12, 'SUBMITTED',  DATE_ADD(@now, INTERVAL -64 DAY)),
( 9, 3, 'KLTN26-03', 'Nhóm AutoPipeline',13, 'SUBMITTED',  DATE_ADD(@now, INTERVAL -64 DAY)),
(10, 3, 'KLTN26-04', 'Nhóm AdmissionBot',14, 'APPROVED',   DATE_ADD(@now, INTERVAL -64 DAY)),
(11, 4, 'TLCN25-01', 'Nhóm MoneyTrack',  15, 'COMPLETED',  DATE_ADD(@now, INTERVAL -133 DAY)),
(12, 4, 'TLCN25-02', 'Nhóm LibSys',      16, 'COMPLETED',  DATE_ADD(@now, INTERVAL -133 DAY)),
(13, 4, 'TLCN25-03', 'Nhóm GroupTool',   17, 'COMPLETED',  DATE_ADD(@now, INTERVAL -133 DAY));

INSERT INTO group_members (group_id, period_id, student_id, is_leader, joined_at) VALUES
-- Đợt 2 (TLCN)
( 1, 2,  1, 1, DATE_ADD(@now, INTERVAL  -3 DAY)), ( 1, 2,  2, 0, DATE_ADD(@now, INTERVAL  -3 DAY)), ( 1, 2,  3, 0, DATE_ADD(@now, INTERVAL  -3 DAY)),
( 2, 2,  4, 1, DATE_ADD(@now, INTERVAL  -2 DAY)), ( 2, 2,  5, 0, DATE_ADD(@now, INTERVAL  -2 DAY)),
( 3, 2,  6, 1, DATE_ADD(@now, INTERVAL  -1 DAY)), ( 3, 2,  7, 0, DATE_ADD(@now, INTERVAL  -1 DAY)),
( 4, 2,  8, 1, DATE_ADD(@now, INTERVAL  -2 DAY)), ( 4, 2,  9, 0, DATE_ADD(@now, INTERVAL  -2 DAY)),
( 5, 2, 10, 1, DATE_ADD(@now, INTERVAL  -1 DAY)), ( 5, 2, 11, 0, DATE_ADD(@now, INTERVAL  -1 DAY)),
( 6, 2, 12, 1, DATE_ADD(@now, INTERVAL  -1 DAY)),
-- Đợt 3 (KLTN)
( 7, 3, 13, 1, DATE_ADD(@now, INTERVAL -64 DAY)), ( 7, 3, 14, 0, DATE_ADD(@now, INTERVAL -64 DAY)), ( 7, 3, 15, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
( 8, 3, 16, 1, DATE_ADD(@now, INTERVAL -64 DAY)), ( 8, 3, 17, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
( 9, 3, 18, 1, DATE_ADD(@now, INTERVAL -64 DAY)), ( 9, 3, 19, 0, DATE_ADD(@now, INTERVAL -64 DAY)), ( 9, 3, 20, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
(10, 3, 21, 1, DATE_ADD(@now, INTERVAL -64 DAY)), (10, 3, 22, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
-- Đợt 4 (TLCN cũ) - cùng SV với đợt 2 / đợt 3 ở trên
(11, 4,  1, 1, DATE_ADD(@now, INTERVAL -133 DAY)), (11, 4,  2, 0, DATE_ADD(@now, INTERVAL -133 DAY)), (11, 4,  3, 0, DATE_ADD(@now, INTERVAL -133 DAY)),
(12, 4,  4, 1, DATE_ADD(@now, INTERVAL -133 DAY)), (12, 4,  5, 0, DATE_ADD(@now, INTERVAL -133 DAY)),
(13, 4, 18, 1, DATE_ADD(@now, INTERVAL -133 DAY)), (13, 4, 19, 0, DATE_ADD(@now, INTERVAL -133 DAY));

/* ---------- Đăng ký đề tài ----------
   g1  -> đã được gv02 (GVHD thứ hai của t6) chấp thuận
   g2  -> PENDING, Trưởng khoa hoặc GVHD gv03 chấp thuận
   g4  -> REJECTED, nhóm được đăng ký lại
   g5, g6 -> PENDING
   g7..g13 -> APPROVED */
INSERT INTO topic_registrations
 (group_id, topic_id, status, registered_at, approved_by, approved_at, rejection_reason) VALUES
( 1,  6, 'APPROVED', DATE_ADD(@now, INTERVAL  -2 DAY), 4, DATE_ADD(@now, INTERVAL  -1 DAY), NULL),
( 2,  7, 'PENDING',  DATE_ADD(@now, INTERVAL  -1 DAY), NULL, NULL, NULL),
( 4,  8, 'REJECTED', DATE_ADD(@now, INTERVAL  -2 DAY), 2, DATE_ADD(@now, INTERVAL  -2 DAY), 'Nhóm chưa nêu rõ phạm vi và công nghệ, vui lòng trao đổi với GVHD rồi đăng ký lại.'),
( 5,  9, 'PENDING',  DATE_ADD(@now, INTERVAL  -1 DAY), NULL, NULL, NULL),
( 6, 10, 'PENDING',  DATE_ADD(@now, INTERVAL  -1 DAY), NULL, NULL, NULL),
( 7, 11, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY), 2, DATE_ADD(@now, INTERVAL -61 DAY), NULL),
( 8, 12, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY), 2, DATE_ADD(@now, INTERVAL -61 DAY), NULL),
( 9, 13, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY), 2, DATE_ADD(@now, INTERVAL -61 DAY), NULL),
(10, 14, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY), 2, DATE_ADD(@now, INTERVAL -61 DAY), NULL),
(11, 15, 'APPROVED', DATE_ADD(@now, INTERVAL -132 DAY),2, DATE_ADD(@now, INTERVAL -131 DAY), NULL),
(12, 16, 'APPROVED', DATE_ADD(@now, INTERVAL -132 DAY),2, DATE_ADD(@now, INTERVAL -131 DAY), NULL),
(13, 17, 'APPROVED', DATE_ADD(@now, INTERVAL -132 DAY),2, DATE_ADD(@now, INTERVAL -131 DAY), NULL);

/* ---------- Báo cáo ----------
   Tệp thật do tao_bao_cao_demo_day_du.py tạo trong app.storage.dir/demo/.
   Bản demo có BÁO CÁO NHIỀU PHIÊN BẢN: g7 nộp 2 lần (bản 1 bị thay thế). */
INSERT INTO reports
 (id, group_id, submitted_by, report_title, file_name, stored_path, file_size, version, description, submitted_at, status) VALUES
(1,  7, 13, 'Báo cáo KLTN - Nhận diện biển số xe (bản 1)', 'bao_cao_KLTN26-01_v1.pdf', 'demo/KLTN-26-101_v1.pdf', 614, 1, 'Bản nộp đầu tiên, sau khi đối chiếu ý kiến GVHD đã sửa lại ở bản 2.', DATE_ADD(@now, INTERVAL -46 DAY), 'SUPERSEDED'),
(2,  7, 13, 'Báo cáo KLTN - Nhận diện biển số xe (bản 2)', 'bao_cao_KLTN26-01_v2.pdf', 'demo/KLTN-26-101_v2.pdf', 614, 2, 'Bản chỉnh sửa theo góp ý của GVHD, đã bổ sung phần đánh giá độ chính xác.', DATE_ADD(@now, INTERVAL -30 DAY), 'SUBMITTED'),
(3,  8, 16, 'Báo cáo KLTN - Sàn thương mại điện tử',       'bao_cao_KLTN26-02_v1.pdf', 'demo/KLTN-26-102_v1.pdf', 614, 1, NULL, DATE_ADD(@now, INTERVAL -30 DAY), 'SUBMITTED'),
(4,  9, 18, 'Báo cáo KLTN - CI/CD tự động',              'bao_cao_KLTN26-03_v1.pdf', 'demo/KLTN-26-103_v1.pdf', 614, 1, NULL, DATE_ADD(@now, INTERVAL -29 DAY), 'SUBMITTED'),
(5, 11,  1, 'Báo cáo TLCN - Theo dõi chi tiêu cá nhân',   'bao_cao_TLCN25-01_v1.pdf', 'demo/TLCN-25-201_v1.pdf', 614, 1, NULL, DATE_ADD(@now, INTERVAL -100 DAY), 'SUBMITTED'),
(6, 12,  4, 'Báo cáo TLCN - Quản lý thư viện khoa',       'bao_cao_TLCN25-02_v1.pdf', 'demo/TLCN-25-202_v1.pdf', 614, 1, NULL, DATE_ADD(@now, INTERVAL -100 DAY), 'SUBMITTED'),
(7, 13, 18, 'Báo cáo TLCN - Trợ lý quản lý nhóm đồ án',  'bao_cao_TLCN25-03_v1.pdf', 'demo/TLCN-25-203_v1.pdf', 614, 1, NULL, DATE_ADD(@now, INTERVAL -100 DAY), 'SUBMITTED');


/* =====================================================================
   7. HỘI ĐỒNG PHẢN BIỆN

   c1  Đợt 3, READY, 5 GV
       → t11: 3 người chấm, THIẾU điểm của gv06  (đăng nhập gv06 để nhập nốt)
       → t12: 3 người chấm, ĐỦ điểm, chưa tổng hợp (đăng nhập gv02 làm chủ tịch)
   c2  Đợt 3, READY, 3 GV
       → t13: 3 người chấm, đủ điểm, ĐÃ TỔNG HỢP nhưng CHƯA CÔNG BỐ
              (Trưởng khoa vào màn hình kết quả để công bố -> hệ thống gửi email)
   c3  Đợt 3, DRAFT, chỉ 2 GV -> hội đồng KHÔNG hợp lệ (cần 3-5 GV), thử thêm thành viên
   c4  Đợt 4, FINISHED, 3 GV -> kết quả t15/t16/t17 ĐÃ CÔNG BỐ
   ⚠ Đề tài t14 (chưa xếp hội đồng) -> demo chức năng "Xếp đề tài vào hội đồng"
   ===================================================================== */
INSERT INTO councils (id, period_id, council_code, council_name, report_date, status, created_by, created_at) VALUES
(1, 3, 'HD-KLTN-26-A', 'Hội đồng KLTN HK1 2026-2027 - A', DATE_ADD(@now, INTERVAL 18 DAY), 'READY',    2, DATE_ADD(@now, INTERVAL -20 DAY)),
(2, 3, 'HD-KLTN-26-B', 'Hội đồng KLTN HK1 2026-2027 - B', DATE_ADD(@now, INTERVAL 18 DAY), 'READY',    2, DATE_ADD(@now, INTERVAL -20 DAY)),
(3, 3, 'HD-KLTN-26-C', 'Hội đồng KLTN HK1 2026-2027 - C (chưa đủ thành viên)', NULL,     'DRAFT',    2, DATE_ADD(@now, INTERVAL  -2 DAY)),
(4, 4, 'HD-TLCN-25-D', 'Hội đồng TLCN HK2 2025-2026 - D', DATE_ADD(@now, INTERVAL -95 DAY), 'FINISHED', 2, DATE_ADD(@now, INTERVAL -110 DAY));

INSERT INTO council_members (council_id, lecturer_id, role, joined_at) VALUES
-- c1: 5 thành viên. gv01 và gv02 là GVHD của t11 -> KHÔNG được chấm t11.
(1, 2, 'CHAIRMAN',  DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv02 (chủ tịch hội đồng A)
(1, 4, 'SECRETARY', DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv04 (thư ký)
(1, 6, 'MEMBER',    DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv06
(1, 1, 'MEMBER',    DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv01
(1, 3, 'MEMBER',    DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv03
-- c2: 3 thành viên. gv08 là chủ tịch -> chỉ gv08 mới tổng hợp được t13.
(2, 8, 'CHAIRMAN',  DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv08 (chủ tịch hội đồng B)
(2, 7, 'SECRETARY', DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv07
(2, 4, 'MEMBER',    DATE_ADD(@now, INTERVAL -20 DAY)),   -- gv04
-- c3: chỉ 2 thành viên -> CHƯA HỢP LỆ (yêu cầu 3-5 GV)
(3, 1, 'CHAIRMAN',  DATE_ADD(@now, INTERVAL  -2 DAY)),
(3, 3, 'SECRETARY', DATE_ADD(@now, INTERVAL  -2 DAY)),
-- c4: 3 thành viên, hội đồng đã hoàn tất
(4, 2, 'CHAIRMAN',  DATE_ADD(@now, INTERVAL -110 DAY)),
(4, 3, 'SECRETARY', DATE_ADD(@now, INTERVAL -110 DAY)),
(4, 5, 'MEMBER',    DATE_ADD(@now, INTERVAL -110 DAY));

INSERT INTO council_topics (council_id, topic_id, added_at) VALUES
(1, 11, DATE_ADD(@now, INTERVAL -19 DAY)),
(1, 12, DATE_ADD(@now, INTERVAL -19 DAY)),
(2, 13, DATE_ADD(@now, INTERVAL -19 DAY)),
(4, 15, DATE_ADD(@now, INTERVAL -109 DAY)),
(4, 16, DATE_ADD(@now, INTERVAL -109 DAY)),
(4, 17, DATE_ADD(@now, INTERVAL -109 DAY));

/* ---------- Phân công chấm ----------
   Tuân thủ 2 ràng buộc của hệ thống:
     1. Người chấm PHẢI là thành viên của hội đồng đó (FK kép council_members).
     2. KHÔNG ai chấm đề tài mình đang hướng dẫn.
        t11 GVHD = gv01, gv02  -> chấm: gv03, gv04, gv06
        t12 GVHD = gv03         -> chấm: gv02, gv04, gv06
        t13 GVHD = gv05, gv06  -> chấm: gv08, gv07, gv04
        t15 GVHD = gv02         -> chấm: gv03, gv05
        t16 GVHD = gv04         -> chấm: gv02, gv03, gv05
        t17 GVHD = gv06         -> chấm: gv02, gv03, gv05 */
INSERT INTO review_assignments (id, council_id, topic_id, lecturer_id, assigned_by, assigned_at, status) VALUES
( 1, 1, 11, 3, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 2, 1, 11, 4, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 3, 1, 11, 6, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'ASSIGNED'),    -- gv06 CHƯA nhập điểm -> demo "còn thiếu điểm"
( 4, 1, 12, 2, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 5, 1, 12, 4, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 6, 1, 12, 6, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 7, 2, 13, 8, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 8, 2, 13, 7, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
( 9, 2, 13, 4, 2, DATE_ADD(@now, INTERVAL -18 DAY), 'COMPLETED'),
(10, 4, 15, 3, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(11, 4, 15, 5, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(12, 4, 16, 2, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(13, 4, 16, 3, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(14, 4, 16, 5, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(15, 4, 17, 2, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(16, 4, 17, 3, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED'),
(17, 4, 17, 5, 2, DATE_ADD(@now, INTERVAL -108 DAY),'COMPLETED');

/* ---------- Điểm thành phần (thang 0-10) ----------
   THIẾU chính ý: phân công id = 3 (gv06 chấm t11) -> để trống cố ý. */
INSERT INTO evaluations (assignment_id, score, comment, evaluated_at, status) VALUES
( 1, 8.50, 'Hệ thống hoàn chỉnh, độ chính xác nhận diện tốt, báo cáo trình bày rõ ràng.', DATE_ADD(@now, INTERVAL -5 DAY), 'SUBMITTED'),
( 2, 8.00, 'Đạt yêu cầu, cần bổ sung thêm kịch bản kiểm thử trong điều kiện thiếu sáng.',  DATE_ADD(@now, INTERVAL -4 DAY), 'SUBMITTED'),
( 4, 7.00, 'Chức năng đầy đủ nhưng phần bảo mật thanh toán còn sơ sài.',                    DATE_ADD(@now, INTERVAL -5 DAY), 'SUBMITTED'),
( 5, 7.50, 'Giao diện tốt, mã nguồn có cấu trúc, cần thêm kiểm thử tự động.',              DATE_ADD(@now, INTERVAL -4 DAY), 'SUBMITTED'),
( 6, 8.00, 'Sản phẩm hoàn thiện, demo ổn định.',                                            DATE_ADD(@now, INTERVAL -4 DAY), 'SUBMITTED'),
( 7, 9.00, 'Quy trình CI/CD đầy đủ, tài liệu hướng dẫn rõ ràng.',                          DATE_ADD(@now, INTERVAL -6 DAY), 'SUBMITTED'),
( 8, 8.50, 'Triển khai tốt, nên mô tả thêm phần giám sát sau khi triển khai.',              DATE_ADD(@now, INTERVAL -6 DAY), 'SUBMITTED'),
( 9, 9.50, 'Xuất sắc, vượt yêu cầu đề ra.',                                                 DATE_ADD(@now, INTERVAL -6 DAY), 'SUBMITTED'),
(10, 7.50, 'Đạt yêu cầu, giao diện cần cải thiện cho đẹp hơn.',                             DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(11, 8.00, 'Hoàn thành tốt các chức năng chính.',                                           DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(12, 4.50, 'Thiếu nhiều chức năng cốt lõi, báo cáo chưa đạt yêu cầu.',                      DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(13, 4.00, 'Chưa đạt yêu cầu tối thiểu về kiểm thử và tài liệu hướng dẫn.',                 DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(14, 5.00, 'Ở mức tối thiểu, cần làm rõ phần phân tích yêu cầu.',                           DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(15, 8.00, 'Công cụ dùng tốt, đồng bộ tiến độ nhóm.',                                        DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(16, 8.50, 'Tính năng hợp lý, giao diện thân thiện.',                                        DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED'),
(17, 8.50, 'Hoàn thành tốt, hỗ trợ nhóm hiệu quả.',                                          DATE_ADD(@now, INTERVAL -100 DAY),'SUBMITTED');

/* ---------- Kết quả ----------
   Điểm cuối = TRUNG BÌNH CỘNG các điểm thành phần (đã kiểm tra đúng số học):
     t15: (7,50 + 8,00) / 2                      = 7,75 -> ĐẠT    (ĐÃ công bố)
     t16: (4,50 + 4,00 + 5,00) / 3                = 4,50 -> KHÔNG ĐẠT (ĐÃ công bố)
     t17: (8,00 + 8,50 + 8,50) / 3                = 8,33 -> ĐẠT    (ĐÃ công bố)
     t13: (9,00 + 8,50 + 9,50) / 3                = 9,00 -> ĐẠT    (CHƯA công bố)

   Ngưỡng ĐẠT: app.passing-score = 5,00
   Chưa có kết quả cho t11 (thiếu điểm gv06) và t12 (chủ tịch gv02 chưa tổng hợp),
   đúng như kịch bản demo từng bước. */
INSERT INTO review_results
 (id, group_id, topic_id, council_id, final_score, result, summary_comment, calculated_by, calculated_at, published, published_at, published_by) VALUES
(1,  9, 13, 2, 9.00, 'PASSED', 'Hội đồng đánh giá cao tính thực tiễn và mức độ hoàn thiện của hệ thống CI/CD.', 10, DATE_ADD(@now, INTERVAL -1 DAY),   0, NULL, NULL),
(2, 11, 15, 4, 7.75, 'PASSED', 'Đạt yêu cầu, giao diện cần cải thiện cho thân thiện hơn.',                 4, DATE_ADD(@now, INTERVAL -92 DAY), 1, DATE_ADD(@now, INTERVAL -90 DAY), 2),
(3, 12, 16, 4, 4.50, 'FAILED', 'Chưa đạt: thiếu chức năng cốt lõi và tài liệu kiểm thử.',                      4, DATE_ADD(@now, INTERVAL -92 DAY), 1, DATE_ADD(@now, INTERVAL -90 DAY), 2),
(4, 13, 17, 4, 8.33, 'PASSED', 'Đạt yêu cầu, đồng bộ tốt tiến độ giữa các thành viên nhóm.',                4, DATE_ADD(@now, INTERVAL -92 DAY), 1, DATE_ADD(@now, INTERVAL -90 DAY), 2);

/* ---------- Nhật ký email của các kết quả ĐÃ CÔNG BỐ ----------
   Quy tắc: mỗi (kết quả, sinh viên) đúng một dòng.
     kết quả t15 -> nhóm 11 = sv01, sv02, sv03  -> 3 email
     kết quả t16 -> nhóm 12 = sv04, sv05        -> 2 email (sv05 để FAILED để thử "Gửi lại")
     kết quả t17 -> nhóm 13 = sv18, sv19        -> 2 email */
INSERT INTO email_logs (result_id, student_id, to_email, subject, status, error_message, attempt_count, created_at, sent_at) VALUES
(2,  1, 'sv01@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY)),
(2,  2, 'sv02@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY)),
(2,  3, 'sv03@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY)),
(3,  4, 'sv04@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY)),
(3,  5, 'sv05@demo.local', 'Kết quả TLCN HK2 2025-2026', 'FAILED', 'Không kết nối được máy chủ SMTP (timeout).', 3, DATE_ADD(@now, INTERVAL -90 DAY), NULL),
(4, 18, 'sv18@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY)),
(4, 19, 'sv19@demo.local', 'Kết quả TLCN HK2 2025-2026', 'SENT',   NULL, 1, DATE_ADD(@now, INTERVAL -90 DAY), DATE_ADD(@now, INTERVAL -90 DAY));


/* =====================================================================
   8. THÔNG BÁO (không có dòng target = gửi cho tất cả vai trò)
   ===================================================================== */
INSERT INTO notifications (id, title, content, type, created_by, status, created_at, published_at) VALUES
(1, 'Lịch làm việc và nghỉ lễ của nhà trường', 'Nhà trường thông báo lịch nghỉ lễ và lịch làm việc của các phòng ban trong học kỳ tới. Đề nghị các đơn vị sắp xếp công việc phù hợp.', 'SCHOOL',  1, 'PUBLISHED',  DATE_ADD(@now, INTERVAL  -6 DAY), DATE_ADD(@now, INTERVAL  -6 DAY)),
(2, 'Mở đăng ký đề tài TLCN HK1 2026-2027',    'Sinh viên lập nhóm và đăng ký đề tài đã công bố trong giai đoạn quy định. Nhóm trưởng là người đăng ký và nộp báo cáo.', 'FACULTY', 2, 'PUBLISHED',  DATE_ADD(@now, INTERVAL  -2 DAY), DATE_ADD(@now, INTERVAL  -2 DAY)),
(3, 'Hạn chót nộp điểm phản biện KLTN',        'Giảng viên được phân công chấm vui lòng nhập điểm và nhận xét trước hạn chót của đợt KLTN HK1 2026-2027.', 'FACULTY', 2, 'PUBLISHED',  DATE_ADD(@now, INTERVAL  -1 DAY), DATE_ADD(@now, INTERVAL  -1 DAY)),
(4, 'Kế hoạch bảo vệ trước hội đồng (bản nháp)', 'Lịch bảo vệ dự kiến của các hội đồng sẽ được công bố sau khi hoàn tất lập hội đồng phản biện.', 'FACULTY', 2, 'DRAFT',      @now, NULL),
(5, 'Thông báo đã được thu hồi',                'Nội dung thông báo này đã bị thu hồi do cập nhật lịch trình bày.', 'SCHOOL', 1, 'RETRACTED', DATE_ADD(@now, INTERVAL -10 DAY), DATE_ADD(@now, INTERVAL -10 DAY));

INSERT INTO notification_target_roles (notification_id, role_id)
SELECT 2, id FROM roles WHERE code = 'SINH_VIEN'
UNION ALL SELECT 3, id FROM roles WHERE code = 'GIANG_VIEN'
UNION ALL SELECT 4, id FROM roles WHERE code = 'GIANG_VIEN';


/* =====================================================================
   9. NHẬT KÝ HỆ THỐNG
   ===================================================================== */
INSERT INTO audit_logs (user_id, action, entity, entity_id, detail, created_at) VALUES
(2, 'PERIOD_PUBLISH',   'registration_periods', 2, 'Công bố đợt TLCN HK1 2026-2027',                          DATE_ADD(@now, INTERVAL -30 DAY)),
(2, 'SUPERVISOR_ASSIGN','topics',                6, 'Bổ sung GVHD thứ hai (GV002) cho đề tài TLCN-26-001',   DATE_ADD(@now, INTERVAL -22 DAY)),
(2, 'RESULT_CALCULATE', 'ReviewResult',           4, 'Tổng hợp điểm đề tài TLCN-25-203 = 8,33 (3 người chấm)', DATE_ADD(@now, INTERVAL -92 DAY)),
(2, 'RESULT_PUBLISH',   'ReviewResult',           2, 'Công bố kết quả đề tài TLCN-25-201, 3 SV đã gửi email',    DATE_ADD(@now, INTERVAL -90 DAY));


/* =====================================================================
   10. KIỂM TRA NHANH SAU KHI NẠP
   ---------------------------------------------------------------------
   Tất cả các bảng dưới đây phải trả về số dòng khác 0.
   Nếu có bảng nào = 0 thì file chưa nạp đủ, hoặc đang nạp nhầm thứ tự.
   ===================================================================== */
SELECT 'departments' AS bang, COUNT(*) AS so_dong FROM departments
UNION ALL SELECT 'users',               COUNT(*) FROM users
UNION ALL SELECT 'lecturers',           COUNT(*) FROM lecturers
UNION ALL SELECT 'students',            COUNT(*) FROM students
UNION ALL SELECT 'registration_periods',COUNT(*) FROM registration_periods
UNION ALL SELECT 'topics',              COUNT(*) FROM topics
UNION ALL SELECT 'topic_supervisors',   COUNT(*) FROM topic_supervisors
UNION ALL SELECT 'student_groups',      COUNT(*) FROM student_groups
UNION ALL SELECT 'group_members',       COUNT(*) FROM group_members
UNION ALL SELECT 'topic_registrations', COUNT(*) FROM topic_registrations
UNION ALL SELECT 'reports',             COUNT(*) FROM reports
UNION ALL SELECT 'councils',            COUNT(*) FROM councils
UNION ALL SELECT 'council_members',     COUNT(*) FROM council_members
UNION ALL SELECT 'council_topics',      COUNT(*) FROM council_topics
UNION ALL SELECT 'review_assignments',  COUNT(*) FROM review_assignments
UNION ALL SELECT 'evaluations',         COUNT(*) FROM evaluations
UNION ALL SELECT 'review_results',      COUNT(*) FROM review_results
UNION ALL SELECT 'email_logs',          COUNT(*) FROM email_logs
UNION ALL SELECT 'notifications',       COUNT(*) FROM notifications
UNION ALL SELECT 'audit_logs',          COUNT(*) FROM audit_logs;

/* ---------- Đối chiếu nhanh tình trạng demo ---------- */
/* Hội đồng: chỉ c4 hợp lệ, c3 cố ý thiếu thành viên */
SELECT v.council_id, v.member_count, v.chairman_count, v.secretary_count, v.is_valid
FROM v_council_validity v ORDER BY v.council_id;

/* Điểm trung bình theo đề tài (đối chiếu với review_results)
   LƯU Ý: review_assignments KHÔNG có cột assigned_count — đó là cột TÍNH TOÁN
   trong view v_topic_score_summary. Query cũ (ra.assigned_count) gây lỗi 1054. */
SELECT v.council_id, v.topic_id, v.assigned_count, v.evaluated_count, v.average_score
FROM v_topic_score_summary v
JOIN council_topics ct ON ct.council_id = v.council_id AND ct.topic_id = v.topic_id
ORDER BY v.council_id, v.topic_id;
.topic_id;
→ Đăng ký
        · Thử đăng ký lần thứ hai  -> phải bị từ chối
     6. Đăng nhập gv03 (GVHD của t7) → Xác nhận đăng ký của nhóm g2
     7. Nhóm g4 từng bị từ chối → thử đăng ký lại đề tài TLCN-26-003

   ── GIAI ĐOẠN 3: Nộp báo cáo ────────────────────────────────────────────
     8. Nhóm trưởng g1 (đăng nhập sv01) → Nộp báo cáo (đã được chấp thuận ở giai đoạn 2)
     9. Thành viên KHÔNG phải trưởng (sv02) → không thấy nút nộp báo cáo

   ── GIAI ĐOẠN 4: Lập hội đồng phản biện  (đợt 3) ────────────────────────
    10. truongkhoa → Hội đồng: c1 (5 GV, hợp lệ) và c2 (3 GV, hợp lệ)
        · c3 chỉ có 2 GV  -> thử xếp đề tài sẽ bị chặn, phải bổ sung thành viên
        · Đề tài t14 chưa vào hội đồng nào -> xếp t14 vào hội đồng

   ── GIAI ĐOẠN 5: Phân công người chấm ────────────────────────────────────
    11. truongkhoa → Phân công chấm cho t11, t12, t13
        · GVHD của đề tài sẽ KHÔNG xuất hiện trong danh sách chọn

   ── GIAI ĐOẠN 6: GV nhập điểm ────────────────────────────────────────────
    12. Đăng nhập gv06 → Nhập điểm cho đề tài KLTN-26-101
        (đây là phần công việc CÒN DỞ trong dữ liệu demo)
    13. Đăng nhập gv01, gv02 → xem phân công của mình (t12)
        · Chức năng tổng hợp của GV không phải chủ tịch hội đồng sẽ bị từ chối

   ── GIAI ĐOẠN 7: Chủ tịch tổng hợp ───────────────────────────────────────
    14. Đăng nhập gv02 (chủ tịch hội đồng A) → Tổng hợp kết quả đề tài KLTN-26-102
        (đủ điểm) -> điểm cuối = trung bình cộng
    15. Thử tổng hợp đề tài KLTN-26-101 -> bị chặn "chưa đủ điểm"
        (đúng vì gv06 vừa mới nhập ở bước 12 thì đã đủ)

   ── GIAI ĐOẠN 8: Trưởng khoa công bố kết quả ─────────────────────────────
    16. truongkhoa → Tổng hợp & công bố → Công bố đề tài KLTN-26-103
        (đã tổng hợp sẵn 9,00 nhưng CHƯA công bố)
        → hệ thống gửi email riêng cho từng SV trong nhóm
    17. → Nhật ký email: xem số email đã gửi; có 1 email FAILED của sv05
        để demo nút "Gửi lại"

   ── GIAI ĐOẠN 9: Sinh viên xem kết quả ────────────────────────────────────
    18. Đăng nhập sv01 (có nhóm ở đợt 2 và đợt 4) → Kết quả của tôi
        · Lọc theo đợt -> xem kết quả của từng đợt
        · Chọn một đợt không tham gia (ví dụ đợt 3) -> bị chặn 403
    19. Đăng nhập sv07 (chỉ có nhóm ở đợt 2, chưa có kết quả)
        → xác nhận không thấy kết quả của nhóm khác

   ── PHỤ: các chức năng quản trị ──────────────────────────────────────────
    20. admin → Quản lý tài khoản: tạo, sửa, khóa/mở khóa, đặt lại mật khẩu
        · Đăng nhập sv24 (đã khóa) -> bị chặn
    21. admin → Vai trò & phân quyền
    22. admin → Bộ môn: thử XÓA bộ môn MMT (id 4, không có dữ liệu tham chiếu)
        · Thử xóa một bộ môn đang có giảng viên -> phải bị chặn
    23. truongkhoa → Thông báo: tạo, công bố, thu hồi

   ── PHỤ: quy tắc nghiệp vụ nên thử trực tiếp để phản biện khi báo cáo ────
    a. Thêm người thứ 4 vào một nhóm            -> phải bị từ chối (tối đa 3)
    b. Cho một sinh viên lập/tham gia nhóm thứ 2  -> phải bị từ chối
    c. Phân công GVHD chấm đề tài mình hướng dẫn -> phải bị từ chối
    d. Gán 3 GVHD cho một đề tài                -> phải bị từ chối (tối đa 2)
    e. Xếp cùng một đề tài vào 2 hội đồng      -> phải bị từ chối
    f. Nhập điểm ngoài khoảng 0-10              -> phải bị từ chối
    g. Công bố kết quả 2 lần                    -> phải bị từ chối
    h. Sinh viên xem kết quả của nhóm khác      -> phải bị từ chối (403)
   ===================================================================== */