package vn.edu.demo.topicmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Kết quả đã công bố mà sinh viên thuộc nhóm được phép xem (mục 5.8). */
public class StudentResultView {
    private final Long periodId;
    private final String periodName;
    private final String groupName;
    private final String topicTitle;
    private final BigDecimal finalScore;
    private final String result;
    private final String councilComment;
    private final LocalDateTime publishedAt;

    public StudentResultView(Long periodId, String periodName, String groupName, String topicTitle,
                             BigDecimal finalScore, String result,
                             String councilComment, LocalDateTime publishedAt) {
        this.periodId = periodId;
        this.periodName = periodName;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.finalScore = finalScore;
        this.result = result;
        this.councilComment = councilComment;
        this.publishedAt = publishedAt;
    }

    public Long getPeriodId() { return periodId; }
    public String getPeriodName() { return periodName; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getResult() { return result; }
    /** Nhãn tiếng Việt của kết quả (ĐẠT / KHÔNG ĐẠT). */
    public String getResultLabel() { return "PASSED".equals(result) ? "ĐẠT" : "KHÔNG ĐẠT"; }
    public String getCouncilComment() { return councilComment; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
}