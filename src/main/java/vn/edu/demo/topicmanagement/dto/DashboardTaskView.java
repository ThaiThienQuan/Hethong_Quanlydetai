package vn.edu.demo.topicmanagement.dto;

/**
 * Một dòng trong bảng "Việc cần làm của tôi" của trang Tổng quan (mục 5.10).
 *
 * @param key         mã định danh của nhóm việc (dùng cho ổn định khi render)
 * @param label       tên việc cần làm, tiếng Việt
 * @param description mô tả ngắn giúp người dùng hiểu cần làm gì
 * @param count       số lượng đang chờ xử lý
 * @param url         đường dẫn màn hình xử lý tương ứng
 * @param tone        màu nhấn: primary / success / warning / danger / info
 */
public record DashboardTaskView(
        String key,
        String label,
        String description,
        long count,
        String url,
        String tone) {
}
