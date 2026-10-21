package vn.edu.demo.topicmanagement.entity;

/** Trạng thái đề tài, khớp {@code chk_topics_status}: PENDING / APPROVED / REJECTED. */
public enum TopicStatus {
    /** Giảng viên đã đề xuất, chờ Trưởng khoa duyệt. */
    PENDING,
    /** Đã được duyệt (còn cờ {@code published} quyết định SV đã thấy chưa). */
    APPROVED,
    /** Bị từ chối, giảng viên sửa và gửi duyệt lại được. */
    REJECTED
}