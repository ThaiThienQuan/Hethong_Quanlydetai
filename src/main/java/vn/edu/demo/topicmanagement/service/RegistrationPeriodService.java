package vn.edu.demo.topicmanagement.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.dto.PeriodListView;
import vn.edu.demo.topicmanagement.dto.RegistrationPeriodForm;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.PeriodType;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.TopicRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Nghiệp vụ đợt đăng ký (mục 5.2 của đề bài).
 *
 * <p>Các quy tắc được kiểm tra ở đây, không chỉ dựa vào ràng buộc CSDL:
 * <ul>
 *   <li>bắt đầu &lt; kết thúc (mỗi giai đoạn);</li>
 *   <li>giai đoạn SV bắt đầu <b>sau</b> khi giai đoạn GV kết thúc;</li>
 *   <li>hạn chót GVPB phải sau ngày kết thúc đăng ký của SV;</li>
 *   <li>ngày báo cáo hội đồng phải sau hạn chót GVPB;</li>
 *   <li>hạn chót GVPB chỉ có ở TLCN/KLTN, ngày hội đồng chỉ có ở KLTN;</li>
 *   <li>chỉ sửa/xóa khi chưa có đề tài; {@code PUBLISHED} chỉ được sửa mốc thời gian;</li>
 *   <li>công bố {@code DRAFT -> PUBLISHED}, đóng {@code -> CLOSED};</li>
 *   <li>chỉ mở đăng ký đúng thời gian quy định, dựa trên giờ máy chủ.</li>
 * </ul>
 *
 * <p>{@code Clock} được tiêm vào để test kiểm soát thời gian, và để sản phẩm dùng
 * giờ máy chủ thực. Khi cần lấy thời gian hiện tại, luôn dùng {@code now()}.
 */
@Service
public class RegistrationPeriodService {

private final RegistrationPeriodRepository periodRepository;
    private final TopicRepository topicRepository;
    private final CouncilRepository councilRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * Constructor cho production: dùng giờ máy chủ mặc định.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public RegistrationPeriodService(RegistrationPeriodRepository periodRepository,
                                     TopicRepository topicRepository,
                                     CouncilRepository councilRepository,
                                     UserRepository userRepository) {
        this(periodRepository, topicRepository, councilRepository, userRepository, Clock.systemDefaultZone());
    }

