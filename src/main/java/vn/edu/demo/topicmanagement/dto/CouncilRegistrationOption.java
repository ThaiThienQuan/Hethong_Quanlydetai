package vn.edu.demo.topicmanagement.dto;

public class CouncilRegistrationOption {
    private final Long registrationId;
    private final String groupName;
    private final String topicTitle;
    public CouncilRegistrationOption(Long registrationId, String groupName, String topicTitle) {
        this.registrationId = registrationId; this.groupName = groupName; this.topicTitle = topicTitle;
    }
    public Long getRegistrationId() { return registrationId; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
}
