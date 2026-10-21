package vn.edu.demo.topicmanagement.dto;

/** Tiến độ chấm của một đề tài trong hội đồng: số phân công / số đã nộp điểm. */
public class CouncilTopicProgressView {
    private final Long councilTopicId;
    private final Long topicId;
    private final String topicCode;
    private final String topicTitle;
    private final String groupName;
    /** Tổng số giảng viên được phân công chấm đề tài này. */
    private final int assignments;
    /** Số giảng viên đã có điểm. */
    private final int scored;

    public CouncilTopicProgressView(Long councilTopicId, Long topicId, String topicCode,
                                    String topicTitle, String groupName,
                                    int assignments, int scored) {
        this.councilTopicId = councilTopicId;
        this.topicId = topicId;
        this.topicCode = topicCode;
        this.topicTitle = topicTitle;
        this.groupName = groupName;
        this.assignments = assignments;
        this.scored = scored;
    }

    public Long getCouncilTopicId() { return councilTopicId; }
    public Long getTopicId() { return topicId; }
    public String getTopicCode() { return topicCode; }
    public String getTopicTitle() { return topicTitle; }
    public String getGroupName() { return groupName; }
    public int getAssignments() { return assignments; }
    public int getScored() { return scored; }

    public int remaining() { return Math.max(0, assignments - scored); }
    public boolean fullyScored() { return assignments > 0 && scored >= assignments; }
}
