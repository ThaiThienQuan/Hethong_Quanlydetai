package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;

/** Dữ liệu hiển thị trang báo cáo, tách khỏi entity JPA để template không tải quan hệ lazy. */
public class StudentReportView {
    private final Long registrationId;
    private final String topicTitle;
    private final RegistrationStatus registrationStatus;
    private final boolean groupLeader;
    private final String fileName;
    private final LocalDateTime submittedAt;
    private final boolean downloadAvailable;

    public StudentReportView(Long registrationId, String topicTitle, RegistrationStatus registrationStatus,
                             boolean groupLeader, String fileName, LocalDateTime submittedAt,
                             boolean downloadAvailable) {
        this.registrationId = registrationId;
        this.topicTitle = topicTitle;
        this.registrationStatus = registrationStatus;
        this.groupLeader = groupLeader;
        this.fileName = fileName;
        this.submittedAt = submittedAt;
        this.downloadAvailable = downloadAvailable;
    }
    public Long getRegistrationId() { return registrationId; }
    public String getTopicTitle() { return topicTitle; }
    public RegistrationStatus getRegistrationStatus() { return registrationStatus; }
    public boolean isGroupLeader() { return groupLeader; }
    public String getFileName() { return fileName; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public boolean hasReport() { return fileName != null; }
    public boolean isDownloadAvailable() { return downloadAvailable; }
}
