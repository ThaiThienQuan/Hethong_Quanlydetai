package vn.edu.demo.topicmanagement.dto;

/**
 * Một thành viên trong nhóm sinh viên.
 *
 * <p>Dùng cho bảng thành viên (cho phép nhóm trưởng xóa thành viên), cho danh sách
 * chọn khi chuyển quyền nhóm trưởng và cho danh sách thành viên ở màn hình duyệt
 * đăng ký của giảng viên.
 */
public class StudentGroupMemberView {
    private final Long studentId;
    private final String fullName;
    private final String studentCode;
    /** {@code true} nếu thành viên này đang là nhóm trưởng (membership.is_leader). */
    private final boolean leader;

    public StudentGroupMemberView(Long studentId, String fullName, String studentCode) {
        this(studentId, fullName, studentCode, false);
    }

    public StudentGroupMemberView(Long studentId, String fullName, String studentCode, boolean leader) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.studentCode = studentCode;
        this.leader = leader;
    }

    public Long getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getStudentCode() { return studentCode; }
    public boolean isLeader() { return leader; }
}