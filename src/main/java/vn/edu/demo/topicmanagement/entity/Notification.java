package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Bảng {@code notifications} - thông báo chính thức của Nhà trường và Khoa.
 * Người dùng chỉ thấy thông báo đã đăng dành cho vai trò của mình.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** SCHOOL (Nhà trường) / FACULTY (Khoa). */
    @Column(nullable = false, length = 20)
    private String type = "FACULTY";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /** DRAFT / PUBLISHED / RETRACTED. */
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at", insertable = false, updatable = false)
    private LocalDateTime publishedAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /**
     * Bảng {@code notification_target_roles} giới hạn người xem theo vai trò.
     * Không có dòng nào = gửi cho tất cả vai trò.
     */
    @ManyToMany(fetch = jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.JoinTable(name = "notification_target_roles",
            joinColumns = @JoinColumn(name = "notification_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> targetRoles = new LinkedHashSet<>();

    public Notification() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<Role> getTargetRoles() {
        return targetRoles;
    }

    public void setTargetRoles(Set<Role> targetRoles) {
        this.targetRoles = targetRoles;
    }

    /** Đã đăng và chưa bị thu hồi. */
    public boolean isVisible() {
        return "PUBLISHED".equals(status);
    }
}