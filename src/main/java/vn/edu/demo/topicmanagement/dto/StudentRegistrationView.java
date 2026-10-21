package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;

public class StudentRegistrationView {
    private final Long id;
    private final String groupName;
    private final String topicTitle;
    private final RegistrationStatus status;
    private final String rejectReason;
    private final LocalDateTime registeredAt;

    public StudentRegistrationView(Long id, String groupName, String topicTitle, RegistrationStatus status,
                                   String rejectReason, LocalDateTime registeredAt) {
        this.id = id;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.status = status;
        this.rejectReason = rejectReason;
        this.registeredAt = registeredAt;
    }
    public Long getId() { return id; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public RegistrationStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
}
