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
@Table(name = "dot_dang_ky")
@Getter @Setter @NoArgsConstructor
public class RegistrationPeriod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false, length = 200)
    private String name;

    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RegistrationType type;

    @NotNull @Column(nullable = false)
    private LocalDateTime teacherStart;
    @NotNull @Column(nullable = false)
    private LocalDateTime teacherEnd;
    @NotNull @Column(nullable = false)
    private LocalDateTime studentStart;
    @NotNull @Column(nullable = false)
    private LocalDateTime studentEnd;

    private LocalDateTime reviewerDeadline;
    private LocalDateTime defenseDate;

    @AssertTrue(message = "Thời gian đăng ký phải hợp lệ")
    public boolean isScheduleValid() {
        return teacherStart == null || teacherEnd == null || studentStart == null || studentEnd == null
                || !teacherStart.isAfter(teacherEnd) && !studentStart.isAfter(studentEnd);
    }

    @AssertTrue(message = "Hạn phản biện chỉ áp dụng cho TLCN hoặc KLTN")
    public boolean isReviewerDeadlineValid() {
        return reviewerDeadline == null || type == RegistrationType.TLCN || type == RegistrationType.KLTN;
    }

    @AssertTrue(message = "Ngày báo cáo hội đồng chỉ áp dụng cho KLTN")
    public boolean isDefenseDateValid() {
        return defenseDate == null || type == RegistrationType.KLTN;
    }
}