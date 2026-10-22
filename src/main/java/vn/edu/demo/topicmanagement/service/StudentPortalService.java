package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.config.AppProperties;
import vn.edu.demo.topicmanagement.dto.PendingRegistrationView;
import vn.edu.demo.topicmanagement.dto.StudentCandidateView;
import vn.edu.demo.topicmanagement.dto.StudentGroupMemberView;
import vn.edu.demo.topicmanagement.dto.StudentGroupView;
import vn.edu.demo.topicmanagement.dto.StudentRegistrationView;
import vn.edu.demo.topicmanagement.dto.TopicOption;
import vn.edu.demo.topicmanagement.entity.GroupMember;
import vn.edu.demo.topicmanagement.entity.GroupStatus;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.Student;
import vn.edu.demo.topicmanagement.entity.StudentGroup;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.entity.TopicStatus;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.GroupMemberRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.StudentGroupRepository;
import vn.edu.demo.topicmanagement.repository.StudentRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.TopicRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/** Nghiệp vụ lập nhóm, đăng ký đề tài và duyệt đăng ký - theo DB {@code HT_QLDT}. */
@Service
public class StudentPortalService {
    private static final Logger log = LoggerFactory.getLogger(StudentPortalService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Số thành viên tối đa khi không đọc được cấu hình (fallback của {@code app.group.max-size}). */
    private static final int FALLBACK_MAX_GROUP_SIZE = 3;
    private static final int FALLBACK_MIN_GROUP_SIZE = 1;

    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final EntityManager entityManager;
    private final AppProperties props;

    public StudentPortalService(StudentGroupRepository groupRepository,
                                GroupMemberRepository memberRepository,
                                TopicRegistrationRepository registrationRepository,
                                TopicRepository topicRepository,
                                UserRepository userRepository,
                                StudentRepository studentRepository,
                                RegistrationPeriodRepository periodRepository,
                                EntityManager entityManager,
                                AppProperties props) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.registrationRepository = registrationRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.periodRepository = periodRepository;
        this.entityManager = entityManager;
        this.props = props;
    }

    /** Số thành viên tối đa của một nhóm (A9) đọc từ {@code app.group.max-size}. */
    private int maxGroupSize() {
        return (props != null && props.getGroup().getMaxSize() > 0)
                ? props.getGroup().getMaxSize() : FALLBACK_MAX_GROUP_SIZE;
    }

    /** Số thành viên tối thiểu của một nhóm đọc từ {@code app.group.min-size}. */
    private int minGroupSize() {
        return (props != null && props.getGroup().getMinSize() > 0)
                ? props.getGroup().getMinSize() : FALLBACK_MIN_GROUP_SIZE;
    }

