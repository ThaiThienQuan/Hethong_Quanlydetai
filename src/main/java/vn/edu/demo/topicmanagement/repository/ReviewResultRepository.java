package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.ReviewResult;

public interface ReviewResultRepository extends JpaRepository<ReviewResult, Long> {

    Optional<ReviewResult> findByTopicId(Long topicId);

    Optional<ReviewResult> findByGroupId(Long groupId);

    /** Kết quả chưa công bố - Trưởng khoa công bố (mục 5.8). */
    List<ReviewResult> findByPublishedFalseOrderByIdAsc();

    /**
     * Kết quả chưa công bố kèm đề tài/nhóm/hội đồng tồn tại thật.
     * Dùng inner join để loại các dòng review_results mồ côi (group_id/topic_id không còn tồn tại),
     * tránh lỗi EntityNotFoundException khi nạp lazy association.
     */
    @Query("""
            select distinct rr from ReviewResult rr
            join fetch rr.topic t
            join fetch rr.group g
            join fetch rr.council c
            where rr.published = false
            order by rr.id asc
            """)
    List<ReviewResult> findUnpublishedWithRefs();

    List<ReviewResult> findByCouncilIdOrderByIdAsc(Long councilId);

    long countByPublishedTrue();

    /**
     * Kết quả đã công bố của các đợt mà sinh viên có nhóm tham gia.
     * Nguồn cho trang "Kết quả của tôi" - danh sách đợt lấy từ group_members.period_id,
     * không tin tham số periodId từ client (mục 5.8).
     */
    @Query("""
            select distinct rr from ReviewResult rr
            join fetch rr.topic t
            join fetch rr.group g
            where rr.published = true
              and exists (select 1 from GroupMember gm
                          where gm.group = g and gm.student.user.id = :studentUserId)
            order by rr.publishedAt desc
            """)
    List<ReviewResult> findPublishedForStudent(@Param("studentUserId") Long studentUserId);

    /** Kết quả đã công bố của một đợt mà sinh viên có nhóm tham gia. */
    @Query("""
            select distinct rr from ReviewResult rr
            join fetch rr.topic t
            join fetch rr.group g
            where rr.published = true
              and g.period.id = :periodId
              and exists (select 1 from GroupMember gm
                          where gm.group = g and gm.student.user.id = :studentUserId)
            """)
    List<ReviewResult> findPublishedForStudentInPeriod(@Param("studentUserId") Long studentUserId,
                                                       @Param("periodId") Long periodId);
}