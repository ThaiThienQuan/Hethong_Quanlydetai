package vn.edu.demo.topicmanagement.dto;

import java.math.BigDecimal;

/** A consolidated council result awaiting publication by the faculty. */
public class HeadResultView {
    private final Long councilTopicId;
    private final String councilName;
    private final String groupName;
    private final String topicTitle;
    private final BigDecimal finalScore;
    private final String result;
    private final String councilComment;

    public HeadResultView(Long councilTopicId, String councilName, String groupName, String topicTitle,
                          BigDecimal finalScore, String result, String councilComment) {
        this.councilTopicId = councilTopicId;
        this.councilName = councilName;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.finalScore = finalScore;
        this.result = result;
        this.councilComment = councilComment;
    }

    public Long getCouncilTopicId() { return councilTopicId; }
    public String getCouncilName() { return councilName; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getResult() { return result; }
    public String getCouncilComment() { return councilComment; }
}
