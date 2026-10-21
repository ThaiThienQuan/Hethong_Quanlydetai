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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng {@code review_results} - kết quả phản biện của một nhóm/đề tài.
 *
 * <p>{@code finalScore} là trung bình cộng các điểm thành phần. Khi {@code published = true}
 * thì kết quả bị khóa và sinh viên mới được xem (mục 5.8).
 */
@Entity
@Table(name = "review_results")
public class ReviewResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    /** Trung bình cộng các điểm thành phần, làm tròn 2 chữ số. */
    @Column(name = "final_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal finalScore;

    /** PASSED / FAILED. */
    @Column(nullable = false, length = 20)
    private String result;

    @Column(name = "summary_comment", length = 2000)
    private String summaryComment;

    /** Người tổng hợp - chủ tịch của đúng hội đồng đó. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculated_by", nullable = false)
    private User calculatedBy;

    @Column(name = "calculated_at", insertable = false, updatable = false)
    private LocalDateTime calculatedAt;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by")
    private User publishedBy;

    public ReviewResult() {
    }

    public Long getId() {
        return id;
    }

    public StudentGroup getGroup() {
        return group;
    }

    public void setGroup(StudentGroup group) {
        this.group = group;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Council getCouncil() {
        return council;
    }

    public void setCouncil(Council council) {
        this.council = council;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getSummaryComment() {
        return summaryComment;
    }

    public void setSummaryComment(String summaryComment) {
        this.summaryComment = summaryComment;
    }

    public User getCalculatedBy() {
        return calculatedBy;
    }

    public void setCalculatedBy(User calculatedBy) {
        this.calculatedBy = calculatedBy;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public User getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(User publishedBy) {
        this.publishedBy = publishedBy;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }
}