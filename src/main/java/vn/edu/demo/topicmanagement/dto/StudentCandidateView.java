package vn.edu.demo.topicmanagement.dto;

/**
 * Sinh viên chưa thuộc nhóm nào trong đợt đăng ký hiện tại - ứng viên để nhóm trưởng
 * thêm vào nhóm của mình (đặc tả 5.4: mỗi sinh viên chỉ ở một nhóm trong mỗi đợt).
 */
public class StudentCandidateView {
    private final Long studentId;
    private final String studentCode;
    private final String fullName;
    private final String className;

    public StudentCandidateView(Long studentId, String studentCode, String fullName, String className) {
        this.studentId = studentId;
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.className = className;
    }

    public Long getStudentId() { return studentId; }
    public String getStudentCode() { return studentCode; }
    public String getFullName() { return fullName; }
    public String getClassName() { return className; }

    /** Nhãn hiển thị trong ô chọn: "Nguyễn Văn A (SV001 - CNTT1.K18)". */
    public String getLabel() {
        return fullName + " (" + studentCode + (className == null || className.isBlank() ? "" : " - " + className) + ")";
    }
}