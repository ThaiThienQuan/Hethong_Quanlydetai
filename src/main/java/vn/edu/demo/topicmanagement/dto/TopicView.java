package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicStatus;

/** Gói 1 đề tài cùng thông tin phụ (tên các GVHD, có được sửa không) để đưa ra trang danh sách. */
public class TopicView {
    private Topic topic;
    private Long id;
    private String topicCode;
    private String topicName;
    private String description;
    private TopicStatus status;
    private Long departmentId;
    private String departmentName;
    private Long periodId;
    private String periodName;
    private String createdByLecturerName;
    /** createdAt của bảng topics — dùng cho cột "Ngày tạo" ở trang danh sách công bố. */
    private LocalDateTime createdAt;
    private List<LecturerOption> supervisors = new ArrayList<>();
    private boolean owner;
    private boolean editable;
    private String supervisorsText;
    private String rejectReason;
    private String groupName;
    private String groupCode;

    public TopicView() {}

    public TopicView(Topic topic, String supervisors, boolean editable) {
        this.topic = topic;
        this.supervisorsText = supervisors;
        this.editable = editable;
        if (topic != null) {
            this.id = topic.getId();
            this.topicCode = topic.getTopicCode();
            this.topicName = topic.getTopicName();
            this.description = topic.getDescription();
            this.status = topic.getStatus();
            this.rejectReason = topic.getRejectReason();
            if (topic.getDepartment() != null) {
                this.departmentId = topic.getDepartment().getId();
                this.departmentName = topic.getDepartment().getName();
            }
            if (topic.getPeriod() != null) {
                this.periodId = topic.getPeriod().getId();
                this.periodName = topic.getPeriod().getName();
            }
        }
    }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public String getSupervisors() { return supervisorsText; }
    public boolean isEditable() { return editable; }
    public String getSupervisorsText() { return supervisorsText; }
    public Long getId() { return id; }
    public String getTopicCode() { return topicCode; }
    public String getTopicName() { return topicName; }
    public String getDescription() { return description; }
    public TopicStatus getStatus() { return status; }
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public Long getPeriodId() { return periodId; }
    public String getPeriodName() { return periodName; }
    public String getCreatedByLecturerName() { return createdByLecturerName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<LecturerOption> getSupervisorList() { return supervisors; }
    public boolean isOwner() { return owner; }
    public String getRejectReason() { return rejectReason; }
    public String getGroupName() { return groupName; }
    public String getGroupCode() { return groupCode; }

    public void setId(Long id) { this.id = id; }
    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(TopicStatus status) { this.status = status; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    public void setCreatedByLecturerName(String createdByLecturerName) { this.createdByLecturerName = createdByLecturerName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setSupervisors(List<LecturerOption> supervisors) { this.supervisors = supervisors; }
    public void setSupervisorsText(String supervisors) { this.supervisorsText = supervisors; }
    public void setOwner(boolean owner) { this.owner = owner; }
    public void setEditable(boolean editable) { this.editable = editable; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
}
