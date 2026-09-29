package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_periods")
@Getter
@Setter
@NoArgsConstructor
public class RegistrationPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "period_id")
    private Long id;

    @NotBlank
    @Column(name = "period_name", nullable = false, length = 150)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private RegistrationType type;

    @NotNull
    @Column(name = "lecturer_start_at", nullable = false)
    private LocalDateTime teacherStart;
    @NotNull
    @Column(name = "lecturer_end_at", nullable = false)
    private LocalDateTime teacherEnd;
    @NotNull
    @Column(name = "student_start_at", nullable = false)
    private LocalDateTime studentStart;
    @NotNull
    @Column(name = "student_end_at", nullable = false)
    private LocalDateTime studentEnd;

    @Column(name = "review_deadline")
    private LocalDateTime reviewerDeadline;

    @Column(name = "council_report_date")
    private LocalDateTime defenseDate;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    public String getStatusLabel() {
        if ("DRAFT".equalsIgnoreCase(status)) {
            return "Bản nháp";
        }
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            return "Đang mở";
        }
        if ("CLOSED".equalsIgnoreCase(status)) {
            return "Đã đóng";
        }
        return status == null ? "" : status;
    }

    @AssertTrue(message = "Thời gian đăng ký không được để trống và không được để quá sát giờ nhau")
    public boolean isScheduleValid() {
        return teacherStart == null || teacherEnd == null || studentStart == null || studentEnd == null ||teacherStart==teacherEnd|| studentStart==studentEnd
                || (!teacherStart.isAfter(teacherEnd) && !studentStart.isAfter(studentEnd)
                && !studentStart.isBefore(teacherEnd));
    }

    @AssertTrue(message = "Hạn phản biện chỉ áp dụng cho Tiểu Luận Chuyên " +
            "Ngành" +
            " hoặc" +
            " Khóa " +
            "Luận " +
            "Tốt Nghiệp")
    public boolean isReviewerDeadlineValid() {
        return reviewerDeadline == null || type == RegistrationType.TLCN || type == RegistrationType.KLTN;
    }

    @AssertTrue(message = "Ngày báo cáo hội đồng chỉ áp dụng cho Khóa Luận Tốt Nghiệp")
    public boolean isDefenseDateValid() {
        return defenseDate == null || type == RegistrationType.KLTN;
    }
}