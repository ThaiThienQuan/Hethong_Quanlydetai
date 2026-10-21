package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Bảng {@code student_groups}. Quy tắc số thành viên tối thiểu/tối đa đọc từ cấu hình
 * {@code app.group.min-size} / {@code app.group.max-size} và kiểm tra ở service.
 */
@Entity
@Table(name = "student_groups")
public class StudentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @Column(name = "group_code", nullable = false, unique = true, length = 30)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    /** Đề tài nhóm đang thực hiện - được gán khi đăng ký được chấp thuận. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupStatus status = GroupStatus.DRAFT;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public StudentGroup() {
    }

    public Long getId() {
        return id;
    }

    public RegistrationPeriod getPeriod() {
        return period;
    }

    public void setPeriod(RegistrationPeriod period) {
        this.period = period;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public void setStatus(GroupStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}