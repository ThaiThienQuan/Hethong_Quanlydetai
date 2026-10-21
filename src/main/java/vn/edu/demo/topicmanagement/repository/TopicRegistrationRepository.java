package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.StudentGroup;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {

    /** Đăng ký còn hiệu lực (PENDING hoặc APPROVED) của một nhóm. */
    Optional<TopicRegistration> findByGroupIdAndStatusIn(Long groupId, List<RegistrationStatus> statuses);

    List<TopicRegistration> findByGroupId(Long groupId);

    List<TopicRegistration> findByTopicId(Long topicId);

    /** Các đăng ký PENDING của đề tài - sẽ bị từ chối khi có nhóm được chấp thuận. */
    List<TopicRegistration> findByTopicIdAndStatus(Long topicId, RegistrationStatus status);

    /** Tất cả đăng ký của một đề tài theo các trạng thái cho trước. */
    List<TopicRegistration> findByTopicIdAndStatusIn(Long topicId, List<RegistrationStatus> statuses);

    /**
     * Đăng ký được chấp thuận của đề tài - nguồn để gán nhóm khi duyệt.
     */
    @Query("""
            select r from TopicRegistration r
            join fetch r.group g
            join fetch g.period
            where r.status = :status
            order by r.registeredAt asc
            """)
    List<TopicRegistration> findByStatusWithGroup(@Param("status") RegistrationStatus status);

    /** Hàng đợi chờ xác nhận của giảng viên hướng dẫn (cả hai GVHD đều có quyền). */
    @Query("""
            select r from TopicRegistration r
            join fetch r.group g
            join fetch r.topic t
            where r.status = :status
              and exists (select 1 from TopicSupervisor ts
                          where ts.topic = t and ts.lecturer.user.id = :lecturerUserId)
            order by r.registeredAt asc
            """)
    List<TopicRegistration> findPendingForSupervisor(@Param("status") RegistrationStatus status,
                                                     @Param("lecturerUserId") Long lecturerUserId);

    /**
     * Người đang đăng nhập có phải GVHD của đề tài trong đăng ký này không.
     * Khoá chặn cuối để GV không GVHD không thể chấp thuận/từ chối nhầm đề tài người khác.
     */
    @Query("""
            select case when count(ts) > 0 then true else false end
            from TopicRegistration r
              join r.topic t
              join TopicSupervisor ts on ts.topic = t
            where r.id = :registrationId
              and ts.lecturer.user.username = :username
            """)
    boolean existsPendingForSupervisor(@Param("registrationId") Long registrationId,
                                       @Param("username") String username);

    /** Hàng đợi chờ xác nhận mà Trưởng khoa thấy. */
    @Query("""
            select r from TopicRegistration r
            join fetch r.group g
            join fetch r.topic t
            where r.status = :status
            order by r.registeredAt asc
            """)
    List<TopicRegistration> findAllPendingWithDetails(@Param("status") RegistrationStatus status);

    /** Đăng ký của một nhóm, dùng để khôi phục trạng thái sau khi bị từ chối. */
    Optional<TopicRegistration> findFirstByGroupAndStatusOrderByIdDesc(StudentGroup group, RegistrationStatus status);
}