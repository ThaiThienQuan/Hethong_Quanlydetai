package vn.edu.demo.topicmanagement.dto;

import java.util.List;

/** Phân công GVPB/GV chấm cho một đề tài trong hội đồng (mục 5.7). */
public class CouncilTopicAssignmentView {
    private final Long councilId;
    private final Long councilTopicId;
    private final Long topicId;
    private final String topicCode;
    private final String topicTitle;
    private final String groupName;
    private final List<ReviewAssigneeView> assignees;
    private final List<LecturerOption> candidates;
    private final boolean ready;

    public CouncilTopicAssignmentView(Long councilId, Long councilTopicId, Long topicId,
                                      String topicCode, String topicTitle, String groupName,
                                      List<ReviewAssigneeView> assignees,
                                      List<LecturerOption> candidates,
                                      boolean ready) {
        this.councilId = councilId;
        this.councilTopicId = councilTopicId;
        this.topicId = topicId;
        this.topicCode = topicCode;
        this.topicTitle = topicTitle;
        this.groupName = groupName;
        this.assignees = assignees == null ? List.of() : List.copyOf(assignees);
        this.candidates = candidates == null ? List.of() : List.copyOf(candidates);
        this.ready = ready;
    }

    public Long getCouncilId() { return councilId; }
    public Long getCouncilTopicId() { return councilTopicId; }
    public Long getTopicId() { return topicId; }
    public String getTopicCode() { return topicCode; }
    public String getTopicTitle() { return topicTitle; }
    public String getGroupName() { return groupName; }
    public List<ReviewAssigneeView> getAssignees() { return assignees; }
    public List<LecturerOption> getCandidates() { return candidates; }
    public boolean isReady() { return ready; }
}
