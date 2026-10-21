package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Điểm và nhận xét GVPB nhập cho một đề tài. */
public class ReviewScoreForm {
    @NotNull(message = "Vui lòng nhập điểm")
    @DecimalMin(value = "0.00", message = "Điểm thấp nhất là 0")
    @DecimalMax(value = "10.00", message = "Điểm cao nhất là 10")
    private BigDecimal score;

    /** Nhận xét bắt buộc - quy tắc A1 mục 5.7. */
    @NotBlank(message = "Vui lòng nhập nhận xét (bắt buộc)")
    @Size(max = 1000, message = "Nhận xét tối đa 1000 ký tự")
    private String comment;

    /** Lý do quá hạn - GV bình thường bị chặn; admin để trống nghĩa là "không có". */
    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String overrideReason;

    public BigDecimal getScore() { return score; }
    public String getComment() { return comment; }
    public String getOverrideReason() { return overrideReason; }

    public void setScore(BigDecimal score) { this.score = score; }
    public void setComment(String comment) { this.comment = comment; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
}
