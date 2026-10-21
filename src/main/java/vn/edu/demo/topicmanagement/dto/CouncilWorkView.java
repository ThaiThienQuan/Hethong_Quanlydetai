package vn.edu.demo.topicmanagement.dto;

import java.math.BigDecimal;
import java.util.List;
import vn.edu.demo.topicmanagement.entity.CouncilRole;

/** Dữ liệu của một đề tài trong hội đồng cho trang chấm điểm/tổng hợp. */
public class CouncilWorkView {
    private final Long councilTopicId;
    private final Long registrationId;
    private final String councilName;
    private final String groupName;
    private final String topicTitle;
    private final CouncilRole currentRole;
    private final List<String> scores;
    private final int scoreCount;
    private final int memberCount;
    private final BigDecimal averageScore;
    private final BigDecimal myScore;
    private final boolean canScore;
    private final boolean canFinalize;
    private final String resultLabel;
    private final String councilComment;
    private final boolean published;
    private final boolean reportAvailable;

    public CouncilWorkView(Long councilTopicId, Long registrationId, String councilName, String groupName,
                           String topicTitle, CouncilRole currentRole, List<String> scores, int scoreCount,
                           int memberCount, BigDecimal averageScore, BigDecimal myScore, boolean canScore,
                           boolean canFinalize, String resultLabel, String councilComment,
                           boolean published, boolean reportAvailable) {
        this.councilTopicId = councilTopicId; this.registrationId = registrationId; this.councilName = councilName;
        this.groupName = groupName; this.topicTitle = topicTitle; this.currentRole = currentRole;
        this.scores = List.copyOf(scores); this.scoreCount = scoreCount; this.memberCount = memberCount;
        this.averageScore = averageScore; this.myScore = myScore; this.canScore = canScore;
        this.canFinalize = canFinalize; this.resultLabel = resultLabel;
        this.councilComment = councilComment; this.published = published;
        this.reportAvailable = reportAvailable;
    }
    public Long getCouncilTopicId() { return councilTopicId; }
    public Long getRegistrationId() { return registrationId; }
    public String getCouncilName() { return councilName; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public CouncilRole getCurrentRole() { return currentRole; }
    public List<String> getScores() { return scores; }
    public int getScoreCount() { return scoreCount; }
    public int getMemberCount() { return memberCount; }
    public BigDecimal getAverageScore() { return averageScore; }
    public BigDecimal getMyScore() { return myScore; }
    public boolean isCanScore() { return canScore; }
    public boolean isCanFinalize() { return canFinalize; }
    public String getResultLabel() { return resultLabel; }
    public String getCouncilComment() { return councilComment; }
    public boolean isPublished() { return published; }
    public boolean isReportAvailable() { return reportAvailable; }
}
