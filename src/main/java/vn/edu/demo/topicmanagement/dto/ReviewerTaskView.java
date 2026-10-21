package vn.edu.demo.topicmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Nhiệm vụ phản biện được giao cho giảng viên đang đăng nhập. */
public class ReviewerTaskView {
    private final Long assignmentId;
    private final Long registrationId;
    private final String groupName;
    private final String topicTitle;
    private final String topicCode;
    private final LocalDateTime deadline;
    private final BigDecimal score;
    private final String comment;
    private final LocalDateTime submittedAt;
    private final boolean canSubmit;
    private final boolean reportAvailable;
    private final boolean isSupervisor;
    private final boolean overdue;

    public ReviewerTaskView(Long assignmentId, Long registrationId, String groupName, String topicTitle,
                            String topicCode, LocalDateTime deadline,
                            BigDecimal score, String comment, LocalDateTime submittedAt,
                            boolean canSubmit, boolean reportAvailable,
                            boolean isSupervisor, boolean overdue) {
        this.assignmentId = assignmentId;
        this.registrationId = registrationId;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.topicCode = topicCode;
        this.deadline = deadline;
        this.score = score;
        this.comment = comment;
        this.submittedAt = submittedAt;
        this.canSubmit = canSubmit;
        this.reportAvailable = reportAvailable;
        this.isSupervisor = isSupervisor;
        this.overdue = overdue;
    }

    public Long getAssignmentId() { return assignmentId; }
    public Long getRegistrationId() { return registrationId; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public String getTopicCode() { return topicCode; }
    public LocalDateTime getDeadline() { return deadline; }
    public BigDecimal getScore() { return score; }
    public String getComment() { return comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public boolean isSubmitted() { return score != null; }
    public boolean isCanSubmit() { return canSubmit; }
    public boolean isReportAvailable() { return reportAvailable; }
    public boolean isSupervisor() { return isSupervisor; }
    public boolean isOverdue() { return overdue; }

    /** Số ngày còn lại đến hạn chót (dương = còn hạn, 0/âm = quá hạn hoặc không hạn). */
    public long daysLeft() {
        if (deadline == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(),
                deadline.toLocalDate());
    }
}