    /**
     * Constructor cho test: tiêm Clock để kiểm soát thời gian.
     */
    public RegistrationPeriodService(RegistrationPeriodRepository periodRepository,
                                     TopicRepository topicRepository,
                                     CouncilRepository councilRepository,
                                     UserRepository userRepository,
                                     Clock clock) {
        this.periodRepository = periodRepository;
        this.topicRepository = topicRepository;
        this.councilRepository = councilRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** Lấy giờ hiện tại qua Clock — luôn dùng phương thức này, không gọi LocalDateTime.now() trực tiếp. */
    LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    // =================================================================
    // Truy vấn
    // =================================================================

    public List<RegistrationPeriod> findAll() {
        return periodRepository.findAllByOrderByIdDesc();
    }

    /**
     * Danh sách đợt cho màn hình quản lý đợt (mục 5.2), đã bổ sung ngày hội đồng và
     * lý do không mở lại được.
     *
     * <p>Ngày hội đồng lấy theo thứ tự ưu tiên: {@code period.councilReportDate} (đợt
     * KLTN) rồi tới ngày báo cáo của các hội đồng thuộc đợt (đợt TLCN). Trước đây
     * cột "Ngày hội đồng" chỉ đọc {@code councilReportDate}, nên đợt TLCN đã có hội
     * đồng vẫn hiện "—" - gây hiểu nhầm là đợt chưa có hội đồng.
     */
    @Transactional(readOnly = true)
    public List<PeriodListView> findAllForList() {
        LocalDateTime current = now();
        return periodRepository.findAllByOrderByIdDesc().stream()
                .map(p -> toListView(p, current))
                .toList();
    }

    private PeriodListView toListView(RegistrationPeriod p, LocalDateTime current) {
        List<Council> councils = councilRepository.findByPeriodIdOrderByIdDesc(p.getId());
        LocalDateTime councilDate = p.getCouncilReportDate();
        if (councilDate == null) {
            // Đợt TLCN không có council_report_date (ràng buộc CSDL chỉ cho KLTN),
            // nên ngày hội đồng lấy từ hội đồng đã lập cho đợt đó.
            councilDate = councils.stream()
                    .map(Council::getReportDate)
                    .filter(java.util.Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
        }
        return new PeriodListView(
                p.getId(),
                p.getName(),
                p.getPeriodType(),
                p.getLecturerStartAt(),
                p.getLecturerEndAt(),
                p.getStudentStartAt(),
                p.getStudentEndAt(),
                p.getReviewDeadline(),
                councilDate,
                p.getStatus(),
                currentPhase(p, current),
                currentPhaseLabel(currentPhase(p, current)),
                councils.size(),
                reopenBlockedReason(p, councils, current));
    }

    /**
     * Giai đoạn hiện tại của đợt, tính theo giờ máy chủ (mục 5.2).
     *
     * <p>Trước đây entity tự tính bằng {@code LocalDateTime.now()}, không dùng
     * {@code Clock} đã tiêm - khiến việc kiểm thử không kiểm soát được thời gian.
     */
    private String currentPhase(RegistrationPeriod p, LocalDateTime current) {
        if (p.getStatus() == PeriodStatus.CLOSED) {
            return "DA_DONG";
        }
        if (p.getStatus() == PeriodStatus.DRAFT) {
            return "CHUA_MO";
        }
        if (current.isBefore(p.getLecturerStartAt())) {
            return "CHUA_MO";
        }
        if (current.isBefore(p.getLecturerEndAt())) {
            return "GIAO_VIEN_DANG_KY";
        }
        if (current.isBefore(p.getStudentEndAt())) {
            return "SINH_VIEN_DANG_KY";
        }
        return "DA_DONG";
    }

    private String currentPhaseLabel(String phase) {
        return switch (phase) {
            case "CHUA_MO" -> "Chưa mở";
            case "GIAO_VIEN_DANG_KY" -> "Giảng viên đăng ký";
            case "SINH_VIEN_DANG_KY" -> "Sinh viên đăng ký";
            default -> "Đã đóng";
        };
    }

    /**
     * Lý do cụ thể vì sao đợt đã đóng không mở lại được, hoặc null nếu mở lại được.
     *
     * <p>Trước đây giao diện luôn hiện nút "Mở lại" cho mọi đợt {@code CLOSED}, người
     * dùng bấm rồi mới nhận thông báo lỗi. Nay giao diện biết trước và hiện lý do
     * ngay trên bảng.
     */
    private String reopenBlockedReason(RegistrationPeriod p, List<Council> councils,
                                       LocalDateTime current) {
        if (p.getStatus() != PeriodStatus.CLOSED) {
            return null;
        }
        if (!councils.isEmpty()) {
            return "Đợt đã có hội đồng phản biện nên không mở lại được.";
        }
        if (!current.isBefore(p.getStudentEndAt())) {
            return "Đợt đã qua khung thời gian đăng ký của sinh viên nên không mở lại được.";
        }
        return null;
    }

    public RegistrationPeriod getById(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký #" + id + "."));
    }

    /** Các đợt GV có thể đề xuất đề tài ngay lúc này (mục 5.2). */
    public List<RegistrationPeriod> findPeriodsOpenForLecturers() {
        return periodRepository.findPeriodsOpenForLecturers(now(), PeriodStatus.PUBLISHED);
    }

    /** Các đợt SV có thể lập nhóm và đăng ký ngay lúc này. */
    public List<RegistrationPeriod> findPeriodsOpenForStudents() {
        return periodRepository.findPeriodsOpenForStudents(now(), PeriodStatus.PUBLISHED);
    }

    // =================================================================
    // Tạo / sửa
    // =================================================================

    @Transactional
    public Long create(RegistrationPeriod period) {
        validateTimes(period);
        validateOptionalDeadlines(period);
        return periodRepository.save(period).getId();
    }

    @Transactional
    public void update(RegistrationPeriod period) {
        RegistrationPeriod existing = getById(period.getId());

        // Đợt đã có đề tài thì không được sửa (mục 5.2).
        rejectIfHasTopics(period.getId());

        // Đợt PUBLISHED chỉ được sửa mốc thời gian, không đổi loại (mục 5.2).
        if (existing.getStatus() == PeriodStatus.PUBLISHED
                && existing.getPeriodType() != period.getPeriodType()) {
            throw new IllegalStateException(
                    "Đợt đã công bố chỉ được sửa mốc thời gian, không được đổi loại đợt.");
        }

        validateTimes(period);
        validateOptionalDeadlines(period);

        periodRepository.save(period);
    }

    @Transactional
    public void delete(Long id) {
        // Chỉ xóa được khi chưa có đề tài (mục 5.2).
        rejectIfHasTopics(id);
        periodRepository.deleteById(id);
    }

    // =================================================================
    // Chuyển trạng thái: công bố / đóng
    // =================================================================

    /**
     * Công bố đợt: {@code DRAFT -> PUBLISHED} (mục 5.2).
     * Không có quy tắc "mở lại đợt đã đóng" trong đề bài nên
     * {@code CLOSED} là trạng thái cuối.
     */
    @Transactional
    public void publish(Long id) {
        RegistrationPeriod period = getById(id);
        if (period.getStatus() != PeriodStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chỉ công bố được đợt ở trạng thái nháp. Trạng thái hiện tại: " + period.getStatus() + ".");
        }
        // Rà lại thời gian trước khi công bố.
        validateTimes(period);
        validateOptionalDeadlines(period);

        period.setStatus(PeriodStatus.PUBLISHED);
        periodRepository.save(period);
    }

    /**
     * Đóng đợt: {@code DRAFT|PUBLISHED -> CLOSED}.
     * Sau đó mọi thao tác đăng ký bị chặn (mục 5.2).
     */
    @Transactional
    public void close(Long id) {
        RegistrationPeriod period = getById(id);
        if (period.getStatus() == PeriodStatus.CLOSED) {
            throw new IllegalStateException("Đợt đã đóng.");
        }
        period.setStatus(PeriodStatus.CLOSED);
        periodRepository.save(period);
    }

    // =================================================================
    // Kiểm tra khung thời gian (mục 5.2)
    // =================================================================

    /**
     * GV chỉ được đề xuất đề tài trong đúng khung thời gian giai đoạn 1 (mục 5.2, 5.3).
     */
    public void requireLecturerPhaseOpen(RegistrationPeriod period) {
        if (period.getStatus() == PeriodStatus.CLOSED) {
            throw new IllegalStateException("Đợt đăng ký đã đóng, không thể đề xuất đề tài.");
        }
        if (period.getStatus() != PeriodStatus.PUBLISHED) {
            throw new IllegalStateException("Đợt đăng ký chưa được công bố nên chưa thể đề xuất đề tài.");
        }
        LocalDateTime current = now();
        if (current.isBefore(period.getLecturerStartAt())) {
            throw new IllegalStateException("Chưa tới khung thời gian giảng viên đề xuất đề tài.");
        }
        if (!current.isBefore(period.getLecturerEndAt())) {
            throw new IllegalStateException("Đã hết khung thời gian giảng viên đề xuất đề tài.");
        }
    }

    /**
     * SV chỉ được lập nhóm / đăng ký trong đúng khung thời gian giai đoạn 2 (mục 5.2, 5.4).
     */
    public void requireStudentPhaseOpen(RegistrationPeriod period) {
        if (period.getStatus() == PeriodStatus.CLOSED) {
            throw new IllegalStateException("Đợt đăng ký đã đóng, không thể lập nhóm hoặc đăng ký đề tài.");
        }
        if (period.getStatus() != PeriodStatus.PUBLISHED) {
            throw new IllegalStateException("Đợt đăng ký chưa được công bố nên chưa thể lập nhóm hoặc đăng ký đề tài.");
        }
        LocalDateTime current = now();
        if (current.isBefore(period.getStudentStartAt())) {
            throw new IllegalStateException("Chưa tới khung thời gian sinh viên đăng ký đề tài.");
        }
        if (!current.isBefore(period.getStudentEndAt())) {
            throw new IllegalStateException("Đã hết khung thời gian sinh viên đăng ký đề tài.");
        }
    }

    // =================================================================
    // Kiểm tra dữ liệu đầu vào
    // =================================================================

    /**
     * Quy tắc thời gian (mục 5.2):
     * - bắt đầu &lt; kết thúc mỗi giai đoạn;
     * - giai đoạn SV bắt đầu sau khi giai đoạn GV kết thúc.
     */
    void validateTimes(RegistrationPeriod period) {
        LocalDateTime ls = period.getLecturerStartAt();
        LocalDateTime le = period.getLecturerEndAt();
        LocalDateTime ss = period.getStudentStartAt();
        LocalDateTime se = period.getStudentEndAt();

        if (le == null || ls == null || ss == null || se == null) {
            throw new IllegalArgumentException("Phải nhập đủ hai khoảng thời gian của đợt.");
        }
        if (!le.isAfter(ls)) {
            throw new IllegalArgumentException(
                    "Thời gian giảng viên: thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (!se.isAfter(ss)) {
            throw new IllegalArgumentException(
                    "Thời gian sinh viên: thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (ss.isBefore(le)) {
            throw new IllegalArgumentException(
                    "Giai đoạn sinh viên phải bắt đầu sau khi giai đoạn giảng viên kết thúc.");
        }
    }

    /**
     * Hạn chót GVPB và ngày hội đồng chỉ thiết lập được cho loại đợt tương ứng (mục 5.2).
     * Backend kiểm tra để chặn giá trị thừa gửi lên từ client, không chỉ ẩn/hiện bằng JS.
     */
    void validateOptionalDeadlines(RegistrationPeriod period) {
        PeriodType type = period.getPeriodType();
        LocalDateTime review = period.getReviewDeadline();
        LocalDateTime council = period.getCouncilReportDate();

        if (review != null && !type.allowsReviewDeadline()) {
            throw new IllegalArgumentException(
                    "Hạn chót giảng viên phản biện chỉ áp dụng cho đợt TLCN hoặc KLTN, không áp dụng cho "
                            + type.getDisplayName() + ".");
        }
        if (council != null && !type.allowsCouncilReportDate()) {
            throw new IllegalArgumentException(
                    "Ngày báo cáo hội đồng chỉ áp dụng cho đợt KLTN, không áp dụng cho "
                            + type.getDisplayName() + ".");
        }
        if (review != null && !review.isAfter(period.getStudentEndAt())) {
            throw new IllegalArgumentException(
                    "Hạn chót giảng viên phản biện phải sau ngày kết thúc đăng ký của sinh viên.");
        }
        if (council != null && review != null && !council.isAfter(review)) {
            throw new IllegalArgumentException(
                    "Ngày báo cáo hội đồng phải sau hạn chót giảng viên phản biện.");
        }
    }

    /** Chỉ sửa/xóa được đợt khi chưa có đề tài (mục 5.2). */
    private void rejectIfHasTopics(Long periodId) {
        if (periodId == null) {
            return;
        }
        if (topicRepository.countByPeriodId(periodId) > 0) {
            throw new IllegalStateException("Đợt đã có đề tài nên không được sửa hoặc xóa.");
        }
    }

    // =================================================================
    // Form-based API cho HEAD controller
    // =================================================================

    @Transactional(readOnly = true)
    public RegistrationPeriodForm getFormForEdit(Long id) {
        RegistrationPeriod p = getById(id);
        RegistrationPeriodForm f = new RegistrationPeriodForm();
        f.setName(p.getName());
        f.setType(p.getPeriodType());
        f.setLecturerStart(p.getLecturerStartAt());
        f.setLecturerEnd(p.getLecturerEndAt());
        f.setStudentStart(p.getStudentStartAt());
        f.setStudentEnd(p.getStudentEndAt());
        f.setReviewDeadline(p.getReviewDeadline());
        f.setDefenseDate(p.getCouncilReportDate());
        return f;
    }

    /**
     * Kiểm tra luật nghiệp vụ và trả về lỗi theo đúng ô trên form.
     *
     * <p>Trước đây mọi lỗi đều bị gán vào ô "name" (tên đợt) nên thông báo hiện sai chỗ.
     * Nay mỗi lỗi được gán về field tương ứng: {@code type}, {@code lecturerStart},
     * {@code lecturerEnd}, {@code studentStart}, {@code studentEnd},
     * {@code reviewDeadline}, {@code defenseDate}.
     */
    public Map<String, String> checkRules(RegistrationPeriodForm form) {
        Map<String, String> errors = new java.util.LinkedHashMap<>();
        if (form == null) {
            errors.put("name", "Thiếu dữ liệu đợt.");
            return errors;
        }
        if (form.getName() == null || form.getName().isBlank()) {
            errors.put("name", "Tên đợt không được trống.");
        }
        if (form.getType() == null) {
            errors.put("type", "Vui lòng chọn loại đợt.");
            return errors;
        }
        RegistrationPeriod tmp = toEntity(form);
        checkTimes(tmp, errors);
        checkOptionalDeadlines(tmp, errors);
        return errors;
    }

    /** Bắt đầu &lt; kết thúc mỗi giai đoạn; giai đoạn SV bắt đầu sau khi giai đoạn GV kết thúc. */
    private void checkTimes(RegistrationPeriod period, Map<String, String> errors) {
        LocalDateTime ls = period.getLecturerStartAt();
        LocalDateTime le = period.getLecturerEndAt();
        LocalDateTime ss = period.getStudentStartAt();
        LocalDateTime se = period.getStudentEndAt();

        if (ls == null) errors.put("lecturerStart", "Vui lòng nhập thời gian bắt đầu cho giảng viên.");
        if (le == null) errors.put("lecturerEnd", "Vui lòng nhập thời gian kết thúc cho giảng viên.");
        if (ss == null) errors.put("studentStart", "Vui lòng nhập thời gian bắt đầu cho sinh viên.");
        if (se == null) errors.put("studentEnd", "Vui lòng nhập thời gian kết thúc cho sinh viên.");

        if (ls != null && le != null && !le.isAfter(ls)) {
            errors.put("lecturerEnd",
                    "Thời gian giảng viên: thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (ss != null && se != null && !se.isAfter(ss)) {
            errors.put("studentEnd",
                    "Thời gian sinh viên: thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (le != null && ss != null && ss.isBefore(le)) {
            errors.put("studentStart",
                    "Giai đoạn sinh viên phải bắt đầu sau khi giai đoạn giảng viên kết thúc.");
        }
    }

    /**
     * Hạn chót GVPB chỉ có ở TLCN/KLTN, ngày báo cáo hội đồng chỉ có ở KLTN;
     * đồng thời phải đúng thứ tự thời gian.
     */
    private void checkOptionalDeadlines(RegistrationPeriod period, Map<String, String> errors) {
        PeriodType type = period.getPeriodType();
        LocalDateTime review = period.getReviewDeadline();
        LocalDateTime council = period.getCouncilReportDate();

        if (review != null && !type.allowsReviewDeadline()) {
            errors.put("reviewDeadline",
                    "Hạn chót giảng viên phản biện chỉ áp dụng cho đợt TLCN hoặc KLTN, không áp dụng cho "
                            + type.getDisplayName() + ".");
        }
        if (council != null && !type.allowsCouncilReportDate()) {
            errors.put("defenseDate",
                    "Ngày báo cáo hội đồng chỉ áp dụng cho đợt KLTN, không áp dụng cho "
                            + type.getDisplayName() + ".");
        }
        if (review != null && period.getStudentEndAt() != null
                && !review.isAfter(period.getStudentEndAt())) {
            errors.put("reviewDeadline",
                    "Hạn chót giảng viên phản biện phải sau ngày kết thúc đăng ký của sinh viên.");
        }
        if (council != null && review != null && !council.isAfter(review)) {
            errors.put("defenseDate",
                    "Ngày báo cáo hội đồng phải sau hạn chót giảng viên phản biện.");
        }
    }

    @Transactional
    public Long create(RegistrationPeriodForm form, String username) {
        checkRules(form);
        RegistrationPeriod p = toEntity(form);
        p.setStatus(PeriodStatus.DRAFT);
        User u = username == null ? null : userRepository.findByUsername(username).orElse(null);
        p.setCreatedBy(u);
        return periodRepository.save(p).getId();
    }

    @Transactional
    public void update(Long id, RegistrationPeriodForm form) {
        RegistrationPeriod existing = getById(id);
        rejectIfHasTopics(id);
        existing.setName(form.getName());
        existing.setPeriodType(form.getType() == null ? PeriodType.TLCN : form.getType());
        existing.setLecturerStartAt(form.getLecturerStart());
        existing.setLecturerEndAt(form.getLecturerEnd());
        existing.setStudentStartAt(form.getStudentStart());
        existing.setStudentEndAt(form.getStudentEnd());
        existing.setReviewDeadline(form.getReviewDeadline());
        existing.setCouncilReportDate(form.getDefenseDate());
        update(existing);
    }

    /**
     * Mở đợt ở trạng thái nháp: {@code DRAFT -> PUBLISHED} (nút "Mở đợt").
     * Chỉ áp dụng cho đợt nháp - đợt đã công bố thì dùng {@link #reopen(Long)}.
     */
    @Transactional
    public void open(Long id) {
        RegistrationPeriod period = getById(id);
        if (period.getStatus() == PeriodStatus.PUBLISHED) {
            throw new IllegalStateException("Đợt đang mở.");
        }
        if (period.getStatus() == PeriodStatus.CLOSED) {
            throw new IllegalStateException("Đợt đã đóng, dùng nút \"Mở lại\" nếu còn điều kiện.");
        }
        period.setStatus(PeriodStatus.PUBLISHED);
        validateTimes(period);
        validateOptionalDeadlines(period);
        periodRepository.save(period);
    }

    /**
     * Mở lại đợt đã đóng: {@code CLOSED -> PUBLISHED} (nút "Mở lại").
     *
     * <p>Điều kiện mà hộp thoại xác nhận trên giao diện từng hứa, nay kiểm tra thật ở
     * service: đợt phải còn thời gian đăng ký và chưa có hội đồng nào thuộc đợt.
     * Trước đây khối {@code if} bị bỏ rỗng nên mọi đợt CLOSED đều mở lại được,
     * kể cả đợt đã quá hạn và đã có hội đồng.
     */
    @Transactional
    public void reopen(Long id) {
        RegistrationPeriod period = getById(id);
        if (period.getStatus() != PeriodStatus.CLOSED) {
            throw new IllegalStateException("Chỉ mở lại được đợt đã đóng. Trạng thái hiện tại: "
                    + period.getStatus().getLabel() + ".");
        }
        if (!councilRepository.findByPeriodIdOrderByIdDesc(id).isEmpty()) {
            throw new IllegalStateException("Đợt đã có hội đồng phản biện nên không mở lại được.");
        }
        if (!now().isBefore(period.getStudentEndAt())) {
            throw new IllegalStateException("Đợt đã qua khung thời gian đăng ký của sinh viên nên không mở lại được.");
        }
        // Rà lại mốc thời gian trước khi mở lại.
        validateTimes(period);
        validateOptionalDeadlines(period);
        period.setStatus(PeriodStatus.PUBLISHED);
        periodRepository.save(period);
    }

    @Transactional
    public void deleteDraft(Long id) {
        RegistrationPeriod period = getById(id);
        if (period.getStatus() != PeriodStatus.DRAFT) {
            throw new IllegalStateException("Chỉ xóa được đợt ở trạng thái nháp.");
        }
        rejectIfHasTopics(id);
        periodRepository.delete(period);
    }

    private RegistrationPeriod toEntity(RegistrationPeriodForm form) {
        RegistrationPeriod p = new RegistrationPeriod();
        p.setName(form.getName());
        p.setPeriodType(form.getType() == null ? PeriodType.TLCN : form.getType());
        p.setLecturerStartAt(form.getLecturerStart());
        p.setLecturerEndAt(form.getLecturerEnd());
        p.setStudentStartAt(form.getStudentStart());
        p.setStudentEndAt(form.getStudentEnd());
        p.setReviewDeadline(form.getReviewDeadline());
        p.setCouncilReportDate(form.getDefenseDate());
        return p;
    }
}
