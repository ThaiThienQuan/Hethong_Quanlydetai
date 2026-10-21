package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicStatus;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByTopicCode(String topicCode);

    boolean existsByTopicCode(String topicCode);

    /** Mã đề tài đã có thuộc đề tài khác không - dùng khi GV sửa đề tài. */
    boolean existsByTopicCodeAndIdNot(String topicCode, Long id);

    long countByStatus(TopicStatus status);

    List<Topic> findByStatus(TopicStatus status);

    /** Số đề tài thuộc một đợt - dùng chặn sửa/xóa đợt đã có đề tài (mục 5.2). */
    long countByPeriodId(Long periodId);

    /**
     * Danh sách đề tài đã công bố của một đợt - nguồn để sinh viên đăng ký ở giai đoạn 2.
     * Dùng fetch join để tránh N+1 khi hiển thị tên GVHD và bộ môn.
     */
    @Query("""
            select distinct t from Topic t
            left join fetch t.period p
            left join fetch t.department d
            left join fetch t.createdByLecturer l
            left join fetch t.supervisors s
            left join fetch s.lecturer sl
            where t.period.id = :periodId
              and t.status = :status
              and t.published = true
            order by t.topicCode
            """)
    List<Topic> findPublishedByPeriod(@Param("periodId") Long periodId,
                                      @Param("status") TopicStatus status);

    /** Đề tài đã công bố mà chưa có nhóm được chấp thuận - dùng cho danh sách chọn. */
    @Query("""
            select distinct t from Topic t
            where t.period.id = :periodId
              and t.status = :status
              and t.published = true
              and not exists (
                  select 1 from TopicRegistration r
                  where r.topic = t and r.status = :approved)
            order by t.topicCode
            """)
    List<Topic> findPublishedWithoutApprovedGroup(@Param("periodId") Long periodId,
                                                  @Param("status") TopicStatus status,
                                                  @Param("approved") RegistrationStatus approved);

    @Query("""
            select t from Topic t
            left join fetch t.period p
            left join fetch t.department d
            left join fetch t.createdByLecturer l
            where l.user.id = :lecturerUserId
            order by t.createdAt desc
            """)
    List<Topic> findByCreatorUserId(@Param("lecturerUserId") Long lecturerUserId);

    /** Hàng đợi đề tài chờ Trưởng khoa duyệt, lọc theo khoảng trạng thái. */
    @Query("""
            select t from Topic t
            left join fetch t.department d
            left join fetch t.createdByLecturer l
            where (:status is null or t.status = :status)
              and (:departmentId is null or t.department.id = :departmentId)
              and (:periodId is null or t.period.id = :periodId)
            order by t.createdAt desc
            """)
    Page<Topic> searchForApproval(@Param("status") TopicStatus status,
                                  @Param("departmentId") Long departmentId,
                                  @Param("periodId") Long periodId,
                                  Pageable pageable);

    /** Danh sách đề tài theo bộ môn (mục 5.3). */
    @Query("""
            select t from Topic t
            left join fetch t.period p
            left join fetch t.supervisors s
            left join fetch s.lecturer sl
            where (:departmentId is null or t.department.id = :departmentId)
              and (:status is null or t.status = :status)
            order by t.topicCode
            """)
    Page<Topic> searchByDepartment(@Param("departmentId") Long departmentId,
                                  @Param("status") TopicStatus status,
                                  Pageable pageable);

    /** Có nhóm nào đang đăng ký đề tài này không - dùng để chặn xóa. */
    @Query("""
            select count(r) from TopicRegistration r where r.topic.id = :topicId
            """)
    long countRegistrationsByTopic(@Param("topicId") Long topicId);
}