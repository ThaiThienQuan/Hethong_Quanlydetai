/* =====================================================================
   HT_QLDT_03_du_lieu_mau_xep_de_tai.sql
   Dữ liệu mẫu để hội đồng MỚI có đề tài để xếp vào phản biện.
   Chạy trên DB đã có dữ liệu demo (HT_QLDT_02_du_lieu_demo_day_du.sql).

   VÌ SAO BẠN THẤY "Không còn đề tài nào đủ điều kiện"?
   ------------------------------------------------------------------
   Màn hình "Xếp đề tài" chỉ hiện đề tài thoả cả 5 điều kiện:
     1. Thuộc đúng đợt của hội đồng
     2. Đăng ký đã được duyệt (topic_registrations.status = 'APPROVED')
     3. Đã nộp báo cáo (reports.status = 'SUBMITTED')
     4. Chưa xếp vào hội đồng này
     5. Chưa thuộc hội đồng nào khác  (uq_council_topics_topic UNIQUE)

   Hội đồng bạn vừa tạo thuộc ĐỢT 4 (TLCN HK2 2025-2026 - Đã công bố
   kết quả). Đợt đó chỉ có 3 đề tài t15/t16/t17 và cả 3 đã nằm trong
   hội đồng HD-TLCN-25-D + đã có kết quả công bố -> không còn gì.

   ĐỢT 3 (KLTN HK1 2026-2027) mới là đợt đang thực sự phản biện.
   Tệp này bổ sung 4 đề tài mới cho ĐỢT 3, đầy đủ nhóm / đăng ký /
   báo cáo, để bạn tạo hội đồng ở đợt 3 rồi xếp chấm được ngay.

   ⚠ ĐỢT 4 KHÔNG THỂ LÀM ĐƯỢC - đã xong, không sinh thêm đề tài được.
     Hãy xóa 2 hội đồng rác của bạn ở đợt 4 rồi tạo lại ở đợt 3
     (xem MỤC 5 ở cuối tệp).

   YÊU CẦU: MySQL 8.0.16+ (để CHECK được thực thi) | InnoDB | utf8mb4
   Chạy: Workbench > File > Run SQL Script, hoặc
         mysql -u root -p student_topic_management < tệp này
   ===================================================================== */

USE student_topic_management;
SET NAMES utf8mb4;
SET @now = NOW();


/* =====================================================================
   1. DỌN SẠCH (cho phép chạy lại nhiều lần không bị trùng khóa)

   Mỗi lần chạy lại sẽ xóa dữ liệu mẫu cũ rồi tạo lại với đúng id.
   KHÔNG xóa hội đồng/điểm/kết quả thật của bạn.
   ===================================================================== */
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM reports             WHERE id IN (8, 9, 10, 11, 12);
DELETE FROM topic_registrations WHERE group_id IN (14, 15, 16, 17);
DELETE FROM group_members       WHERE group_id IN (14, 15, 16, 17);
DELETE FROM student_groups      WHERE id      IN (14, 15, 16, 17);
DELETE FROM topic_supervisors   WHERE topic_id IN (18, 19, 20, 21);
DELETE FROM topics              WHERE id      IN (18, 19, 20, 21);

-- Trả nhóm g10 (đề tài t14) về trạng thái cũ nếu đã chạy MỤC 3.
UPDATE student_groups SET status = 'APPROVED'
 WHERE id = 10 AND status = 'SUBMITTED';

SET FOREIGN_KEY_CHECKS = 1;


/* =====================================================================
   2. 4 ĐỀ TÀI MỚI Ở ĐỢT 3 (KLTN HK1 2026-2027)

   Mỗi đề tài: APPROVED + published = 1  -> sinh viên đã đăng ký được.
   GVHD: lấy theo khoa của giảng viên đề xuất.
     t18 -> GVHD gv01 (KHMT) | t19 -> GVHD gv03 (HTTT)
     t20 -> GVHD gv05 (KTPM) | t21 -> GVHD gv08 (HTTT)
   ⚠ GVHD được chọn cố ý ĐỂ LỆCH với các GVHD của t11..t14, để bạn
     còn nhiều giảng viên khác để làm người chấm.
   ===================================================================== */
INSERT INTO topics
 (id, period_id, department_id, created_by_lecturer_id, topic_code, topic_name, description,
  status, reject_reason, approved_by, approved_at, published, created_at) VALUES
