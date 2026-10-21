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
 * Bảng {@code councils} - hội đồng phản biện gồm 3–5 GV, đúng 1 chủ tịch và 1 thư ký.
 * Chỉ khi hội đồng hợp lệ mới chuyển sang {@link CouncilStatus#READY}.
 */
@Entity
@Table(name = "councils")
public class Council {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @Column(name = "council_code", nullable = false, unique = true, length = 30)
    private String councilCode;

    @Column(name = "council_name", nullable = false, length = 150)
    private String councilName;

    /** Với đợt KLTN lấy theo ngày báo cáo hội đồng của đợt. */
    @Column(name = "report_date")
    private LocalDateTime reportDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouncilStatus status = CouncilStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Council() {
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

    public String getCouncilCode() {
        return councilCode;
    }

    public void setCouncilCode(String councilCode) {
        this.councilCode = councilCode;
    }

    public String getCouncilName() {
        return councilName;
    }

    public void setCouncilName(String councilName) {
        this.councilName = councilName;
    }

    public LocalDateTime getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }

    public CouncilStatus getStatus() {
        return status;
    }

    public void setStatus(CouncilStatus status) {
        this.status = status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}