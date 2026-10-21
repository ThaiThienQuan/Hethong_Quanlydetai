package vn.edu.demo.topicmanagement.dto;

/** A reviewer assignment displayed to the faculty administrator. */
public class ReviewAssigneeView {
    private final Long assignmentId;
    private final String fullName;
    private final boolean submitted;

    public ReviewAssigneeView(Long assignmentId, String fullName, boolean submitted) {
        this.assignmentId = assignmentId;
        this.fullName = fullName;
        this.submitted = submitted;
    }

    public Long getAssignmentId() { return assignmentId; }
    public String getFullName() { return fullName; }
    public boolean isSubmitted() { return submitted; }
}
