package vn.edu.demo.topicmanagement.dto;

/** Một phân công GVPB của một đề tài trong hội đồng. */
public class ReviewAssigneeView {
    private final Long assignmentId;
    private final String fullName;
    private final String lecturerCode;
    private final boolean submitted;
    private final boolean isSupervisor;

    public ReviewAssigneeView(Long assignmentId, String fullName, String lecturerCode,
                              boolean submitted, boolean isSupervisor) {
        this.assignmentId = assignmentId;
        this.fullName = fullName;
        this.lecturerCode = lecturerCode;
        this.submitted = submitted;
        this.isSupervisor = isSupervisor;
    }

    public Long getAssignmentId() { return assignmentId; }
    public String getFullName() { return fullName; }
    public String getLecturerCode() { return lecturerCode; }
    public boolean isSubmitted() { return submitted; }
    public boolean isSupervisor() { return isSupervisor; }
}
