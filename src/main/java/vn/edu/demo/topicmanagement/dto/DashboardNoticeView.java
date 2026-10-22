package vn.edu.demo.topicmanagement.dto;

/**
 * Một thông báo mới hiển thị ở khối "Thông báo mới" của trang Tổng quan (mục 5.10).
 *
 * @param id          id thông báo
 * @param title       tiêu đề
 * @param scopeLabel  nhãn loại thông báo (Nhà trường / Khoa)
 * @param publishedAt thời điểm đăng, đã định dạng {@code dd/MM/yyyy HH:mm}
 */
public record DashboardNoticeView(
        Long id,
        String title,
        String scopeLabel,
        String publishedAt) {
}