    @Transactional(readOnly = true)
    public StudentGroupView getMyGroup(String username) {
        return memberRepository.findByStudentUsername(username)
                .map(gm -> toGroupView(gm.getGroup(), username, gm.isLeader()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<StudentGroupView> getJoinableGroups(String username) {
        if (memberRepository.findByStudentUsername(username).isPresent()) return List.of();
        List<StudentGroupView> views = new ArrayList<>();
        for (StudentGroup g : groupRepository.findAllByOrderByIdDesc()) {
            if (g.getStatus() != vn.edu.demo.topicmanagement.entity.GroupStatus.DRAFT
                    && g.getStatus() != vn.edu.demo.topicmanagement.entity.GroupStatus.REGISTERED) continue;
            if (registrationRepository.findByGroupIdAndStatusIn(g.getId(), List.of(RegistrationStatus.APPROVED, RegistrationStatus.PENDING)).isPresent()) continue;
            if (memberRepository.countByGroupId(g.getId()) >= maxGroupSize()) continue;
            views.add(toGroupView(g, username, false));
        }
        return views;
    }

    @Transactional
    public void createGroup(String name, String username) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Vui lòng nhập tên nhóm.");
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Tài khoản chưa có hồ sơ sinh viên, vui lòng liên hệ quản trị viên để được bổ sung."));
        if (memberRepository.existsByStudentIdAndPeriodId(student.getId(), pickPeriodIdForRegistration())) {
            throw new IllegalStateException("Bạn đã tham gia nhóm trong đợt này.");
        }
        // Nhóm phải gắn với đúng đợt mà SV đang đăng ký, nếu không sẽ không thấy
        // danh sách đề tài của đợt đó (đề tài chỉ hiện khi cùng period_id).
        RegistrationPeriod period = pickPeriodForStudents();
        if (period == null) {
            throw new IllegalStateException(
                    "Hiện không có đợt đăng ký nào đang mở cho sinh viên, vui lòng liên hệ Trưởng khoa.");
        }

        StudentGroup group = new StudentGroup();
        group.setPeriod(period);
        group.setGroupName(name.trim());
        group.setGroupCode("G_" + System.currentTimeMillis());
        group.setStatus(vn.edu.demo.topicmanagement.entity.GroupStatus.DRAFT);
        groupRepository.save(group);
        GroupMember gm = new GroupMember();
        gm.setGroup(group);
        gm.setStudent(student);
        gm.setPeriodId(period.getId());
        gm.setLeader(true);
        memberRepository.save(gm);
    }

    @Transactional
    public void joinGroup(Long groupId, String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        Student student = studentRepository.findByUserId(user.getId()).orElse(null);
        if (student == null) throw new IllegalStateException(
                "Tài khoản chưa có hồ sơ sinh viên, vui lòng liên hệ quản trị viên để được bổ sung.");
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm."));
        if (memberRepository.countByGroupId(groupId) >= maxGroupSize()) {
            throw new IllegalStateException("Nhóm đã đủ tối đa " + maxGroupSize() + " thành viên.");
        }
        GroupMember gm = new GroupMember();
        gm.setGroup(group);
        gm.setStudent(student);
        gm.setPeriodId(group.getPeriod().getId());
        gm.setLeader(false);
        memberRepository.save(gm);
    }

    @Transactional(readOnly = true)
    public List<TopicOption> getAvailableTopics(String username) {
        // Đề tài phải cùng đợt với nhóm (đặc tả 5.4): nhóm gán vào đợt nào thì chỉ thấy
        // đề tài của đúng đợt đó, nếu không sẽ không thấy danh sách đề tài.
        Long groupPeriodId = getGroupPeriodId(username);
        if (groupPeriodId == null) return List.of();

        List<TopicOption> options = new ArrayList<>();
        List<Topic> topics = topicRepository.findPublishedWithoutApprovedGroup(groupPeriodId,
                TopicStatus.APPROVED, RegistrationStatus.APPROVED);
        for (Topic t : topics) {
            options.add(new TopicOption(t.getId(), t.getTopicName(),
                    t.getDepartment() == null ? "" : t.getDepartment().getName()));
        }
        return options;
    }

    /** period_id của nhóm mà sinh viên đang thuộc (null nếu chưa có nhóm). */
    private Long getGroupPeriodId(String username) {
        return memberRepository.findByStudentUsername(username)
                .map(gm -> gm.getGroup().getPeriod().getId())
                .orElse(null);
    }

    @Transactional
    public void registerTopic(Long topicId, String username) {
        GroupMember gm = memberRepository.findByStudentUsername(username)
                .orElseThrow(() -> new IllegalStateException("Bạn cần tham gia một nhóm trước."));
        if (!gm.isLeader()) throw new IllegalStateException("Chỉ nhóm trưởng mới được đăng ký đề tài.");
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài."));
        RegistrationPeriod period = topic.getPeriod();
        if (!topic.isPublished() || period.getStatus() != PeriodStatus.PUBLISHED) {
            throw new IllegalStateException("Đề tài không nằm trong đợt đang mở đăng ký cho sinh viên.");
        }
        // Đề tài phải cùng đợt với nhóm (đặc tả 5.4).
        if (!period.getId().equals(gm.getGroup().getPeriod().getId())) {
            throw new IllegalStateException("Đề tài không thuộc đợt đăng ký của nhóm bạn. "
                    + "Nhóm đang đăng ký trong đợt \"" + gm.getGroup().getPeriod().getName() + "\".");
        }
        TopicRegistration reg = registrationRepository.findByGroupIdAndStatusIn(gm.getGroup().getId(),
                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED)).orElse(new TopicRegistration());
        reg.setGroup(gm.getGroup());
        reg.setTopic(topic);
        reg.setStatus(RegistrationStatus.PENDING);
        registrationRepository.save(reg);
    }

    /**
     * Hủy đăng ký khi còn PENDING (đặc tả 5.4). Chỉ nhóm trưởng được hủy,
     * và đăng ký đã APPROVED thì không hủy được.
     */
    @Transactional
    public void cancelRegistration(Long registrationId, String username) {
        GroupMember gm = memberRepository.findByStudentUsername(username)
                .orElseThrow(() -> new IllegalStateException("Bạn cần tham gia một nhóm trước."));
        if (!gm.isLeader()) throw new IllegalStateException("Chỉ nhóm trưởng mới được hủy đăng ký.");
        TopicRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký."));
        if (!reg.getGroup().getId().equals(gm.getGroup().getId())) {
            throw new IllegalStateException("Chỉ hủy được đăng ký của nhóm bạn.");
        }
        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException(
                    "Chỉ hủy được khi đăng ký còn chờ duyệt (PENDING).");
        }
        reg.setStatus(RegistrationStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public List<PendingRegistrationView> getPendingRegistrations() {
        List<PendingRegistrationView> views = new ArrayList<>();
        for (TopicRegistration reg : registrationRepository.findAllPendingWithDetails(RegistrationStatus.PENDING)) {
            Topic t = reg.getTopic();
            views.add(toPendingView(reg, t));
        }
        return views;
    }

    /** Hàng đợi chờ duyệt dành riêng cho GVHD (cả hai GVHD đều thấy). */
    @Transactional(readOnly = true)
    public List<PendingRegistrationView> getPendingForSupervisor(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản.")).getId();
        List<PendingRegistrationView> views = new ArrayList<>();
        for (TopicRegistration reg : registrationRepository.findPendingForSupervisor(RegistrationStatus.PENDING, userId)) {
            views.add(toPendingView(reg, reg.getTopic()));
        }
        return views;
    }

    private PendingRegistrationView toPendingView(TopicRegistration reg, Topic t) {
        return new PendingRegistrationView(
                reg.getId(),
                reg.getGroup().getGroupName(),
                leaderNameOf(reg.getGroup()),
                collectMembers(reg.getGroup().getId()),
                t.getTopicName(),
                t.getDescription(),
                t.getDepartment() == null ? "" : t.getDepartment().getName(),
                t.getPeriod().getName(),
                t.getPeriod().getPeriodType().getDisplayName(),
                reg.getRegisteredAt());
    }

    /**
     * GVHD chấp thuận đăng ký (đặc tả 5.4): đăng ký APPROVED, gán {@code student_groups.topic_id},
     * nhóm chuyển APPROVED và tự từ chối các đăng ký PENDING khác của cùng đề tài -
     * tất cả trong một transaction. Chỉ GVHD của đề tài đó mới được quyền (cả hai GVHD).
     */
    @Transactional
    public void approveRegistration(Long id, String username) {
        log.info("GVHD {} goi approveRegistration(id={})", username, id);
        TopicRegistration reg = getPendingRegistrationFor(id, username);
        Topic topic = reg.getTopic();
        StudentGroup group = reg.getGroup();

        // (1) Gán topic_id vào nhóm, chuyển nhóm sang APPROVED
        group.setTopic(topic);
        group.setStatus(GroupStatus.APPROVED);
        groupRepository.save(group);

        // (2) Duyệt đăng ký này — saveAndFlush rồi clear để JPA không nhầm entity
        reg.setStatus(RegistrationStatus.APPROVED);
        reg.setRejectionReason(null);
        reg.setApprovedAt(LocalDateTime.now(ZONE));
        registrationRepository.saveAndFlush(reg);
        entityManager.clear(); // reset persistence context trước khi sửa các entity khác

        // (3) Từ chối các đăng ký PENDING khác của cùng đề tài (cùng transaction)
        for (TopicRegistration other : registrationRepository
                .findByTopicIdAndStatusIn(topic.getId(), List.of(RegistrationStatus.PENDING))) {
            other.setStatus(RegistrationStatus.REJECTED);
            other.setRejectionReason("Đề tài đã được duyệt cho nhóm khác.");
            registrationRepository.save(other);
        }
        log.info("approveRegistration(id={}) thanh cong", id);
    }

    /**
     * GVHD từ chối đăng ký: bắt buộc có lý do, nhóm quay về trạng thái có thể đăng ký đề tài khác.
     * Chỉ GVHD của đề tài đó mới được quyền.
     */
    @Transactional
    public void rejectRegistration(Long id, String reason, String username) {
        log.info("GVHD {} goi rejectRegistration(id={}, reason='{}')", username, id, reason);
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        if (reason.trim().length() > 500) throw new IllegalArgumentException("Lý do từ chối tối đa 500 ký tự.");
        TopicRegistration reg = getPendingRegistrationFor(id, username);
        reg.setStatus(RegistrationStatus.REJECTED);
        reg.setRejectionReason(reason.trim());

        // Nhóm quay về trạng thái có thể đăng ký đề tài khác (đặc tả 5.4)
        StudentGroup group = reg.getGroup();
        group.setStatus(GroupStatus.DRAFT);
        group.setTopic(null);
        groupRepository.save(group);
        log.info("rejectRegistration(id={}) thanh cong", id);
    }

    @Transactional(readOnly = true)
    public List<StudentRegistrationView> getMyRegistrations(String username) {
        GroupMember gm = memberRepository.findByStudentUsername(username).orElse(null);
        if (gm == null) return List.of();
        List<TopicRegistration> regs = registrationRepository.findByGroupId(gm.getGroup().getId());
        List<StudentRegistrationView> views = new ArrayList<>();
        for (TopicRegistration r : regs) {
            views.add(new StudentRegistrationView(r.getId(),
                    r.getGroup().getGroupName(),
                    r.getTopic().getTopicName(),
                    r.getStatus(),
                    r.getRejectionReason(),
                    r.getRegisteredAt()));
        }
        return views;
    }

    /**
     * Nhóm trưởng được gửi đăng ký khi nhóm chưa có đăng ký đang hiệu lực.
     * Đã hủy (CANCELLED) hoặc bị từ chối (REJECTED) thì được gửi lại.
     */
    @Transactional(readOnly = true)
    public boolean canRegister(String username) {
        GroupMember gm = memberRepository.findByStudentUsername(username).orElse(null);
        if (gm == null || !gm.isLeader()) return false;
        return registrationRepository.findByGroupIdAndStatusIn(gm.getGroup().getId(),
                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED)).isEmpty();
    }

    /**
     * Lấy đăng ký đang PENDING và xác nhận người đang đăng nhập là GVHD của đề tài đó.
     * Áp dụng đặc tả 5.4 (A10): cả hai GVHD đều có quyền, GV khác bị từ chối.
     */
    private TopicRegistration getPendingRegistrationFor(Long id, String username) {
        TopicRegistration reg = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài."));
        log.info("reg id={} status={} topic={} user={}", id, reg.getStatus(),
                reg.getTopic() == null ? null : reg.getTopic().getId(), username);
        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException("Chỉ xử lý đăng ký đang chờ duyệt.");
        }
        if (!registrationRepository.existsPendingForSupervisor(reg.getId(), username)) {
            throw new IllegalStateException("Chỉ giảng viên hướng dẫn đề tài này mới chấp thuận hoặc từ chối được.");
        }
        return reg;
    }

