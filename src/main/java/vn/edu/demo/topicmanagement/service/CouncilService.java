package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.CouncilDetailView;
import vn.edu.demo.topicmanagement.dto.CouncilForm;
import vn.edu.demo.topicmanagement.dto.CouncilMemberForm;
import vn.edu.demo.topicmanagement.dto.CouncilMemberView;
import vn.edu.demo.topicmanagement.dto.CouncilRegistrationOption;
import vn.edu.demo.topicmanagement.dto.CouncilSummaryView;
import vn.edu.demo.topicmanagement.dto.CouncilTopicProgressView;
import vn.edu.demo.topicmanagement.dto.LecturerOption;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.CouncilMember;
import vn.edu.demo.topicmanagement.entity.CouncilRole;
import vn.edu.demo.topicmanagement.entity.CouncilStatus;
import vn.edu.demo.topicmanagement.entity.CouncilTopic;
import vn.edu.demo.topicmanagement.entity.Lecturer;
import vn.edu.demo.topicmanagement.entity.PeriodType;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.Report;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.CouncilMemberRepository;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.CouncilTopicRepository;
import vn.edu.demo.topicmanagement.repository.EvaluationRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.ReportRepository;
import vn.edu.demo.topicmanagement.repository.ReviewAssignmentRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/** Nghiệp vụ lập hội đồng phản biện (mục 5.6).
 *
 * <p>Tóm tắt quy tắc:
 * <ul>
 *   <li>Chỉ tạo hội đồng cho đợt TLCN/KLTN.</li>
 *   <li>Ngày báo cáo: với KLTN mặc định theo {@code period.councilReportDate} nếu form trống.</li>
 *   <li>3-5 thành viên, đúng 1 chủ tịch, đúng 1 thư ký; kiểm tra khi {@code activate}.</li>
 *   <li>Xóa thành viên ở hội đồng READY chỉ được khi vẫn ≥3 thành viên, không làm mất
 *       chủ tịch/thư ký và thành viên đó chưa có phân công chấm/điểm.</li>
 *   <li>Xóa hội đồng: chỉ khi chưa có phân công chấm nào.</li>
 *   <li>Đề tài đưa vào hội đồng phải cùng đợt, đã có báo cáo, chưa thuộc hội đồng khác
 *       (DB {@code uq_council_topics_topic UNIQUE(topic_id)}).</li>
 * </ul>
 */
@Service
public class CouncilService {

