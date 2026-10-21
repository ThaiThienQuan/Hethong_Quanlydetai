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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Bảng {@code review_assignments} - phân công giảng viên chấm (GVPB) một đề tài của hội đồng.
 *
 * <p>CSDL bảo đảm người chấm phải là thành viên hội đồng và đề tài phải thuộc đúng hội đồng đó
 * (hai khóa ngoại kép). Quy tắc "người chấm không phải GVHD của đề tài" kiểm tra ở service.
 */
@Entity
@Table(name = "review_assignments", uniqueConstraints =
        @UniqueConstraint(name = "uq_assignments_unique", columnNames = {"council_id", "topic_id", "lecturer_id"}))
public class ReviewAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Column(name = "assigned_at", insertable = false, updatable = false)
    private LocalDateTime assignedAt;

    /** ASSIGNED / COMPLETED. */
    @Column(nullable = false, length = 20)
    private String status = "ASSIGNED";

    public ReviewAssignment() {
    }

    public ReviewAssignment(Council council, Topic topic, Lecturer lecturer, User assignedBy) {
        this.council = council;
        this.topic = topic;
        this.lecturer = lecturer;
        this.assignedBy = assignedBy;
    }

    public Long getId() {
        return id;
    }

    public Council getCouncil() {
        return council;
    }

    public void setCouncil(Council council) {
        this.council = council;
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

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}