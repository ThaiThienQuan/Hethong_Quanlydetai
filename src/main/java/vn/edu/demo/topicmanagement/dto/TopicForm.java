package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dữ liệu form giảng viên đăng ký / sửa đề tài. */
public class TopicForm {

    /**
     * Mã đề tài do HỆ THỐNG cấp, không phải người dùng nhập. Form không có ô nhập
     * mã; giá trị này được {@code TopicService.generateTopicCode} gán lúc lưu.
     *
     * <p>Giữ field để mã cũ vẫn hiện readonly khi sửa đề tài. Bỏ ràng buộc
     * {@code @NotBlank} vì lúc tạo mới mã còn trống hoàn toàn.
     */
    @Size(max = 30, message = "Mã đề tài tối đa 30 ký tự")
    private String topicCode;

    @NotBlank(message = "Vui lòng nhập tên đề tài")
    @Size(max = 200, message = "Tên đề tài tối đa 200 ký tự")
    private String topicName;

    @NotBlank(message = "Vui lòng mô tả nội dung đề tài")
    @Size(min = 20, max = 5000, message = "Mô tả nội dung phải từ 20 đến 5000 ký tự")
    private String description;

    @NotNull(message = "Vui lòng chọn bộ môn")
    private Long departmentId;

    @NotNull(message = "Vui lòng chọn đợt đăng ký")
    private Long periodId;

    /** Không bắt buộc: GVHD thứ hai. */
    private Long coSupervisorId;

    public String getTopicCode() { return topicCode; }
    public String getTopicName() { return topicName; }
    public String getDescription() { return description; }
    public Long getDepartmentId() { return departmentId; }
    public Long getPeriodId() { return periodId; }
    public Long getCoSupervisorId() { return coSupervisorId; }

    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public void setDescription(String description) { this.description = description; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public void setCoSupervisorId(Long coSupervisorId) { this.coSupervisorId = coSupervisorId; }

    /** Tương thích code cũ gọi {@code getTitle()}. */
    public String getTitle() { return topicName; }
    public void setTitle(String title) { this.topicName = title; }
}