package vn.edu.demo.topicmanagement.entity;

/**
 * Trạng thái nhóm sinh viên, khớp {@code chk_groups_status}.
 * DRAFT -> REGISTERED -> APPROVED -> SUBMITTED -> COMPLETED
 */
public enum GroupStatus {
    /** Vừa lập, chưa đăng ký đề tài. */
    DRAFT,
    /** Đã đăng ký đề tài, đang chờ GVHD xác nhận. */
    REGISTERED,
    /** Đăng ký được chấp thuận, bắt đầu thực hiện theo GVHD được phân công. */
    APPROVED,
    /** Đã nộp báo cáo đề tài. */
    SUBMITTED,
    /** Đã chấm và công bố kết quả. */
    COMPLETED
}