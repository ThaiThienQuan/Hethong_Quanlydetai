package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Bảng {@code audit_logs} - nhật ký các hành động quan trọng: tạo/duyệt đề tài, chấp thuận
 * đăng ký, phân công, nhập điểm, tổng hợp, công bố, đổi quyền, khóa tài khoản.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Ví dụ: TOPIC_APPROVE, REGISTRATION_CONFIRM, RESULT_PUBLISH. */
    @Column(nullable = false, length = 60)
    private String action;

    /** Tên đối tượng bị tác động: Topic, Registration, Council... */
    @Column(nullable = false, length = 60)
    private String entity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 1000)
    private String detail;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(User user, String action, String entity, Long entityId, String detail) {
        this.user = user;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getAction() {
        return action;
    }

    public String getEntity() {
        return entity;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}