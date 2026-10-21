package vn.edu.demo.topicmanagement.dto;

/** Dữ liệu Trưởng khoa chọn khi phân công hoặc thay đổi GVHD. */
public class HeadSupervisorForm {
    private Long primarySupervisorId;
    private Long coSupervisorId;

    public Long getPrimarySupervisorId() { return primarySupervisorId; }
    public void setPrimarySupervisorId(Long id) { this.primarySupervisorId = id; }
    public Long getCoSupervisorId() { return coSupervisorId; }
    public void setCoSupervisorId(Long id) { this.coSupervisorId = id; }
}
