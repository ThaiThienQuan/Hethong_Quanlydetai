package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.config.PermissionCatalog;
import vn.edu.demo.topicmanagement.dto.CurrentUserView;
import vn.edu.demo.topicmanagement.dto.DashboardNoticeView;
import vn.edu.demo.topicmanagement.dto.DashboardStatView;
import vn.edu.demo.topicmanagement.dto.DashboardTaskView;
import vn.edu.demo.topicmanagement.dto.OpenPeriodView;
import vn.edu.demo.topicmanagement.dto.WorkflowStepView;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.CouncilMember;
import vn.edu.demo.topicmanagement.entity.CouncilRole;
import vn.edu.demo.topicmanagement.entity.CouncilStatus;
import vn.edu.demo.topicmanagement.entity.GroupMember;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.ReviewAssignment;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.entity.TopicStatus;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.entity.UserRole;
import vn.edu.demo.topicmanagement.repository.CouncilMemberRepository;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.CouncilTopicRepository;
import vn.edu.demo.topicmanagement.repository.EmailLogRepository;
import vn.edu.demo.topicmanagement.repository.EvaluationRepository;
import vn.edu.demo.topicmanagement.repository.GroupMemberRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.ReportRepository;
import vn.edu.demo.topicmanagement.repository.ReviewAssignmentRepository;
import vn.edu.demo.topicmanagement.repository.ReviewResultRepository;
import vn.edu.demo.topicmanagement.repository.StudentGroupRepository;
import vn.edu.demo.topicmanagement.repository.StudentRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.TopicRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Nghiệp vụ trang Tổng quan (dashboard) theo vai trò - mục 5.10 của đề bài.
 *
 * <p>Trang này gồm 4 phần cho mọi vai trò:
 * <ol>
 *   <li><b>Thông báo mới</b> - thông báo đã đăng dành cho vai trò của người dùng.</li>
 *   <li><b>Việc cần làm của tôi</b> - bảng theo vai trò (Trưởng khoa / GV / chủ tịch HD / SV).</li>
 *   <li><b>Thanh tiến trình quy trình</b> của đợt hiện hành (6 bước).</li>
 *   <li><b>Số liệu tổng</b> (Trưởng khoa/admin) + bảng đợt đang mở.</li>
 * </ol>
 *
 * <p>Mọi số liệu đều tính từ CSDL theo đúng vai trò của người đang đăng nhập
 * (không tin tham số từ client).
 */
