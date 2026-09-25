package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.ReviewResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewResultRepository extends JpaRepository<ReviewResult, Long> {
    Optional<ReviewResult> findByGroupIdAndTopicIdAndCouncilId(Long groupId, Long topicId, Long councilId);
}