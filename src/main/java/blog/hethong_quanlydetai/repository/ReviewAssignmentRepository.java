package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.ReviewAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {
    List<ReviewAssignment> findAllByOrderByAssignedAtDesc();
    boolean existsByCouncilIdAndTopicIdAndLecturerId(Long councilId, Long topicId, Long lecturerId);
    List<ReviewAssignment> findByCouncilIdAndTopicId(Long councilId, Long topicId);
}