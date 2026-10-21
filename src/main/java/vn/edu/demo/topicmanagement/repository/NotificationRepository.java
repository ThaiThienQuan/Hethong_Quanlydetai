package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Thông báo đã đăng mà người dùng có vai trò phù hợp được xem (mục 5.9).
     * Thông báo không có dòng target_roles nào thì gửi cho tất cả vai trò.
     */
    @Query("""
            select distinct n from Notification n
            left join n.targetRoles r
            where n.status = :published
              and (r is null or r.code in :roleCodes)
            order by n.publishedAt desc
            """)
    List<Notification> findVisibleForRoles(@Param("roleCodes") List<String> roleCodes,
                                           @Param("published") String published);

    @Query("""
            select n from Notification n
            where (:type is null or n.type = :type)
              and (:status is null or n.status = :status)
            order by n.createdAt desc
            """)
    Page<Notification> search(@Param("type") String type,
                              @Param("status") String status,
                              Pageable pageable);

    long countByStatus(String status);
}