@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Số ngày xem trước để coi là "hạn chót sắp tới". */
    private static final int UPCOMING_DEADLINE_DAYS = 7;

    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final ReviewAssignmentRepository assignmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final ReportRepository reportRepository;
    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final EmailLogRepository emailLogRepository;
    private final AnnouncementService announcementService;

    public DashboardService(UserRepository userRepository,
                            TopicRepository topicRepository,
                            TopicRegistrationRepository registrationRepository,
                            CouncilRepository councilRepository,
                            CouncilMemberRepository councilMemberRepository,
                            CouncilTopicRepository councilTopicRepository,
                            ReviewAssignmentRepository assignmentRepository,
                            EvaluationRepository evaluationRepository,
                            ReviewResultRepository reviewResultRepository,
                            ReportRepository reportRepository,
                            StudentGroupRepository groupRepository,
                            GroupMemberRepository memberRepository,
                            StudentRepository studentRepository,
                            LecturerRepository lecturerRepository,
                            RegistrationPeriodRepository periodRepository,
                            EmailLogRepository emailLogRepository,
                            AnnouncementService announcementService) {
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.registrationRepository = registrationRepository;
        this.councilRepository = councilRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.councilTopicRepository = councilTopicRepository;
        this.assignmentRepository = assignmentRepository;
        this.evaluationRepository = evaluationRepository;
        this.reviewResultRepository = reviewResultRepository;
        this.reportRepository = reportRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.periodRepository = periodRepository;
        this.emailLogRepository = emailLogRepository;
        this.announcementService = announcementService;
    }

    // =====================================================================================
    // Thông báo mới (mọi vai trò)
    // =====================================================================================

    /**
     * Người dùng đang đăng nhập: họ tên + mã vai trò + nhãn vai trò.
     * Dùng để hiển thị "tên + chức vụ" ở lời chào và ở thanh trên cùng.
     */
    @Transactional(readOnly = true)
    public CurrentUserView getCurrentUser(String username) {
        if (username == null) {
            return new CurrentUserView("", "", "");
        }
        return userRepository.findByUsername(username)
                .map(u -> {
                    UserRole role = u.getRole();
                    String label = role == null ? "" : role.getLabel();
                    return new CurrentUserView(
                            u.getFullName() == null || u.getFullName().isBlank() ? username : u.getFullName(),
                            role == null ? "" : role.name(),
                            label);
                })
                .orElseGet(() -> new CurrentUserView(username, "", ""));
    }

    /** 5 thông báo mới nhất mà người đọc được phép xem. */
    @Transactional(readOnly = true)
    public List<DashboardNoticeView> getRecentNotices(String username) {
        List<DashboardNoticeView> views = new ArrayList<>();
        try {
            for (var notice : announcementService.getVisible(username)) {
                views.add(new DashboardNoticeView(
                        notice.getId(),
                        notice.getTitle(),
                        "SCHOOL".equals(notice.getScope()) ? "Nhà trường" : "Khoa",
                        notice.getPublishedAt() == null ? "" : notice.getPublishedAt().format(DATE_TIME)));
                if (views.size() >= 5) break;
            }
        } catch (RuntimeException e) {
            // Thông báo lỗi không được làm hỏng trang Tổng quan.
            return List.of();
        }
        return views;
    }

    // =====================================================================================
    // Việc cần làm của tôi (mục 5.10)
    // =====================================================================================

    /**
     * Bảng "việc cần làm của tôi" theo vai trò của người đang đăng nhập.
     */
    @Transactional(readOnly = true)
    public List<DashboardTaskView> getMyTasks(String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();

        List<DashboardTaskView> tasks = new ArrayList<>();
        if (me.hasRole(PermissionCatalog.ROLE_ADMIN) || me.hasRole(PermissionCatalog.ROLE_DEAN)) {
            tasks.addAll(headTasks());
            return tasks;
        }
        if (me.hasRole(PermissionCatalog.ROLE_LECTURER)) {
            tasks.addAll(lecturerTasks(me));
            return tasks;
        }
        if (me.hasRole(PermissionCatalog.ROLE_STUDENT)) {
            tasks.addAll(studentTasks(me));
            return tasks;
        }
        return tasks;
    }

    /** Trưởng khoa / admin (mục 5.10). */
    private List<DashboardTaskView> headTasks() {
        List<DashboardTaskView> tasks = new ArrayList<>();

        long waitingApproval = topicRepository.countByStatus(TopicStatus.PENDING);
        tasks.add(new DashboardTaskView("topic-approval",
                "Đề tài chờ duyệt",
                "Đề xuất của giảng viên đang chờ Trưởng khoa phê duyệt.",
                waitingApproval, "/head/topics?status=PENDING",
                waitingApproval > 0 ? "warning" : "primary"));

        long missingSupervisor = topicRepository.countApprovedWithoutSupervisor(TopicStatus.APPROVED);
        tasks.add(new DashboardTaskView("topic-supervisor",
                "Đề tài chưa đủ GVHD",
                "Đề tài đã duyệt nhưng chưa có giảng viên hướng dẫn.",
                missingSupervisor, "/head/topics?status=APPROVED",
                missingSupervisor > 0 ? "danger" : "primary"));

        long unassigned = councilTopicRepository.countUnassignedInReadyCouncils(CouncilStatus.READY);
        tasks.add(new DashboardTaskView("topic-unassigned",
                "Đề tài chưa phân công chấm",
                "Đề tài trong hội đồng sẵn sàng chưa có người chấm.",
                unassigned, "/head/councils",
                unassigned > 0 ? "warning" : "primary"));

        long awaitingPublish = reviewResultRepository.countByPublishedFalse();
        tasks.add(new DashboardTaskView("result-publish",
                "Kết quả chờ công bố",
                "Kết quả đã tổng hợp, chờ công bố và gửi email cho sinh viên.",
                awaitingPublish, "/head/results",
                awaitingPublish > 0 ? "warning" : "primary"));

        long invalidCouncils = countInvalidCouncils();
        tasks.add(new DashboardTaskView("council-invalid",
                "Hội đồng chưa hợp lệ",
                "Hội đồng chưa đủ 3–5 giảng viên hoặc thiếu chủ tịch/thư ký.",
                invalidCouncils, "/head/councils",
                invalidCouncils > 0 ? "danger" : "primary"));

        long failedEmails = emailLogRepository.countByStatus("FAILED");
        tasks.add(new DashboardTaskView("email-failed",
                "Email gửi lỗi",
                "Email kết quả gửi cho sinh viên không thành công, cần gửi lại.",
                failedEmails, "/head/emails?status=FAILED",
                failedEmails > 0 ? "danger" : "primary"));

        return tasks;
    }

    /** Hội đồng chưa hợp lệ: thiếu thành viên, thiếu chủ tịch hoặc thiếu thư ký (mục 5.6). */
    private long countInvalidCouncils() {
        long invalid = 0;
        for (Council council : councilRepository.findAll()) {
            if (council.getStatus() == CouncilStatus.FINISHED) continue;
            List<CouncilMember> members = councilMemberRepository.findByCouncilId(council.getId());
            boolean enoughMembers = members.size() >= 3 && members.size() <= 5;
            boolean hasChairman = members.stream().anyMatch(m -> m.getRole() == CouncilRole.CHAIRMAN);
            boolean hasSecretary = members.stream().anyMatch(m -> m.getRole() == CouncilRole.SECRETARY);
            if (!enoughMembers || !hasChairman || !hasSecretary) invalid++;
        }
        return invalid;
    }

    /** Giảng viên (mục 5.10). */
    private List<DashboardTaskView> lecturerTasks(User me) {
        List<DashboardTaskView> tasks = new ArrayList<>();

        List<TopicRegistration> pending = registrationRepository.findPendingForSupervisor(
                RegistrationStatus.PENDING, me.getId());
        tasks.add(new DashboardTaskView("registration-pending",
                "Đăng ký chờ xác nhận",
                "Nhóm sinh viên đăng ký đề tài bạn hướng dẫn đang chờ duyệt.",
                pending.size(), "/lecturer/registrations",
                pending.isEmpty() ? "primary" : "warning"));

        List<ReviewAssignment> toScore = assignmentRepository.findPendingScoringForLecturer(me.getId());
        tasks.add(new DashboardTaskView("review-scoring",
                "Đề tài cần chấm",
                "Đề tài được phân công phản biện mà bạn chưa nộp điểm.",
                toScore.size(), "/lecturer/reviews",
                toScore.isEmpty() ? "primary" : "warning"));

        LocalDateTime now = now();
        List<ReviewAssignment> upcoming = assignmentRepository.findUpcomingDeadlinesForLecturer(
                me.getId(), now, now.plusDays(UPCOMING_DEADLINE_DAYS));
        tasks.add(new DashboardTaskView("review-deadline",
                "Hạn chót sắp tới",
                "Hạn nộp điểm phản biện sắp đến trong " + UPCOMING_DEADLINE_DAYS + " ngày.",
                upcoming.size(), "/lecturer/reviews",
                upcoming.isEmpty() ? "primary" : "danger"));

        // GV là chủ tịch của một hội đồng: đề tài đủ điểm chờ tổng hợp.
        List<Long> chairmanCouncilIds = councilMemberRepository
                .findCouncilsWhereChairman(me.getId(), CouncilRole.CHAIRMAN).stream()
                .map(Council::getId)
                .toList();
        if (!chairmanCouncilIds.isEmpty()) {
            long readyToFinalize = councilTopicRepository.countReadyToFinalize(chairmanCouncilIds);
            tasks.add(new DashboardTaskView("result-finalize",
                    "Đề tài đủ điểm chờ tổng hợp",
                    "Bạn là chủ tịch hội đồng - tổng hợp điểm và kết quả.",
                    readyToFinalize, "/lecturer/council-work",
                    readyToFinalize > 0 ? "warning" : "primary"));
        }

        return tasks;
    }

    /** Sinh viên (mục 5.10). */
    private List<DashboardTaskView> studentTasks(User me) {
        List<DashboardTaskView> tasks = new ArrayList<>();
        List<GroupMember> memberships = memberRepository.findAllByStudentUsername(me.getUsername());

        int pendingRegistration = 0;
        int approvedRegistration = 0;
        int submittedReport = 0;
        int publishedResult = 0;
        for (GroupMember gm : memberships) {
            var reg = registrationRepository.findByGroupIdAndStatusIn(gm.getGroup().getId(),
                    List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED)).orElse(null);
            if (reg != null) {
                if (reg.getStatus() == RegistrationStatus.PENDING) pendingRegistration++;
                else approvedRegistration++;
            }
            if (reportRepository.existsByGroupIdAndStatus(gm.getGroup().getId(), "SUBMITTED")) {
                submittedReport++;
            }
        }
        for (var result : reviewResultRepository.findPublishedForStudent(me.getId())) {
            publishedResult++;
        }

        tasks.add(new DashboardTaskView("group-registration",
                "Trạng thái nhóm/đăng ký",
                pendingRegistration > 0
                        ? "Đăng ký đề tài đang chờ giảng viên hướng dẫn xác nhận."
                        : "Đăng ký đề tài của nhóm bạn đã được chấp thuận.",
                pendingRegistration > 0 ? pendingRegistration : approvedRegistration,
                "/student/registrations",
                pendingRegistration > 0 ? "warning" : "success"));

        tasks.add(new DashboardTaskView("group-report",
                "Báo cáo đã nộp",
                "Số báo cáo đề tài nhóm bạn đã nộp thành công.",
                submittedReport, "/student/reports",
                submittedReport > 0 ? "success" : "primary"));

        tasks.add(new DashboardTaskView("group-result",
                "Kết quả đã công bố",
                "Kết quả phản biện đã được khoa công bố cho nhóm bạn.",
                publishedResult, "/student/results",
                publishedResult > 0 ? "success" : "primary"));

        return tasks;
    }

    // =====================================================================================
    // Thanh tiến trình quy trình của đợt hiện hành (mọi vai trò)
    // =====================================================================================

    /** 6 bước quy trình của đợt hiện hành, đánh dấu bước đã xong / đang diễn ra. */
    @Transactional(readOnly = true)
    public List<WorkflowStepView> getWorkflowSteps() {
        RegistrationPeriod period = getCurrentPeriod();
        List<String> titles = List.of(
                "Tạo đợt",
                "Đề xuất & duyệt đề tài",
                "SV đăng ký",
                "Thực hiện & nộp báo cáo",
                "Phản biện & chấm",
                "Công bố");
        List<String> descriptions = List.of(
                "Trưởng khoa mở đợt và cấp phép đăng ký",
                "Giảng viên đề xuất, khoa duyệt và công bố danh sách",
                "Nhóm sinh viên chọn đề tài đã công bố",
                "Nhóm trưởng nộp báo cáo đề tài theo hạn",
                "Hội đồng chấm điểm và chủ tịch tổng hợp",
                "Khoa công bố kết quả và gửi email cho sinh viên");

        boolean[] done = new boolean[titles.size()];
        if (period != null) {
            Long periodId = period.getId();
            done[0] = period.getStatus() != PeriodStatus.DRAFT;
            done[1] = topicRepository.countApprovedPublishedByPeriod(periodId, TopicStatus.APPROVED) > 0;
            done[2] = registrationRepository.countApprovedByPeriod(periodId, RegistrationStatus.APPROVED) > 0;
            done[3] = reportRepository.countByPeriodAndStatus(periodId, "SUBMITTED") > 0;
            done[4] = evaluationRepository.countByPeriod(periodId) > 0;
            done[5] = reviewResultRepository.countPublishedByPeriod(periodId) > 0;
        }

        int current = -1;
        for (int i = 0; i < done.length; i++) {
            if (!done[i]) {
                current = i;
                break;
            }
        }

        List<WorkflowStepView> steps = new ArrayList<>();
        for (int i = 0; i < titles.size(); i++) {
            String state;
            if (done[i]) {
                state = "done";
            } else if (i == current) {
                state = "current";
            } else {
                state = "todo";
            }
            steps.add(new WorkflowStepView(i + 1, titles.get(i), descriptions.get(i), state));
        }
        return steps;
    }

    /** Đợt hiện hành: đợt đang công bố gần nhất, nếu không có thì đợt mới nhất. */
    @Transactional(readOnly = true)
    public RegistrationPeriod getCurrentPeriod() {
        List<RegistrationPeriod> published = periodRepository.findByStatusOrderByIdDesc(PeriodStatus.PUBLISHED);
        if (!published.isEmpty()) return published.get(0);
        List<RegistrationPeriod> all = periodRepository.findAllByOrderByIdDesc();
        return all.isEmpty() ? null : all.get(0);
    }

    // =====================================================================================
    // Số liệu tổng (Trưởng khoa / admin) + bảng đợt đang mở
    // =====================================================================================

    /** Các thẻ số liệu tổng cho Trưởng khoa/admin (mục 5.10). */
    @Transactional(readOnly = true)
    public List<DashboardStatView> getHeadStats() {
        List<DashboardStatView> stats = new ArrayList<>();
        long users = userRepository.count();
        long students = studentRepository.count();
        long lecturers = lecturerRepository.count();
        long pendingTopics = topicRepository.countByStatus(TopicStatus.PENDING);
        long approvedTopics = topicRepository.countByStatus(TopicStatus.APPROVED);
        long groups = groupRepository.count();
        long reports = reportRepository.countByStatus("SUBMITTED");
        long publishedResults = reviewResultRepository.countByPublishedTrue();
        long openPeriods = periodRepository.findByStatusOrderByIdDesc(PeriodStatus.PUBLISHED).size();

        stats.add(new DashboardStatView("Người dùng", String.valueOf(users),
                students + " sinh viên · " + lecturers + " giảng viên", "primary"));
        stats.add(new DashboardStatView("Đề tài", String.valueOf(approvedTopics),
                pendingTopics + " đề tài đang chờ duyệt", pendingTopics > 0 ? "warning" : "success"));
        stats.add(new DashboardStatView("Nhóm sinh viên", String.valueOf(groups),
                "Tổng số nhóm đã lập trong các đợt", "primary"));
        stats.add(new DashboardStatView("Báo cáo", String.valueOf(reports),
                "Báo cáo đề tài đã nộp", "primary"));
        stats.add(new DashboardStatView("Kết quả đã công bố", String.valueOf(publishedResults),
                "Kết quả sinh viên đã được thông báo", "success"));
        stats.add(new DashboardStatView("Đợt đang mở", String.valueOf(openPeriods),
                "Đợt đăng ký đang công bố", openPeriods > 0 ? "info" : "primary"));
        return stats;
    }

    /** Bảng "đợt đang mở" - các đợt đang công bố, mới nhất lên đầu. */
    @Transactional(readOnly = true)
    public List<OpenPeriodView> getOpenPeriods() {
        List<OpenPeriodView> views = new ArrayList<>();
        for (RegistrationPeriod period : periodRepository.findByStatusOrderByIdDesc(PeriodStatus.PUBLISHED)) {
            views.add(new OpenPeriodView(
                    period.getId(),
                    period.getName(),
                    period.getPeriodType() == null ? "" : period.getPeriodType().getDisplayName(),
                    phaseLabel(period),
                    window(period.getLecturerStartAt(), period.getLecturerEndAt()),
                    window(period.getStudentStartAt(), period.getStudentEndAt()),
                    period.getReviewDeadline() == null ? null : period.getReviewDeadline().format(DATE_TIME)));
        }
        return views;
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private String phaseLabel(RegistrationPeriod period) {
        return switch (period.currentPhase()) {
            case "CHUA_MO" -> "Chưa mở";
            case "GIAO_VIEN_DANG_KY" -> "Giảng viên đăng ký";
            case "SINH_VIEN_DANG_KY" -> "Sinh viên đăng ký";
            default -> "Đã đóng";
        };
    }

    private String window(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return "";
        return from.format(DATE) + " → " + to.format(DATE);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
