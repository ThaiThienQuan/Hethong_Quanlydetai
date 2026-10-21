package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.CouncilTopic;
import vn.edu.demo.topicmanagement.entity.ReviewAssignment;

public interface CouncilTopicRepository extends JpaRepository<CouncilTopic, Long> {

    List<CouncilTopic> findByCouncilId(Long councilId);

    Optional<CouncilTopic> findByTopicId(Long topicId);

    boolean existsByCouncilIdAndTopicId(Long councilId, Long topicId);

    @Query("""
            select ct from CouncilTopic ct
            join fetch ct.topic t
            join fetch t.period p
            where ct.council.id = :councilId
            order by t.topicCode
            """)
    List<CouncilTopic> findByCouncilIdWithTopic(@Param("councilId") Long councilId);

    /** Các hội đồng mà một đề tài được đưa vào phản biện. */
    @Query("select ct.council.id from CouncilTopic ct where ct.topic.id = :topicId")
    List<Long> findCouncilIdsByTopicId(@Param("topicId") Long topicId);
}