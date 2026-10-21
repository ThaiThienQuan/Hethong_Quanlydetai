/* =====================================================================
   HT_QLDT_01_danh_muc_quyen.sql
   Danh mục VAI TRÒ - QUYỀN - GÁN QUYỀN MẶC ĐỊNH (theo bảng quyền mục 3 prompt v2).

   - Dùng được cho cả môi trường thật lẫn demo: KHÔNG chứa tài khoản/mật khẩu.
   - IDEMPOTENT: chạy lại nhiều lần an toàn (INSERT IGNORE theo code).
     Lưu ý: chạy lại sẽ thêm lại quyền mặc định nếu bạn đã gỡ bằng màn hình Phân quyền.
   - Ứng dụng cũng có thể nạp danh mục này khi khởi động (cùng nội dung, cùng code).
   - Chạy SAU HT_QLDT.sql, TRƯỚC HT_QLDT_02_du_lieu_mau.sql.
   ===================================================================== */

USE student_topic_management;
SET NAMES utf8mb4;

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
  'PERIOD_MANAGE','TOPIC_VIEW','TOPIC_APPROVE','SUPERVISOR_ASSIGN','REGISTRATION_CONFIRM',
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