    private TopicRegistration getPendingRegistration(Long id) {
        TopicRegistration reg = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài."));
        if (reg.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException("Chỉ xử lý đăng ký đang chờ duyệt.");
        }
        return reg;
    }

    private String leaderNameOf(StudentGroup group) {
        GroupMember lead = memberRepository.findByGroupIdAndLeaderTrue(group.getId()).orElse(null);
        return lead == null || lead.getStudent() == null || lead.getStudent().getUser() == null
                ? "" : lead.getStudent().getUser().getFullName();
    }

    private List<StudentGroupMemberView> collectMembers(Long groupId) {
        List<StudentGroupMemberView> list = new ArrayList<>();
        for (GroupMember gm : memberRepository.findByGroupIdWithStudent(groupId)) {
            Student s = gm.getStudent();
            list.add(new StudentGroupMemberView(s.getId(), s.getUser().getFullName(),
                    s.getStudentCode(), gm.isLeader()));
        }
        return list;
    }

    /**
     * Sinh viên chưa thuộc nhóm nào trong cùng đợt của nhóm đang xét.
     * Chỉ nhóm trưởng mới thấy danh sách này; nhóm đã đủ người thì không cần gợi ý.
     */
    private List<StudentCandidateView> collectCandidates(StudentGroup group, boolean leader) {
        if (!leader) return List.of();
        if (memberRepository.countByGroupId(group.getId()) >= maxGroupSize()) return List.of();
        RegistrationPeriod period = group.getPeriod();
        if (period == null) return List.of();
        List<StudentCandidateView> candidates = new ArrayList<>();
        for (Student s : studentRepository.findStudentsWithoutGroupInPeriod(period.getId(), null)) {
            candidates.add(new StudentCandidateView(s.getId(), s.getStudentCode(),
                    s.getUser().getFullName(), s.getClassName()));
        }
        return candidates;
    }

