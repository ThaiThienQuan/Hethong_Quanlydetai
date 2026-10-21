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
import vn.edu.demo.topicmanagement.config.AppProperties;
import vn.edu.demo.topicmanagement.dto.CouncilResultForm;
import vn.edu.demo.topicmanagement.dto.CouncilWorkView;
import vn.edu.demo.topicmanagement.dto.HeadResultView;
import vn.edu.demo.topicmanagement.dto.ReviewScoreForm;
import vn.edu.demo.topicmanagement.dto.StudentResultView;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.CouncilMember;
import vn.edu.demo.topicmanagement.entity.CouncilRole;
import vn.edu.demo.topicmanagement.entity.CouncilStatus;
import vn.edu.demo.topicmanagement.entity.CouncilTopic;
import vn.edu.demo.topicmanagement.entity.Evaluation;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.ReviewAssignment;
import vn.edu.demo.topicmanagement.entity.ReviewResult;
import vn.edu.demo.topicmanagement.entity.StudentGroup;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.entity.TopicSupervisor;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.CouncilMemberRepository;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.CouncilTopicRepository;
import vn.edu.demo.topicmanagement.repository.EvaluationRepository;
import vn.edu.demo.topicmanagement.repository.ReviewAssignmentRepository;
import vn.edu.demo.topicmanagement.repository.ReviewResultRepository;
import vn.edu.demo.topicmanagement.repository.StudentGroupRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.TopicSupervisorRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Nghiệp vụ GV chấm điểm / chủ tịch tổng hợp / Trưởng khoa công bố kết quả (mục 5.8).
 *
 * <p>Quy tắc chính:
 * <ul>
 *   <li>Tổng hợp chỉ khi <b>mọi</b> người chấm được phân công đã nộp điểm.</li>
 *   <li>Điểm cuối = trung bình cộng các điểm thành phần (làm tròn 2 chữ số).</li>
 *   <li>Kết quả ĐẠT/KHÔNG ĐẠT tính tự động theo {@code app.passing-score} (A4).</li>
 *   <li>Chưa công bố thì tổng hợp lại được; đã công bố thì <b>khóa</b>.</li>
 *   <li>Công bố do Trưởng khoa; sau đó gửi email riêng cho từng SV trong nhóm.</li>
 * </ul>
 */
