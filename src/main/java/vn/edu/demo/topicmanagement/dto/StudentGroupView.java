package vn.edu.demo.topicmanagement.dto;

import java.util.List;

/**
 * Dữ liệu đã làm phẳng để hiển thị nhóm mà không phụ thuộc entity lazy trong template.
 *
 * <p>Các cờ quyền của người dùng hiện tại:
 * <ul>
 *   <li>{@link #isCurrentUserLeader()} – người đang đăng nhập có phải nhóm trưởng không.</li>
 *   <li>{@link #isCanManageMembers()} – nhóm trưởng có thể thêm/xóa thành viên không
 *       (chỉ khi nhóm chưa có đăng ký đề tài hiệu lực và còn chỗ trống).</li>
 *   <li>{@link #isCanTransferLeadership()} – nhóm trưởng có thể chuyển quyền cho
 *       thành viên khác không (chỉ khi còn nhiều hơn 1 thành viên và chưa có đăng ký).</li>
 * </ul>
 */
public class StudentGroupView {
    private final Long id;
    private final String name;
    private final String leaderName;
    private final Long leaderStudentId;
    private final List<String> memberNames;
    private final List<StudentGroupMemberView> members;
    private final List<StudentCandidateView> availableCandidates;
    private final boolean currentUserLeader;
    private final boolean canManageMembers;
    private final boolean canTransferLeadership;
    private final int minSize;
    private final int maxSize;

    public StudentGroupView(Long id, String name, String leaderName, Long leaderStudentId,
                            List<String> memberNames, List<StudentGroupMemberView> members,
                            List<StudentCandidateView> availableCandidates,
                            boolean currentUserLeader, boolean canManageMembers,
                            boolean canTransferLeadership, int minSize, int maxSize) {
        this.id = id;
        this.name = name;
        this.leaderName = leaderName;
        this.leaderStudentId = leaderStudentId;
        this.memberNames = List.copyOf(memberNames);
        this.members = List.copyOf(members);
        this.availableCandidates = availableCandidates == null ? List.of() : List.copyOf(availableCandidates);
        this.currentUserLeader = currentUserLeader;
        this.canManageMembers = canManageMembers;
        this.canTransferLeadership = canTransferLeadership;
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLeaderName() { return leaderName; }
    public Long getLeaderStudentId() { return leaderStudentId; }
    public List<String> getMemberNames() { return memberNames; }
    public List<StudentGroupMemberView> getMembers() { return members; }
    public List<StudentCandidateView> getAvailableCandidates() { return availableCandidates; }
    public int getMemberCount() { return memberNames.size(); }
    public int getMinSize() { return minSize; }
    public int getMaxSize() { return maxSize; }
    public boolean isCurrentUserLeader() { return currentUserLeader; }
    public boolean isCanManageMembers() { return canManageMembers; }
    public boolean isCanTransferLeadership() { return canTransferLeadership; }
    /** Còn chỗ trống để thêm thành viên mới hay không (dùng để tắt dropdown). */
    public boolean isHasVacancy() { return memberNames.size() < maxSize; }
}