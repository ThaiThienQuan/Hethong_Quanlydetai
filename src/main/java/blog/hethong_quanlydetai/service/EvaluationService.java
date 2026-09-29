package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.Evaluation;
import blog.hethong_quanlydetai.entity.ReviewAssignment;
import blog.hethong_quanlydetai.entity.ReviewResult;
import blog.hethong_quanlydetai.repository.EvaluationRepository;
import blog.hethong_quanlydetai.repository.ReviewAssignmentRepository;
import blog.hethong_quanlydetai.repository.ReviewResultRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final ReviewAssignmentRepository assignmentRepository;
    private final ReviewResultRepository resultRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EvaluationService(EvaluationRepository evaluationRepository,
            ReviewAssignmentRepository assignmentRepository,
            ReviewResultRepository resultRepository) {
        this.evaluationRepository = evaluationRepository;
        this.assignmentRepository = assignmentRepository;
        this.resultRepository = resultRepository;
    }

    public List<Evaluation> findAll() {
        return evaluationRepository.findAllByOrderByEvaluatedAtDesc();
    }

    public List<ReviewResult> findResults() {
        return resultRepository.findAll();
    }

        @SuppressWarnings("unchecked")
        public List<CalculationOption> findReadyCalculations(String username, boolean admin) {
                String chairmanFilter = admin ? "" : """
                                AND EXISTS (
                                        SELECT 1
                                        FROM council_members cm
                                        JOIN lecturers l ON l.lecturer_id = cm.lecturer_id
                                        JOIN users u ON u.user_id = l.user_id
                                        WHERE cm.council_id = c.council_id
                                            AND cm.role = 'CHAIRMAN'
                                            AND u.username = :username
                                )
                                """;
                var query = entityManager.createNativeQuery("""
                                SELECT sg.group_id, sg.group_name, t.topic_id, t.topic_name, c.council_id, c.council_name
                                FROM topic_registrations tr
                                JOIN student_groups sg ON sg.group_id = tr.group_id
                                JOIN topics t ON t.topic_id = tr.topic_id
                                JOIN review_assignments ra ON ra.topic_id = t.topic_id
                                JOIN councils c ON c.council_id = ra.council_id
                                LEFT JOIN evaluations e ON e.assignment_id = ra.assignment_id
                                WHERE tr.status = 'APPROVED'
                                    AND NOT EXISTS (
                                            SELECT 1
                                            FROM review_results rr
                                            WHERE rr.group_id = sg.group_id
                                                AND rr.topic_id = t.topic_id
                                                AND rr.council_id = c.council_id
                                                AND rr.published = TRUE
                                    )
                                """ + chairmanFilter + """
                                GROUP BY sg.group_id, sg.group_name, t.topic_id, t.topic_name, c.council_id, c.council_name
                                HAVING COUNT(DISTINCT ra.assignment_id) > 0
                                     AND COUNT(DISTINCT e.evaluation_id) = COUNT(DISTINCT ra.assignment_id)
                                ORDER BY sg.group_name, t.topic_name, c.council_name
                                """);
                if (!admin) {
                        query.setParameter("username", username);
                }

                List<Object[]> rows = query.getResultList();
                return rows.stream()
                                .map(row -> new CalculationOption(
                                                ((Number) row[0]).longValue(),
                                                (String) row[1],
                                                ((Number) row[2]).longValue(),
                                                (String) row[3],
                                                ((Number) row[4]).longValue(),
                                                (String) row[5]))
                                .toList();
        }

    @SuppressWarnings("unchecked")
    public List<AssignmentOption> findPendingAssignments(String username, boolean admin) {
        String ownershipFilter = admin ? "" : "AND u.username = :username";
        var query = entityManager.createNativeQuery("""
                SELECT ra.assignment_id, t.topic_name, c.council_name
                FROM review_assignments ra
                JOIN topics t ON t.topic_id = ra.topic_id
                JOIN councils c ON c.council_id = ra.council_id
                JOIN lecturers l ON l.lecturer_id = ra.lecturer_id
                JOIN users u ON u.user_id = l.user_id
                LEFT JOIN evaluations e ON e.assignment_id = ra.assignment_id
                WHERE e.evaluation_id IS NULL
                """ + ownershipFilter + " ORDER BY ra.assigned_at DESC");
        if (!admin) {
            query.setParameter("username", username);
        }

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new AssignmentOption(((Number) row[0]).longValue(), (String) row[1], (String) row[2]))
                .toList();
    }

    public void save(Long assignmentId, BigDecimal score, String comment, String username, boolean admin) {
        ReviewAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Mã phân công không tồn tại."));

        if (!admin && !isAssignedLecturer(assignmentId, username)) {
            throw new IllegalArgumentException("Bạn chỉ được nhập điểm cho đề tài đã phân công cho mình.");
        }
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng từ 0 đến 10.");
        }

        if (evaluationRepository.existsByAssignmentId(assignmentId)) {
            throw new IllegalArgumentException("Phân công này đã được nhập điểm.");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setAssignmentId(assignmentId);
        evaluation.setScore(score);
        evaluation.setComment(comment);
        evaluation.setEvaluatedAt(LocalDateTime.now());
        evaluation.setStatus("SUBMITTED");

        evaluationRepository.save(evaluation);
        assignment.setStatus("COMPLETED");
        assignmentRepository.save(assignment);
    }

    public void calculateResult(Long groupId, Long topicId, Long councilId, String username, boolean admin) {
        verifyChairman(councilId, username, admin);
        verifyGroupTopic(groupId, topicId);

        List<ReviewAssignment> assignments = assignmentRepository.findByCouncilIdAndTopicId(councilId, topicId);

        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("Chưa có giảng viên nào được phân công chấm đề tài này.");
        }

        List<Long> assignmentIds = assignments.stream()
                .map(ReviewAssignment::getId)
                .toList();

        List<Evaluation> evaluations = evaluationRepository.findByAssignmentIdIn(assignmentIds);

        if (evaluations.size() != assignments.size()) {
            throw new IllegalArgumentException("Chỉ được tổng hợp khi tất cả giảng viên được phân công đã nhập điểm.");
        }

        ReviewResult result = resultRepository
                .findByGroupIdAndTopicIdAndCouncilId(groupId, topicId, councilId)
                .orElse(new ReviewResult());
        if (Boolean.TRUE.equals(result.getPublished())) {
            throw new IllegalArgumentException("Kết quả đã công bố, không thể tổng hợp lại.");
        }

        BigDecimal total = evaluations.stream()
                .map(Evaluation::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalScore = total.divide(
                BigDecimal.valueOf(evaluations.size()),
                2,
                RoundingMode.HALF_UP);

        result.setGroupId(groupId);
        result.setTopicId(topicId);
        result.setCouncilId(councilId);
        result.setFinalScore(finalScore);
        result.setResult(finalScore.compareTo(BigDecimal.valueOf(5)) >= 0 ? "PASS" : "FAIL");
        result.setPublished(false);
        result.setPublishedAt(null);

        resultRepository.save(result);
    }

    public void publishResult(Long resultId, String username, boolean admin) {
        ReviewResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả cần công bố."));
        verifyChairman(result.getCouncilId(), username, admin);

        List<ReviewAssignment> assignments = assignmentRepository
                .findByCouncilIdAndTopicId(result.getCouncilId(), result.getTopicId());
        List<Long> assignmentIds = assignments.stream().map(ReviewAssignment::getId).toList();
        if (assignments.isEmpty()
                || evaluationRepository.findByAssignmentIdIn(assignmentIds).size() != assignments.size()) {
            throw new IllegalArgumentException("Chưa thể công bố vì còn phân công chưa có điểm.");
        }

        result.setPublished(true);
        result.setPublishedAt(LocalDateTime.now());
        resultRepository.save(result);
    }

    @SuppressWarnings("unchecked")
    public List<StudentResultView> findPublishedResultsForStudent(String username) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT sg.group_name, t.topic_name, rr.final_score, rr.result, rr.published_at
                FROM review_results rr
                JOIN student_groups sg ON sg.group_id = rr.group_id
                JOIN topics t ON t.topic_id = rr.topic_id
                JOIN group_members gm ON gm.group_id = sg.group_id
                JOIN students s ON s.student_id = gm.student_id
                JOIN users u ON u.user_id = s.user_id
                WHERE u.username = :username AND rr.published = TRUE
                ORDER BY rr.published_at DESC
                """)
                .setParameter("username", username)
                .getResultList();

        return rows.stream()
                .map(row -> new StudentResultView(
                        (String) row[0],
                        (String) row[1],
                        (BigDecimal) row[2],
                        (String) row[3],
                        toLocalDateTime(row[4])))
                .toList();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new IllegalStateException("Kiểu thời gian công bố không được hỗ trợ: " + value.getClass().getName());
    }

    private boolean isAssignedLecturer(Long assignmentId, String username) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM review_assignments ra
                JOIN lecturers l ON l.lecturer_id = ra.lecturer_id
                JOIN users u ON u.user_id = l.user_id
                WHERE ra.assignment_id = :assignmentId AND u.username = :username
                """)
                .setParameter("assignmentId", assignmentId)
                .setParameter("username", username)
                .getSingleResult();
        return count.longValue() > 0;
    }

    private void verifyChairman(Long councilId, String username, boolean admin) {
        if (admin) {
            return;
        }

        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM council_members cm
                JOIN lecturers l ON l.lecturer_id = cm.lecturer_id
                JOIN users u ON u.user_id = l.user_id
                WHERE cm.council_id = :councilId
                  AND cm.role = 'CHAIRMAN'
                  AND u.username = :username
                """)
                .setParameter("councilId", councilId)
                .setParameter("username", username)
                .getSingleResult();
        if (count.longValue() == 0) {
            throw new IllegalArgumentException("Chỉ chủ tịch hội đồng mới được tổng hợp hoặc công bố kết quả.");
        }
    }

    private void verifyGroupTopic(Long groupId, Long topicId) {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM topic_registrations
                WHERE group_id = :groupId
                  AND topic_id = :topicId
                  AND status = 'APPROVED'
                """)
                .setParameter("groupId", groupId)
                .setParameter("topicId", topicId)
                .getSingleResult();

        if (count.longValue() == 0) {
            throw new IllegalArgumentException(
                    "Nhóm sinh viên chưa được đăng ký và phê duyệt đề tài này.");
        }
    }

    public record StudentResultView(
            String groupName,
            String topicName,
            BigDecimal finalScore,
            String result,
            LocalDateTime publishedAt) {
    }

    public record AssignmentOption(Long id, String topicName, String councilName) {
    }

    public record CalculationOption(
            Long groupId,
            String groupName,
            Long topicId,
            String topicName,
            Long councilId,
            String councilName) {
    }
}