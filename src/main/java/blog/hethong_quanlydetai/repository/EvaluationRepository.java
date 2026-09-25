package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findAllByOrderByEvaluatedAtDesc();
    boolean existsByAssignmentId(Long assignmentId);
    List<Evaluation> findByAssignmentIdIn(List<Long> assignmentIds);
}