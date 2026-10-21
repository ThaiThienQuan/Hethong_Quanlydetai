package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Điểm và nhận xét GVPB nhập cho một đề tài. */
public class ReviewScoreForm {
    @NotNull(message = "Vui lòng nhập điểm")
    @DecimalMin(value = "0.00", message = "Điểm thấp nhất là 0")
    @DecimalMax(value = "10.00", message = "Điểm cao nhất là 10")
    private BigDecimal score;
    @Size(max = 5000, message = "Nhận xét tối đa 5000 ký tự")
    private String comment;
    public BigDecimal getScore() { return score; }
    public String getComment() { return comment; }
    public void setScore(BigDecimal score) { this.score = score; }
    public void setComment(String comment) { this.comment = comment; }
}
