package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.Evaluation;
import vn.edu.demo.topicmanagement.entity.ReviewAssignment;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    /** Mỗi phân công chỉ có một bản chấm. */
    Optional<Evaluation> findByAssignmentId(Long assignmentId);

    boolean existsByAssignmentId(Long assignmentId);

    /** Điểm của một đề tài trong hội đồng - dùng tính điểm trung bình cộng. */
    @Query("""
            select e from Evaluation e
            join fetch e.assignment a
            join fetch a.lecturer l
            join fetch l.user u
            where a.council.id = :councilId and a.topic.id = :topicId
            order by u.fullName
            """)
    List<Evaluation> findByCouncilAndTopic(@Param("councilId") Long councilId,
                                           @Param("topicId") Long topicId);

    /** Đếm số phân công đã có điểm, so với tổng số phân công để biết đã đủ điểm chưa. */
    @Query("""
            select count(e) from Evaluation e
            where e.assignment.council.id = :councilId and e.assignment.topic.id = :topicId
            """)
    long countByCouncilAndTopic(@Param("councilId") Long councilId, @Param("topicId") Long topicId);

    /** Bản chấm của một phân công cụ thể (dùng cho màn hình nhập điểm). */
    @Query("select e from Evaluation e where e.assignment = :assignment")
    Optional<Evaluation> findByAssignment(@Param("assignment") ReviewAssignment assignment);
}