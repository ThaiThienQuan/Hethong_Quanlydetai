package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.Evaluation;
import blog.hethong_quanlydetai.entity.ReviewAssignment;
import blog.hethong_quanlydetai.entity.ReviewResult;
import blog.hethong_quanlydetai.repository.EvaluationRepository;
import blog.hethong_quanlydetai.repository.ReviewAssignmentRepository;
import blog.hethong_quanlydetai.repository.ReviewResultRepository;
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

    public void save(Long assignmentId, BigDecimal score, String comment) {
        if (!assignmentRepository.existsById(assignmentId)) {
        throw new IllegalArgumentException("Mã phân công không tồn tại.");
}
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0) {
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
    }

    public void calculateResult(Long groupId, Long topicId, Long councilId) {
        List<ReviewAssignment> assignments = assignmentRepository.findByCouncilIdAndTopicId(councilId, topicId);

        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("Chưa có giảng viên nào được phân công chấm đề tài này.");
        }

        List<Long> assignmentIds = assignments.stream()
                .map(ReviewAssignment::getId)
                .toList();

        List<Evaluation> evaluations = evaluationRepository.findByAssignmentIdIn(assignmentIds);

        if (evaluations.isEmpty()) {
            throw new IllegalArgumentException("Chưa có điểm để tính kết quả.");
        }

        BigDecimal total = evaluations.stream()
                .map(Evaluation::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalScore = total.divide(
                BigDecimal.valueOf(evaluations.size()),
                2,
                RoundingMode.HALF_UP
        );

        ReviewResult result = resultRepository
                .findByGroupIdAndTopicIdAndCouncilId(groupId, topicId, councilId)
                .orElse(new ReviewResult());

        result.setGroupId(groupId);
        result.setTopicId(topicId);
        result.setCouncilId(councilId);
        result.setFinalScore(finalScore);
        result.setResult(finalScore.compareTo(BigDecimal.valueOf(5)) >= 0 ? "PASS" : "FAIL");
        result.setPublished(false);

        resultRepository.save(result);
    }
}