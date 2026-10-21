package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Thông tin nhóm và đề tài để GVHD xem trước khi xác nhận đăng ký. */
public class PendingRegistrationView {
    private final Long id;
    private final String groupName;
    private final String leaderName;
    private final List<StudentGroupMemberView> members;
    private final String topicTitle;
    private final String topicDescription;
    private final String departmentName;
    private final String periodName;
    private final String periodType;
    private final LocalDateTime registeredAt;

    public PendingRegistrationView(Long id, String groupName, String leaderName,
                                   List<StudentGroupMemberView> members, String topicTitle,
                                   String topicDescription, String departmentName, String periodName,
                                   String periodType, LocalDateTime registeredAt) {
        this.id = id;
        this.groupName = groupName;
        this.leaderName = leaderName;
        this.members = List.copyOf(members);
        this.topicTitle = topicTitle;
        this.topicDescription = topicDescription;
        this.departmentName = departmentName;
        this.periodName = periodName;
        this.periodType = periodType;
        this.registeredAt = registeredAt;
    }

    public Long getId() { return id; }
    public String getGroupName() { return groupName; }
    public String getLeaderName() { return leaderName; }
    public List<StudentGroupMemberView> getMembers() { return members; }
    /** Tên hiển thị các thành viên cho mẫu HTML. */
    public List<String> getMemberNames() {
        return members.stream().map(StudentGroupMemberView::getFullName).toList();
    }
    public String getTopicTitle() { return topicTitle; }
    public String getTopicDescription() { return topicDescription; }
    public String getDepartmentName() { return departmentName; }
    public String getPeriodName() { return periodName; }
    public String getPeriodType() { return periodType; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
}
