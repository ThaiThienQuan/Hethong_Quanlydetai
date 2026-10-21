package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityAndEntityIdOrderByCreatedAtDesc(String entity, Long entityId);

    @Query("""
            select a from AuditLog a
            left join fetch a.user u
            order by a.createdAt desc
            """)
    Page<AuditLog> findRecent(Pageable pageable);

    @Query("""
            select a from AuditLog a
            left join fetch a.user u
            where (:action is null or a.action = :action)
              and (:entity is null or a.entity = :entity)
            order by a.createdAt desc
            """)
    Page<AuditLog> search(@Param("action") String action, @Param("entity") String entity, Pageable pageable);
}