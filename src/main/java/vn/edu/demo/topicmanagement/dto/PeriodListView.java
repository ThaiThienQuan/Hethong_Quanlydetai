package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.PeriodType;

/**
 * Bản trình bày một đợt đăng ký cho màn hình "Quản lý đợt đăng ký" (mục 5.2).
 *
 * <p>Khác với entity {@code RegistrationPeriod}, DTO này bổ sung hai thông tin
 * mà bảng giao diện cần nhưng entity không biết:
 * <ul>
 *   <li>{@code councilDate} - ngày hội đồng đã được quyết định. Cột "Ngày hội đồng"
 *       trước đây chỉ đọc {@code period.councilReportDate}, nên đợt TLCN (vốn không
 *       được phép có {@code council_report_date} do ràng buộc CSDL
 *       {@code chk_period_council_type}) luôn hiện "—" dù đợt đã có hội đồng phản biện.
 *       Nay ngày đó được lấy theo thứ tự ưu tiên: ngày của đợt (KLTN), rồi ngày báo
 *       cáo của các hội đồng thuộc đợt (TLCN).</li>
 *   <li>{@code reopenBlockedReason} - lý do cụ thể vì sao không mở lại được đợt đã
 *       đóng, để giao diện hiện ngay thay vì để người dùng bấm rồi mới thấy lỗi.</li>
 * </ul>
 */
public class PeriodListView {

    private final Long id;
    private final String name;
    private final PeriodType periodType;
    private final LocalDateTime lecturerStartAt;
    private final LocalDateTime lecturerEndAt;
    private final LocalDateTime studentStartAt;
    private final LocalDateTime studentEndAt;
    private final LocalDateTime reviewDeadline;
    private final LocalDateTime councilDate;
    private final PeriodStatus status;
    private final String currentPhase;
    private final String currentPhaseLabel;
    private final long councilCount;
    private final String reopenBlockedReason;

    public PeriodListView(Long id, String name, PeriodType periodType,
                           LocalDateTime lecturerStartAt, LocalDateTime lecturerEndAt,
                           LocalDateTime studentStartAt, LocalDateTime studentEndAt,
                           LocalDateTime reviewDeadline, LocalDateTime councilDate,
                           PeriodStatus status, String currentPhase, String currentPhaseLabel,
                           long councilCount, String reopenBlockedReason) {
        this.id = id;
        this.name = name;
        this.periodType = periodType;
        this.lecturerStartAt = lecturerStartAt;
        this.lecturerEndAt = lecturerEndAt;
        this.studentStartAt = studentStartAt;
        this.studentEndAt = studentEndAt;
        this.reviewDeadline = reviewDeadline;
        this.councilDate = councilDate;
        this.status = status;
        this.currentPhase = currentPhase;
        this.currentPhaseLabel = currentPhaseLabel;
        this.councilCount = councilCount;
        this.reopenBlockedReason = reopenBlockedReason;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public PeriodType getPeriodType() { return periodType; }
    public LocalDateTime getLecturerStartAt() { return lecturerStartAt; }
    public LocalDateTime getLecturerEndAt() { return lecturerEndAt; }
    public LocalDateTime getStudentStartAt() { return studentStartAt; }
    public LocalDateTime getStudentEndAt() { return studentEndAt; }
    public LocalDateTime getReviewDeadline() { return reviewDeadline; }

    /** Ngày hội đồng đã quyết định (null nếu chưa có) - dùng cho cột "Ngày hội đồng". */
    public LocalDateTime getCouncilDate() { return councilDate; }

    public PeriodStatus getStatus() { return status; }
    public String getCurrentPhase() { return currentPhase; }
    public String getCurrentPhaseLabel() { return currentPhaseLabel; }

    /** Số hội đồng phản biện đã lập cho đợt này. */
    public long getCouncilCount() { return councilCount; }

    /** Null nghĩa là có thể mở lại; ngược lại là lý do cụ thể không mở lại được. */
    public String getReopenBlockedReason() { return reopenBlockedReason; }

    public boolean isReopenable() { return reopenBlockedReason == null; }
}
