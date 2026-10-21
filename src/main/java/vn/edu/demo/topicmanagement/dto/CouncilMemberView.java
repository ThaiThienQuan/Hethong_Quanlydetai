package vn.edu.demo.topicmanagement.dto;

import vn.edu.demo.topicmanagement.entity.CouncilRole;

public class CouncilMemberView {
    private final Long memberId;
    private final String fullName;
    private final String lecturerCode;
    private final CouncilRole role;
    public CouncilMemberView(Long memberId, String fullName, String lecturerCode, CouncilRole role) {
        this.memberId = memberId;
        this.fullName = fullName; this.lecturerCode = lecturerCode; this.role = role;
    }
    public Long getMemberId() { return memberId; }
    public String getFullName() { return fullName; }
    public String getLecturerCode() { return lecturerCode; }
    public CouncilRole getRole() { return role; }
}
