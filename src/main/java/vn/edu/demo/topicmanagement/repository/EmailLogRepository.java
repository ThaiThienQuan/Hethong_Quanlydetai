package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.EmailLog;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    /**
     * UNIQUE (result_id, student_id) - tìm bản ghi để cập nhật khi gửi lại,
     * tránh tạo bản ghi trùng.
     */
    Optional<EmailLog> findByResultIdAndStudentId(Long resultId, Long studentId);

    List<EmailLog> findByResultId(Long resultId);

    List<EmailLog> findByStatus(String status);

    /** Email lỗi - Trưởng khoa bấm "Gửi lại" (mục 7.10). */
    @Query("""
            select e from EmailLog e
            where e.status = :failed
            order by e.createdAt desc
            """)
    List<EmailLog> findFailed(@Param("failed") String failed);

    @Query("""
            select e from EmailLog e
            join fetch e.result r
            join fetch r.group g
            where (:status is null or e.status = :status)
              and (:periodId is null or g.period.id = :periodId)
            order by e.createdAt desc
            """)
    Page<EmailLog> search(@Param("status") String status,
                          @Param("periodId") Long periodId,
                          Pageable pageable);

    long countByStatus(String status);
}