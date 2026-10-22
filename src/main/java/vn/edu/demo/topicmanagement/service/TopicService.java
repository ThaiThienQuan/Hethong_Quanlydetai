package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.HeadSupervisorForm;
import vn.edu.demo.topicmanagement.dto.HeadSupervisorInfo;
import vn.edu.demo.topicmanagement.dto.LecturerOption;
import vn.edu.demo.topicmanagement.dto.TopicForm;
import vn.edu.demo.topicmanagement.dto.TopicView;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.entity.Lecturer;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicStatus;
import vn.edu.demo.topicmanagement.entity.TopicSupervisor;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.DepartmentRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.ReviewAssignmentRepository;
import vn.edu.demo.topicmanagement.repository.StudentGroupRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.TopicRepository;
import vn.edu.demo.topicmanagement.repository.TopicSupervisorRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Nghiệp vụ đề tài - mục 5.3 của đề bài.
 *
 * <p>Các quy tắc được ép ở service (vì không thể diễn đạt bằng CHECK của MySQL):
 * <ul>
 *   <li>Mã đề tài duy nhất; 1 ≤ số GVHD ≤ 2; hai GVHD không trùng nhau.</li>
 *   <li>GV chỉ đề xuất được trong khung giai đoạn 1 của đợt {@code PUBLISHED},
 *       và chỉ chọn được bộ môn của chính mình.</li>
 *   <li>Đề tài đã có đăng ký (PENDING hoặc APPROVED) thì không xóa được.</li>
 *   <li>Không thêm GVHD là người đang được phân công chấm đề tài đó.</li>
 * </ul>
 */
