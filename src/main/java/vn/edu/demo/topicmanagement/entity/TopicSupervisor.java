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
 * Bảng {@code topic_supervisors}: mỗi dòng là một giảng viên hướng dẫn (GVHD) của một đề tài.
 * Quy tắc "mỗi đề tài có 1–2 GVHD" kiểm tra ở service (không ép được bằng CHECK).
 */
@Entity
@Table(name = "topic_supervisors")
public class TopicSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    /** 1 = GVHD chính (người đề xuất), 2 = GVHD thứ hai. */
    @Column(name = "supervisor_order", nullable = false)
    private int supervisorOrder;

    /** Trưởng khoa phân công/điều chỉnh GVHD (nếu có). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "assigned_at", insertable = false, updatable = false)
    private LocalDateTime assignedAt;

    public TopicSupervisor() {
    }

    public TopicSupervisor(Topic topic, Lecturer lecturer, int supervisorOrder) {
        this.topic = topic;
        this.lecturer = lecturer;
        this.supervisorOrder = supervisorOrder;
    }

    public Long getId() {
        return id;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Lecturer getLecturer() {
        return lecturer;
    }

    public void setLecturer(Lecturer lecturer) {
        this.lecturer = lecturer;
    }

    public int getSupervisorOrder() {
        return supervisorOrder;
    }

    public void setSupervisorOrder(int supervisorOrder) {
        this.supervisorOrder = supervisorOrder;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
}