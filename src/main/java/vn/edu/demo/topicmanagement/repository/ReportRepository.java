package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByGroupIdOrderByVersionDesc(Long groupId);

    /** Phiên bản cao nhất của nhóm - dùng tính số phiên bản kế tiếp. */
    @Query("select coalesce(max(r.version), 0) from Report r where r.group.id = :groupId")
    int findMaxVersionByGroup(@Param("groupId") Long groupId);

    long countByGroupId(Long groupId);

    boolean existsByGroupId(Long groupId);

    /** Nhóm có báo cáo đang ở trạng thái SUBMITTED (bản hiện lực). */
    boolean existsByGroupIdAndStatus(Long groupId, String status);

    /** Báo cáo đã nộp của một đề tài - dùng chặn phân công khi đề tài chưa có báo cáo. */
    @Query("""
            select r from Report r
            join fetch r.group g
            where g.topic.id = :topicId
              and r.status = 'SUBMITTED'
            """)
    List<Report> findByTopicIdAndStatus(@Param("topicId") Long topicId, @Param("status") String status);

    /** Danh sách báo cáo theo bộ lọc đợt/đề tài/nhóm (mục 5.5). */
    @Query("""
            select r from Report r
            join fetch r.group g
            join fetch g.topic t
            where (:periodId is null or g.period.id = :periodId)
              and (:topicId is null or t.id = :topicId)
              and (:groupId is null or g.id = :groupId)
            order by r.submittedAt desc
            """)
    List<Report> search(@Param("periodId") Long periodId,
                        @Param("topicId") Long topicId,
                        @Param("groupId") Long groupId);

    /**
     * Báo cáo của các nhóm đang thực hiện những đề tài mà giảng viên hướng dẫn.
     * Dùng cho màn hình "Báo cáo hướng dẫn" của GVHD (mục 5.5).
     */
    @Query("""
            select r from Report r
            join fetch r.group g
            join fetch g.topic t
            where t.id in :topicIds
              and r.status = 'SUBMITTED'
            order by r.submittedAt desc
            """)
    List<Report> findByTopicIds(@Param("topicIds") List<Long> topicIds);
}