package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;

/** A row of the head's reviewer-assignment table. */
public class ReviewAssignmentView {
    private final Long registrationId;
    private final String groupName;
    private final String topicTitle;
    private final String periodType;
    private final LocalDateTime deadline;
    private final List<ReviewAssigneeView> assignees;
    private final List<LecturerOption> candidates;

    public ReviewAssignmentView(Long registrationId, String groupName, String topicTitle,
                                String periodType, LocalDateTime deadline,
                                List<ReviewAssigneeView> assignees,
                                List<LecturerOption> candidates) {
        this.registrationId = registrationId;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.periodType = periodType;
        this.deadline = deadline;
        this.assignees = assignees;
        this.candidates = candidates;
    }

    public Long getRegistrationId() { return registrationId; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public String getPeriodType() { return periodType; }
    public LocalDateTime getDeadline() { return deadline; }
    public List<ReviewAssigneeView> getAssignees() { return assignees; }
    public List<LecturerOption> getCandidates() { return candidates; }
}