@Service
public class CouncilScoringService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Ngưỡng ĐẠT khi người dùng không tự nhập (rubric: kết quả do hệ thống quyết định). */
    public static final String PASSED = "PASSED";
    public static final String FAILED = "FAILED";

    private final CouncilTopicRepository councilTopicRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final EvaluationRepository evaluationRepository;
    private final ReviewAssignmentRepository assignmentRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final CouncilRepository councilRepository;
    private final UserRepository userRepository;
    private final StudentGroupRepository groupRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final StudentReportService reportService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final AppProperties props;

    public CouncilScoringService(CouncilTopicRepository councilTopicRepository,
                                 CouncilMemberRepository councilMemberRepository,
                                 TopicSupervisorRepository supervisorRepository,
                                 EvaluationRepository evaluationRepository,
                                 ReviewAssignmentRepository assignmentRepository,
                                 ReviewResultRepository reviewResultRepository,
                                 CouncilRepository councilRepository,
                                 UserRepository userRepository,
                                 StudentGroupRepository groupRepository,
                                 TopicRegistrationRepository registrationRepository,
                                 StudentReportService reportService,
                                 EmailService emailService,
                                 AuditLogService auditLogService,
                                 AppProperties props) {
        this.councilTopicRepository = councilTopicRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.supervisorRepository = supervisorRepository;
        this.evaluationRepository = evaluationRepository;
        this.assignmentRepository = assignmentRepository;
        this.reviewResultRepository = reviewResultRepository;
        this.councilRepository = councilRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.registrationRepository = registrationRepository;
        this.reportService = reportService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.props = props;
    }

    // =====================================================================================
    // GV chấm điểm
    // =====================================================================================

    /** Các phân công chấm của giảng viên đăng nhập hiện tại. */
    @Transactional(readOnly = true)
    public List<CouncilWorkView> getMyWork(String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();
        List<CouncilWorkView> list = new ArrayList<>();
        for (ReviewAssignment a : assignmentRepository.findByLecturerWithDetails(me.getId())) {
            list.add(toWorkView(a));
        }
        return list;
    }

    /**
     * Lưu/cập nhật điểm cho một phân công chấm (mục 5.7).
     *
     * @param assignmentId id của {@code review_assignments}, KHÔNG phải {@code council_topics.id}
     */
    @Transactional
    public void submitScore(Long assignmentId, String username, ReviewScoreForm form) {
        ReviewAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phân công chấm."));
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (!assignment.getLecturer().getUser().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Phân công này không thuộc về bạn.");
        }
        Topic topic = assignment.getTopic();
        Council council = assignment.getCouncil();
        if (council.getStatus() != CouncilStatus.READY) {
            throw new IllegalStateException("Hội đồng chưa sẵn sàng chấm.");
        }
        if (isSupervisor(topic, assignment.getLecturer().getId())) {
            throw new IllegalStateException("GVHD không được chấm đề tài mình hướng dẫn.");
        }
        // Hạn chót GVPB của đợt (A6): quá hạn thì từ chối, admin được phép ghi đè (có nhật ký).
        LocalDateTime deadline = topic.getPeriod() == null ? null : topic.getPeriod().getReviewDeadline();
        boolean overDeadline = deadline == null || LocalDateTime.now(ZONE).isAfter(deadline);
        if (overDeadline && !me.hasRole("ADMIN")) {
            throw new IllegalStateException("Đã quá hạn nộp điểm phản biện.");
        }

        BigDecimal value = form.getScore();
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng 0 đến 10.");
        }
        String comment = form.getComment() == null ? null : form.getComment().trim();
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nhận xét (bắt buộc).");
        }

        Evaluation eval = evaluationRepository.findByAssignmentId(assignmentId).orElseGet(() -> {
            Evaluation e = new Evaluation();
            e.setAssignment(assignment);
            return e;
        });
        eval.setScore(value.setScale(2, RoundingMode.HALF_UP));
        eval.setComment(comment);
        eval.setStatus("SUBMITTED");
        evaluationRepository.save(eval);
        assignment.setStatus("COMPLETED");
        assignmentRepository.save(assignment);

        String detail = "Điểm=" + value + ", đề tài id=" + topic.getId();
        if (overDeadline && me.hasRole("ADMIN")) {
            detail += " (admin ghi đè quá hạn: "
                    + (form.getOverrideReason() == null || form.getOverrideReason().isBlank()
                        ? "không nêu lý do" : form.getOverrideReason().trim()) + ")";
        }
        auditLogService.record(username,
                overDeadline && me.hasRole("ADMIN") ? "EVALUATION_SUBMIT_OVERDUE" : AuditLogService.EVALUATION_SUBMIT,
                "ReviewAssignment", assignmentId, detail);
    }

    // =====================================================================================
    // Chủ tịch tổng hợp (mục 5.8)
    // =====================================================================================

    /** Bảng điểm từng thành viên + nhận xét, để chủ tịch xem trước khi chốt. */
    @Transactional(readOnly = true)
    public List<String> getScoreBoard(Long councilTopicId) {
        CouncilTopic councilTopic = councilTopicRepository.findById(councilTopicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài trong hội đồng."));
        Council council = councilTopic.getCouncil();
        Topic topic = councilTopic.getTopic();
        List<Evaluation> evals = evaluationRepository.findByCouncilAndTopic(council.getId(), topic.getId());
        List<String> rows = new ArrayList<>();
        for (Evaluation ev : evals) {
            rows.add(ev.getAssignment().getLecturer().getUser().getFullName()
                    + " — " + ev.getScore() + " — " + (ev.getComment() == null ? "" : ev.getComment()));
        }
        return rows;
    }

    /**
     * Chủ tịch (hoặc admin) tổng hợp điểm thành kết quả.
     * Chỉ khi mọi người chấm đã nộp; đã công bố thì khóa.
     */
    @Transactional
    public void finalizeResult(Long councilTopicId, String username, CouncilResultForm form) {
        CouncilTopic councilTopic = councilTopicRepository.findById(councilTopicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài trong hội đồng."));
        Council council = councilTopic.getCouncil();
        if (council.getStatus() == CouncilStatus.DRAFT) {
            throw new IllegalStateException("Hội đồng chưa sẵn sàng.");
        }
        if (council.getStatus() == CouncilStatus.FINISHED) {
            throw new IllegalStateException("Hội đồng đã hoàn tất.");
        }
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (!me.hasRole("ADMIN")) {
            requireChair(council.getId(), username);
        }
        Topic topic = councilTopic.getTopic();

        List<ReviewAssignment> assignments =
                assignmentRepository.findByCouncilAndTopicWithEvaluation(council.getId(), topic.getId());
        if (assignments.isEmpty()) {
            throw new IllegalStateException("Chưa phân công giảng viên chấm đề tài này.");
        }
        List<Evaluation> evals = evaluationRepository.findByCouncilAndTopic(council.getId(), topic.getId());
        if (evals.size() != assignments.size()) {
            throw new IllegalStateException("Chưa đủ điểm: còn "
                    + (assignments.size() - evals.size()) + " người chấm chưa nộp.");
        }
        for (Evaluation ev : evals) {
            if (isSupervisor(topic, ev.getAssignment().getLecturer().getId())) {
                throw new IllegalStateException("Có GVHD chấm đề tài của mình; sửa trước khi tổng hợp.");
            }
        }

        StudentGroup group = groupRepository.findByTopicId(topic.getId()).orElse(null);
        if (group == null) {
            throw new IllegalStateException("Chưa tìm thấy nhóm đang thực hiện đề tài này.");
        }

        ReviewResult existing = reviewResultRepository.findByTopicId(topic.getId()).orElse(null);
        if (existing != null && existing.isPublished()) {
            throw new IllegalStateException("Kết quả đã công bố thì không thể tổng hợp lại.");
        }

        // Điểm cuối = trung bình cộng các điểm thành phần (A4).
        BigDecimal total = BigDecimal.ZERO;
        for (Evaluation ev : evals) total = total.add(ev.getScore());
        BigDecimal avg = total.divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP);
        // Kết quả ĐẠT/KHÔNG ĐẠT do hệ thống quyết định theo ngưỡng cấu hình (A4).
        String outcome = avg.compareTo(props.getPassingScore()) >= 0 ? PASSED : FAILED;

        ReviewResult result = existing == null ? new ReviewResult() : existing;
        result.setTopic(topic);
        result.setCouncil(council);
        result.setGroup(group);
        result.setFinalScore(avg);
        result.setResult(outcome);
        String comment = form.getCouncilComment() == null ? null : form.getCouncilComment().trim();
        result.setSummaryComment(comment == null || comment.isBlank()
                ? "Kết quả " + ("PASSED".equals(outcome) ? "ĐẠT" : "KHÔNG ĐẠT") : comment);
        result.setCalculatedBy(me);
        reviewResultRepository.save(result);

        auditLogService.record(username, AuditLogService.RESULT_CALCULATE, "ReviewResult",
                result.getId(), "Điểm cuối=" + avg + ", kết quả=" + outcome
                        + " (" + assignments.size() + " người chấm)");
    }

    // =====================================================================================
    // Trưởng khoa công bố (mục 5.8)
    // =====================================================================================

    /** Trưởng khoa (hoặc admin) công bố một kết quả. Sau đó gửi email cho các SV trong nhóm. */
    @Transactional
    public void publishResultByHead(Long councilTopicId, String username) {
        User publisher = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (!publisher.hasRole("TRUONG_KHOA") && !publisher.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Trưởng khoa được công bố kết quả.");
        }
        CouncilTopic councilTopic = councilTopicRepository.findById(councilTopicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài trong hội đồng."));
        Topic topic = councilTopic.getTopic();
        ReviewResult result = reviewResultRepository.findByTopicId(topic.getId())
                .orElseThrow(() -> new IllegalStateException("Chủ tịch cần tổng hợp kết quả trước khi công bố."));
        if (result.isPublished()) throw new IllegalStateException("Kết quả này đã được công bố.");

        result.setPublished(true);
        result.setPublishedAt(now());
        result.setPublishedBy(publisher);
        reviewResultRepository.save(result);

        // Gửi email riêng cho từng SV trong nhóm (kể cả thành viên không phải nhóm trưởng).
        int sent = emailService.sendResultToGroup(result);

        auditLogService.record(username, AuditLogService.RESULT_PUBLISH, "ReviewResult",
                result.getId(), "Công bố kết quả đề tài id=" + topic.getId()
                        + ", điểm=" + result.getFinalScore() + ", " + sent + " SV đã gửi email");

        markCouncilFinishedIfAllPublished(councilTopic.getCouncil());
    }

    /** Công bố toàn bộ kết quả của một hội đồng (một lần bấm cho cả hội đồng). */
    @Transactional
    public int publishWholeCouncil(Long councilId, String username) {
        User publisher = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (!publisher.hasRole("TRUONG_KHOA") && !publisher.hasRole("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Trưởng khoa được công bố kết quả.");
        }
        Council council = councilRepository.findById(councilId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội đồng."));
        int published = 0;
        for (CouncilTopic ct : councilTopicRepository.findByCouncilIdWithTopic(councilId)) {
            ReviewResult rr = reviewResultRepository.findByTopicId(ct.getTopic().getId()).orElse(null);
            if (rr == null) continue;
            if (rr.isPublished()) continue;
            rr.setPublished(true);
            rr.setPublishedAt(now());
            rr.setPublishedBy(publisher);
            reviewResultRepository.save(rr);
            emailService.sendResultToGroup(rr);
            auditLogService.record(username, AuditLogService.RESULT_PUBLISH, "ReviewResult",
                    rr.getId(), "Công bố theo hội đồng id=" + councilId);
            published++;
        }
        markCouncilFinishedIfAllPublished(council);
        if (published == 0) {
            throw new IllegalStateException("Không có kết quả nào đang chờ công bố trong hội đồng này.");
        }
        return published;
    }

    /** Kết quả chờ công bố - cho Trưởng khoa (kèm số SV đã nhận email). */
    @Transactional(readOnly = true)
    public List<HeadResultView> getResultsAwaitingPublication() {
        List<HeadResultView> pending = new ArrayList<>();
        for (ReviewResult result : reviewResultRepository.findUnpublishedWithRefs()) {
            CouncilTopic ct = councilTopicRepository.findByTopicId(result.getTopic().getId()).orElse(null);
            if (ct == null) continue;
            Council council = ct.getCouncil();
            if (council == null) continue;
            if (council.getStatus() == CouncilStatus.DRAFT
                    || council.getStatus() == CouncilStatus.FINISHED) continue;
            StudentGroup group = result.getGroup();
            if (group == null) continue;
            pending.add(new HeadResultView(ct.getId(), result.getId(), council.getId(),
                    council.getCouncilName(),
                    group.getGroupName(),
                    result.getTopic().getTopicName(),
                    result.getFinalScore(), result.getResult(), result.getSummaryComment()));
        }
        return pending;
    }

    /** Các hội đồng có ít nhất một kết quả chờ công bố (để gom nút "Công bố cả hội đồng"). */
    @Transactional(readOnly = true)
    public List<Council> getCouncilsAwaitingPublication() {
        List<Council> out = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (ReviewResult rr : reviewResultRepository.findUnpublishedWithRefs()) {
            CouncilTopic ct = councilTopicRepository.findByTopicId(rr.getTopic().getId()).orElse(null);
            if (ct == null) continue;
            Council council = ct.getCouncil();
            if (council == null) continue;
            if (council.getStatus() == CouncilStatus.DRAFT
                    || council.getStatus() == CouncilStatus.FINISHED) continue;
            if (seen.add(council.getId())) out.add(council);
        }
        return out;
    }

    // =====================================================================================
    // Sinh viên xem kết quả (mục 5.8)
    // =====================================================================================

    /**
     * Sinh viên xem kết quả đã công bố của nhóm mình, có thể lọc theo đợt.
     * Nếu {@code periodId} được truyền vào mà sinh viên KHÔNG tham gia đợt đó thì ném 403.
     */
    @Transactional(readOnly = true)
    public List<StudentResultView> getResultsForStudent(String username, Long periodId) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();
        List<ReviewResult> mine;
        if (periodId != null) {
            // Chặn truy cập đợt SV không tham gia (kể cả khi đoán URL / gọi API).
            boolean participates = groupRepository.findAllByStudent(me.getId()).stream()
                    .anyMatch(g -> g.getPeriod() != null && periodId.equals(g.getPeriod().getId()));
            if (!participates) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn không tham gia đợt được chọn nên không được xem kết quả của đợt đó.");
            }
            mine = reviewResultRepository.findPublishedForStudentInPeriod(me.getId(), periodId);
        } else {
            mine = reviewResultRepository.findPublishedForStudent(me.getId());
        }
        List<StudentResultView> views = new ArrayList<>();
        for (ReviewResult r : mine) {
            views.add(new StudentResultView(
                    r.getTopic() == null ? null : r.getTopic().getPeriod() == null
                            ? null : r.getTopic().getPeriod().getId(),
                    r.getTopic() == null ? null : r.getTopic().getPeriod() == null
                            ? null : r.getTopic().getPeriod().getName(),
                    r.getGroup() == null ? null : r.getGroup().getGroupName(),
                    r.getTopic() == null ? null : r.getTopic().getTopicName(),
                    r.getFinalScore(),
                    r.getResult(),
                    r.getSummaryComment(),
                    r.getPublishedAt()));
        }
        return views;
    }

    /** Danh sách đợt mà sinh viên CÓ nhóm tham gia - nguồn cho bộ chọn "Tra cứu theo đợt". */
    @Transactional(readOnly = true)
    public List<vn.edu.demo.topicmanagement.dto.PeriodOptionView> getPeriodsForStudent(String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();
        List<vn.edu.demo.topicmanagement.dto.PeriodOptionView> out = new ArrayList<>();
        List<StudentGroup> groups = groupRepository.findAllByStudent(me.getId());
        java.util.Set<Long> seen = new java.util.LinkedHashSet<>();
        for (StudentGroup g : groups) {
            vn.edu.demo.topicmanagement.entity.RegistrationPeriod period = g.getPeriod();
            if (period == null) continue;
            if (!seen.add(period.getId())) continue;
            out.add(new vn.edu.demo.topicmanagement.dto.PeriodOptionView(
                    period.getId(), period.getName(),
                    period.getPeriodType() == null ? "" : period.getPeriodType().getDisplayName()));
        }
        return out;
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    /** Khi mọi đề tài của hội đồng đã công bố thì chuyển hội đồng sang FINISHED. */
    private void markCouncilFinishedIfAllPublished(Council council) {
        boolean allPublished = true;
        for (CouncilTopic ct : councilTopicRepository.findByCouncilIdWithTopic(council.getId())) {
            ReviewResult rr = reviewResultRepository.findByTopicId(ct.getTopic().getId()).orElse(null);
            if (rr == null || !rr.isPublished()) { allPublished = false; break; }
        }
        if (allPublished && council.getStatus() != CouncilStatus.FINISHED) {
            council.setStatus(CouncilStatus.FINISHED);
            councilRepository.save(council);
        }
    }

    private CouncilWorkView toWorkView(ReviewAssignment a) {
        Council council = a.getCouncil();
        Topic topic = a.getTopic();
        boolean supervisor = isSupervisor(topic, a.getLecturer().getId());
        Evaluation own = evaluationRepository.findByAssignmentId(a.getId()).orElse(null);
        List<Evaluation> evals = evaluationRepository.findByCouncilAndTopic(council.getId(), topic.getId());
        int assigned = assignmentRepository.findByCouncilAndTopicWithEvaluation(council.getId(), topic.getId()).size();
        ReviewResult result = reviewResultRepository.findByTopicId(topic.getId()).orElse(null);
        BigDecimal average = null;
        if (!evals.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Evaluation e : evals) sum = sum.add(e.getScore());
            average = sum.divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP);
        }
        List<String> scoreLabels = new ArrayList<>();
        for (Evaluation e : evals) {
            scoreLabels.add(e.getAssignment().getLecturer().getUser().getFullName()
                    + ": " + e.getScore()
                    + (e.getComment() == null || e.getComment().isBlank() ? "" : " — " + e.getComment()));
        }

        CouncilMember member = councilMemberRepository.findByCouncilId(council.getId()).stream()
                .filter(m -> m.getLecturer().getUser().getId().equals(a.getLecturer().getUser().getId()))
                .findFirst().orElse(null);
        CouncilRole role = member == null ? null : member.getRole();
        boolean allScored = assigned > 0 && evals.size() >= assigned;
        boolean isChairman = role == CouncilRole.CHAIRMAN;
        LocalDateTime deadline = topic.getPeriod() == null ? null : topic.getPeriod().getReviewDeadline();
        boolean inDeadline = deadline != null && !now().isAfter(deadline);
        // GV sửa được điểm của mình khi còn hạn và chưa công bố; sau hạn thì chỉ admin.
        boolean canScore = !supervisor
                && council.getStatus() == CouncilStatus.READY
                && (inDeadline || a.getAssignedBy() != null && a.getAssignedBy().hasRole("ADMIN"))
                && (result == null || !result.isPublished());
        boolean canFinalize = (isChairman || a.getAssignedBy() != null && a.getAssignedBy().hasRole("ADMIN"))
                && allScored
                && (result == null || !result.isPublished())
                && council.getStatus() == CouncilStatus.READY;
        boolean isPublished = result != null && result.isPublished();

        TopicRegistration reg = registrationRepository
                .findByTopicIdAndStatusIn(topic.getId(), List.of(RegistrationStatus.APPROVED))
                .stream().findFirst().orElse(null);
        boolean reportAvailable = reg != null && reportService.isDownloadAvailableByTopic(topic.getId());

        return new CouncilWorkView(
                a.getId(),                // assignmentId (review_assignments.id)
                reg != null ? reg.getId() : null,   // registrationId để tải báo cáo
                council.getCouncilName(),
                reg != null ? reg.getGroup().getGroupName() : "",
                topic.getTopicName(),
                role,
                scoreLabels,
                evals.size(),
                assigned,
                average,
                own == null ? null : own.getScore(),
                own == null ? null : own.getComment(),
                deadline,
                canScore,
                canFinalize,
                resultLabel(result == null ? null : result.getResult()),
                result == null ? null : result.getSummaryComment(),
                isPublished,
                reportAvailable);
    }

    /** "PASSED" -> "ĐẠT", "FAILED" -> "KHÔNG ĐẠT". */
    private String resultLabel(String code) {
        if (code == null) return null;
        return PASSED.equals(code) ? "ĐẠT" : "KHÔNG ĐẠT";
    }

    private boolean isSupervisor(Topic topic, Long lecturerId) {
        for (TopicSupervisor ts : supervisorRepository.findByTopicIdWithLecturer(topic.getId())) {
            if (ts.getLecturer().getId().equals(lecturerId)) return true;
        }
        return false;
    }

    private CouncilMember requireChair(Long councilId, String username) {
        CouncilMember member = councilMemberRepository.findByCouncilId(councilId).stream()
                .filter(m -> m.getLecturer().getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thuộc hội đồng này."));
        if (member.getRole() != CouncilRole.CHAIRMAN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ tịch hội đồng được tổng hợp.");
        }
        return member;
    }

    private LocalDateTime now() { return LocalDateTime.now(ZONE); }
}