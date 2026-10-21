package vn.edu.demo.topicmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import vn.edu.demo.topicmanagement.entity.CouncilRole;

/** Dữ liệu của một đề tài trong hội đồng cho trang chấm điểm/tổng hợp (mục 5.7 &amp; 5.8). */
public class CouncilWorkView {
    /** {@code review_assignments.id} - dùng cho form nộp điểm của GV được phân công. */
    private final Long assignmentId;
    /** {@code topic_registrations.id} - dùng để tải báo cáo của nhóm. */
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
    private final String myComment;
    private final LocalDateTime deadline;
    private final boolean canScore;
    private final boolean canFinalize;
    private final String resultLabel;
    private final String councilComment;
    private final boolean published;
    private final boolean reportAvailable;

    public CouncilWorkView(Long assignmentId, Long registrationId, String councilName, String groupName,
                           String topicTitle, CouncilRole currentRole, List<String> scores, int scoreCount,
                           int memberCount, BigDecimal averageScore, BigDecimal myScore, String myComment,
                           LocalDateTime deadline, boolean canScore, boolean canFinalize,
                           String resultLabel, String councilComment,
                           boolean published, boolean reportAvailable) {
        this.assignmentId = assignmentId;
        this.registrationId = registrationId;
        this.councilName = councilName;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.currentRole = currentRole;
        this.scores = scores == null ? List.of() : List.copyOf(scores);
        this.scoreCount = scoreCount;
        this.memberCount = memberCount;
        this.averageScore = averageScore;
        this.myScore = myScore;
        this.myComment = myComment;
        this.deadline = deadline;
        this.canScore = canScore;
        this.canFinalize = canFinalize;
        this.resultLabel = resultLabel;
        this.councilComment = councilComment;
        this.published = published;
        this.reportAvailable = reportAvailable;
    }

    public Long getAssignmentId() { return assignmentId; }
    public Long getRegistrationId() { return registrationId; }
    /** Tương thích với template cũ: đường dẫn form dùng giá trị này. */
    public Long getCouncilTopicId() { return assignmentId; }
    public String getCouncilName() { return councilName; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public CouncilRole getCurrentRole() { return currentRole; }
    public List<String> getScores() { return scores; }
    public int getScoreCount() { return scoreCount; }
    public int getMemberCount() { return memberCount; }
    public BigDecimal getAverageScore() { return averageScore; }
    public BigDecimal getMyScore() { return myScore; }
    public String getMyComment() { return myComment; }
    public LocalDateTime getDeadline() { return deadline; }
    public boolean isCanScore() { return canScore; }
    public boolean isCanFinalize() { return canFinalize; }
    public String getResultLabel() { return resultLabel; }
    public String getCouncilComment() { return councilComment; }
    public boolean isPublished() { return published; }
    public boolean isReportAvailable() { return reportAvailable; }
}