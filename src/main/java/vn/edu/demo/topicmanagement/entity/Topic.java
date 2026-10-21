package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** Bảng {@code topics} - đề tài do GV đề xuất, Trưởng khoa duyệt rồi công bố cho SV. */
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    /** Giảng viên đề xuất đề tài. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_lecturer_id", nullable = false)
    private Lecturer createdByLecturer;

    @Column(name = "topic_code", nullable = false, unique = true, length = 30)
    private String topicCode;

    @Column(name = "topic_name", nullable = false, length = 200)
    private String topicName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TopicStatus status = TopicStatus.PENDING;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** Người duyệt đề tài (Trưởng khoa / admin). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Đề tài APPROVED đã công bố để SV thấy ở giai đoạn 2. */
    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("supervisorOrder asc")
    private Set<TopicSupervisor> supervisors = new LinkedHashSet<>();

    public Topic() {
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Lecturer getCreatedByLecturer() {
        return createdByLecturer;
    }

    public void setCreatedByLecturer(Lecturer createdByLecturer) {
        this.createdByLecturer = createdByLecturer;
    }

    public String getTopicCode() {
        return topicCode;
    }

    public void setTopicCode(String topicCode) {
        this.topicCode = topicCode;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TopicStatus getStatus() {
        return status;
    }

    public void setStatus(TopicStatus status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<TopicSupervisor> getSupervisors() {
        return supervisors;
    }

    public void setSupervisors(Set<TopicSupervisor> supervisors) {
        this.supervisors = supervisors;
    }
}