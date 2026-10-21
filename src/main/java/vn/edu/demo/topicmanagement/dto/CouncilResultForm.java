package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nhận xét và xếp loại do Chủ tịch nhập trước khi công bố điểm trung bình. */
public class CouncilResultForm {
    @NotBlank(message = "Vui lòng nhập kết quả/xếp loại")
    @Size(max = 100, message = "Kết quả tối đa 100 ký tự")
    private String result;
    @NotBlank(message = "Vui lòng nhập nhận xét tổng hợp")
    private String councilComment;
    public String getResult() { return result; }
    public String getCouncilComment() { return councilComment; }
    public void setResult(String result) { this.result = result; }
    public void setCouncilComment(String councilComment) { this.councilComment = councilComment; }
}
