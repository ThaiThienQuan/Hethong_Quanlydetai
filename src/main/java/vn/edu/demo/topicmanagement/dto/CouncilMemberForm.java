package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.demo.topicmanagement.entity.CouncilRole;

/** Một giảng viên và chức vụ được thêm vào hội đồng. */
public class CouncilMemberForm {
    @NotNull(message = "Vui lòng chọn giảng viên")
    private Long lecturerId;
    @NotNull(message = "Vui lòng chọn vai trò")
    private CouncilRole role;
    public Long getLecturerId() { return lecturerId; }
    public CouncilRole getRole() { return role; }
    public void setLecturerId(Long lecturerId) { this.lecturerId = lecturerId; }
    public void setRole(CouncilRole role) { this.role = role; }
}
