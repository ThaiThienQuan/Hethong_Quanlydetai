package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;

/** Một dòng nhật ký email gửi kết quả cho sinh viên (mục 7.10). */
public class EmailLogView {
    private final Long id;
    private final Long resultId;
    private final String studentName;
    private final String studentCode;
    private final String groupName;
    private final String topicTitle;
    private final String toEmail;
    private final String subject;
    private final String status;
    private final String errorMessage;
    private final int attemptCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime sentAt;

    public EmailLogView(Long id, Long resultId, String studentName, String studentCode,
                        String groupName, String topicTitle, String toEmail, String subject,
                        String status, String errorMessage, int attemptCount,
                        LocalDateTime createdAt, LocalDateTime sentAt) {
        this.id = id;
        this.resultId = resultId;
        this.studentName = studentName;
        this.studentCode = studentCode;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.toEmail = toEmail;
        this.subject = subject;
        this.status = status;
        this.errorMessage = errorMessage;
        this.attemptCount = attemptCount;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public Long getId() { return id; }
    public Long getResultId() { return resultId; }
    public String getStudentName() { return studentName; }
    public String getStudentCode() { return studentCode; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public String getToEmail() { return toEmail; }
    public String getSubject() { return subject; }
    public String getStatus() { return status; }
    /** Nhãn tiếng Việt cho trạng thái. */
    public String getStatusLabel() {
        return switch (status == null ? "" : status) {
            case "SENT" -> "Đã gửi";
            case "FAILED" -> "Lỗi";
            case "SKIPPED" -> "Bỏ qua (demo)";
            default -> status;
        };
    }
    public boolean isFailed() { return "FAILED".equals(status); }
    public boolean isSkipped() { return "SKIPPED".equals(status); }
    public String getErrorMessage() { return errorMessage; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }
}