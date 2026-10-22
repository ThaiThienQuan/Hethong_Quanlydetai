package vn.edu.demo.topicmanagement.dto;

/**
 * Dòng tóm tắt một đợt đăng ký đang mở, hiển thị ở bảng "Đợt đang mở" (mục 5.10).
 *
 * @param id              id đợt
 * @param name            tên đợt
 * @param typeLabel       loại đợt
 * @param phaseLabel      giai đoạn hiện tại
 * @param lecturerWindow  khoảng thời gian GV đăng ký
 * @param studentWindow   khoảng thời gian SV đăng ký
 * @param reviewDeadline  hạn chót GVPB (null nếu không có)
 */
public record OpenPeriodView(
        Long id,
        String name,
        String typeLabel,
        String phaseLabel,
        String lecturerWindow,
        String studentWindow,
        String reviewDeadline) {
}