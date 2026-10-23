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
   ---------------------------------------------------------------------
   Mã GV/SV ở đây được đánh số LIÊN TỤC đúng theo cơ chế hệ thống tự cấp
   (ProfileCodeService): GV001..GV008, SV23001..SV23024.
   Nhờ vậy khi Admin tạo tài khoản mới, hệ thống cấp tiếp SV23025, GV009...
   và không bao giờ trùng mã đang có.
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


/* =====================================================================
   11. KỊCH BẢN DEMO - THỨ TỰ THỰC HIỆN (đọc khi trình bày)

   ⚠ BẢN NÀY VIẾT THEO ĐÚNG TRẠNG THÁI "MỚI NẠP" (chỉ HT_QLDT.sql + tệp này).
     Đã đối chiếu từng bước với mã nguồn và với dữ liệu thật trong DB, nên mọi
     thông điệp ghi sau đây là thông điệp THẬT do service ném ra.
     Nếu đã nạp thêm HT_QLDT_03_du_lieu_mau_xep_de_tai.sql thì xem mục "BIẾN THỂ"
     ở cuối phần này.

   ────────────────────────────────────────────────────────────────────
   ĐĂNG NHẬP - mọi tài khoản mật khẩu: 123456
     admin · truongkhoa · gv01..gv08 · sv01..sv24      (sv24 cố ý bị KHÓA)
     Tài khoản KHÔNG được đăng nhập: sv24 (đã khóa); sai mật khẩu 5 lần
     liên tiếp sẽ bị khóa tạm 15 phút.

   ────────────────────────────────────────────────────────────────────
   CHUẨN BỊ - làm trước khi trình bày
   ────────────────────────────────────────────────────────────────────
     · Đợt 1 đang mở cho GV đến +10 ngày, đợt 2 đang mở cho SV đến +9 ngày,
       đợt 3 còn hạn nộp điểm đến +8 ngày. Mọi mốc tính theo @now lúc nạp file
       nên đợt luôn đúng giai đoạn, không phải sửa tay.
     · TỆP BÁO CÁO: báo cáo trong DB trỏ tới thư mục storage/reports/demo
       (tệp .pdf). Nếu thư mục này chưa có tệp thì mọi nút "Tải báo cáo" báo
       "Không tìm thấy tệp báo cáo". Phải chép tệp PDF thật vào đó trước.
     · app.mail.enabled=false (mặc định) nên khi công bố kết quả, các dòng
       email_logs ghi trạng thái SKIPPED chứ không gửi thật. Muốn gửi thật thì
       bật Mailpit: $env:MAIL_ENABLED="true"; $env:MAIL_HOST="localhost"

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 1 - GIẢNG VIÊN ĐỀ XUẤT ĐỀ TÀI  (đợt 1 · MON_HOC · đang mở cho GV)
   ════════════════════════════════════════════════════════════════════
   Mục tiêu: thấy đề tài đi từ "Chờ duyệt" → "Đã duyệt" → "Đã công bố".

     1. Đăng nhập gv01 → Đề xuất đề tài → điền đủ → Lưu  → trạng thái "Chờ duyệt"
        · Bộ môn KHÔNG chọn được: hệ thống tự lấy bộ môn của GV (gv01 = KHMT).
          Gửi bộ môn khác sẽ báo "Bạn chỉ được đề xuất đề tài thuộc bộ môn của mình."
        · Bộ môn không có GV nào (MMT, id 4) KHÔNG xuất hiện trong danh sách.
        · Mã đề tài do hệ thống cấp: KHMT-DA26-006 (mã bộ môn + mã đợt, trong đó
          loại MON_HOC viết tắt "DA" + 2 số năm + số thứ tự 3 số). Có xem trước mã.
        · Mô tả dưới 20 ký tự sẽ bị DB chặn (chk_topics_description) → lỗi 500.

2. Sửa đề xuất rồi gửi duyệt lại: gv01 → Đề xuất đề tài → mở đề tài vừa tạo
       → Sửa → đổi nội dung → Gửi lại. Trạng thái quay về "Chờ duyệt".

     3. Đăng nhập truongkhoa → Đề tài → lọc Trạng thái = "Chờ duyệt"
        · Duyệt từng đề tài → "Đã duyệt". Trong dữ liệu có sẵn 3 đề tài PENDING:
          DA-MH-26-001, DA-MH-26-002, DA-MH-26-005.
        · Bấm Từ chối mà không gõ lý do → "Vui lòng nhập lý do từ chối."
          Gõ lý do rồi từ chối → "Bị từ chối" kèm lý do hiển thị.
        · Bấm Duyệt/Từ chối với đề tài không ở trạng thái chờ duyệt →
          "Chỉ duyệt đề tài đang chờ duyệt." / "Chỉ từ chối đề tài đang chờ duyệt."

     4. Phân công / đổi GVHD (tab Giảng viên hướng dẫn của một đề tài)
        · Chỉ sửa được khi đề tài còn PENDING, hoặc APPROVED mà chưa công bố.
          Đã công bố → "Đề tài đã công bố nên không thể đổi giảng viên hướng dẫn."
        · Đề tài đã bị từ chối → "Đề tài đã bị từ chối nên không thể phân công lại
          giảng viên hướng dẫn."
        · Hai GVHD không được trùng nhau: "Hai giảng viên hướng dẫn phải là hai người
          khác nhau." (tối đa 2 người)
        · Danh sách ứng viên GVHD thứ hai đã loại sẵn GV đang hướng dẫn đề tài khác
          trong cùng đợt (tránh trùng).

     5. Công bố danh sách đề tài: chọn Đợt = "Đồ án môn học HK1 2026-2027"
        → Công bố theo đợt. Lệnh này công bố TẤT CẢ đề tài APPROVED chưa công bố
        của đợt đó (trong dữ liệu có sẵn DA-MH-26-003 đã duyệt nhưng chưa công bố).
        Bấm Công bố lần 2 → "Đề tài này đã được công bố."

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 2 - SINH VIÊN LẬP NHÓM  (đợt 2 · TLCN · đang mở cho SV)
   ════════════════════════════════════════════════════════════════════
   Mục tiêu: nhóm trưởng tạo nhóm, thêm thành viên, chuẩn bị đăng ký đề tài.

     6. Đăng nhập sv13 (chưa có nhóm ở đợt 2) → Nhóm sinh viên → Tạo nhóm
        · sv13 là nhóm trưởng ngay. Mã nhóm do hệ thống sinh (G_<timestamp>).
        · Nhóm tự gắn vào đợt đang mở cho SV là đợt 2.
        · Bấm Tạo nhóm lần nữa → "Bạn đã tham gia nhóm trong đợt này."

     7. Nhóm trưởng sv13 → Thêm thành viên: sv14, rồi sv15  → nhóm đủ 3/3 người
        · Danh sách ứng viên là các SV CHƯA có nhóm ở đợt 2: sv14..sv23
          (sv01..sv12 đã vào nhóm g1..g6, sv13 là chính nhóm trưởng).
        · Vượt 3 người → "Nhóm đã đủ tối đa 3 thành viên."
        · Thêm SV đã có nhóm ở đợt 2 → "Sinh viên này đã thuộc một nhóm khác trong
          đợt đăng ký hiện tại."
        · Gỡ nhóm trưởng → "Không thể gỡ nhóm trưởng. Hãy dùng chức năng Chuyển
          quyền nhóm trưởng."
        · Chuyển quyền nhóm trưởng cho sv14 được, nhưng chỉ khi nhóm CHƯA đăng ký
          đề tài. Sau khi đăng ký, mọi thao tác thành viên bị khoá: "Chỉ thay đổi
          thành viên nhóm được trước khi nhóm đăng ký đề tài."

8. Nhóm trưởng sv13 → Đăng ký đề tài → chọn "TLCN-26-002 - Hồ sơ bệnh án điện tử"
       → Gửi đăng ký. Trạng thái: CHỜ DUYỆT.
       · Danh sách chỉ hiện đề tài ĐÃ CÔNG BỐ + CÙNG ĐỢT với nhóm + chưa có nhóm
         nào được DUYỆT. Ra 4 đề tài: TLCN-26-002, -003, -004, -005.
         "TLCN-26-001" KHÔNG xuất hiện (đã có nhóm g1 được duyệt).
       · TLCN-26-002 VẪN xuất hiện dù nhóm MediRecord (g2) đang đăng ký nó ở
         trạng thái CHỜ DUYỆT — vì hệ thống chỉ ẩn đề tài đã có nhóm được DUYỆT.
         Đây chính là tình huống để demo quy tắc ở bước 9.
       · Gửi đăng ký lần thứ hai: form biến mất, màn hình hiện "Nhóm đã có đăng ký
         đang hiệu lực, không thể gửi thêm." (không phải lỗi).
       · Đăng ký còn CHỜ DUYỆT thì nhóm trưởng bấm "Hủy đăng ký" được. Sau khi GVHD
         duyệt thì không hủy được: "Chỉ hủy được khi đăng ký còn chờ duyệt (PENDING)."
       ⚠ Thứ tự đúng: bước 8 (đăng ký) phải xong trước bước 9 (GVHD duyệt).

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 3 - GVHD XÁC NHẬN ĐĂNG KÝ
   ════════════════════════════════════════════════════════════════════
9. Đăng nhập gv03 (là GVHD của TLCN-26-002) → Đăng ký chờ duyệt
       · Sẽ thấy HAI đăng ký chờ duyệt của cùng đề tài TLCN-26-002:
         nhóm MediRecord (g2, có sẵn trong dữ liệu) và nhóm của sv13 (bước 8).
         ⚠ Trước khi bấm, hãy xác nhận với khách / giảng viên để tránh nhầm.
       · Chấp thuận đăng ký của nhóm sv13 → nhóm chuyển APPROVED, đăng ký chuyển
         ĐÃ DUYỆT, và hệ thống TỰ TỪ CHỐI đăng ký PENDING còn lại của cùng đề tài
         với lý do "Đề tài đã được duyệt cho nhóm khác."
         Xem lại: đăng ký của nhóm MediRecord giờ mang trạng thái BỊ TỪ CHỐI
         kèm lý do trên → đây là bằng chứng rõ nhất của quy tắc "một đề tài chỉ
         thuộc một nhóm", tự động thực hiện trong cùng transaction.
       · Nếu ngược lại, bấm Chấp thuận cho nhóm MediRecord thì nhóm sv13 mới bị
         từ chối và vẫn ở trạng thái DRAFT, vẫn đăng ký lại được.
       · GV KHÔNG phải GVHD của đề tài mà bấm duyệt → "Chỉ giảng viên hướng dẫn
         đề tài này mới chấp thuận hoặc từ chối được." (cả GVHD chính lẫn GVHD
         thứ hai đều có quyền)
       · Bấm Từ chối mà không gõ lý do → "Vui lòng nhập lý do từ chối."
         Từ chối xong thì nhóm trở về DRAFT và được đăng ký đề tài khác.

    10. Nhóm từng bị từ chối đăng ký lại: đăng nhập sv08 (nhóm Scrumboard đã bị
        từ chối ở TLCN-26-003) → Đăng ký đề tài → chọn lại "TLCN-26-003"
        → Gửi đăng ký. Lần này được chấp nhận vì đăng ký cũ đã REJECTED nên không
        còn ràng buộc "mỗi nhóm chỉ có một đăng ký hiệu lực".

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 4 - NHÓM TRƯỞNG NỘP BÁO CÁO  (đợt 2)
   ════════════════════════════════════════════════════════════════════
    11. Đăng nhập sv13 (nhóm vừa được gv03 duyệt) → Nộp báo cáo → chọn tệp
        (PDF/DOC/DOCX, tối đa 10 MB) → Nộp báo cáo.
        · Nhóm chuyển SUBMITTED; bản cũ (nếu có) tự chuyển SUPERSEDED.
        · Nộp lần hai → "Nhóm đã nộp báo cáo cho đề tài này."
        · Chọn sai định dạng (vd .txt) → "Chỉ nhận tệp PDF, DOC hoặc DOCX."
        · Chọn tệp > 10 MB → "Tệp báo cáo không được vượt quá 10 MB."
        · Đăng ký chưa được duyệt → "Chỉ nộp báo cáo sau khi đăng ký đề tài được duyệt."

    12. Đăng nhập sv14 (thành viên, KHÔNG phải trưởng) → Nộp báo cáo
        · Giao diện hiện "Nhóm trưởng chưa nộp báo cáo. Chỉ nhóm trưởng mới được
          nộp tệp." — không có form nộp.
        · Cố POST thẳng vào /student/reports → "Chỉ nhóm trưởng mới được nộp báo cáo."

    13. Thành viên tải được báo cáo của nhóm: sv14 → bấm "Tải báo cáo đã nộp".
        SV ngoài nhóm bị chặn: "Chỉ thành viên trong nhóm mới được tải báo cáo."

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 5 - TRƯỞNG KHOA LẬP HỘI ĐỒNG PHẢN BIỆN  (đợt 3 · KLTN)
   ════════════════════════════════════════════════════════════════════
   Trong dữ liệu có sẵn:
     HD-KLTN-26-A   READY     5 GV   2 đề tài (KLTN-26-101, KLTN-26-102)
     HD-KLTN-26-B   READY     3 GV   1 đề tài (KLTN-26-103)
     HD-KLTN-26-C   DRAFT     2 GV   0 đề tài   ← cố ý THIẾU thành viên
     HD-TLCN-25-D   FINISHED  3 GV   3 đề tài (đợt 4, đã công bố kết quả)

    14. Đăng nhập truongkhoa → Hội đồng. Mã hội đồng do hệ thống cấp:
        HD-KLTN26-001, HD-KLTN26-002... Ngày báo cáo của hội đồng KLTN LUÔN lấy
        theo ngày báo cáo hội đồng của đợt (ô này chỉ để xem, không nhập tay được).

    15. Mở HD-KLTN-26-C (DRAFT) → bấm "Kích hoạt hội đồng" khi chưa đủ điều kiện
        · Dưới 3 GV → "Hội đồng phải có từ 3 đến 5 giảng viên (hiện có 2)."
        · Đủ người nhưng thiếu thư ký → "Hội đồng phải có đúng một chủ tịch và
          một thư ký."
        · Đủ người mà chưa xếp đề tài → "Hãy xếp ít nhất một đề tài vào hội đồng."
        ⚠ Số thành viên KHÔNG được kiểm tra lúc xếp đề tài mà chỉ kiểm tra khi
          Kích hoạt. Đừng nói "xếp đề tài vào hội đồng thiếu người sẽ bị chặn".

    16. Thêm thành viên vào HD-KLTN-26-C: GV005 + vai trò MEMBER → đủ 3 GV.
        · Thêm thành viên thứ 6 → "Một hội đồng có tối đa 5 giảng viên."
        · Thêm 2 người cùng làm CHAIRMAN → "Hội đồng chỉ có một chủ tịch."
          (tương tự với thư ký — ràng buộc này do DB chặn uq_council_members_one_chair)
        · Thêm trùng một GV đã có trong hội đồng → "Giảng viên này đã ở trong hội đồng."

    17. Xếp đề tài vào hội đồng
        · Ở đợt 3, sau khi chỉ nạp tệp này thì KHÔNG còn đề tài nào đủ điều kiện:
          KLTN-26-101..103 đã nằm trong hội đồng A/B, còn KLTN-26-104 (nhóm
          AdmissionBot) thì CHƯA NỘP BÁO CÁO nên không đưa vào phản biện được.
        · Muốn có đề tài để xếp thì nạp thêm
          database\HT_QLDT_03_du_lieu_mau_xep_de_tai.sql (xem mục BIẾN THỂ).
        · Cố xếp đề tài đã thuộc hội đồng khác → "Đề tài này đã thuộc một hội đồng khác."
        · Đề tài ở đợt khác → "Đề tài phải thuộc đúng đợt của hội đồng."
        · Đề tài chưa nộp báo cáo → "Đề tài chưa có báo cáo được nộp - không đưa
          vào phản biện."

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 6 - PHÂN CÔNG NGƯỜI CHẤM  (hội đồng phải READY)
   ════════════════════════════════════════════════════════════════════
    18. Mở HD-KLTN-26-A → bấm "Phân công chấm" ở từng đề tài. Dữ liệu đã có sẵn:
          KLTN-26-101 (GVHD gv01, gv02) chấm: gv03 8,50 · gv04 8,00 · gv06 CHƯA CHẤM
          KLTN-26-102 (GVHD gv03)           chấm: gv02 7,00 · gv04 7,50 · gv06 8,00
          KLTN-26-103 (GVHD gv05, gv06)     chấm: gv08 9,00 · gv07 8,50 · gv04 9,50
        · Danh sách chọn KHÔNG hiện GVHD và không hiện người đề xuất đề tài — đây
          là cách demo quy tắc "GVHD không được chấm đề tài mình hướng dẫn".
        · Chọn GV đã được phân công → "Giảng viên này đã được phân công chấm đề tài này."
        · Chọn GV không thuộc hội đồng → "Giảng viên không thuộc hội đồng này; chỉ có
          thể chọn GV trong hội đồng."
        · Hội đồng còn DRAFT → "Chỉ phân công khi hội đồng đã ở trạng thái READY."
        · Nút Gỡ bị khoá (mờ) với phân công đã có điểm: "Phân công đã có điểm; không
          thể gỡ."

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 7 - GIẢNG VIÊN NHẬP ĐIỂM  (công việc CÒN DỞ trong dữ liệu demo)
   ════════════════════════════════════════════════════════════════════
    19. Đăng nhập gv06 → Chấm điểm hội đồng. Sẽ thấy KLTN-26-101 với bảng điểm
        "2/3 đã nộp" và KLTN-26-102 đã đủ 3/3.
        · Nhập Điểm thành phần (0-10) + Nhận xét (bắt buộc) → Gửi điểm.
        · Nhập 11 hoặc -1 → "Điểm phải nằm trong khoảng 0 đến 10." (form chặn ngay,
          service vẫn kiểm tra lần nữa).
        · Để trống nhận xét → "Vui lòng nhập nhận xét (bắt buộc)."
        · Còn hạn thì GV sửa được điểm của mình. Quá hạn (+8 ngày) thì bị chặn
          "Đã quá hạn nộp điểm phản biện." (trừ admin được ghi đè kèm lý do, và
          thao tác đó được ghi vào nhật ký hệ thống EVALUATION_SUBMIT_OVERDUE).

    20. Demo quy tắc "GVHD không được chấm đề tài mình hướng dẫn": đăng nhập gv01
        (GVHD chính của KLTN-26-101) → mở Chấm điểm hội đồng → mục KLTN-26-101
        hiện "Bạn không thể chấm đề tài này." thay vì form nhập điểm.

    21. Bảng điểm + tải báo cáo: mỗi đề tài có link "Tải báo cáo đề tài" (GV được
        phân công hoặc GVHD đều tải được; GV khác bị 403 "Bạn không được phân công
        xem báo cáo này."). Nếu tệp chưa có trên đĩa sẽ báo "Không tìm thấy tệp
        báo cáo trong kho lưu trữ."

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 8 - CHỦ TỊCH HỘI ĐỒNG TỔNG HỢP  (trước khi Trưởng khoa công bố)
   ════════════════════════════════════════════════════════════════════
    22. Đăng nhập gv02 (CHỦ TỊCH hội đồng A) → Chấm điểm hội đồng
        · KLTN-26-102 đã đủ 3/3 điểm → có nút "Tổng hợp kết quả". Gõ nhận xét
          tổng hợp rồi bấm → điểm cuối = TRUNG BÌNH CỘNG
          (7,00 + 7,50 + 8,00) / 3 = 7,50 → ĐẠT (ngưỡng app.passing-score = 5,00).
        · KẾT QUẢ ĐẠT/KHÔNG ĐẠT do hệ thống tự quyết, không nhập tay được.
        · Muốn demo "chưa đủ điểm" thì làm TRƯỚC bước 19: lúc đó KLTN-26-101 mới
          có 2/3 điểm nên nút Tổng hợp KHÔNG HIỆN (dòng "Bảng điểm thành phần
          (2/3 đã nộp)"). Sau khi gv06 nộp điểm thì đủ 3/3 và nút mới xuất hiện;
          điểm cuối lúc đó = (8,50 + 8,00 + <điểm của gv06>) / 3.
        · Chưa có phân công nào → "Chưa phân công giảng viên chấm đề tài này."
        · Cố tổng hợp lại kết quả đã công bố → "Kết quả đã công bố thì không thể
          tổng hợp lại." Trước khi công bố thì tổng hợp lại được (để sửa điểm).

    23. Demo phân quyền chủ tịch: đăng nhập gv04 (THƯ KÝ hội đồng A) → thấy bảng điểm
        của KLTN-26-102 nhưng KHÔNG có nút Tổng hợp. Gọi thẳng
        POST /lecturer/council-work/{id}/finalize sẽ nhận 403
        "Chỉ chủ tịch hội đồng được tổng hợp."
        GV không thuộc hội đồng → 403 "Bạn không thuộc hội đồng này."

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 9 - TRƯỞNG KHOA CÔNG BỐ KẾT QUẢ  + GỬI EMAIL
   ════════════════════════════════════════════════════════════════════
    24. Đăng nhập truongkhoa → Kết quả → chọn đợt 3. Sẵn có KLTN-26-103 (CI/CD tự
        động) đã tổng hợp 9,00 ĐẠT nhưng CHƯA công bố.
        · Bấm "Công bố" → hệ thống gửi email RIÊNG cho từng SV trong nhóm
          (nhóm AutoPipeline gồm sv18, sv19, sv20 — kể cả thành viên không phải
          nhóm trưởng).
        · Sau khi công bố, kết quả bị KHÓA: chủ tịch không tổng hợp lại được nữa.
        · Bấm Công bố lần 2 → "Kết quả này đã được công bố."
        · Có thể bấm "Công bố cả hội đồng" để công bố gọn một lần cho cả hội đồng.
        · Chưa có GV chủ tịch tổng hợp → "Chủ tịch cần tổng hợp kết quả trước khi
          công bố."
        · Khi mọi đề tài của hội đồng đã công bố thì hội đồng tự chuyển FINISHED.

    25. Nhật ký email: đăng nhập truongkhoa → Nhật ký email
        · Trong dữ liệu có sẵn 7 dòng của đợt 4 (TLCN HK2 2025-2026), trong đó dòng
          của sv05 cố ý FAILED ("Không kết nối được máy chủ SMTP (timeout).", đã thử
          3 lần) → dùng để demo nút "Gửi lại".
        · Bấm "Gửi lại": attempt_count tăng 1; vẫn FAILED nếu SMTP chưa bật.
        · Bấm "Gửi lại" trên dòng đã SENT → "Email này đã gửi thành công, không cần
          gửi lại."
        · Với app.mail.enabled=false (mặc định) thì các dòng mới ghi SKIPPED với ghi
          chú "Cấu hình app.mail.enabled=false nên không gửi thật".
        · Một kết quả + một sinh viên chỉ có ĐÚNG 1 dòng nhật ký (UNIQUE), nên gửi
          lại không tạo dòng trùng.

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 10 - SINH VIÊN XEM KẾT QUẢ
   ════════════════════════════════════════════════════════════════════
    26. Đăng nhập sv01 → Kết quả của tôi. sv01 có nhóm ở cả đợt 2 (EduCode) và đợt 4
        (MoneyTrack) nên bộ chọn đợt hiện đúng 2 đợt đó. Kết quả đã công bố của sv01
        là của đợt 4.
        · Lọc theo đợt → xem kết quả của từng đợt mình tham gia.
        · Chọn đợt khác → 403 "Bạn không tham gia đợt được chọn nên không được xem kết
          quả của đợt đó." (Bộ chọn đợt lấy từ group_members, KHÔNG tin tham số URL;
          gọi thẳng /student/results?periodId=... cũng bị 403.)
        · Xem API: /api/my/results và /api/my/results/periods (trả JSON).

    27. Đăng nhập sv10 (chỉ có nhóm ở đợt 2, chưa có kết quả được công bố) → Kết quả
        của tôi hiện danh sách rỗng. Xác nhận không thấy kết quả của nhóm khác (đó là
        bản chất của màn hình: chỉ trả kết quả ĐÃ CÔNG BỐ của chính nhóm mình).

   ════════════════════════════════════════════════════════════════════
   GIAI ĐOẠN 11 - CÁC CHỨC NĂNG QUẢN TRỊ
   ════════════════════════════════════════════════════════════════════
    28. admin → Quản lý tài khoản (/admin/users)
        · Tạo tài khoản: nếu chọn Giảng viên/Trưởng khoa thì BẮT BUỘC chọn bộ môn;
          nếu chọn Sinh viên thì BẮT BUỘC nhập lớp và ngành.
        · Mã sinh viên / mã giảng viên do hệ thống TỰ CẤP và hiển thị xem trước
          trên form (SV23025, GV009...) — không nhập tay nên không thể trùng.
        · Để trống email → "Vui lòng nhập email" (cột NOT NULL + UNIQUE).
          Trùng email → "Email đã được dùng cho tài khoản khác."
          Mật khẩu dưới 6 ký tự → "Mật khẩu tối thiểu 6 ký tự".
        · Sửa tài khoản: để trống mật khẩu = giữ mật khẩu cũ.
        · Khóa/mở khóa: không khóa được chính mình ("Không thể khóa chính tài khoản đang
          đăng nhập.") và không khóa được tài khoản quản trị cuối cùng ("Không thể
          khóa tài khoản quản trị cuối cùng của hệ thống.").
        · Đặt lại mật khẩu: về mật khẩu mặc định 123456, mở khoá tạm và xoá bộ đếm
          đăng nhập sai.
        · Gán vai trò: không tự đổi vai trò của mình; không bỏ ADMIN của tài khoản
          admin cuối cùng.
        · Đăng nhập sv24 (đã khóa) → bị từ chối, quay về /login kèm thông báo lỗi.

    29. admin → Đổi mật khẩu của chính mình (/account/password)
        · Sai mật khẩu hiện tại → "Mật khẩu hiện tại không chính xác."
        · Mật khẩu mới dưới 8 ký tự / thiếu chữ / thiếu số → bị chặn với thông báo riêng.
        · Xác nhận không khớp → "Mật khẩu mới nhập lại không khớp."
        · Đổi xong thì bộ đếm sai và trạng thái khoá tạm được xoá.

    30. admin → Vai trò & phân quyền (/roles)
        · Vai trò gốc có cờ hệ thống: không xoá được.
        · Gỡ quyền của một vai trò → giao diện mất ngay các chức năng tương ứng, vì
          SecurityConfig khai báo quyền theo đúng mã trong bảng role_permissions.

    31. admin → Bộ môn (/admin/departments)
        · Thêm bộ môn mới, sửa, bật/tắt hoạt động.
        · Xoá bộ môn MMT (id 4, cố ý không có GV/đề tài nào tham chiếu) → XOÁ ĐƯỢC.
        · Xoá bộ môn đang có giảng viên → bị chặn: "Không thể xoá bộ môn vì đang được
          sử dụng bởi giảng viên hoặc đề tài. Hãy chuyển trạng thái sang
          'Không hoạt động'."
        · Thêm bộ môn trùng mã → "Mã bộ môn đã tồn tại."

    32. admin → Hồ sơ GV / SV (/admin/profiles): xem và sửa hồ sơ. Email không xoá
        được, chỉ đổi được sang email khác.

    33. truongkhoa → Đợt đăng ký (/periods)
        · Đợt nháp NCKH (id 5) dùng để demo Tạo / Sửa / Xoá và Mở đợt.
        · Sửa hoặc xoá đợt đã có đề tài → "Đợt đã có đề tài nên không được sửa hoặc xoá."
        · Xoá đợt đã công bố → "Chỉ xoá được đợt ở trạng thái nháp."
        · Đặt hạn nộp điểm phản biện cho đợt Đồ án môn học → "Hạn chót giảng viên
          phản biện chỉ áp dụng cho đợt TLCN hoặc KLTN, không áp dụng cho đợt
          Đồ án môn học."
        · Ngày báo cáo hội đồng cho đợt không phải KLTN → "Ngày báo cáo hội đồng chỉ
          áp dụng cho đợt KLTN, không áp dụng cho đợt ..."
        · Giai đoạn SV bắt đầu sớm hơn khi giai đoạn GV kết thúc → "Giai đoạn sinh
          viên phải bắt đầu sau khi giai đoạn giảng viên kết thúc."
        · Đợt đã đóng và đã có hội đồng → "Đợt đã có hội đồng phản biện nên không mở
          lại được."

    34. truongkhoa → Thông báo
        · Tạo thông báo ở trạng thái nháp → Công bố → Thu hồi được.
        · Chọn vai trò nhận: bỏ chọn hết = gửi cho tất cả vai trò.

   ════════════════════════════════════════════════════════════════════
   CÁC QUY TẮC NGHIỆP VỤ NÊN THỬ TRỰC TIẾP KHI PHẢN BIỆN
   (thông điệp dưới đây là câu chữ thật trong mã nguồn)
   ════════════════════════════════════════════════════════════════════
     a. Thêm người thứ 4 vào nhóm             → "Nhóm đã đủ tối đa 3 thành viên."
     b. SV lập nhóm thứ 2 trong cùng đợt     → "Bạn đã tham gia nhóm trong đợt này."
     c. Thêm SV đã có nhóm vào nhóm khác     → "Sinh viên này đã thuộc một nhóm khác
                                                 trong đợt đăng ký hiện tại."
     d. Gỡ chính nhóm trưởng                 → "Không thể gỡ nhóm trưởng..."
     e. Gán 3 GVHD cho một đề tài            → tối đa 2 (MAX_SUPERVISORS); form chỉ có
                                                 1 ô "GVHD thứ hai"
     f. GVHD chấm đề tài mình hướng dẫn      → "GVHD (kể cả đồng hướng dẫn) không được
                                                 chấm đề tài mình hướng dẫn."
     g. Người đề xuất chấm đề tài của mình    → "Người đề xuất đề tài không được chấm
                                                 đề tài đó."
     h. GV không thuộc hội đồng được phân công → "Giảng viên không thuộc hội đồng này..."
     i. Xếp cùng một đề tài vào 2 hội đồng  → "Đề tài này đã thuộc một hội đồng khác."
     j. Kích hoạt hội đồng dưới 3 GV         → "Hội đồng phải có từ 3 đến 5 giảng viên
                                                 (hiện có 2)."
     k. Nhập điểm ngoài khoảng 0-10           → "Điểm phải nằm trong khoảng 0 đến 10."
     l. Nộp báo cáo khi đề tài chưa được duyệt → "Chỉ nộp báo cáo sau khi đăng ký đề tài
                                                 được duyệt."
     m. Công bố kết quả 2 lần                → "Kết quả này đã được công bố."
     n. Tổng hợp lại kết quả đã công bố      → "Kết quả đã công bố thì không thể tổng
                                                 hợp lại."
     o. GV không phải chủ tịch tổng hợp       → 403 "Chỉ chủ tịch hội đồng được tổng hợp."
     p. SV xem kết quả của đợt không tham gia → 403 "Bạn không tham gia đợt được chọn nên
                                                 không được xem kết quả của đợt đó."
     q. SV không trong nhóm tải báo cáo      → 403 "Chỉ thành viên trong nhóm mới được
                                                 tải báo cáo."
     r. GV không liên quan tải báo cáo      → 403 "Bạn không được phân công xem báo cáo này."
     s. Đăng nhập sai 5 lần liên tiếp        → tài khoản bị khoá tạm 15 phút
                                                 (app.security.max-failed-attempts)

   ════════════════════════════════════════════════════════════════════
   BIẾN THỂ - NẾU ĐÃ NẠP THÊM HT_QLDT_03_du_lieu_mau_xep_de_tai.sql
   ════════════════════════════════════════════════════════════════════
   Tệp đó bổ sung 4 đề tài ở ĐỢT 3 (KLTN-26-105..108) với đầy đủ nhóm, đăng ký đã
   duyệt và báo cáo đã nộp. Khi đó bước 17 có việc để làm:

    · Mở HD-KLTN-26-C (DRAFT), hoàn tất 3 GV + 1 chủ tịch + 1 thư ký
      (thêm GV005 làm MEMBER là được, vì gv01/gv03 đã là chủ tịch/thư ký).
    · Bấm "Xếp đề tài" → DANH SÁCH SẼ HIỆN 4 đề tài KLTN-26-105..108. Mỗi đề tài
      có GVHD cố ý KHÁC với GVHD của các đề tài cũ (gv01, gv03, gv05, gv08) để còn
      nhiều giảng viên làm người chấm.
    · Kích hoạt hội đồng → READY → Phân công chấm → GV nhập điểm → chủ tịch tổng
      hợp → Trưởng khoa công bố. Đây là vòng tròn khép kín để demo khi không muốn
      dùng sẵn dữ liệu của đợt 3.
    · Lưu ý: tệp 03 chỉ tạo DÒNG trong DB, tệp PDF báo cáo của nhóm mới KHÔNG có
      trên đĩa → bấm "Tải báo cáo" sẽ báo không tìm thấy tệp. Muốn tải được thì
      chép tệp thật vào app.storage.dir/demo/.
    · Nếu đã chạy MỤC 3 của tệp 03 (mở nhóm g10 của KLTN-26-104) thì đề tài đó cũng
      hiện trong danh sách xếp được.

   ════════════════════════════════════════════════════════════════════
   SAU KHI TRÌNH BÀY - muốn demo lại từ đầu
   ════════════════════════════════════════════════════════════════════
     Xoá database rồi nạp lại: database\HT_QLDT.sql rồi tệp này, sau đó
       powershell -File scripts\reset-passwords.ps1 -Password 123456
     Mọi dữ liệu đều dựa trên @now = NOW() nên không cần sửa tay mốc thời gian.
   ===================================================================== */