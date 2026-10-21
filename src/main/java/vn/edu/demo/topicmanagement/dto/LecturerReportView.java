package vn.edu.demo.topicmanagement.dto;

/** A submitted report belonging to a topic supervised by the current lecturer. */
public class LecturerReportView {
    private final Long registrationId;
    private final String groupName;
    private final String topicTitle;
    private final String fileName;
    private final boolean downloadAvailable;

    public LecturerReportView(Long registrationId, String groupName, String topicTitle,
                              String fileName, boolean downloadAvailable) {
        this.registrationId = registrationId;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.fileName = fileName;
        this.downloadAvailable = downloadAvailable;
    }

    public Long getRegistrationId() { return registrationId; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public String getFileName() { return fileName; }
    public boolean isDownloadAvailable() { return downloadAvailable; }
}