    private static final int MIN_MEMBERS = 3;
    private static final int MAX_MEMBERS = 5;

    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final LecturerRepository lecturerRepository;
    private final ReportRepository reportRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final ReviewAssignmentRepository assignmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CouncilService(CouncilRepository councilRepository,
                          CouncilMemberRepository memberRepository,
                          CouncilTopicRepository councilTopicRepository,
                          TopicRegistrationRepository registrationRepository,
                          LecturerRepository lecturerRepository,
                          ReportRepository reportRepository,
                          RegistrationPeriodRepository periodRepository,
                          ReviewAssignmentRepository assignmentRepository,
                          EvaluationRepository evaluationRepository,
                          UserRepository userRepository,
                          AuditLogService auditLogService) {
        this.councilRepository = councilRepository;
        this.memberRepository = memberRepository;
        this.councilTopicRepository = councilTopicRepository;
        this.registrationRepository = registrationRepository;
        this.lecturerRepository = lecturerRepository;
        this.reportRepository = reportRepository;
        this.periodRepository = periodRepository;
        this.assignmentRepository = assignmentRepository;
        this.evaluationRepository = evaluationRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /** Danh sách đợt TLCN/KLTN dùng cho combobox tạo hội đồng. */
    @Transactional(readOnly = true)
    public List<RegistrationPeriod> getPeriodsForCouncil() {
        return periodRepository.findAllByOrderByIdDesc().stream()
                .filter(p -> p.getPeriodType() == PeriodType.TLCN || p.getPeriodType() == PeriodType.KLTN)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CouncilSummaryView> getCouncils() {
        return councilRepository.findAll().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public CouncilSummaryView getSummary(Long councilId) {
        return toSummary(getCouncil(councilId));
    }

    /** Tạo hội đồng mới ở trạng thái {@code DRAFT}. */
    @Transactional
    public Long create(CouncilForm form, String username) {
        RegistrationPeriod period = periodRepository.findById(form.getPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký."));
        if (period.getPeriodType() != PeriodType.TLCN
                && period.getPeriodType() != PeriodType.KLTN) {
            throw new IllegalArgumentException("Chỉ tạo hội đồng cho đợt TLCN/KLTN.");
        }
        if (form.getCouncilCode() != null && !form.getCouncilCode().isBlank()
                && councilRepository.existsByCouncilCode(form.getCouncilCode().trim())) {
            throw new IllegalArgumentException("Mã hội đồng đã tồn tại.");
        }
        Council council = new Council();
        council.setPeriod(period);
        council.setCouncilCode(form.getCouncilCode() == null || form.getCouncilCode().isBlank()
                ? "HD-" + System.currentTimeMillis() : form.getCouncilCode().trim());
        council.setCouncilName(form.getCouncilName() == null ? "" : form.getCouncilName().trim());
        // Mặc định ngày báo cáo: form trước; KLTN thì lấy theo period.councilReportDate (mục 5.6).
        LocalDateTime reportDate = form.getReportDate();
        if (reportDate == null && period.getPeriodType() == PeriodType.KLTN) {
            reportDate = period.getCouncilReportDate();
        }
        council.setReportDate(reportDate);
        council.setStatus(CouncilStatus.DRAFT);
        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            council.setCreatedBy(user);
        }
        Long id = councilRepository.save(council).getId();
        auditLogService.record(username, AuditLogService.COUNCIL_CREATE, "Council", id,
                "Tạo hội đồng cho đợt " + period.getName());
        return id;
    }

    /** Sửa mã / tên / ngày báo cáo. Hội đồng DRAFT cho phép đổi tất cả;
     *  hội đồng READY chỉ cho phép đổi tên và ngày báo cáo (không đổi mã, không đổi đợt). */
    @Transactional
    public void update(Long councilId, CouncilForm form, String username) {
        Council council = getCouncil(councilId);
        if (council.getStatus() == CouncilStatus.FINISHED) {
            throw new IllegalStateException("Hội đồng đã hoàn tất, không thể sửa.");
        }
        if (council.getStatus() == CouncilStatus.DRAFT) {
            if (form.getCouncilCode() != null && !form.getCouncilCode().isBlank()) {
                String newCode = form.getCouncilCode().trim();
                if (!newCode.equals(council.getCouncilCode())
                        && councilRepository.existsByCouncilCode(newCode)) {
                    throw new IllegalArgumentException("Mã hội đồng đã tồn tại.");
                }
                council.setCouncilCode(newCode);
            }
        }
        if (form.getCouncilName() != null && !form.getCouncilName().isBlank()) {
            council.setCouncilName(form.getCouncilName().trim());
        }
        LocalDateTime reportDate = form.getReportDate();
        if (reportDate != null) {
            council.setReportDate(reportDate);
        }
        councilRepository.save(council);
        auditLogService.record(username, AuditLogService.COUNCIL_CREATE, "Council", councilId,
                "Sửa hội đồng " + council.getCouncilCode());
    }

    /** Xóa hội đồng - chỉ khi chưa có phân công chấm và chưa có điểm.
     *  Hội đồng DRAFT có thể xóa; READY/FINISHED thì phải xóa hết thành viên
     *  và phân công trước. */
    @Transactional
    public void delete(Long councilId, String username) {
        Council council = getCouncil(councilId);
        if (council.getStatus() == CouncilStatus.FINISHED) {
            throw new IllegalStateException("Hội đồng đã hoàn tất, không thể xóa.");
        }
        if (!assignmentRepository.findByCouncilIdWithLecturer(councilId).isEmpty()) {
            throw new IllegalStateException(
                    "Hội đồng đã có phân công chấm - không thể xóa. Hãy gỡ các phân công trước.");
        }
        if (!evaluationRepository.findByCouncilAndTopic(councilId, -1L).isEmpty()) {
            throw new IllegalStateException("Hội đồng đã có điểm phản biện - không thể xóa.");
        }
        // Xóa các thành viên & đề tài trong hội đồng
        for (CouncilMember m : memberRepository.findByCouncilId(councilId)) {
            memberRepository.delete(m);
        }
        for (CouncilTopic t : councilTopicRepository.findByCouncilId(councilId)) {
            councilTopicRepository.delete(t);
        }
        councilRepository.delete(council);
        auditLogService.record(username, AuditLogService.COUNCIL_CREATE, "Council", councilId,
                "Xóa hội đồng " + council.getCouncilCode());
    }

    /** Thêm thành viên vào hội đồng. DRAFT: đủ ràng buộc 3-5, 1 chủ tịch, 1 thư ký. */
    @Transactional
    public void addMember(Long councilId, CouncilMemberForm form, String username) {
        Council council = getCouncil(councilId);
        requireDraftOrReady(council, "thêm");
        if (memberRepository.countByCouncilId(councilId) >= MAX_MEMBERS) {
            throw new IllegalStateException("Một hội đồng có tối đa " + MAX_MEMBERS + " giảng viên.");
        }
        Lecturer lecturer = lecturerRepository.findById(form.getLecturerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên."));
        if (!lecturer.getUser().isEnabled()) {
            throw new IllegalArgumentException("Tài khoản giảng viên đang bị khóa.");
        }
        if (memberRepository.existsByCouncilIdAndLecturerId(councilId, lecturer.getId())) {
            throw new IllegalStateException("Giảng viên này đã ở trong hội đồng.");
        }
        if (form.getRole() == CouncilRole.CHAIRMAN
                && memberRepository.findByCouncilIdAndRole(councilId, CouncilRole.CHAIRMAN).isPresent()) {
            throw new IllegalStateException("Hội đồng chỉ có một chủ tịch.");
        }
        if (form.getRole() == CouncilRole.SECRETARY
                && memberRepository.findByCouncilIdAndRole(councilId, CouncilRole.SECRETARY).isPresent()) {
            throw new IllegalStateException("Hội đồng chỉ có một thư ký.");
        }
        CouncilMember m = new CouncilMember();
        m.setCouncil(council);
        m.setLecturer(lecturer);
        m.setRole(form.getRole());
        memberRepository.save(m);
        auditLogService.record(username, AuditLogService.COUNCIL_CREATE, "Council", councilId,
                "Thêm GV " + lecturer.getLecturerCode() + " vào hội đồng");
    }

    /** Đổi vai trò của thành viên (chỉ DRAFT) - hữu ích khi chuyển chủ tịch. */
    @Transactional
    public void updateMemberRole(Long councilId, Long memberId, CouncilRole newRole, String username) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        CouncilMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên."));
        if (!member.getCouncil().getId().equals(councilId)) {
            throw new IllegalArgumentException("Thành viên này không thuộc hội đồng đã chọn.");
        }
        if (member.getRole() == newRole) return;
        if (newRole == CouncilRole.CHAIRMAN
                && memberRepository.findByCouncilIdAndRole(councilId, CouncilRole.CHAIRMAN).isPresent()) {
            throw new IllegalStateException("Hội đồng chỉ có một chủ tịch.");
        }
        if (newRole == CouncilRole.SECRETARY
                && memberRepository.findByCouncilIdAndRole(councilId, CouncilRole.SECRETARY).isPresent()) {
            throw new IllegalStateException("Hội đồng chỉ có một thư ký.");
        }
        member.setRole(newRole);
        memberRepository.save(member);
        auditLogService.record(username, AuditLogService.COUNCIL_CREATE, "Council", councilId,
                "Đổi vai trò thành viên " + memberId + " → " + newRole);
    }

    /** Xóa thành viên. DRAFT: cho xóa tự do; READY: chỉ xóa khi còn đủ điều kiện 3-5,
     *  không làm mất chủ tịch/thư ký, và thành viên chưa được phân công chấm/có điểm. */
    @Transactional
    public void removeMember(Long councilId, Long memberId, String username) {
        Council council = getCouncil(councilId);
        if (council.getStatus() == CouncilStatus.FINISHED) {
            throw new IllegalStateException("Hội đồng đã hoàn tất, không thể xóa thành viên.");
        }
        CouncilMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên hội đồng."));
        if (!member.getCouncil().getId().equals(councilId)) {
            throw new IllegalArgumentException("Thành viên này không thuộc hội đồng đã chọn.");
        }
        if (council.getStatus() == CouncilStatus.READY) {
            // Không cho xóa chủ tịch/thư ký khi READY
            if (member.getRole() == CouncilRole.CHAIRMAN || member.getRole() == CouncilRole.SECRETARY) {
                throw new IllegalStateException(
                        "Hội đồng đang hoạt động: không thể gỡ chủ tịch/thư ký.");
            }
            // Nếu xóa thì phải còn ≥3
            long remaining = memberRepository.countByCouncilId(councilId) - 1;
            if (remaining < MIN_MEMBERS) {
                throw new IllegalStateException(
                        "Hội đồng đang hoạt động: phải giữ tối thiểu " + MIN_MEMBERS
                                + " thành viên (sau khi gỡ chỉ còn " + remaining + ").");
            }
            // Không cho gỡ khi thành viên đã được phân công chấm/kết quả
            for (CouncilTopic ct : councilTopicRepository.findByCouncilId(councilId)) {
                boolean hasAssignment = assignmentRepository
                        .findByCouncilIdAndTopicIdAndLecturerId(councilId,
                                ct.getTopic().getId(), member.getLecturer().getId())
                        .isPresent();
                if (hasAssignment) {
                    throw new IllegalStateException(
                            "Giảng viên đã được phân công chấm đề tài - không thể gỡ. "
                                    + "Hãy gỡ các phân công trước.");
                }
            }
        }
        memberRepository.delete(member);
        auditLogService.record(username, AuditLogService.COUNCIL_CREATE, "Council", councilId,
                "Gỡ thành viên " + memberId);
    }

    /** Xếp đề tài vào hội đồng (DRAFT). Đề tài phải cùng đợt, đã có báo cáo, chưa thuộc
     *  hội đồng khác. */
    @Transactional
    public void addTopic(Long councilId, Long topicId, String username) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        TopicRegistration reg = registrationRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài."));
        if (reg.getStatus() != RegistrationStatus.APPROVED) {
            throw new IllegalStateException("Chỉ xếp đề tài của đăng ký đã được duyệt.");
        }
        if (!reg.getTopic().getPeriod().getId().equals(council.getPeriod().getId())) {
            throw new IllegalStateException("Đề tài phải thuộc đúng đợt của hội đồng.");
        }
        if (councilTopicRepository.findByTopicId(reg.getTopic().getId()).isPresent()) {
            throw new IllegalStateException("Đề tài này đã thuộc một hội đồng khác.");
        }
        if (!hasSubmittedReport(reg)) {
            throw new IllegalStateException("Đề tài chưa có báo cáo được nộp - không đưa vào phản biện.");
        }
        CouncilTopic ct = new CouncilTopic();
        ct.setCouncil(council);
        ct.setTopic(reg.getTopic());
        councilTopicRepository.save(ct);
        auditLogService.record(username, AuditLogService.COUNCIL_CREATE, "Council", councilId,
                "Xếp đề tài " + reg.getTopic().getTopicCode() + " vào hội đồng");
    }

    @Transactional
    public void removeTopic(Long councilId, Long topicId, String username) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        CouncilTopic ct = councilTopicRepository.findByCouncilId(councilId).stream()
                .filter(x -> x.getTopic().getId().equals(topicId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Đề tài chưa được xếp vào hội đồng."));
        if (!assignmentRepository.findByCouncilIdWithLecturer(councilId).isEmpty()) {
            throw new IllegalStateException("Đã có phân công chấm - gỡ phân công trước khi gỡ đề tài.");
        }
        councilTopicRepository.delete(ct);
        auditLogService.record(username, AuditLogService.COUNCIL_CREATE, "Council", councilId,
                "Gỡ đề tài " + topicId);
    }

    /** Kích hoạt hội đồng DRAFT → READY khi đủ điều kiện. */
    @Transactional
    public void activate(Long councilId, String username) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        long count = memberRepository.countByCouncilId(councilId);
        if (count < MIN_MEMBERS || count > MAX_MEMBERS) {
            throw new IllegalStateException("Hội đồng phải có từ " + MIN_MEMBERS + " đến "
                    + MAX_MEMBERS + " giảng viên (hiện có " + count + ").");
        }
        if (memberRepository.findByCouncilIdAndRole(councilId, CouncilRole.CHAIRMAN).isEmpty()
                || memberRepository.findByCouncilIdAndRole(councilId, CouncilRole.SECRETARY).isEmpty()) {
            throw new IllegalStateException("Hội đồng phải có đúng một chủ tịch và một thư ký.");
        }
        List<CouncilTopic> topics = councilTopicRepository.findByCouncilId(councilId);
        if (topics.isEmpty()) {
            throw new IllegalStateException("Hãy xếp ít nhất một đề tài vào hội đồng.");
        }
        council.setStatus(CouncilStatus.READY);
        councilRepository.save(council);
        auditLogService.record(username, AuditLogService.COUNCIL_CREATE, "Council", councilId,
                "Kích hoạt hội đồng");
    }

    /** Trang chi tiết hội đồng - đầy đủ thành viên, đề tài, phân công và tiến độ chấm. */
    @Transactional(readOnly = true)
    public CouncilDetailView getDetail(Long councilId) {
        Council council = getCouncil(councilId);
        List<CouncilMember> members = memberRepository.findByCouncilIdWithLecturer(councilId);
        List<CouncilMemberView> memberViews = members.stream().map(m -> new CouncilMemberView(
                m.getId(),
                m.getLecturer().getUser().getFullName(),
                m.getLecturer().getLecturerCode(),
                m.getRole())).toList();

        List<LecturerOption> lecturerCandidates = lecturerRepository.findAllActiveWithDepartment().stream()
                .map(l -> new LecturerOption(l.getId(), l.getUser().getFullName(), l.getLecturerCode()))
                .toList();

        Set<Long> assignedTopicIds = councilTopicRepository.findByCouncilId(councilId).stream()
                .filter(ct -> ct.getTopic() != null)
                .map(ct -> ct.getTopic().getId())
                .collect(Collectors.toSet());

        // Đề tài trong hội đồng kèm tiến độ chấm
        List<CouncilTopicProgressView> topicViews = new ArrayList<>();
        for (CouncilTopic ct : councilTopicRepository.findByCouncilIdWithTopic(councilId)) {
            Long tid = ct.getTopic().getId();
            String groupName = studentGroupNameOf(ct.getTopic());
            long assignments = assignmentRepository.findByCouncilAndTopicWithEvaluation(
                    councilId, tid).size();
            long scored = evaluationRepository.countByCouncilAndTopic(councilId, tid);
            topicViews.add(new CouncilTopicProgressView(
                    ct.getId(), tid, ct.getTopic().getTopicCode(), ct.getTopic().getTopicName(),
                    groupName, (int) assignments, (int) scored));
        }

        // Đề tài chưa thuộc hội đồng nào, cùng đợt, đã có báo cáo, đề xuất xếp vào
        List<CouncilRegistrationOption> available = new ArrayList<>();
        List<TopicRegistration> candidates = registrationRepository
                .findByStatusWithGroup(RegistrationStatus.APPROVED).stream()
                .filter(r -> r.getTopic().getPeriod().getId().equals(council.getPeriod().getId()))
                .filter(r -> hasSubmittedReport(r))
                .toList();
        for (TopicRegistration r : candidates) {
            if (assignedTopicIds.contains(r.getTopic().getId())) continue;
            available.add(new CouncilRegistrationOption(r.getId(),
                    r.getGroup().getGroupName(), r.getTopic().getTopicName()));
        }
        // Danh sách đề tài đã xếp (group + topic)
        List<CouncilRegistrationOption> assigned = topicViews.stream()
                .map(tv -> topicRegistrationIdOf(tv.getTopicId())).filter(Optional::isPresent)
                .map(Optional::get).map(rid -> {
                    TopicRegistration reg = registrationRepository.findById(rid).orElse(null);
                    if (reg == null) return null;
                    return new CouncilRegistrationOption(reg.getId(),
                            reg.getGroup().getGroupName(), reg.getTopic().getTopicName());
                }).filter(java.util.Objects::nonNull).toList();

        return new CouncilDetailView(toSummary(council), memberViews, lecturerCandidates,
                available, assigned, topicViews);
    }

    /** Lookup id của topic_registrations trong trạng thái APPROVED của 1 topic. */
    private Optional<Long> topicRegistrationIdOf(Long topicId) {
        return registrationRepository.findByTopicIdAndStatusIn(topicId,
                List.of(RegistrationStatus.APPROVED)).stream().findFirst().map(TopicRegistration::getId);
    }

    private String studentGroupNameOf(Topic t) {
        return t == null ? "" : registrationRepository.findByTopicIdAndStatusIn(t.getId(),
                List.of(RegistrationStatus.APPROVED)).stream().findFirst()
                .map(r -> r.getGroup().getGroupName()).orElse("");
    }

    private boolean hasSubmittedReport(TopicRegistration reg) {
        return reportRepository.findByGroupIdOrderByVersionDesc(reg.getGroup().getId()).stream()
                .anyMatch(rp -> "SUBMITTED".equals(rp.getStatus()));
    }

    private CouncilSummaryView toSummary(Council council) {
        List<String> memberNames = memberRepository.findByCouncilId(council.getId()).stream()
                .map(m -> m.getRole().getDisplayName() + ": " + m.getLecturer().getUser().getFullName()).toList();
        List<String> topicNames = councilTopicRepository.findByCouncilId(council.getId()).stream()
                .map(ct -> ct.getTopic().getTopicName()).toList();
        return new CouncilSummaryView(council.getId(), council.getCouncilCode(),
                council.getCouncilName(),
                council.getPeriod() == null ? null : council.getPeriod().getName(),
                council.getReportDate(), null, council.getStatus(), memberNames, topicNames);
    }

    private Council getCouncil(Long id) {
        return councilRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hội đồng."));
    }

    private void requireDraft(Council council) {
        if (council.getStatus() != CouncilStatus.DRAFT) {
            throw new IllegalStateException("Chỉ chỉnh sửa hội đồng đang ở trạng thái nháp.");
        }
    }

    /** Thao tác cho phép trên DRAFT và READY (thêm thành viên / đổi vai trò thành viên khi DRAFT). */
    private void requireDraftOrReady(Council council, String op) {
        if (council.getStatus() == CouncilStatus.DRAFT) return;
        if (council.getStatus() == CouncilStatus.READY) return;
        throw new IllegalStateException("Hội đồng đã hoàn tất - không thể " + op + ".");
    }

    }
