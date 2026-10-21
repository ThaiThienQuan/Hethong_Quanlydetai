package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotNull;

public class TopicRegistrationForm {
    @NotNull(message = "Vui lòng chọn đề tài")
    private Long topicId;
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
}