    private StudentGroupView toGroupView(StudentGroup group, String username, boolean selfLeader) {
        List<String> memberNames = new ArrayList<>();
        List<StudentGroupMemberView> members = new ArrayList<>();
        Long leaderStudentId = null;
        boolean leader = selfLeader;
        for (GroupMember gm : memberRepository.findByGroupIdWithStudent(group.getId())) {
            Student s = gm.getStudent();
            memberNames.add(s.getUser().getFullName());
            members.add(new StudentGroupMemberView(s.getId(), s.getUser().getFullName(),
                    s.getStudentCode(), gm.isLeader()));
            if (gm.isLeader()) {
                leaderStudentId = s.getId();
                if (s.getUser() != null && username.equals(s.getUser().getUsername())) leader = true;
            }
        }
        // Nhóm đã có đăng ký đề tài hiệu lực (PENDING/APPROVED) thì khoá mọi thao tác
        // đổi người - tránh việc điểm số thay đổi giữa kỳ.
        boolean canManageMembers = leader && !hasActiveRegistration(group.getId());
        boolean canTransfer = canManageMembers && members.size() > 1;
        return new StudentGroupView(group.getId(), group.getGroupName(),
                leaderNameOf(group), leaderStudentId, memberNames, members,
                collectCandidates(group, canManageMembers), leader, canManageMembers,
                canTransfer, minGroupSize(), maxGroupSize());
    }

