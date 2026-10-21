package vn.edu.demo.topicmanagement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.LecturerOption;
import vn.edu.demo.topicmanagement.dto.ReviewScoreForm;
import vn.edu.demo.topicmanagement.dto.ReviewerTaskView;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.Lecturer;
import vn.edu.demo.topicmanagement.entity.PeriodType;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.ReviewAssignment;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.entity.TopicSupervisor;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.EvaluationRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.ReviewAssignmentRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.TopicSupervisorRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Nghiệp vụ cho GV được phân công phản biện: xem nhiệm vụ, nộp/sửa điểm & nhận xét.
 * Bảng phân công dùng {@code review_assignments}, điểm/nhận xét qua {@code evaluations}.
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
    private final UserAccountService userAccountService; // không dùng ở đây - để giữ constructor cũ
    private final StudentReportService reportService;

    public ReviewerService(ReviewAssignmentRepository assignmentRepository,
                           EvaluationRepository evaluationRepository,
                           TopicRegistrationRepository registrationRepository,
                           TopicSupervisorRepository supervisorRepository,
                           UserRepository userRepository,
                           LecturerRepository lecturerRepository,
                           RegistrationPeriodRepository periodRepository,
                           StudentReportService reportService) {
        this.assignmentRepository = assignmentRepository;
        this.evaluationRepository = evaluationRepository;
        this.registrationRepository = registrationRepository;
        this.supervisorRepository = supervisorRepository;
        this.userRepository = userRepository;
        this.lecturerRepository = lecturerRepository;
        this.periodRepository = periodRepository;
        this.reportService = reportService;
        this.userAccountService = null;
    }

    /** Danh sách đề tài được phân công chấm cho giảng viên hiện tại. */
    @Transactional(readOnly = true)
    public List<ReviewerTaskView> getMyTasks(String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();
        List<ReviewAssignment> assignments = assignmentRepository.findByLecturerWithDetails(me.getId());
        List<ReviewerTaskView> views = new ArrayList<>();
        for (ReviewAssignment a : assignments) {
            Topic topic = a.getTopic();
            LocalDateTime deadline = topic.getPeriod().getReviewDeadline();
            boolean inDeadline = deadline != null && !now().isAfter(deadline);
            BigDecimal ownScore = evaluationRepository.findByAssignmentId(a.getId()).map(e -> e.getScore()).orElse(null);
            boolean downloadable = reportService.isDownloadAvailableByTopic(topic.getId());
            views.add(new ReviewerTaskView(
                    a.getId(),
                    topic.getId(),
                    null,
                    topic.getTopicName(),
                    deadline,
                    ownScore,
                    null,
                    null,
                    inDeadline,
                    downloadable));
        }
        return views;
    }

    @Transactional
    public void submitReview(Long assignmentId, String username, ReviewScoreForm form) {
        ReviewAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phân công phản biện."));
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null || !assignment.getLecturer().getUser().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Đây không phải phân công của bạn.");
        }
        LocalDateTime deadline = assignment.getTopic().getPeriod().getReviewDeadline();
        if (deadline == null || now().isAfter(deadline)) {
            throw new IllegalStateException("Đã quá hạn nộp điểm phản biện.");
        }
        BigDecimal score = form.getScore();
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng 0 đến 10.");
        }
        evaluationRepository.findByAssignmentId(assignmentId).orElseGet(() -> {
            vn.edu.demo.topicmanagement.entity.Evaluation e = new vn.edu.demo.topicmanagement.entity.Evaluation();
            e.setAssignment(assignment);
            return e;
        });
        vn.edu.demo.topicmanagement.entity.Evaluation eval = evaluationRepository.findByAssignmentId(assignmentId).get();
        eval.setScore(score.setScale(2, RoundingMode.HALF_UP));
        eval.setComment(form.getComment() == null ? null : form.getComment().trim());
        eval.setStatus("SUBMITTED");
        evaluationRepository.save(eval);
        assignment.setStatus("COMPLETED");
        assignmentRepository.save(assignment);
    }

    /** GV xem danh sách các đề tài được phân công phản biện (dùng cho màn hình reviewer). */
    @Transactional(readOnly = true)
    public List<LecturerOption> listActiveReviewerCandidates() {
        return lecturerRepository.findAllActiveWithDepartment().stream()
                .map(l -> new LecturerOption(l.getId(), l.getUser().getFullName(), l.getLecturerCode())).toList();
    }

    /** HEAD: danh sách các đề tài cần phân công phản biện (trả view cho trang list). */
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
                assignees.add(new vn.edu.demo.topicmanagement.dto.ReviewAssigneeView(a.getId(), name, submitted));
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

    @Transactional
    public void assign(Long registrationId, Long lecturerId) {
        TopicRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài."));
        if (reg.getStatus() != vn.edu.demo.topicmanagement.entity.RegistrationStatus.APPROVED) {
            throw new IllegalStateException("Chỉ phân công cho đăng ký đã duyệt.");
        }
        Lecturer lecturer = lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên."));
        // Tạo một review assignment mới - đơn giản: gán với council nếu có, không thì tạo null council
        ReviewAssignment ra = new ReviewAssignment();
        ra.setTopic(reg.getTopic());
        ra.setLecturer(lecturer);
        // assignedBy: lấy từ SecurityContext - nếu cần, truyền qua param
        ra.setAssignedBy(null);
        ra.setStatus("ASSIGNED");
        assignmentRepository.save(ra);
    }

    @Transactional
    public void removeAssignment(Long registrationId, Long assignmentId) {
        ReviewAssignment ra = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công."));
        if (ra.getTopic() == null || !ra.getTopic().getId().equals(
                registrationRepository.findById(registrationId).map(TopicRegistration::getTopic).map(Topic::getId).orElse(-1L))) {
            throw new IllegalArgumentException("Phân công không khớp đăng ký.");
        }
        if (!"ASSIGNED".equals(ra.getStatus())) {
            throw new IllegalStateException("Chỉ gỡ phân công chưa nộp điểm.");
        }
        assignmentRepository.delete(ra);
    }

    private LocalDateTime now() { return LocalDateTime.now(ZONE); }
}
