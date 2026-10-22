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

    /**
     * Số đề tài trong các hội đồng {@code READY} chưa được phân công người chấm -
     * "đề tài chưa phân công chấm" trong bảng việc cần làm của Trưởng khoa (mục 5.10).
     */
    @Query("""
            select count(ct) from CouncilTopic ct
            where ct.council.status = :ready
              and not exists (
                  select 1 from ReviewAssignment ra
                  where ra.council = ct.council and ra.topic = ct.topic)
            """)
    long countUnassignedInReadyCouncils(
            @Param("ready") vn.edu.demo.topicmanagement.entity.CouncilStatus ready);

    /**
     * Số đề tài đã có đủ điểm (mọi người chấm đã nộp) nhưng chưa công bố, tính theo
     * danh sách hội đồng - dùng cho "GV là chủ tịch" (mục 5.10).
     */
    @Query("""
            select count(ct) from CouncilTopic ct
            where ct.council.id in :councilIds
              and exists (
                  select 1 from ReviewAssignment ra
                  where ra.council = ct.council and ra.topic = ct.topic)
              and not exists (
                  select 1 from ReviewAssignment ra
                  where ra.council = ct.council and ra.topic = ct.topic
                    and not exists (select 1 from vn.edu.demo.topicmanagement.entity.Evaluation e
                                    where e.assignment = ra))
              and not exists (
                  select 1 from vn.edu.demo.topicmanagement.entity.ReviewResult rr
                  where rr.topic = ct.topic and rr.published = true)
            """)
    long countReadyToFinalize(@Param("councilIds") List<Long> councilIds);
}