    /** Đợt đang mở cho sinh viên mà nhóm phải gắn vào (nếu có). */
    private RegistrationPeriod pickPeriodForStudents() {
        List<RegistrationPeriod> periods = periodRepository.findPeriodsOpenForStudents(
                LocalDateTime.now(ZONE), PeriodStatus.PUBLISHED);
        return periods.isEmpty() ? null : periods.get(0);
    }

    /** Chọn đợt đang mở cho sinh viên (nếu có) để biết period_id khi lập nhóm. */
    private Long pickPeriodIdForRegistration() {
        RegistrationPeriod p = pickPeriodForStudents();
        return p == null ? null : p.getId();
    }

    /**
     * Nhóm trưởng chuyển quyền nhóm trưởng cho thành viên khác trong nhóm.
     *
     * <p>Chỉ thực hiện được khi nhóm chưa có đăng ký đề tài hiệu lực (PENDING/APPROVED)
     * để tránh việc đổi người chịu trách nhiệm sau khi đã đăng ký.
     */
    @Transactional
    public void transferGroupLeadership(Long groupId, Long newLeaderStudentId, String currentUsername) {
        GroupMember currentLeader = requireLeaderOf(groupId, currentUsername);
        if (hasActiveRegistration(groupId)) {
            throw new IllegalStateException("Chỉ chuyển được quyền nhóm trưởng trước khi nhóm đăng ký đề tài.");
        }
        GroupMember newLeader = null;
        for (GroupMember m : memberRepository.findByGroupIdWithStudent(groupId)) {
            if (m.getStudent() != null && m.getStudent().getId().equals(newLeaderStudentId)) {
                newLeader = m;
                break;
            }
        }
        if (newLeader == null) throw new IllegalArgumentException("Sinh viên không thuộc nhóm.");
        if (newLeader.isLeader()) {
            throw new IllegalStateException("Người được chọn đã là nhóm trưởng hiện tại.");
        }
        currentLeader.setLeader(false);
        newLeader.setLeader(true);
        memberRepository.save(currentLeader);
        memberRepository.save(newLeader);
    }

