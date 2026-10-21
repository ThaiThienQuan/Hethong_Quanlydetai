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
import vn.edu.demo.topicmanagement.entity.ReviewAssignment;
import vn.edu.demo.topicmanagement.entity.ReviewResult;
import vn.edu.demo.topicmanagement.entity.StudentGroup;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicSupervisor;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.CouncilMemberRepository;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.CouncilTopicRepository;
import vn.edu.demo.topicmanagement.repository.EvaluationRepository;
import vn.edu.demo.topicmanagement.repository.ReviewAssignmentRepository;
import vn.edu.demo.topicmanagement.repository.ReviewResultRepository;
import vn.edu.demo.topicmanagement.repository.StudentGroupRepository;
import vn.edu.demo.topicmanagement.repository.TopicSupervisorRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/** Nghiệp vụ GV chấm điểm / chủ tịch tổng hợp / Trưởng khoa công bố kết quả. */
@Service
public class CouncilScoringService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CouncilTopicRepository councilTopicRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final EvaluationRepository evaluationRepository;
    private final ReviewAssignmentRepository assignmentRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final CouncilRepository councilRepository;
    private final UserRepository userRepository;
    private final StudentGroupRepository groupRepository;
    private final StudentReportService reportService;

    public CouncilScoringService(CouncilTopicRepository councilTopicRepository,
                                 CouncilMemberRepository councilMemberRepository,
                                 TopicSupervisorRepository supervisorRepository,
                                 EvaluationRepository evaluationRepository,
                                 ReviewAssignmentRepository assignmentRepository,
                                 ReviewResultRepository reviewResultRepository,
                                 CouncilRepository councilRepository,
                                 UserRepository userRepository,
                                 StudentGroupRepository groupRepository,
                                 StudentReportService reportService) {
        this.councilTopicRepository = councilTopicRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.supervisorRepository = supervisorRepository;
        this.evaluationRepository = evaluationRepository;
        this.assignmentRepository = assignmentRepository;
        this.reviewResultRepository = reviewResultRepository;
        this.councilRepository = councilRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.reportService = reportService;
    }

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

    /** Lưu/cập nhật điểm cho một phân công chấm. */
    @Transactional
    public void submitScore(Long assignmentId, String username, ReviewScoreForm form) {
        ReviewAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phân công chấm."));
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (!assignment.getLecturer().getUser().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Phân công này không thuộc về bạn.");
        }
        Council council = assignment.getCouncil();
        if (council.getStatus() != CouncilStatus.READY && council.getStatus() != CouncilStatus.ACTIVE) {
            throw new IllegalStateException("Hội đồng chưa sẵn sàng chấm.");
        }
        if (isSupervisor(assignment.getTopic(), assignment.getLecturer().getId())) {
            throw new IllegalStateException("GVHD không được chấm đề tài mình hướng dẫn.");
        }
        BigDecimal value = form.getScore();
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng 0 đến 10.");
        }
        Evaluation eval = evaluationRepository.findByAssignmentId(assignmentId).orElseGet(() -> {
            Evaluation e = new Evaluation();
            e.setAssignment(assignment);
            return e;
        });
        eval.setScore(value.setScale(2, RoundingMode.HALF_UP));
        eval.setComment(form.getComment() == null ? null : form.getComment().trim());
        eval.setStatus("SUBMITTED");
        evaluationRepository.save(eval);
        assignment.setStatus("COMPLETED");
        assignmentRepository.save(assignment);
    }

    /** Chủ tịch tổng hợp điểm thành kết quả. */
    @Transactional
    public void finalizeResult(Long councilTopicId, String username, CouncilResultForm form) {
        CouncilTopic councilTopic = councilTopicRepository.findById(councilTopicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài trong hội đồng."));
        Council council = councilTopic.getCouncil();
        if (council.getStatus() != CouncilStatus.READY && council.getStatus() != CouncilStatus.ACTIVE) {
            throw new IllegalStateException("Hội đồng chưa sẵn sàng.");
        }
        CouncilMember me = requireChair(council.getId(), username);
        Topic topic = councilTopic.getTopic();

        List<ReviewAssignment> assignments = assignmentRepository.findByCouncilAndTopicWithEvaluation(council.getId(), topic.getId());
        if (assignments.isEmpty()) {
            throw new IllegalStateException("Chưa phân công giảng viên chấm đề tài này.");
        }
        List<Evaluation> evals = evaluationRepository.findByCouncilAndTopic(council.getId(), topic.getId());
        if (evals.size() != assignments.size()) {
            throw new IllegalStateException("Chưa đủ điểm từ mọi thành viên được phân công.");
        }
        for (Evaluation ev : evals) {
            if (isSupervisor(topic, ev.getAssignment().getLecturer().getId())) {
                throw new IllegalStateException("Có GVHD chấm đề tài của mình; sửa trước khi tổng hợp.");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Evaluation ev : evals) total = total.add(ev.getScore());
        BigDecimal avg = total.divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP);

        StudentGroup group = groupRepository.findByTopicId(topic.getId()).orElse(null);
        if (group == null) {
            throw new IllegalStateException("Chưa tìm thấy nhóm đang thực hiện đề tài này.");
        }

        ReviewResult result = reviewResultRepository.findByTopicId(topic.getId()).orElseGet(ReviewResult::new);
        result.setTopic(topic);
        result.setCouncil(council);
        result.setGroup(group);
        result.setFinalScore(avg);
        result.setResult(form.getResult() == null ? "PASSED" : form.getResult().toUpperCase());
        result.setSummaryComment(form.getCouncilComment() == null ? null : form.getCouncilComment().trim());
        result.setCalculatedBy(me.getLecturer().getUser());
        reviewResultRepository.save(result);
    }

    /** Trưởng khoa công bố kết quả. */
    @Transactional
    public void publishResultByHead(Long councilTopicId, String username) {
        User publisher = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (!publisher.hasRole("TRUONG_KHOA")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Trưởng khoa được công bố kết quả.");
        }
        CouncilTopic councilTopic = councilTopicRepository.findById(councilTopicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài trong hội đồng."));
        Topic topic = councilTopic.getTopic();
        ReviewResult result = reviewResultRepository.findByTopicId(topic.getId())
                .orElseThrow(() -> new IllegalStateException("Chủ tịch cần tổng hợp kết quả trước khi Trưởng khoa công bố."));
        if (result.isPublished()) throw new IllegalStateException("Kết quả này đã được công bố.");
        result.setPublished(true);
        result.setPublishedAt(now());
        result.setPublishedBy(publisher);
        reviewResultRepository.save(result);

        Council council = councilTopic.getCouncil();
        boolean allPublished = true;
        for (CouncilTopic ct : councilTopicRepository.findByCouncilId(council.getId())) {
            ReviewResult rr = reviewResultRepository.findByTopicId(ct.getTopic().getId()).orElse(null);
            if (rr == null || !rr.isPublished()) { allPublished = false; break; }
        }
        if (allPublished) {
            council.setStatus(CouncilStatus.FINISHED);
            councilRepository.save(council);
        }
    }

    /** Kết quả chờ công bố - cho Trưởng khoa. */
    @Transactional(readOnly = true)
    public List<HeadResultView> getResultsAwaitingPublication() {
        List<HeadResultView> pending = new ArrayList<>();
        for (ReviewResult result : reviewResultRepository.findByPublishedFalseOrderByIdAsc()) {
            CouncilTopic ct = councilTopicRepository.findByTopicId(result.getTopic().getId()).orElse(null);
            if (ct == null) continue;
            if (ct.getCouncil().getStatus() != CouncilStatus.READY
                    && ct.getCouncil().getStatus() != CouncilStatus.ACTIVE) continue;
            pending.add(new HeadResultView(ct.getId(), ct.getCouncil().getCouncilName(),
                    result.getGroup().getGroupName(), result.getTopic().getTopicName(),
                    result.getFinalScore(), result.getResult(), result.getSummaryComment()));
        }
        return pending;
    }

    /** SV xem kết quả đã công bố. */
    @Transactional(readOnly = true)
    public List<StudentResultView> getResultsForStudent(String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();
        List<ReviewResult> mine = reviewResultRepository.findPublishedForStudent(me.getId());
        List<StudentResultView> views = new ArrayList<>();
        for (ReviewResult r : mine) {
            views.add(new StudentResultView(
                    r.getGroup() == null ? null : r.getGroup().getGroupName(),
                    r.getTopic() == null ? null : r.getTopic().getTopicName(),
                    r.getFinalScore(),
                    r.getResult(),
                    r.getSummaryComment(),
                    r.getPublishedAt()));
        }
        return views;
    }

    // ===== Helpers =====

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
            scoreLabels.add(e.getAssignment().getLecturer().getUser().getFullName() + ": " + e.getScore());
        }

        CouncilMember member = councilMemberRepository.findByCouncilId(council.getId()).stream()
                .filter(m -> m.getLecturer().getUser().getId().equals(a.getLecturer().getUser().getId()))
                .findFirst().orElse(null);
        CouncilRole role = member == null ? null : member.getRole();
        boolean allScored = evals.size() >= assigned;
        boolean isChairman = role == CouncilRole.CHAIRMAN;
        boolean canScore = !supervisor && (own == null);
        boolean canFinalize = isChairman && allScored && (result == null || !result.isPublished());
        boolean isPublished = result != null && result.isPublished();

        return new CouncilWorkView(
                a.getId(),
                topic.getId(),
                council.getCouncilName(),
                null,
                topic.getTopicName(),
                role,
                scoreLabels,
                evals.size(),
                assigned,
                average,
                own == null ? null : own.getScore(),
                canScore,
                canFinalize,
                result == null ? null : result.getResult(),
                result == null ? null : result.getSummaryComment(),
                isPublished,
                false);
    }

    private boolean isSupervisor(Topic topic, Long lecturerId) {
        List<TopicSupervisor> supervisors = supervisorRepository.findByTopicIdWithLecturer(topic.getId());
        for (TopicSupervisor ts : supervisors) {
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
