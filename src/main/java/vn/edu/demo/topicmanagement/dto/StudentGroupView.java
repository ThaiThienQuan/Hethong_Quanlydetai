package vn.edu.demo.topicmanagement.dto;

import java.util.List;

/** Dữ liệu đã làm phẳng để hiển thị nhóm mà không phụ thuộc entity lazy trong template. */
public class StudentGroupView {
    private final Long id;
    private final String name;
    private final String leaderName;
    private final List<String> memberNames;
    private final List<StudentGroupMemberView> members;
    private final boolean currentUserLeader;
    private final boolean canTransferLeadership;

    public StudentGroupView(Long id, String name, String leaderName, List<String> memberNames,
                            List<StudentGroupMemberView> members, boolean currentUserLeader,
                            boolean canTransferLeadership) {
        this.id = id;
        this.name = name;
        this.leaderName = leaderName;
        this.memberNames = List.copyOf(memberNames);
        this.members = List.copyOf(members);
        this.currentUserLeader = currentUserLeader;
        this.canTransferLeadership = canTransferLeadership;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLeaderName() { return leaderName; }
    public List<String> getMemberNames() { return memberNames; }
    public List<StudentGroupMemberView> getMembers() { return members; }
    public int getMemberCount() { return memberNames.size(); }
    public boolean isCurrentUserLeader() { return currentUserLeader; }
    public boolean isCanTransferLeadership() { return canTransferLeadership; }
}
