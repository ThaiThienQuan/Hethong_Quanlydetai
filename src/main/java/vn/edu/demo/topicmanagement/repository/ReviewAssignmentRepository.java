package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.ReviewAssignment;

public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {

    List<ReviewAssignment> findByLecturerId(Long lecturerId);

    List<ReviewAssignment> findByTopicId(Long topicId);

    boolean existsByCouncilIdAndTopicIdAndLecturerId(Long councilId, Long topicId, Long lecturerId);

    long countByTopicId(Long topicId);

    /**
     * Danh sách đề tài giảng viên được phân công chấm, kèm điểm nếu đã chấm.
     * Dùng cho màn hình "Nhiệm vụ phản biện" của giảng viên (mục 5.7).
     */
    @Query("""
            select ra from ReviewAssignment ra
            join fetch ra.topic t
            join fetch t.period p
            where ra.lecturer.user.id = :lecturerUserId
            order by p.studentEndAt desc, t.topicCode
            """)
    List<ReviewAssignment> findByLecturerWithDetails(@Param("lecturerUserId") Long lecturerUserId);

    /** Phân công của một hội đồng, dùng cho trang chi tiết hội đồng. */
    @Query("""
            select ra from ReviewAssignment ra
            join fetch ra.lecturer l
            join fetch l.user u
            where ra.council.id = :councilId
            order by u.fullName
            """)
    List<ReviewAssignment> findByCouncilIdWithLecturer(@Param("councilId") Long councilId);

    Optional<ReviewAssignment> findByCouncilIdAndTopicIdAndLecturerId(Long councilId, Long topicId, Long lecturerId);

    /** Tất cả phân công của một đề tài trong hội đồng - dùng tính điểm trung bình. */
    @Query("""
            select ra from ReviewAssignment ra
            where ra.council.id = :councilId and ra.topic.id = :topicId
            """)
    List<ReviewAssignment> findByCouncilAndTopicWithEvaluation(@Param("councilId") Long councilId,
                                                              @Param("topicId") Long topicId);

    /** Bản gọn: tất cả phân công của một đề tài trong hội đồng (không cần fetch evaluation). */
    @Query("""
            select ra from ReviewAssignment ra
            where ra.council.id = :councilId and ra.topic.id = :topicId
            """)
    List<ReviewAssignment> findByCouncilAndTopic(@Param("councilId") Long councilId,
                                                 @Param("topicId") Long topicId);
}