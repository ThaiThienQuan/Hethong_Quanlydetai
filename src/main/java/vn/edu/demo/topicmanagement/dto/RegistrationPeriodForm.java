package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import vn.edu.demo.topicmanagement.entity.PeriodType;

/** Dữ liệu tạo một đợt đăng ký. */
public class RegistrationPeriodForm {

    @NotBlank(message = "Vui lòng nhập tên đợt")
    @Size(max = 255, message = "Tên đợt tối đa 255 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn loại đợt")
    private PeriodType type;

    @NotNull(message = "Vui lòng chọn thời gian bắt đầu của giảng viên")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime lecturerStart;
    @NotNull(message = "Vui lòng chọn thời gian kết thúc của giảng viên")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime lecturerEnd;
    @NotNull(message = "Vui lòng chọn thời gian bắt đầu của sinh viên")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime studentStart;
    @NotNull(message = "Vui lòng chọn thời gian kết thúc của sinh viên")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime studentEnd;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime reviewDeadline;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime defenseDate;

    public String getName() { return name; }
    public PeriodType getType() { return type; }
    public LocalDateTime getLecturerStart() { return lecturerStart; }
    public LocalDateTime getLecturerEnd() { return lecturerEnd; }
    public LocalDateTime getStudentStart() { return studentStart; }
    public LocalDateTime getStudentEnd() { return studentEnd; }
    public LocalDateTime getReviewDeadline() { return reviewDeadline; }
    public LocalDateTime getDefenseDate() { return defenseDate; }

    public void setName(String name) { this.name = name; }
    public void setType(PeriodType type) { this.type = type; }
    public void setLecturerStart(LocalDateTime value) { this.lecturerStart = value; }
    public void setLecturerEnd(LocalDateTime value) { this.lecturerEnd = value; }
    public void setStudentStart(LocalDateTime value) { this.studentStart = value; }
    public void setStudentEnd(LocalDateTime value) { this.studentEnd = value; }
    public void setReviewDeadline(LocalDateTime value) { this.reviewDeadline = value; }
    public void setDefenseDate(LocalDateTime value) { this.defenseDate = value; }
}
