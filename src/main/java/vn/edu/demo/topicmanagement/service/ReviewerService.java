package vn.edu.demo.topicmanagement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.LecturerOption;
import vn.edu.demo.topicmanagement.dto.ReviewAssigneeView;
import vn.edu.demo.topicmanagement.dto.ReviewScoreForm;
import vn.edu.demo.topicmanagement.dto.ReviewerTaskView;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.CouncilMember;
import vn.edu.demo.topicmanagement.entity.CouncilStatus;
import vn.edu.demo.topicmanagement.entity.CouncilTopic;
import vn.edu.demo.topicmanagement.entity.Lecturer;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.Report;
import vn.edu.demo.topicmanagement.entity.ReviewAssignment;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.entity.TopicSupervisor;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.CouncilMemberRepository;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.CouncilTopicRepository;
import vn.edu.demo.topicmanagement.repository.EvaluationRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.ReportRepository;
import vn.edu.demo.topicmanagement.repository.ReviewAssignmentRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.TopicSupervisorRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Nghiệp vụ cho GV được phân công phản biện: xem nhiệm vụ, nộp/sửa điểm & nhận xét,
 * và cho Trưởng khoa/Admin phân công GVPB/GV chấm trong một hội đồng (mục 5.7).
 *
 * <p>Quy tắc phân công (A3):
 * <ul>
 *   <li>Hội đồng phải ở trạng thái {@link CouncilStatus#READY}.</li>
 *   <li>GV được phân công phải là thành viên hội đồng.</li>
 *   <li>Hội đồng và đề tài phải cùng đợt.</li>
 *   <li>Đề tài đã có báo cáo được nộp.</li>
 *   <li>GV được phân công KHÔNG được là GVHD của đề tài (kể cả GVHD chính và đồng hướng dẫn).</li>
 *   <li>Không trùng phân công.</li>
 *   <li>Không xóa phân công đã có điểm.</li>
 * </ul>
 */
@Service
public class ReviewerService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ReviewAssignmentRepository assignmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final UserRepository userRepository;
    private final LecturerRepository lecturerRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final ReportRepository reportRepository;
    private final StudentReportService reportService;
    private final AuditLogService auditLogService;

    public ReviewerService(ReviewAssignmentRepository assignmentRepository,
                           EvaluationRepository evaluationRepository,
                           TopicRegistrationRepository registrationRepository,
                           TopicSupervisorRepository supervisorRepository,
                           UserRepository userRepository,
                           LecturerRepository lecturerRepository,
                           RegistrationPeriodRepository periodRepository,
                           CouncilTopicRepository councilTopicRepository,
                           CouncilRepository councilRepository,
                           CouncilMemberRepository councilMemberRepository,
                           ReportRepository reportRepository,
                           StudentReportService reportService,
                           AuditLogService auditLogService) {
        this.assignmentRepository = assignmentRepository;
        this.evaluationRepository = evaluationRepository;
        this.registrationRepository = registrationRepository;
        this.supervisorRepository = supervisorRepository;
        this.userRepository = userRepository;
        this.lecturerRepository = lecturerRepository;
        this.periodRepository = periodRepository;
        this.councilTopicRepository = councilTopicRepository;
        this.councilRepository = councilRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.reportRepository = reportRepository;
        this.reportService = reportService;
        this.auditLogService = auditLogService;
    }

    // =====================================================================================
    // Phần GV: nhận nhiệm vụ phản biện và nộp điểm
    // =====================================================================================

    /** Danh sách đề tài được phân công chấm cho giảng viên hiện tại (mục 5.7 - màn hình GV). */
    @Transactional(readOnly = true)
    public List<ReviewerTaskView> getMyTasks(String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();
        List<ReviewAssignment> assignments = assignmentRepository.findByLecturerWithDetails(me.getId());
        List<ReviewerTaskView> views = new ArrayList<>();
        for (ReviewAssignment a : assignments) {
            views.add(toTaskView(a));
        }
        return views;
    }

    private ReviewerTaskView toTaskView(ReviewAssignment a) {
        Topic topic = a.getTopic();
        Council council = a.getCouncil();
        LocalDateTime deadline = topic.getPeriod().getReviewDeadline();
        boolean inDeadline = deadline != null && !now().isAfter(deadline);
        BigDecimal ownScore = evaluationRepository.findByAssignmentId(a.getId()).map(e -> e.getScore()).orElse(null);
        String ownComment = evaluationRepository.findByAssignmentId(a.getId()).map(e -> e.getComment()).orElse(null);
        LocalDateTime submittedAt = evaluationRepository.findByAssignmentId(a.getId()).map(e -> e.getEvaluatedAt()).orElse(null);
        // GVPB không thể chấm đề tài mình hướng dẫn.
        boolean supervisor = supervisorRepository
                .existsByTopicIdAndLecturerId(topic.getId(), a.getLecturer().getId());
        // Đăng ký được chấp thuận của đề tài - cần id để tải báo cáo.
        TopicRegistration reg = registrationRepository
                .findByTopicIdAndStatusIn(topic.getId(), List.of(RegistrationStatus.APPROVED))
                .stream().findFirst().orElse(null);
        boolean downloadable = reportService.isDownloadAvailableByTopic(topic.getId());

        // GV có thể nộp/sửa khi: hội đồng READY, còn hạn, không phải GVHD, hội đồng cùng đợt.
        Council readyCouncil = council;
        boolean canSubmit = !supervisor
                && readyCouncil != null
                && readyCouncil.getStatus() == CouncilStatus.READY
                && inDeadline;

        return new ReviewerTaskView(
                a.getId(),
                reg != null ? reg.getId() : null,
                reg != null ? reg.getGroup().getGroupName() : "",
                topic.getTopicName(),
                topic.getTopicCode(),
                deadline,
                ownScore,
                ownComment,
                submittedAt,
                canSubmit,
                downloadable && reg != null,
                supervisor,
                !inDeadline);
    }

    /** GV nộp điểm - nhận từ form (đã validate cơ bản). */
    @Transactional
    public void submitReview(Long assignmentId, String username, ReviewScoreForm form) {
        ReviewAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phân công phản biện."));
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null || !assignment.getLecturer().getUser().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Đây không phải phân công của bạn.");
        }
        Topic topic = assignment.getTopic();
        Council council = assignment.getCouncil();
        if (council.getStatus() != CouncilStatus.READY) {
            throw new IllegalStateException("Hội đồng chưa sẵn sàng chấm.");
        }
        if (supervisorRepository.existsByTopicIdAndLecturerId(topic.getId(), assignment.getLecturer().getId())) {
            throw new IllegalStateException("GVHD không được chấm đề tài mình hướng dẫn.");
        }
        LocalDateTime deadline = topic.getPeriod().getReviewDeadline();
        boolean overDeadline = deadline == null || LocalDateTime.now(ZONE).isAfter(deadline);
        boolean adminOverride = me.hasRole("ADMIN");
        if (overDeadline && !adminOverride) {
            throw new IllegalStateException("Đã quá hạn nộp điểm phản biện.");
        }
        BigDecimal score = form.getScore();
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng 0 đến 10.");
        }
        String comment = form.getComment() == null ? null : form.getComment().trim();
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nhận xét.");
        }
        vn.edu.demo.topicmanagement.entity.Evaluation eval = evaluationRepository.findByAssignmentId(assignmentId)
                .orElseGet(() -> {
                    vn.edu.demo.topicmanagement.entity.Evaluation e = new vn.edu.demo.topicmanagement.entity.Evaluation();
                    e.setAssignment(assignment);
                    return e;
                });
        eval.setScore(score.setScale(2, RoundingMode.HALF_UP));
        eval.setComment(comment);
        eval.setStatus("SUBMITTED");
        evaluationRepository.save(eval);
        assignment.setStatus("COMPLETED");
        assignmentRepository.save(assignment);

        String detail = String.format("Điểm=%s, Đề tài id=%d", score, topic.getId());
        if (overDeadline && adminOverride) {
            detail += " (admin ghi đè quá hạn: " + (form.getOverrideReason() == null ? "không lý do" : form.getOverrideReason()) + ")";
        }
        auditLogService.record(username,
                overDeadline && adminOverride ? "EVALUATION_SUBMIT_OVERDUE" : "EVALUATION_SUBMIT",
                "ReviewAssignment", assignmentId, detail);
    }

    // =====================================================================================
    // Phần Trưởng khoa / Admin: phân công GVPB trong một đề tài của hội đồng
    // =====================================================================================

    /**
     * Phân công giảng viên chấm một đề tài của hội đồng. Áp dụng toàn bộ quy tắc mục 5.7 (A3).
     */
    @Transactional
    public void assign(Long councilTopicId, Long lecturerId, String username) {
        CouncilTopic councilTopic = councilTopicRepository.findById(councilTopicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài trong hội đồng."));
        Council council = councilTopic.getCouncil();
        if (council.getStatus() != CouncilStatus.READY) {
            throw new IllegalStateException("Chỉ phân công khi hội đồng đã ở trạng thái READY.");
        }
        Lecturer lecturer = lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên."));

        // (a) GV phải là thành viên hội đồng
        boolean isMember = councilMemberRepository.existsByCouncilIdAndLecturerId(council.getId(), lecturer.getId());
        if (!isMember) {
            throw new IllegalArgumentException("Giảng viên không thuộc hội đồng này; chỉ có thể chọn GV trong hội đồng.");
        }
        Topic topic = councilTopic.getTopic();

        // (b) Hội đồng và đề tài phải cùng đợt
        if (council.getPeriod() == null || topic.getPeriod() == null
                || !council.getPeriod().getId().equals(topic.getPeriod().getId())) {
            throw new IllegalStateException("Hội đồng và đề tài không cùng đợt.");
        }
        // (c) Đề tài đã có báo cáo
        List<Report> submitted = reportRepository.findByTopicIdAndStatus(topic.getId(), "SUBMITTED");
        if (submitted.isEmpty()) {
            throw new IllegalStateException("Đề tài chưa có báo cáo được nộp; không thể phân công chấm.");
        }
        // (d) Người chấm không được là GVHD (kể cả đồng hướng dẫn) hay người tạo đề tài
        if (supervisorRepository.existsByTopicIdAndLecturerId(topic.getId(), lecturer.getId())) {
            throw new IllegalStateException("GVHD (kể cả đồng hướng dẫn) không được chấm đề tài mình hướng dẫn.");
        }
        if (topic.getCreatedByLecturer() != null
                && topic.getCreatedByLecturer().getId().equals(lecturer.getId())) {
            throw new IllegalStateException("Người đề xuất đề tài không được chấm đề tài đó.");
        }
        // (e) Không trùng phân công
        boolean exists = assignmentRepository.existsByCouncilIdAndTopicIdAndLecturerId(
                council.getId(), topic.getId(), lecturer.getId());
        if (exists) {
            throw new IllegalStateException("Giảng viên này đã được phân công chấm đề tài này.");
        }

        User me = userRepository.findByUsername(username).orElse(null);
        ReviewAssignment ra = new ReviewAssignment(council, topic, lecturer, me);
        ra.setStatus("ASSIGNED");
        assignmentRepository.save(ra);
        auditLogService.record(username, AuditLogService.ASSIGNMENT_CREATE,
                "CouncilTopic", councilTopicId,
                "Phân công GV id=" + lecturer.getId() + " (" + lecturer.getUser().getFullName() + ") chấm đề tài id=" + topic.getId());
    }

    /** Gỡ phân công - chỉ khi phân công chưa có điểm. */
    @Transactional
    public void removeAssignment(Long assignmentId, String username) {
        ReviewAssignment ra = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công."));
        if (evaluationRepository.existsByAssignmentId(assignmentId)) {
            throw new IllegalStateException("Phân công đã có điểm; không thể gỡ.");
        }
        assignmentRepository.delete(ra);
        auditLogService.record(username, "ASSIGNMENT_REMOVE",
                "ReviewAssignment", assignmentId, "Gỡ phân công chưa nộp điểm.");
    }

    /** Danh sách phân công của một đề tài trong hội đồng (cho trang phân công). */
    @Transactional(readOnly = true)
    public List<ReviewAssigneeView> listAssignments(Long councilTopicId) {
        CouncilTopic councilTopic = councilTopicRepository.findById(councilTopicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài trong hội đồng."));
        Council council = councilTopic.getCouncil();
        List<ReviewAssignment> list = assignmentRepository
                .findByCouncilAndTopic(council.getId(), councilTopic.getTopic().getId());
        List<ReviewAssigneeView> rows = new ArrayList<>();
        for (ReviewAssignment a : list) {
            String fullName = a.getLecturer() != null && a.getLecturer().getUser() != null
                    ? a.getLecturer().getUser().getFullName() : "";
            String code = a.getLecturer() != null ? a.getLecturer().getLecturerCode() : "";
            boolean submitted = evaluationRepository.existsByAssignmentId(a.getId())
                    && "COMPLETED".equalsIgnoreCase(a.getStatus());
            boolean supervisor = supervisorRepository
                    .existsByTopicIdAndLecturerId(councilTopic.getTopic().getId(), a.getLecturer().getId());
            rows.add(new ReviewAssigneeView(a.getId(), fullName, code, submitted, supervisor));
        }
        return rows;
    }

    /**
     * Danh sách ứng viên có thể được phân công chấm một đề tài của hội đồng:
     * thành viên hội đồng nhưng không phải GVHD/không tạo đề tài/không trùng phân công.
     */
    @Transactional(readOnly = true)
    public List<LecturerOption> listReviewerCandidates(Long councilTopicId) {
        CouncilTopic councilTopic = councilTopicRepository.findById(councilTopicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài trong hội đồng."));
        Council council = councilTopic.getCouncil();
        Topic topic = councilTopic.getTopic();
        // Lấy member của hội đồng kèm lecturer
        List<CouncilMember> members = councilMemberRepository.findByCouncilIdWithLecturer(council.getId());
        List<Long> existingLecturerIds = assignmentRepository
                .findByCouncilAndTopic(council.getId(), topic.getId())
                .stream().map(a -> a.getLecturer().getId()).toList();
        List<LecturerOption> candidates = new ArrayList<>();
        for (CouncilMember m : members) {
            Lecturer l = m.getLecturer();
            if (existingLecturerIds.contains(l.getId())) continue;
            if (supervisorRepository.existsByTopicIdAndLecturerId(topic.getId(), l.getId())) continue;
            if (topic.getCreatedByLecturer() != null
                    && topic.getCreatedByLecturer().getId().equals(l.getId())) continue;
            String name = l.getUser() != null ? l.getUser().getFullName() : "";
            candidates.add(new LecturerOption(l.getId(), name, l.getLecturerCode()));
        }
        return candidates;
    }

    /**
     * Backward-compatible entrypoint - được giữ để trang cũ {@code /head/reviewers}
     * (nếu còn tham chiếu) vẫn hoạt động. Phiên bản đầy đủ là {@link #assign(Long, Long, String)}.
     */
    @Transactional
    public void assign(Long registrationId, Long lecturerId) {
        // Tìm councilTopic tương ứng (lấy council READY đầu tiên có cùng đề tài)
        TopicRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài."));
        CouncilTopic councilTopic = councilTopicRepository.findByTopicId(reg.getTopic().getId()).orElse(null);
        if (councilTopic == null) {
            throw new IllegalStateException("Đề tài chưa được xếp vào hội đồng nào.");
        }
        assign(councilTopic.getId(), lecturerId, "system");
    }

    @Transactional(readOnly = true)
    public List<LecturerOption> listActiveReviewerCandidates() {
        return lecturerRepository.findAllActiveWithDepartment().stream()
                .map(l -> new LecturerOption(l.getId(), l.getUser().getFullName(), l.getLecturerCode())).toList();
    }

    /** GVHD xem danh sách đề tài của hội đồng (trang cũ - giữ lại cho tương thích). */
    @Transactional(readOnly = true)
    public List<vn.edu.demo.topicmanagement.dto.ReviewAssignmentView> getAssignmentsForHead() {
        // Bản đơn giản: trả các đề tài APPROVED + có hạn review_deadline
        List<TopicRegistration> regs = registrationRepository.findByStatusWithGroup(
                vn.edu.demo.topicmanagement.entity.RegistrationStatus.APPROVED);
        List<vn.edu.demo.topicmanagement.dto.ReviewAssignmentView> rows = new java.util.ArrayList<>();
        List<LecturerOption> candidates = listActiveReviewerCandidates();
        for (TopicRegistration r : regs) {
            var period = r.getTopic().getPeriod();
            if (period == null || period.getReviewDeadline() == null) continue;
            List<ReviewAssignment> assignments = assignmentRepository.findByTopicId(r.getTopic().getId());
            List<vn.edu.demo.topicmanagement.dto.ReviewAssigneeView> assignees = new java.util.ArrayList<>();
            for (ReviewAssignment a : assignments) {
                Lecturer lec = a.getLecturer();
                String name = "";
                if (lec != null && lec.getUser() != null) {
                    name = lec.getUser().getFullName() != null ? lec.getUser().getFullName() : "";
                }
                boolean submitted = "COMPLETED".equalsIgnoreCase(a.getStatus()) || "LOCKED".equalsIgnoreCase(a.getStatus());
                assignees.add(new vn.edu.demo.topicmanagement.dto.ReviewAssigneeView(a.getId(), name, "", submitted, false));
            }
            String periodTypeName = period.getPeriodType() != null ? period.getPeriodType().getDisplayName() : "";
            rows.add(new vn.edu.demo.topicmanagement.dto.ReviewAssignmentView(
                    r.getId(),
                    r.getGroup().getGroupName(),
                    r.getTopic().getTopicName(),
                    periodTypeName,
                    period.getReviewDeadline(),
                    assignees,
                    candidates));
        }
        return rows;
    }

    /** Xem các phân công của một đề tài trong hội đồng (gói gọn dùng nội bộ). */
    @Transactional(readOnly = true)
    public List<ReviewAssignment> findAssignmentsForTopicInCouncil(Long councilId, Long topicId) {
        return assignmentRepository.findByCouncilAndTopic(councilId, topicId);
    }

    private LocalDateTime now() { return LocalDateTime.now(ZONE); }
}
