package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.Size;

/**
 * Nhận xét tổng hợp do Chủ tịch hội đồng nhập trước khi chốt kết quả (mục 5.8).
 *
 * <p>Không có ô "kết quả" vì ĐẠT/KHÔNG ĐẠT do hệ thống tính từ điểm trung bình
 * và ngưỡng {@code app.passing-score} (A4). Nhận xét không bắt buộc - nếu để trống,
 * hệ thống tự sinh nhận xét mặc định theo kết quả.
 */
public class CouncilResultForm {
    @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự")
    private String councilComment;

    public String getCouncilComment() { return councilComment; }
    public void setCouncilComment(String councilComment) { this.councilComment = councilComment; }
}