    /**
     * Nhóm trưởng thêm một sinh viên chưa có nhóm vào nhóm của mình.
     *
     * <p>Điều kiện kiểm tra:
     * <ul>
     *   <li>Người gọi phải là nhóm trưởng của nhóm này.</li>
     *   <li>Nhóm chưa có đăng ký đề tài hiệu lực (PENDING/APPROVED).</li>
     *   <li>Nhóm còn chỗ trống (chưa đạt {@code app.group.max-size}).</li>
     *   <li>Sinh viên được thêm phải có hồ sơ và chưa thuộc nhóm nào trong cùng đợt
     *       (ràng buộc UNIQUE {@code (student_id, period_id)} ở CSDL).</li>
     * </ul>
     */
    @Transactional
    public void addMemberToGroup(Long groupId, Long studentId, String currentUsername) {
        GroupMember currentLeader = requireLeaderOf(groupId, currentUsername);
        StudentGroup group = currentLeader.getGroup();
        if (hasActiveRegistration(group.getId())) {
            throw new IllegalStateException(
                    "Chỉ thay đổi thành viên nhóm được trước khi nhóm đăng ký đề tài.");
        }
        if (memberRepository.countByGroupId(group.getId()) >= maxGroupSize()) {
            throw new IllegalStateException("Nhóm đã đủ tối đa " + maxGroupSize() + " thành viên.");
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên."));
        Long periodId = group.getPeriod().getId();
        if (memberRepository.existsByStudentIdAndPeriodId(student.getId(), periodId)) {
            throw new IllegalStateException(
                    "Sinh viên này đã thuộc một nhóm khác trong đợt đăng ký hiện tại.");
        }
        GroupMember gm = new GroupMember();
        gm.setGroup(group);
        gm.setStudent(student);
        gm.setPeriodId(periodId);
        gm.setLeader(false);
        memberRepository.save(gm);
    }

    /**
     * Nhóm trưởng gỡ một thành viên (khác chính mình) khỏi nhóm.
     *
     * <p>Không xóa được nhóm trưởng: muốn đổi người phụ trách thì dùng chức năng
     * chuyển quyền nhóm trưởng. Nhóm không được xuống dưới {@code app.group.min-size}.
     */
    @Transactional
    public void removeMemberFromGroup(Long groupId, Long studentId, String currentUsername) {
        GroupMember currentLeader = requireLeaderOf(groupId, currentUsername);
        StudentGroup group = currentLeader.getGroup();
        if (hasActiveRegistration(group.getId())) {
            throw new IllegalStateException(
                    "Chỉ thay đổi thành viên nhóm được trước khi nhóm đăng ký đề tài.");
        }
        GroupMember target = null;
        for (GroupMember m : memberRepository.findByGroupIdWithStudent(group.getId())) {
            if (m.getStudent() != null && m.getStudent().getId().equals(studentId)) {
                target = m;
                break;
            }
        }
        if (target == null) throw new IllegalArgumentException("Sinh viên không thuộc nhóm.");
        if (target.isLeader()) {
            throw new IllegalStateException(
                    "Không thể gỡ nhóm trưởng. Hãy dùng chức năng Chuyển quyền nhóm trưởng.");
        }
        if (memberRepository.countByGroupId(group.getId()) <= minGroupSize()) {
            throw new IllegalStateException("Nhóm phải có tối thiểu " + minGroupSize() + " thành viên.");
        }
        memberRepository.delete(target);
    }

    /** Nhóm có đăng ký đề tài đang hiệu lực (PENDING hoặc APPROVED) hay không. */
    private boolean hasActiveRegistration(Long groupId) {
        return registrationRepository.findByGroupIdAndStatusIn(groupId,
                List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED)).isPresent();
    }

    /**
     * Xác nhận người đang đăng nhập là nhóm trưởng của nhóm và trả về membership của họ.
     * Dùng chung cho mọi thao tác quản lý thành viên để chỗ kiểm tra quyền tập trung.
     */
    private GroupMember requireLeaderOf(Long groupId, String currentUsername) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm."));
        GroupMember leader = memberRepository.findByGroupIdAndLeaderTrue(group.getId())
                .orElseThrow(() -> new IllegalStateException("Nhóm chưa có nhóm trưởng."));
        Student leaderStudent = leader.getStudent();
        if (leaderStudent == null || leaderStudent.getUser() == null
                || !currentUsername.equals(leaderStudent.getUser().getUsername())) {
            throw new IllegalStateException("Chỉ nhóm trưởng mới được thực hiện thao tác này.");
        }
        return leader;
    }
}
