package vn.edu.demo.topicmanagement.dto;

import java.util.List;

/** Dữ liệu của trang quản lý một hội đồng. */
public class CouncilDetailView {
    private final CouncilSummaryView council;
    private final List<CouncilMemberView> members;
    private final List<LecturerOption> lecturerCandidates;
    private final List<CouncilRegistrationOption> availableRegistrations;
    private final List<CouncilRegistrationOption> assignedRegistrations;
    public CouncilDetailView(CouncilSummaryView council, List<CouncilMemberView> members,
                             List<LecturerOption> lecturerCandidates,
                             List<CouncilRegistrationOption> availableRegistrations,
                             List<CouncilRegistrationOption> assignedRegistrations) {
        this.council = council; this.members = List.copyOf(members);
        this.lecturerCandidates = List.copyOf(lecturerCandidates);
        this.availableRegistrations = List.copyOf(availableRegistrations);
        this.assignedRegistrations = List.copyOf(assignedRegistrations);
    }
    public CouncilSummaryView getCouncil() { return council; }
    public List<CouncilMemberView> getMembers() { return members; }
    public List<LecturerOption> getLecturerCandidates() { return lecturerCandidates; }
    public List<CouncilRegistrationOption> getAvailableRegistrations() { return availableRegistrations; }
    public List<CouncilRegistrationOption> getAssignedRegistrations() { return assignedRegistrations; }
}
