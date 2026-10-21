package vn.edu.demo.topicmanagement.dto;

import java.math.BigDecimal;

/** Một kết quả đã tổng hợp đang chờ Trưởng khoa công bố (mục 5.8). */
public class HeadResultView {
    private final Long councilTopicId;
    private final Long resultId;
    private final Long councilId;
    private final String councilName;
    private final String groupName;
    private final String topicTitle;
    private final BigDecimal finalScore;
    private final String result;
    private final String councilComment;

    public HeadResultView(Long councilTopicId, Long resultId, Long councilId, String councilName,
                          String groupName, String topicTitle,
                          BigDecimal finalScore, String result, String councilComment) {
        this.councilTopicId = councilTopicId;
        this.resultId = resultId;
        this.councilId = councilId;
        this.councilName = councilName;
        this.groupName = groupName;
        this.topicTitle = topicTitle;
        this.finalScore = finalScore;
        this.result = result;
        this.councilComment = councilComment;
    }

    public Long getCouncilTopicId() { return councilTopicId; }
    public Long getResultId() { return resultId; }
    public Long getCouncilId() { return councilId; }
    public String getCouncilName() { return councilName; }
    public String getGroupName() { return groupName; }
    public String getTopicTitle() { return topicTitle; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getResult() { return result; }
    /** Nhãn tiếng Việt của kết quả (ĐẠT / KHÔNG ĐẠT). */
    public String getResultLabel() {
        return "PASSED".equals(result) ? "ĐẠT" : "KHÔNG ĐẠT";
    }
    public String getCouncilComment() { return councilComment; }
}