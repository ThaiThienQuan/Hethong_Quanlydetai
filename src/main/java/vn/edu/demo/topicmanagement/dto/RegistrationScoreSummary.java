package vn.edu.demo.topicmanagement.dto;

import java.math.BigDecimal;

/** Projection đọc điểm trung bình từ view v_registration_avg_score. */
public interface RegistrationScoreSummary {
    Long getTopicRegistrationId();
    Long getScoreCount();
    BigDecimal getAvgScore();
}
