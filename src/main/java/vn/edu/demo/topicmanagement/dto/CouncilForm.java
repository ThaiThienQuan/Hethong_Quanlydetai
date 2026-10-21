package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/** Dữ liệu tạo hội đồng cho một đợt TLCN/KLTN. */
public class CouncilForm {
    @NotBlank(message = "Vui lòng nhập mã hội đồng")
    @Size(max = 30, message = "Mã hội đồng tối đa 30 ký tự")
    private String councilCode;

    @NotBlank(message = "Vui lòng nhập tên hội đồng")
    @Size(max = 150, message = "Tên hội đồng tối đa 150 ký tự")
    private String councilName;

    @NotNull(message = "Vui lòng chọn đợt đăng ký")
    private Long periodId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime reportDate;

    public String getCouncilCode() { return councilCode; }
    public String getCouncilName() { return councilName; }
    public Long getPeriodId() { return periodId; }
    public LocalDateTime getReportDate() { return reportDate; }

    public void setCouncilCode(String councilCode) { this.councilCode = councilCode; }
    public void setCouncilName(String councilName) { this.councilName = councilName; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }

    // Tương thích code cũ dùng getName/setName, getDefenseDate/setDefenseDate
    public String getName() { return councilName; }
    public void setName(String name) { this.councilName = name; }
    public LocalDateTime getDefenseDate() { return reportDate; }
    public void setDefenseDate(LocalDateTime d) { this.reportDate = d; }
    public String getRoom() { return null; }
}