(18, 3, 1, 1, 'KLTN-26-105', 'Hệ thống nhận diện giọng nói tiếng Việt',
    'Xây dựng mô hình chuyển giọng nói thành văn bản cho tiếng Việt và đánh giá độ chính xác trên tập dữ liệu mở.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -60 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
(19, 3, 2, 3, 'KLTN-26-106', 'Nền tảng chia sẻ tài liệu học tập nội bộ khoa',
    'Quản lý tài liệu học tập, phân quyền truy cập theo vai trò người dùng và ghi nhận lịch sử tải tệp.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -60 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
(20, 3, 3, 5, 'KLTN-26-107', 'Ứng dụng theo dõi sức khỏe từ xa qua thiết bị IoT',
    'Thu thập chỉ số sức khỏe từ cảm biến, cảnh báo sớm và theo dõi bệnh nhân ngay tại nhà.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -60 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY)),
(21, 3, 2, 8, 'KLTN-26-108', 'Trợ lý tối ưu lịch trình sản xuất cho nhà máy',
    'Lập lịch chạy máy, phân bổ nguồn lực và tối ưu thời gian dừng máy trong dây chuyền sản xuất.',
    'APPROVED', NULL, 2, DATE_ADD(@now, INTERVAL -60 DAY), 1, DATE_ADD(@now, INTERVAL -78 DAY));

/* ---------- GV hướng dẫn (1 người mỗi đề tài) ---------- */
INSERT INTO topic_supervisors (topic_id, lecturer_id, supervisor_order, assigned_by, assigned_at) VALUES
(18, 1, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(19, 3, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(20, 5, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY)),
(21, 8, 1, NULL, DATE_ADD(@now, INTERVAL -78 DAY));


/* =====================================================================
   3. NHÓM SINH VIÊN + ĐĂNG KÝ + BÁO CÁO

   Sinh viên dùng: sv01..sv09, sv23  (đợt 3 chưa ai trong số này
   tham gia nhóm nào -> không vi phạm UNIQUE(student_id, period_id)).
   Nhóm trưởng nộp báo cáo (reports.submitted_by = students.id).
   ===================================================================== */
INSERT INTO student_groups (id, period_id, group_code, group_name, topic_id, status, created_at) VALUES
(14, 3, 'KLTN26-05', 'Nhóm VoiceAI',   18, 'SUBMITTED', DATE_ADD(@now, INTERVAL -64 DAY)),
(15, 3, 'KLTN26-06', 'Nhóm DocShare',  19, 'SUBMITTED', DATE_ADD(@now, INTERVAL -64 DAY)),
(16, 3, 'KLTN26-07', 'Nhóm HealthIoT', 20, 'SUBMITTED', DATE_ADD(@now, INTERVAL -64 DAY)),
(17, 3, 'KLTN26-08', 'Nhóm LineOpt',   21, 'SUBMITTED', DATE_ADD(@now, INTERVAL -64 DAY));

INSERT INTO group_members (group_id, period_id, student_id, is_leader, joined_at) VALUES
-- g14 Nhóm VoiceAI (3 người, trưởng = sv01)
(14, 3,  1, 1, DATE_ADD(@now, INTERVAL -64 DAY)), (14, 3,  2, 0, DATE_ADD(@now, INTERVAL -64 DAY)), (14, 3,  3, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
-- g15 Nhóm DocShare (2 người, trưởng = sv04)
(15, 3,  4, 1, DATE_ADD(@now, INTERVAL -64 DAY)), (15, 3,  5, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
-- g16 Nhóm HealthIoT (2 người, trưởng = sv06)
(16, 3,  6, 1, DATE_ADD(@now, INTERVAL -64 DAY)), (16, 3,  7, 0, DATE_ADD(@now, INTERVAL -64 DAY)),
-- g17 Nhóm LineOpt (3 người, trưởng = sv08)
(17, 3,  8, 1, DATE_ADD(@now, INTERVAL -64 DAY)), (17, 3,  9, 0, DATE_ADD(@now, INTERVAL -64 DAY)), (17, 3, 23, 0, DATE_ADD(@now, INTERVAL -64 DAY));

/* ---------- Đăng ký đề tài: nhóm trưởng đăng ký, Trưởng khoa duyệt ---------- */
INSERT INTO topic_registrations
 (group_id, topic_id, status, registered_at, approved_by, approved_at, rejection_reason) VALUES
(14, 18, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY), 2, DATE_ADD(@now, INTERVAL -61 DAY), NULL),
(15, 19, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY), 2, DATE_ADD(@now, INTERVAL -61 DAY), NULL),
(16, 20, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY), 2, DATE_ADD(@now, INTERVAL -61 DAY), NULL),
(17, 21, 'APPROVED', DATE_ADD(@now, INTERVAL -62 DAY), 2, DATE_ADD(@now, INTERVAL -61 DAY), NULL);

/* ---------- Báo cáo đã nộp  => đề tài đủ điều kiện đưa vào phản biện ----------
   ⚠ stored_path trỏ tới tệp CHƯA có thật trên đĩa. Nếu GV/SV bấm
     "Tải báo cáo" sẽ báo lỗi tệp không tồn tại. Muốn tải được thì
     chép tệp PDF thật vào app.storage.dir/demo/ trước, hoặc chạy lại
     tệp tạo dữ liệu demo gốc để có KLTN-26-101..104.               */
INSERT INTO reports
 (id, group_id, submitted_by, report_title, file_name, stored_path, file_size, version, description, submitted_at, status) VALUES
( 8, 14,  1, 'Báo cáo KLTN - Nhận dạng giọng nói tiếng Việt',      'bao_cao_KLTN26-05_v1.pdf', 'demo/KLTN-26-105_v1.pdf', 214800, 1, NULL, DATE_ADD(@now, INTERVAL -30 DAY), 'SUBMITTED'),
( 9, 15,  4, 'Báo cáo KLTN - Chia sẻ tài liệu học tập nội bộ',   'bao_cao_KLTN26-06_v1.pdf', 'demo/KLTN-26-106_v1.pdf', 198300, 1, NULL, DATE_ADD(@now, INTERVAL -30 DAY), 'SUBMITTED'),
(10, 16,  6, 'Báo cáo KLTN - Theo dõi sức khỏe từ xa qua IoT',    'bao_cao_KLTN26-07_v1.pdf', 'demo/KLTN-26-107_v1.pdf', 256100, 1, NULL, DATE_ADD(@now, INTERVAL -29 DAY), 'SUBMITTED'),
(11, 17,  8, 'Báo cáo KLTN - Tối ưu lịch trình sản xuất',        'bao_cao_KLTN26-08_v1.pdf', 'demo/KLTN-26-108_v1.pdf', 233500, 1, NULL, DATE_ADD(@now, INTERVAL -29 DAY), 'SUBMITTED');


/* =====================================================================
   4. (TÙY CHỌN) MỞ THÊM 1 ĐỀ TÀI ĐANG CHỜ: t14 Chatbot tuyển sinh

   Bộ dữ liệu demo cố ý để t14 CHƯA nộp báo cáo để dùng làm ví dụ
   "xếp đề tài". Nếu bạn muốn 5 đề tài thay vì 4, chạy phần này.
   Nhóm g10 (t14) đã có đăng ký APPROVED, chỉ thiếu báo cáo.
   ===================================================================== */
-- UPDATE student_groups SET status = 'SUBMITTED' WHERE id = 10;
-- INSERT INTO reports
--  (id, group_id, submitted_by, report_title, file_name, stored_path, file_size, version, description, submitted_at, status) VALUES
--  (12, 10, 21, 'Báo cáo KLTN - Chatbot tư vấn tuyển sinh 24/7', 'bao_cao_KLTN26-04_v1.pdf', 'demo/KLTN-26-104_v1.pdf', 189400, 1, NULL, DATE_ADD(@now, INTERVAL -28 DAY), 'SUBMITTED');


/* =====================================================================
   5. KIỂM TRA KẾT QUẢ

   Chạy phần này sau khi chạy xong: cột "xep_duoc" = YES nghĩa là
   hội đồng MỚI tạo ở ĐỢT 3 sẽ thấy đề tài này trong "Xếp đề tài".
   ===================================================================== */
SELECT t.id                AS topic_id,
       t.topic_code        AS ma_de_tai,
       t.topic_name        AS ten_de_tai,
       g.group_name        AS nhom,
       r.status            AS trang_thai_dang_ky,
       CASE WHEN EXISTS (SELECT 1 FROM reports rp
                         WHERE rp.group_id = g.id AND rp.status = 'SUBMITTED')
            THEN 'SUBMITTED' ELSE 'CHUA CO' END AS bao_cao,
       CASE WHEN EXISTS (SELECT 1 FROM council_topics ct
                         WHERE ct.topic_id = t.id)
            THEN 'NO' ELSE 'YES' END AS xep_duoc
FROM topics t
JOIN student_groups g       ON g.topic_id  = t.id
JOIN topic_registrations r  ON r.topic_id  = t.id
WHERE t.period_id = 3
ORDER BY t.id;


/* =====================================================================
   6. XÓA 2 HỘI ĐỒNG RÁC Ở ĐỢT 4 (bạn nhập tay lúc test)

   HD-KLTN-26-F : tên ghi KLTN nhưng thuộc đợt TLCN, 0 thành viên
   123123 / 31123: dữ liệu nhập tay

   Cả hai đều DRAFT + 0 thành viên + 0 đề tài nên xóa được an toàn.
   Sau khi xóa hãy tạo hội đồng MỚI và CHỌN ĐỢT 3, khi đó 4 đề tài
   ở MỤC 2 sẽ hiện ra để bạn xếp và phân công chấm.
   ===================================================================== */
-- DELETE FROM councils WHERE council_code IN ('HD-KLTN-26-F', '123123');
-- SELECT id, council_code, council_name, period_id, status FROM councils ORDER BY id;