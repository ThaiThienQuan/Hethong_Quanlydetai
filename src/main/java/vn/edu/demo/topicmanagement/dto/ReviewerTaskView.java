package vn.edu.demo.topicmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Nhiệm vụ phản biện được giao cho giảng viên đang đăng nhập. */
public class ReviewerTaskView {
    private final Long assignmentId;
    private final Long registrationId;
    private final String groupName;
    private final String topicTitle;
    private final LocalDateTime deadline;
    private final BigDecimal score;
    private final String comment;
    private final LocalDateTime submittedAt;
    private final boolean canSubmit;
    private final boolean reportAvailable;
    public ReviewerTaskView(Long assignmentId, Long registrationId, String groupName, String topicTitle,
                            LocalDateTime deadline, BigDecimal score, String comment, LocalDateTime submittedAt,
                            boolean canSubmit, boolean reportAvailable) {
        this.assignmentId = assignmentId;
        this.registrationId = registrationId;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.deadline = deadline;
        this.score = score;
        this.comment = comment;
        this.submittedAt = submittedAt;
        this.canSubmit = canSubmit;
        this.reportAvailable = reportAvailable;
    }
    public Long getAssignmentId() { return assignmentId; }
    public Long getRegistrationId() { return registrationId; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public LocalDateTime getDeadline() { return deadline; }
    public BigDecimal getScore() { return score; }
    public String getComment() { return comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public boolean isSubmitted() { return score != null; }
    public boolean isCanSubmit() { return canSubmit; }
    public boolean isReportAvailable() { return reportAvailable; }
}
