package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng {@code evaluations} - điểm và nhận xét của một giảng viên được phân công chấm.
 * Mỗi phân công chỉ có một bản chấm ({@code UNIQUE (assignment_id)}).
 */
@Entity
@Table(name = "evaluations", uniqueConstraints =
        @UniqueConstraint(name = "uq_evaluations_assignment", columnNames = "assignment_id"))
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ReviewAssignment assignment;

    /** Thang điểm 0–10, tối đa 2 chữ số thập phân. */
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, length = 1000)
    private String comment;

    @Column(name = "evaluated_at", insertable = false, updatable = false)
    private LocalDateTime evaluatedAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** DRAFT / SUBMITTED. */
    @Column(nullable = false, length = 20)
    private String status = "SUBMITTED";

    public Evaluation() {
    }

    public Evaluation(ReviewAssignment assignment, BigDecimal score, String comment) {
        this.assignment = assignment;
        this.score = score;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public ReviewAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(ReviewAssignment assignment) {
        this.assignment = assignment;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}