@Service
public class TopicService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Số GVHD tối đa trên mỗi đề tài. */
    public static final int MAX_SUPERVISORS = 2;

    private final TopicRepository topicRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final UserRepository userRepository;
    private final LecturerRepository lecturerRepository;
    private final DepartmentRepository departmentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final StudentGroupRepository groupRepository;
    private final ReviewAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    public TopicService(TopicRepository topicRepository,
                        TopicSupervisorRepository supervisorRepository,
                        UserRepository userRepository,
                        LecturerRepository lecturerRepository,
                        DepartmentRepository departmentRepository,
                        RegistrationPeriodRepository periodRepository,
                        TopicRegistrationRepository registrationRepository,
                        StudentGroupRepository groupRepository,
                        ReviewAssignmentRepository assignmentRepository,
                        AuditLogService auditLogService) {
        this.topicRepository = topicRepository;
        this.supervisorRepository = supervisorRepository;
        this.userRepository = userRepository;
        this.lecturerRepository = lecturerRepository;
        this.departmentRepository = departmentRepository;
        this.periodRepository = periodRepository;
        this.registrationRepository = registrationRepository;
        this.groupRepository = groupRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditLogService = auditLogService;
    }

    // ------------------------------------------------------------------
    // 5.3 - Danh sách cho giảng viên
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TopicView> getMyTopics(String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();
        List<Topic> mine = topicRepository.findByCreatorUserId(me.getId());
        return toViews(mine, username);
    }

    /** Danh sách đề tài đã công bố cho mọi vai trò (mục 5.3, giai đoạn 2 cho sinh viên). */
    @Transactional(readOnly = true)
    public List<TopicView> getPublishedTopics() {
        List<Topic> topics = topicRepository.findAll().stream()
                .filter(t -> t.isPublished() && t.getStatus() == TopicStatus.APPROVED)
                .sorted(Comparator0())
                .toList();
        return topics.stream().map(t -> toView(t, null)).toList();
    }

    // ------------------------------------------------------------------
    // 5.3 - Hàng đợi Trưởng khoa (lọc bộ môn / đợt / trạng thái, có phân trang)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<TopicView> getApprovalQueue(String status, Long departmentId, Long periodId, int page) {
        TopicStatus st = parseStatus(status);
        Page<Topic> result = topicRepository.searchForApproval(
                st, departmentId, periodId, PageRequest.of(Math.max(page, 0), 10));
        return result.map(t -> toView(t, null));
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> getAllPeriodsForFilter() {
        return periodRepository.findAllByOrderByIdDesc();
    }

    // ------------------------------------------------------------------
    // 5.3 - Danh mục cho form
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Department> getDepartments() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    /**
     * Bộ môn của chính giảng viên (đề bài: đề xuất đề tài phải thuộc bộ môn của GV,
     * không được chọn bộ môn khác). Trả về giá trị thuần để đọc an toàn ngoài
     * transaction (app chạy open-in-view=false).
     */
    @Transactional(readOnly = true)
    public MyDepartment getMyDepartment(String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return null;
        return lecturerRepository.findByUserIdWithDepartment(me.getId())
                .map(l -> new MyDepartment(l.getDepartment().getId(), l.getDepartment().getName()))
                .orElse(null);
    }

    /** Id + tên bộ môn của GV, tách khỏi entity để không phụ thuộc session JPA. */
    public record MyDepartment(Long id, String name) {
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> getOpenPeriods() {
        return periodRepository.findPeriodsOpenForLecturers(now(), PeriodStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public List<LecturerOption> getActiveLecturerOptions() {
        return lecturerRepository.findAllActiveWithDepartment().stream()
                .map(l -> new LecturerOption(l.getId(), l.getUser().getFullName(), l.getLecturerCode()))
                .toList();
    }

    /**
     * Ứng viên GVHD thứ hai. Chỉ trả về giảng viên khác mình, đang hoạt động và
     * chưa làm GVHD của đề tài nào khác trong cùng đợt (tránh trùng GVHD).
     */
    @Transactional(readOnly = true)
    public List<LecturerOption> getCoSupervisorCandidates(String username, Long topicId, Long periodId) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();
        Set<Long> busy = periodId == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(supervisorRepository.findLecturerIdsByPeriodId(periodId));
        busy.add(me.getId());
        return lecturerRepository.findAllActiveWithDepartment().stream()
                .filter(l -> !busy.contains(l.getId()))
                .map(l -> new LecturerOption(l.getId(), l.getUser().getFullName(), l.getLecturerCode()))
                .toList();
    }

    // ------------------------------------------------------------------
    // 5.3 - Kiểm tra quy tắc khi GV gửi form
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, String> checkRules(TopicForm f, String username, Long topicId) {
        Map<String, String> errors = new LinkedHashMap<>();
        User me = userRepository.findByUsername(username).orElse(null);
        Lecturer lecturer = me == null ? null : lecturerRepository.findByUserId(me.getId()).orElse(null);
        if (lecturer == null) {
            errors.put("periodId", "Chỉ giảng viên mới được đề xuất đề tài.");
            return errors;
        }

        // Mã đề tài phải duy nhất (trừ chính đề tài đang sửa).
        if (f.getTopicCode() != null && !f.getTopicCode().isBlank()) {
            String code = f.getTopicCode().trim();
            boolean exists = topicId == null
                    ? topicRepository.existsByTopicCode(code)
                    : topicRepository.existsByTopicCodeAndIdNot(code, topicId);
            if (exists) errors.put("topicCode", "Mã đề tài đã tồn tại, vui lòng chọn mã khác");
        }

        // Bộ môn phải là bộ môn của chính GV (đề bài: không được chọn bộ môn khác).
        if (f.getDepartmentId() != null && !f.getDepartmentId().equals(lecturer.getDepartment().getId())) {
            errors.put("departmentId", "Bạn chỉ được đề xuất đề tài thuộc bộ môn của mình.");
        } else if (f.getDepartmentId() != null && !departmentRepository.existsById(f.getDepartmentId())) {
            errors.put("departmentId", "Bộ môn không tồn tại");
        }

        // Đợt phải PUBLISHED và đang trong khung giai đoạn 1.
        if (f.getPeriodId() != null) {
            RegistrationPeriod p = periodRepository.findById(f.getPeriodId()).orElse(null);
            LocalDateTime n = now();
            if (p == null) {
                errors.put("periodId", "Đợt đăng ký không tồn tại");
            } else if (p.getStatus() != PeriodStatus.PUBLISHED
                    || n.isBefore(p.getLecturerStartAt()) || n.isAfter(p.getLecturerEndAt())) {
                errors.put("periodId", "Đợt này hiện không mở cho giảng viên đề xuất đề tài");
            }
        }

        // GVHD thứ hai: không trùng GVHD chính (chính là người đang đăng nhập).
        if (f.getCoSupervisorId() != null && f.getCoSupervisorId().equals(lecturer.getId())) {
            errors.put("coSupervisorId", "GVHD thứ hai không được trùng với GVHD chính.");
        } else if (f.getCoSupervisorId() != null
                && !lecturerRepository.existsById(f.getCoSupervisorId())) {
            errors.put("coSupervisorId", "Giảng viên hướng dẫn không tồn tại.");
        }

        // Chỉ cho sửa nếu đề tài chưa APPROVED, hoặc đã APPROVED nhưng chưa có đăng ký.
        if (topicId != null) {
            Topic existing = topicRepository.findById(topicId).orElse(null);
            if (existing == null) {
                errors.put("topicId", "Không tìm thấy đề tài.");
            } else if (existing.getStatus() == TopicStatus.APPROVED
                    && hasAnyRegistration(topicId)) {
                errors.put("periodId", "Đề tài đã có nhóm đăng ký nên không thể sửa.");
            }
        }
        return errors;
    }

    // ------------------------------------------------------------------
    // 5.3 - Tạo / sửa / xóa của GV
    // ------------------------------------------------------------------

    @Transactional
    public void create(TopicForm f, String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        Lecturer lecturer = me == null ? null : lecturerRepository.findByUserId(me.getId()).orElse(null);
        if (lecturer == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ giảng viên được tạo đề tài.");
        }

        Topic t = new Topic();
        copyFormToTopic(f, t);
        t.setCreatedByLecturer(lecturer);
        t.setStatus(TopicStatus.PENDING);
        topicRepository.saveAndFlush(t);

        saveSupervisor(t, lecturer, 1, null);
        if (f.getCoSupervisorId() != null) {
            Lecturer co = lecturerRepository.findById(f.getCoSupervisorId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Giảng viên hướng dẫn không tồn tại."));
            saveSupervisor(t, co, 2, null);
        }
        auditLogService.record(username, AuditLogService.TOPIC_CREATE, "topics", t.getId(),
                "GV đề xuất đề tài " + t.getTopicCode());
    }

    @Transactional(readOnly = true)
    public TopicForm getFormForEdit(Long id, String username) {
        Topic t = loadOwnedEditable(id, username);
        TopicForm f = new TopicForm();
        f.setTopicCode(t.getTopicCode());
        f.setTopicName(t.getTopicName());
        f.setDescription(t.getDescription());
        f.setDepartmentId(t.getDepartment() == null ? null : t.getDepartment().getId());
        f.setPeriodId(t.getPeriod() == null ? null : t.getPeriod().getId());
        Long coId = null;
        for (TopicSupervisor ts : supervisorRepository.findByTopicIdWithLecturer(id)) {
            if (ts.getSupervisorOrder() > 1) {
                coId = ts.getLecturer().getId();
                break;
            }
        }
        f.setCoSupervisorId(coId);
        return f;
    }

    @Transactional
    public void update(Long id, TopicForm f, String username) {
        Topic t = loadOwnedEditable(id, username);
        copyFormToTopic(f, t);
        // Gửi lại Trưởng khoa duyệt (đề bài: "gửi lại sau khi bị từ chối" - A8).
        t.setStatus(TopicStatus.PENDING);
        t.setRejectReason(null);
        t.setApprovedBy(null);
        t.setApprovedAt(null);
        t.setPublished(false);
        topicRepository.saveAndFlush(t);

        Lecturer lecturer = lecturerRepository
                .findByUserId(userRepository.findByUsername(username).orElseThrow().getId())
                .orElseThrow();
        supervisorRepository.deleteByTopicId(id);
        supervisorRepository.flush();
        saveSupervisor(t, lecturer, 1, null);
        if (f.getCoSupervisorId() != null) {
            Lecturer co = lecturerRepository.findById(f.getCoSupervisorId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Giảng viên hướng dẫn không tồn tại."));
            saveSupervisor(t, co, 2, null);
        }
        auditLogService.record(username, AuditLogService.TOPIC_CREATE, "topics", t.getId(),
                "GV sửa và gửi duyệt lại đề tài " + t.getTopicCode());
    }

    /** GV xóa đề tài của mình khi PENDING/REJECTED và chưa có đăng ký nào. */
    @Transactional
    public void deleteOwnUnsubmitted(Long id, String username) {
        Topic t = loadOwnedEditable(id, username);
        deleteTopicInternal(t);
        auditLogService.record(username, AuditLogService.TOPIC_CREATE, "topics", id,
                "GV xóa đề tài " + t.getTopicCode());
    }

    // ------------------------------------------------------------------
    // 5.3 - Trưởng khoa duyệt / từ chối / công bố
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public HeadSupervisorInfo getHeadSupervisorInfo(Long topicId) {
        Topic topic = topicRepository.findById(topicId).orElseThrow();
        assertSupervisorsEditable(topic);
        List<TopicSupervisor> assigned = supervisorRepository.findByTopicIdWithLecturer(topicId);
        Long main = null;
        Long secondary = null;
        List<String> names = new ArrayList<>();
        for (TopicSupervisor ts : assigned) {
            names.add(ts.getLecturer().getUser().getFullName()
                    + (ts.getSupervisorOrder() == 1 ? " (chính)" : " (thứ hai)"));
            if (ts.getSupervisorOrder() == 1) main = ts.getLecturer().getId();
            else secondary = ts.getLecturer().getId();
        }
        return new HeadSupervisorInfo(topic.getTopicName(), topic.getPeriod().getName(),
                statusLabel(topic.getStatus()), String.join(", ", names), main, secondary);
    }

    @Transactional
    public void approve(Long id, String username) {
        Topic t = topicRepository.findById(id).orElseThrow();
        if (t.getStatus() != TopicStatus.PENDING) {
            throw new IllegalStateException("Chỉ duyệt đề tài đang chờ duyệt.");
        }
        // Đề tài phải có ít nhất 1 GVHD trước khi duyệt.
        if (supervisorRepository.countByTopicId(id) < 1) {
            throw new IllegalStateException("Đề tài chưa có giảng viên hướng dẫn, không thể duyệt.");
        }
        User approver = userRepository.findByUsername(username).orElse(null);
        t.setStatus(TopicStatus.APPROVED);
        t.setApprovedBy(approver);
        t.setApprovedAt(now());
        topicRepository.save(t);
        auditLogService.record(username, AuditLogService.TOPIC_APPROVE, "topics", id,
                "Duyệt đề tài " + t.getTopicCode());
    }

    @Transactional
    public void reject(Long id, String reason, String username) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        }
        Topic t = topicRepository.findById(id).orElseThrow();
        if (t.getStatus() != TopicStatus.PENDING) {
            throw new IllegalStateException("Chỉ từ chối đề tài đang chờ duyệt.");
        }
        t.setStatus(TopicStatus.REJECTED);
        t.setRejectReason(reason.trim());
        t.setPublished(false);
        t.setApprovedBy(null);
        t.setApprovedAt(null);
        topicRepository.save(t);
        auditLogService.record(username, AuditLogService.TOPIC_REJECT, "topics", id,
                "Từ chối đề tài " + t.getTopicCode() + ": " + reason.trim());
    }

    @Transactional
    public void publish(Long id, String username) {
        Topic t = topicRepository.findById(id).orElseThrow();
        if (t.getStatus() != TopicStatus.APPROVED) {
            throw new IllegalStateException("Chỉ công bố đề tài đã duyệt.");
        }
        // Đã công bố rồi thì không công bố lại lần nữa (nút "Công bố" chỉ hiện 1 lần).
        if (t.isPublished()) {
            throw new IllegalStateException("Đề tài này đã được công bố.");
        }
        t.setPublished(true);
        topicRepository.save(t);
        auditLogService.record(username, AuditLogService.TOPIC_PUBLISH, "topics", id,
                "Công bố đề tài " + t.getTopicCode());
    }

    /** Công bố danh sách đề tài đã duyệt **theo đợt** (giai đoạn 1 của mục 5.3). */
    @Transactional
    public int publishAllApproved(Long periodId, String username) {
        if (periodId == null) {
            throw new IllegalArgumentException("Vui lòng chọn đợt cần công bố danh sách đề tài.");
        }
        List<Topic> approved = topicRepository.findByStatus(TopicStatus.APPROVED).stream()
                .filter(t -> t.getPeriod().getId().equals(periodId))
                .filter(t -> !t.isPublished())   // bỏ qua đề tài đã công bố từ trước
                .toList();
        for (Topic t : approved) {
            t.setPublished(true);
        }
        topicRepository.saveAll(approved);
        if (!approved.isEmpty()) {
            auditLogService.record(username, AuditLogService.TOPIC_PUBLISH, "topics", null,
                    "Công bố danh sách " + approved.size() + " đề tài của đợt #" + periodId);
        }
        return approved.size();
    }

    /** Trưởng khoa xóa đề tài khi chưa có đăng ký nào. */
    @Transactional
    public void deleteByHead(Long id, String username) {
        Topic t = topicRepository.findById(id).orElseThrow();
        deleteTopicInternal(t);
        auditLogService.record(username, AuditLogService.TOPIC_CREATE, "topics", id,
                "Trưởng khoa xóa đề tài " + t.getTopicCode());
    }

    // ------------------------------------------------------------------
    // 5.3 - Trưởng khoa phân công / điều chỉnh GVHD (A11)
    // ------------------------------------------------------------------

    @Transactional
    public void updateSupervisorsByHead(Long topicId, HeadSupervisorForm form, String username) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài"));

        // Chỉ được phân công / đổi GVHD khi đề tài còn đang chờ duyệt, hoặc đã duyệt
        // nhưng chưa công bố. Đề tài bị từ chối (GV sẽ sửa và gửi duyệt lại) hoặc đã
        // công bố thì không đổi GVHD được nữa.
        assertSupervisorsEditable(topic);

        Long primaryId = form.getPrimarySupervisorId();
        if (primaryId == null) {
            throw new IllegalArgumentException("Vui lòng chọn GVHD chính.");
        }
        Long coId = form.getCoSupervisorId();
        // Hai GVHD không được trùng nhau.
        if (coId != null && coId.equals(primaryId)) {
            throw new IllegalArgumentException("Hai giảng viên hướng dẫn phải là hai người khác nhau.");
        }

        Lecturer primary = lecturerRepository.findById(primaryId)
                .orElseThrow(() -> new IllegalArgumentException("GVHD chính không tồn tại."));
        Lecturer co = null;
        if (coId != null) {
            co = lecturerRepository.findById(coId)
                    .orElseThrow(() -> new IllegalArgumentException("GVHD thứ hai không tồn tại."));
        }

        // Không thêm làm GVHD một GV đang được phân công chấm đề tài đó (mục 5.3 + 5.7).
        assertNotAssignedAsReviewer(topicId, primary.getId());
        if (co != null) {
            assertNotAssignedAsReviewer(topicId, co.getId());
        }

        // Đề tài đã có phân công chấm thì không được đổi GVHD (HTML đã cảnh báo điều này).
        if (assignmentRepository.countByTopicId(topicId) > 0) {
            throw new IllegalStateException("Đề tài đã có phân công phản biện nên không thể đổi GVHD.");
        }

        User assigner = userRepository.findByUsername(username).orElse(null);
        supervisorRepository.deleteByTopicId(topicId);
        supervisorRepository.flush();
        saveSupervisor(topic, primary, 1, assigner);
        if (co != null) {
            saveSupervisor(topic, co, 2, assigner);
        }
        auditLogService.record(username, AuditLogService.SUPERVISOR_ASSIGN, "topic_supervisors", topicId,
                "Phân công GVHD cho đề tài " + topic.getTopicCode());
    }

    // ------------------------------------------------------------------
    // Nội bộ
    // ------------------------------------------------------------------

    /**
     * Kiểm tra đề tài còn cho phép phân công / đổi GVHD hay không.
     * Chỉ được đổi GVHD khi đề tài đang chờ duyệt (PENDING) hoặc đã duyệt nhưng
     * chưa công bố. Đề tài bị từ chối (GV sẽ sửa rồi gửi duyệt lại) hoặc đã công bố
     * thì không được đổi GVHD nữa.
     */
    private void assertSupervisorsEditable(Topic topic) {
        if (topic.getStatus() == TopicStatus.REJECTED) {
            throw new IllegalStateException(
                    "Đề tài đã bị từ chối nên không thể phân công lại giảng viên hướng dẫn.");
        }
        if (topic.isPublished()) {
            throw new IllegalStateException(
                    "Đề tài đã công bố nên không thể đổi giảng viên hướng dẫn.");
        }
    }

    private void assertNotAssignedAsReviewer(Long topicId, Long lecturerId) {
        List<ReviewAssignmentRef> refs = assignmentRepository.findByTopicId(topicId).stream()
                .map(a -> new ReviewAssignmentRef(a.getLecturer().getId()))
                .toList();
        if (refs.stream().anyMatch(r -> r.lecturerId().equals(lecturerId))) {
            throw new IllegalStateException(
                    "Giảng viên này đang được phân công phản biện đề tài nên không thể làm GVHD.");
        }
    }

    private record ReviewAssignmentRef(Long lecturerId) { }

    private void saveSupervisor(Topic topic, Lecturer lecturer, int order, User assignedBy) {
        TopicSupervisor ts = new TopicSupervisor();
        ts.setTopic(topic);
        ts.setLecturer(lecturer);
        ts.setSupervisorOrder(order);
        ts.setAssignedBy(assignedBy);
        supervisorRepository.save(ts);
    }

    private void deleteTopicInternal(Topic t) {
        if (hasAnyRegistration(t.getId())) {
            throw new IllegalStateException("Đề tài đã có nhóm đăng ký nên không thể xóa.");
        }
        supervisorRepository.deleteByTopicId(t.getId());
        supervisorRepository.flush();
        topicRepository.delete(t);
    }

    private boolean hasAnyRegistration(Long topicId) {
        return !registrationRepository.findByTopicId(topicId).isEmpty();
    }

    private Topic loadOwnedEditable(Long id, String username) {
        Topic t = topicRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài."));
        if (t.getCreatedByLecturer() == null || t.getCreatedByLecturer().getUser() == null
                || !t.getCreatedByLecturer().getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải người tạo đề tài này");
        }
        if (t.getStatus() == TopicStatus.APPROVED) {
            if (hasAnyRegistration(id)) {
                throw new IllegalStateException(
                        "Đề tài đã có nhóm đăng ký nên không thể sửa hoặc xóa.");
            }
            // Đề tài APPROVED chưa có đăng ký: cho sửa, sau đó gửi duyệt lại.
            return t;
        }
        if (t.getStatus() != TopicStatus.PENDING && t.getStatus() != TopicStatus.REJECTED) {
            throw new IllegalStateException("Chỉ sửa đề tài đang chờ duyệt hoặc bị từ chối.");
        }
        return t;
    }

    private void copyFormToTopic(TopicForm f, Topic t) {
        t.setTopicCode(f.getTopicCode() == null ? null : f.getTopicCode().trim());
        t.setTopicName(f.getTopicName() == null ? "" : f.getTopicName().trim());
        t.setDescription(f.getDescription() == null ? "" : f.getDescription().trim());
        if (f.getDepartmentId() != null) {
            t.setDepartment(departmentRepository.getReferenceById(f.getDepartmentId()));
        }
        if (f.getPeriodId() != null) {
            t.setPeriod(periodRepository.getReferenceById(f.getPeriodId()));
        }
    }

    private TopicStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return TopicStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String statusLabel(TopicStatus s) {
        return switch (s) {
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Bị từ chối";
        };
    }

    private java.util.Comparator<Topic> Comparator0() {
        return (a, b) -> {
            String ca = a.getTopicCode() == null ? "" : a.getTopicCode();
            String cb = b.getTopicCode() == null ? "" : b.getTopicCode();
            return ca.compareToIgnoreCase(cb);
        };
    }

    private TopicView toView(Topic t, String username) {
        TopicView v = new TopicView();
        v.setId(t.getId());
        v.setTopicCode(t.getTopicCode());
        v.setTopicName(t.getTopicName());
        v.setDescription(t.getDescription());
        v.setStatus(t.getStatus());
        v.setRejectReason(t.getRejectReason());
        v.setPublished(t.isPublished());
        if (t.getDepartment() != null) {
            v.setDepartmentId(t.getDepartment().getId());
            v.setDepartmentName(t.getDepartment().getName());
        }
        if (t.getPeriod() != null) {
            v.setPeriodId(t.getPeriod().getId());
            v.setPeriodName(t.getPeriod().getName());
        }
        if (t.getCreatedByLecturer() != null && t.getCreatedByLecturer().getUser() != null) {
            v.setCreatedByLecturerName(t.getCreatedByLecturer().getUser().getFullName());
        }
        v.setCreatedAt(t.getCreatedAt());
        List<LecturerOption> sups = new ArrayList<>();
        StringBuilder names = new StringBuilder();
        for (TopicSupervisor ts : supervisorRepository.findByTopicIdWithLecturer(t.getId())) {
            if (ts.getLecturer() != null && ts.getLecturer().getUser() != null) {
                String full = ts.getLecturer().getUser().getFullName();
                sups.add(new LecturerOption(ts.getLecturer().getId(), full,
                        ts.getLecturer().getLecturerCode()));
                if (names.length() > 0) names.append(", ");
                names.append(full);
            }
        }
        v.setSupervisors(sups);
        v.setSupervisorsText(names.toString());
        // Nhóm đang thực hiện đề tài (nếu có) - yêu cầu dòng 183.
        groupRepository.findByTopicId(t.getId()).ifPresent(g -> {
            v.setGroupName(g.getGroupName());
            v.setGroupCode(g.getGroupCode());
        });
        v.setOwner(username != null && t.getCreatedByLecturer() != null
                && t.getCreatedByLecturer().getUser() != null
                && username.equals(t.getCreatedByLecturer().getUser().getUsername()));
        v.setEditable(username != null && v.isOwner()
                && (t.getStatus() == TopicStatus.PENDING || t.getStatus() == TopicStatus.REJECTED
                    || (t.getStatus() == TopicStatus.APPROVED && !hasAnyRegistration(t.getId()))));
        return v;
    }

    private List<TopicView> toViews(List<Topic> topics, String username) {
        List<TopicView> views = new ArrayList<>();
        for (Topic t : topics) {
            views.add(toView(t, username));
        }
        return views;
    }

    private LocalDateTime now() { return LocalDateTime.now(ZONE); }
}