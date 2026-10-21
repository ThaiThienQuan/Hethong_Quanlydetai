package vn.edu.demo.topicmanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Kết quả đã công bố mà sinh viên thuộc nhóm đăng ký được phép xem. */
public class StudentResultView {
    private final String groupName;
    private final String topicTitle;
    private final BigDecimal finalScore;
    private final String result;
    private final String councilComment;
    private final LocalDateTime publishedAt;
    public StudentResultView(String groupName, String topicTitle, BigDecimal finalScore, String result,
                             String councilComment, LocalDateTime publishedAt) {
        this.groupName = groupName; this.topicTitle = topicTitle; this.finalScore = finalScore;
        this.result = result; this.councilComment = councilComment; this.publishedAt = publishedAt;
    }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getResult() { return result; }
    public String getCouncilComment() { return councilComment; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
}
