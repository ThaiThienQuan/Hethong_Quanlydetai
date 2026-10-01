package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.AppUser;
import blog.hethong_quanlydetai.entity.GroupMember;
import blog.hethong_quanlydetai.entity.Lecturer;
import blog.hethong_quanlydetai.entity.RegistrationPeriod;
import blog.hethong_quanlydetai.entity.Student;
import blog.hethong_quanlydetai.entity.StudentGroup;
import blog.hethong_quanlydetai.entity.Topic;
import blog.hethong_quanlydetai.entity.TopicRegistration;
import blog.hethong_quanlydetai.entity.TopicSupervisor;
import blog.hethong_quanlydetai.repository.AppUserRepository;
import blog.hethong_quanlydetai.repository.DepartmentRepository;
import blog.hethong_quanlydetai.repository.GroupMemberRepository;
import blog.hethong_quanlydetai.repository.LecturerRepository;
import blog.hethong_quanlydetai.repository.RegistrationPeriodRepository;
import blog.hethong_quanlydetai.repository.StudentGroupRepository;
import blog.hethong_quanlydetai.repository.StudentRepository;
import blog.hethong_quanlydetai.repository.TopicRegistrationRepository;
import blog.hethong_quanlydetai.repository.TopicRepository;
import blog.hethong_quanlydetai.repository.TopicSupervisorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TopicWorkflowService {
    private static final List<String> ACTIVE_REGISTRATION_STATUSES = List.of("PENDING", "APPROVED");

    private final TopicRepository topicRepository;
    private final DepartmentRepository departmentRepository;
    private final LecturerRepository lecturerRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final StudentRepository studentRepository;
    private final AppUserRepository userRepository;

    public TopicWorkflowService(TopicRepository topicRepository, DepartmentRepository departmentRepository,
                                LecturerRepository lecturerRepository, TopicSupervisorRepository supervisorRepository,
                                RegistrationPeriodRepository periodRepository,
                                TopicRegistrationRepository registrationRepository,
                                StudentGroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
                                StudentRepository studentRepository, AppUserRepository userRepository) {
        this.topicRepository = topicRepository;
        this.departmentRepository = departmentRepository;
        this.lecturerRepository = lecturerRepository;
        this.supervisorRepository = supervisorRepository;
        this.periodRepository = periodRepository;
        this.registrationRepository = registrationRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Topic> findTopics(String username, boolean canApprove, boolean canCreate) {
        if (canApprove) {
            return topicRepository.findAllByOrderByCreatedAtDesc();
        }
        if (canCreate) {
            Lecturer lecturer = requireLecturer(username);
            return topicRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(topic -> topic.getCreatedBy().getId().equals(lecturer.getId()))
                    .toList();
        }
        return topicRepository.findByStatusOrderByCreatedAtDesc("APPROVED");
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> findTeacherPeriods() {
        LocalDateTime now = LocalDateTime.now();
        return periodRepository.findAllByOrderByStudentStartDesc().stream()
                .filter(period -> "PUBLISHED".equals(period.getStatus())
                        && !now.isBefore(period.getTeacherStart()) && !now.isAfter(period.getTeacherEnd()))
                .toList();
    }

    public Topic propose(String code, String title, String content, Long periodId, String username) {
        if (topicRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Mã đề tài đã được sử dụng.");
        }
        Lecturer lecturer = requireLecturer(username);
        RegistrationPeriod period = requirePeriod(periodId);
        requireOpen(period, false);

        Topic topic = new Topic();
        topic.setCode(code.trim());
        topic.setTitle(title.trim());
        topic.setContent(content.trim());
        topic.setDepartment(departmentRepository.findById(lecturer.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Bộ môn của giảng viên không tồn tại.")));
        topic.setPeriod(period);
        topic.setCreatedBy(lecturer);
        topic.setStatus("PENDING");
        Topic savedTopic = topicRepository.save(topic);

        TopicSupervisor supervisor = new TopicSupervisor();
        supervisor.setTopicId(savedTopic.getId());
        supervisor.setLecturerId(lecturer.getId());
        supervisor.setSupervisorOrder(1);
        supervisorRepository.save(supervisor);
        return savedTopic;
    }

    public void reviewTopic(Long topicId, boolean approved) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Đề tài không tồn tại."));
        if (!"PENDING".equals(topic.getStatus())) {
            throw new IllegalArgumentException("Đề tài này đã được xử lý.");
        }
        topic.setStatus(approved ? "APPROVED" : "REJECTED");
        topic.setApprovedAt(approved ? LocalDateTime.now() : null);
        topicRepository.save(topic);
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> findStudentPeriods() {
        LocalDateTime now = LocalDateTime.now();
        return periodRepository.findAllByOrderByStudentStartDesc().stream()
                .filter(period -> "PUBLISHED".equals(period.getStatus())
                        && !now.isBefore(period.getStudentStart()) && !now.isAfter(period.getStudentEnd()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Topic> findAvailableTopics() {
        Set<Long> openPeriodIds = findStudentPeriods().stream().map(period -> period.getId())
                .collect(java.util.stream.Collectors.toSet());
        return topicRepository.findByStatusOrderByCreatedAtDesc("APPROVED").stream()
                .filter(topic -> openPeriodIds.contains(topic.getPeriod().getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> findGroupsForUser(String username) {
        Student student = requireStudent(username);
        return groupMemberRepository.findByStudentId(student.getId()).stream()
                .filter(member -> member.isLeader())
                .map(member -> member.getGroup())
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentOption> findAvailableStudents(String username) {
        Student currentStudent = requireStudent(username);
        return studentRepository.findAll().stream()
                .filter(student -> !student.getId().equals(currentStudent.getId()))
                .filter(student -> !groupMemberRepository.existsByStudentId(student.getId()))
                .map(student -> userRepository.findById(student.getUserId())
                        .map(user -> new StudentOption(student.getId(), student.getStudentCode(), user.getFullName()))
                        .orElse(null))
                .filter(option -> option != null)
                .toList();
    }

    public StudentGroup createGroup(String name, String code, Long periodId, List<Long> memberIds,
                                    String username) {
        Student leader = requireStudent(username);
        RegistrationPeriod period = requirePeriod(periodId);
        requireOpen(period, true);
        if (groupRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Mã nhóm đã được sử dụng.");
        }

        List<Long> requestedMembers = memberIds == null ? List.of() : memberIds;
        if (requestedMembers.size() > 2 || new HashSet<>(requestedMembers).size() != requestedMembers.size()
                || requestedMembers.contains(leader.getId())) {
            throw new IllegalArgumentException("Nhóm gồm tối đa 03 sinh viên và không được chọn trùng thành viên.");
        }

        List<Student> members = new ArrayList<>();
        for (Long memberId : requestedMembers) {
            Student member = studentRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên được chọn."));
            if (groupMemberRepository.existsByStudentId(memberId)) {
                throw new IllegalArgumentException("Một sinh viên được chọn đã thuộc nhóm khác.");
            }
            members.add(member);
        }

        StudentGroup group = new StudentGroup();
        group.setName(name.trim());
        group.setCode(code.trim());
        group.setPeriodId(periodId);
        group.setStatus("DRAFT");
        groupRepository.save(group);

        saveMember(group, leader, true);
        members.forEach(member -> saveMember(group, member, false));
        return group;
    }

    public void registerGroup(Long groupId, Long topicId, String username) {
        Student student = requireStudent(username);
        GroupMember member = groupMemberRepository.findByGroupIdAndStudentId(groupId, student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Bạn không thuộc nhóm này."));
        if (!member.isLeader()) {
            throw new IllegalArgumentException("Chỉ nhóm trưởng mới được đăng ký đề tài.");
        }

        StudentGroup group = member.getGroup();
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Đề tài không tồn tại."));
        if (!"APPROVED".equals(topic.getStatus()) || !group.getPeriodId().equals(topic.getPeriod().getId())) {
            throw new IllegalArgumentException("Đề tài chưa được duyệt hoặc không thuộc đợt của nhóm.");
        }
        requireOpen(requirePeriod(group.getPeriodId()), true);
        if (registrationRepository.existsByGroupIdAndStatusIn(groupId, ACTIVE_REGISTRATION_STATUSES)) {
            throw new IllegalArgumentException("Nhóm đã có đăng ký đang chờ hoặc đã được xác nhận.");
        }

        TopicRegistration registration = new TopicRegistration();
        registration.setGroupId(groupId);
        registration.setTopicId(topicId);
        registration.setStatus("PENDING");
        registration.setRegisteredAt(LocalDateTime.now());
        registrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<ConfirmationOption> findPendingConfirmations(String username) {
        Lecturer lecturer = requireLecturer(username);
        return registrationRepository.findByStatusOrderByRegisteredAtAsc("PENDING").stream()
                .filter(registration -> isSupervisor(registration.getTopicId(), lecturer.getId()))
                .map(registration -> new ConfirmationOption(
                        registration.getId(),
                        groupRepository.findById(registration.getGroupId()).orElseThrow().getName(),
                        topicRepository.findById(registration.getTopicId()).orElseThrow().getTitle(),
                        registration.getRegisteredAt()))
                .toList();
    }

    public void confirmRegistration(Long registrationId, boolean approved, String username) {
        Lecturer lecturer = requireLecturer(username);
        TopicRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Đăng ký không tồn tại."));
        if (!isSupervisor(registration.getTopicId(), lecturer.getId())) {
            throw new IllegalArgumentException("Chỉ giảng viên hướng dẫn mới được xác nhận đăng ký này.");
        }
        if (!"PENDING".equals(registration.getStatus())) {
            throw new IllegalArgumentException("Đăng ký này đã được xử lý.");
        }

        registration.setStatus(approved ? "APPROVED" : "REJECTED");
        registration.setApprovedBy(lecturer.getUserId());
        registration.setApprovedAt(LocalDateTime.now());
        StudentGroup group = groupRepository.findById(registration.getGroupId()).orElseThrow();
        group.setStatus(approved ? "APPROVED" : "DRAFT");
        groupRepository.save(group);
        registrationRepository.save(registration);
    }

    private boolean isSupervisor(Long topicId, Long lecturerId) {
        return supervisorRepository.existsByTopicIdAndLecturerId(topicId, lecturerId)
                || topicRepository.findById(topicId)
                .map(topic -> topic.getCreatedBy().getId().equals(lecturerId)).orElse(false);
    }

    private void saveMember(StudentGroup group, Student student, boolean leader) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setStudent(student);
        member.setLeader(leader);
        groupMemberRepository.save(member);
    }

    private Lecturer requireLecturer(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        return lecturerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Hồ sơ giảng viên không tồn tại."));
    }

    private Student requireStudent(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Hồ sơ sinh viên không tồn tại."));
    }

    private RegistrationPeriod requirePeriod(Long periodId) {
        return periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Đợt đăng ký không tồn tại."));
    }

    private void requireOpen(RegistrationPeriod period, boolean studentPhase) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = studentPhase ? period.getStudentStart() : period.getTeacherStart();
        LocalDateTime end = studentPhase ? period.getStudentEnd() : period.getTeacherEnd();
        if (!"PUBLISHED".equals(period.getStatus()) || now.isBefore(start) || now.isAfter(end)) {
            throw new IllegalArgumentException("Đợt đăng ký hiện không mở cho thao tác này.");
        }
    }

    public record StudentOption(Long id, String code, String fullName) {
    }

    public record ConfirmationOption(Long id, String groupName, String topicTitle, LocalDateTime registeredAt) {
    }
}