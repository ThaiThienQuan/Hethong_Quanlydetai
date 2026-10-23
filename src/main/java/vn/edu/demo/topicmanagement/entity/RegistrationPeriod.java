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
 * Bảng {@code registration_periods} - đợt đăng ký gồm hai giai đoạn riêng biệt:
 * GĐ1 GV đề xuất đề tài, GĐ2 nhóm SV đăng ký thực hiện.
 */
@Entity
@Table(name = "registration_periods")
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    /** MON_HOC / NCKH / TLCN / KLTN. */
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    @Column(name = "lecturer_start_at", nullable = false)
    private LocalDateTime lecturerStartAt;

    @Column(name = "lecturer_end_at", nullable = false)
    private LocalDateTime lecturerEndAt;

    @Column(name = "student_start_at", nullable = false)
    private LocalDateTime studentStartAt;

    @Column(name = "student_end_at", nullable = false)
    private LocalDateTime studentEndAt;

    /** Hạn chót GVPB nộp điểm về Khoa - chỉ thiết lập khi loại là TLCN hoặc KLTN. */
    @Column(name = "review_deadline")
    private LocalDateTime reviewDeadline;

    /** Ngày báo cáo hội đồng - chỉ thiết lập khi loại là KLTN. */
    @Column(name = "council_report_date")
    private LocalDateTime councilReportDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PeriodStatus status = PeriodStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    /** JPA bắt buộc. */
    public RegistrationPeriod() {
    }

    /** Tạo entity đã set id — dùng cho unit test không cần persistence. */
    public static RegistrationPeriod withId(Long id) {
        RegistrationPeriod p = new RegistrationPeriod();
        p.setId(id);
        return p;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public LocalDateTime getLecturerStartAt() {
        return lecturerStartAt;
    }

    public void setLecturerStartAt(LocalDateTime lecturerStartAt) {
        this.lecturerStartAt = lecturerStartAt;
    }

    public LocalDateTime getLecturerEndAt() {
        return lecturerEndAt;
    }

    public void setLecturerEndAt(LocalDateTime lecturerEndAt) {
        this.lecturerEndAt = lecturerEndAt;
    }

    public LocalDateTime getStudentStartAt() {
        return studentStartAt;
    }

    public void setStudentStartAt(LocalDateTime studentStartAt) {
        this.studentStartAt = studentStartAt;
    }

    public LocalDateTime getStudentEndAt() {
        return studentEndAt;
    }

    public void setStudentEndAt(LocalDateTime studentEndAt) {
        this.studentEndAt = studentEndAt;
    }

    public LocalDateTime getReviewDeadline() {
        return reviewDeadline;
    }

    public void setReviewDeadline(LocalDateTime reviewDeadline) {
        this.reviewDeadline = reviewDeadline;
    }

    public LocalDateTime getCouncilReportDate() {
        return councilReportDate;
    }

    public void setCouncilReportDate(LocalDateTime councilReportDate) {
        this.councilReportDate = councilReportDate;
    }

    public PeriodStatus getStatus() {
        return status;
    }

    public void setStatus(PeriodStatus status) {
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Giai đoạn hiện tại tính theo giờ máy chủ: CHUA_MO / GIAO_VIEN_DANG_KY /
     * SINH_VIEN_DANG_KY / DA_DONG. Dùng cho màn hình danh sách và thanh tiến trình.
     *
     * <p>Lưu ý: bảng quản lý đợt dùng {@code RegistrationPeriodService.findAllForList()}
     * nên giai đoạn hiện tại được tính theo {@code Clock} đã tiêm (kiểm thử kiểm soát
     * được thời gian). Các màn hình khác gọi trực tiếp các hàm này nên vẫn dùng
     * giờ hệ thống.
     */
    public String currentPhase() {
        return currentPhase(LocalDateTime.now());
    }

    /** Giai đoạn hiện tại tại một mốc thời gian cho trước - dùng để kiểm thử. */
    public String currentPhase(LocalDateTime now) {
        if (status == PeriodStatus.CLOSED) {
            return "DA_DONG";
        }
        if (status == PeriodStatus.DRAFT) {
            return "CHUA_MO";
        }
        if (now.isBefore(lecturerStartAt)) {
            return "CHUA_MO";
        }
        if (now.isBefore(lecturerEndAt)) {
            return "GIAO_VIEN_DANG_KY";
        }
        if (now.isBefore(studentEndAt)) {
            return "SINH_VIEN_DANG_KY";
        }
        return "DA_DONG";
    }

    /**
     * Giai đoạn hiện tại ở dạng nhãn tiếng Việt, dùng chung cho bảng "đợt đang mở"
     * ở dashboard và bảng quản lý đợt (mục 5.2).
     */
    public String getCurrentPhaseLabel() {
        return switch (currentPhase()) {
            case "CHUA_MO" -> "Chưa mở";
            case "GIAO_VIEN_DANG_KY" -> "Giảng viên đăng ký";
            case "SINH_VIEN_DANG_KY" -> "Sinh viên đăng ký";
            default -> "Đã đóng";
        };
    }

    /** GV chỉ đề xuất đề tài được trong đúng khung thời gian giai đoạn 1. */
    public boolean isLecturerPhaseOpen() {
        return status == PeriodStatus.PUBLISHED
                && !LocalDateTime.now().isBefore(lecturerStartAt)
                && LocalDateTime.now().isBefore(lecturerEndAt);
    }

    /** SV chỉ lập nhóm / đăng ký được trong đúng khung thời gian giai đoạn 2. */
    public boolean isStudentPhaseOpen() {
        return status == PeriodStatus.PUBLISHED
                && !LocalDateTime.now().isBefore(studentStartAt)
                && LocalDateTime.now().isBefore(studentEndAt);
    }
}