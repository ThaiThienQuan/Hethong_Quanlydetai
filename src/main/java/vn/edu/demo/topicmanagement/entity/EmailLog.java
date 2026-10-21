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
 * Bảng {@code email_logs} - nhật ký gửi email kết quả cho từng sinh viên trong nhóm.
 * {@code UNIQUE (result_id, student_id)} bảo đảm mỗi kết quả chỉ gửi đúng một email cho mỗi SV;
 * bấm "Gửi lại" sẽ cập nhật dòng đó thay vì tạo dòng mới.
 */
@Entity
@Table(name = "email_logs", uniqueConstraints =
        @UniqueConstraint(name = "uq_email_logs_result_student", columnNames = {"result_id", "student_id"}))
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private ReviewResult result;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "to_email", nullable = false, length = 100)
    private String toEmail;

    @Column(nullable = false, length = 250)
    private String subject;

    /** SENT / FAILED / SKIPPED. */
    @Column(nullable = false, length = 20)
    private String status = "FAILED";

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    protected EmailLog() {
    }

    public EmailLog(ReviewResult result, Student student, String toEmail, String subject) {
        this.result = result;
        this.student = student;
        this.toEmail = toEmail;
        this.subject = subject;
    }

    public Long getId() {
        return id;
    }

    public ReviewResult getResult() {
        return result;
    }

    public void setResult(ReviewResult result) {
        this.result = result;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}