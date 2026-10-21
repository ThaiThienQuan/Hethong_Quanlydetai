package vn.edu.demo.topicmanagement.dto;

/** Student group member option used when transferring group leadership. */
public class StudentGroupMemberView {
    private final Long studentId;
    private final String fullName;
    private final String studentCode;

    public StudentGroupMemberView(Long studentId, String fullName, String studentCode) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.studentCode = studentCode;
    }

    public Long getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getStudentCode() { return studentCode; }
}
