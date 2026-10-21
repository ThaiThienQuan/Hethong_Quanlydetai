package vn.edu.demo.topicmanagement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.LecturerReportView;
import vn.edu.demo.topicmanagement.dto.ReportDownload;
import vn.edu.demo.topicmanagement.dto.StudentReportView;
import vn.edu.demo.topicmanagement.config.PermissionCatalog;
import vn.edu.demo.topicmanagement.entity.GroupMember;
import vn.edu.demo.topicmanagement.entity.GroupStatus;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.Report;
import vn.edu.demo.topicmanagement.entity.StudentGroup;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.CouncilMemberRepository;
import vn.edu.demo.topicmanagement.repository.GroupMemberRepository;
import vn.edu.demo.topicmanagement.repository.ReportRepository;
import vn.edu.demo.topicmanagement.repository.StudentGroupRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.TopicSupervisorRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Xử lý nộp và tải báo cáo. Chỉ trưởng nhóm được nộp; thành viên cùng nhóm được tải báo cáo.
 * Tệp được lưu ngoài thư mục upload và tên lưu trên đĩa được sinh ngẫu nhiên để tránh ghi đè.
 */
@Service
public class StudentReportService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final GroupMemberRepository memberRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final StudentGroupRepository groupRepository;
    private final Path reportDirectory;

    public StudentReportService(GroupMemberRepository memberRepository,
                                TopicRegistrationRepository registrationRepository,
                                ReportRepository reportRepository,
                                UserRepository userRepository,
                                TopicSupervisorRepository supervisorRepository,
                                CouncilMemberRepository councilMemberRepository,
                                StudentGroupRepository groupRepository,
                                @Value("${app.storage.dir:./storage/reports}") String reportDirectory) {
        this.memberRepository = memberRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.supervisorRepository = supervisorRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.groupRepository = groupRepository;
        this.reportDirectory = Paths.get(reportDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public StudentReportView getOverview(String username) {
        GroupMember member = memberRepository.findByStudentUsername(username).orElse(null);
        if (member == null) return new StudentReportView(null, null, null, false, null, null, false);

        // SV có thể tham gia nhiều đợt; ưu tiên nhóm đang có đăng ký còn hiệu lực
        // thay vì cứ lấy nhóm mới nhất (có thể thuộc đợt khác chưa đăng ký).
        GroupMember activeMember = pickActiveMembership(username).orElse(member);
        StudentGroup group = activeMember.getGroup();
        boolean leader = activeMember.isLeader();
        TopicRegistration reg = registrationRepository.findByGroupIdAndStatusIn(
                group.getId(), List.of(RegistrationStatus.APPROVED, RegistrationStatus.PENDING)).orElse(null);
        if (reg == null) return new StudentReportView(null, null, null, leader, null, null, false);

        Report report = reportRepository.findByGroupIdOrderByVersionDesc(group.getId()).stream()
                .filter(r -> "SUBMITTED".equals(r.getStatus()))
                .findFirst().orElse(null);
        return new StudentReportView(reg.getId(), reg.getTopic().getTopicName(), reg.getStatus(),
                leader, report == null ? null : report.getFileName(), report == null ? null : report.getSubmittedAt(),
                report != null && isStoredFileAvailable(report));
    }

    /**
     * Chọn membership mà sinh viên này có đăng ký đề tài còn hiệu lực (APPROVED/PENDING),
     * ưu tiên nhóm đã nộp báo cáo. Nhờ vậy màn hình báo cáo luôn mở đúng đợt
     * mà sinh viên đang thực hiện, kể cả khi họ tham gia nhiều đợt.
     */
    private java.util.Optional<GroupMember> pickActiveMembership(String username) {
        return memberRepository.findAllByStudentUsername(username).stream()
                .filter(gm -> registrationRepository.findByGroupIdAndStatusIn(
                        gm.getGroup().getId(),
                        List.of(RegistrationStatus.APPROVED, RegistrationStatus.PENDING)).isPresent())
                .max(Comparator
                        .comparing((GroupMember gm) -> !reportRepository
                                .existsByGroupIdAndStatus(gm.getGroup().getId(), "SUBMITTED"))
                        .thenComparing(gm -> gm.getGroup().getId()));
    }

    @Transactional
    public void submit(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp báo cáo.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp báo cáo không được vượt quá 10 MB.");

        GroupMember member = pickActiveMembership(username)
                .orElseThrow(() -> new IllegalStateException("Bạn chưa tham gia nhóm."));
        StudentGroup group = member.getGroup();
        if (!member.isLeader()) {
            throw new IllegalStateException("Chỉ nhóm trưởng mới được nộp báo cáo.");
        }
        TopicRegistration reg = registrationRepository.findByGroupIdAndStatusIn(
                group.getId(), List.of(RegistrationStatus.APPROVED)).orElse(null);
        if (reg == null || reg.getStatus() != RegistrationStatus.APPROVED) {
            throw new IllegalStateException("Chỉ nộp báo cáo sau khi đăng ký đề tài được duyệt.");
        }
        List<Report> existing = reportRepository.findByGroupIdOrderByVersionDesc(group.getId());
        if (!existing.isEmpty() && isStoredFileAvailable(existing.get(0))) {
            throw new IllegalStateException("Nhóm đã nộp báo cáo cho đề tài này.");
        }

        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = getAllowedExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = reportDirectory.resolve(storedName).normalize();
        if (!target.startsWith(reportDirectory)) throw new IllegalStateException("Đường dẫn lưu tệp không hợp lệ.");

        try {
            Files.createDirectories(reportDirectory);
            file.transferTo(target);
            int nextVersion = reportRepository.findMaxVersionByGroup(group.getId()) + 1;
            Report report = new Report();
            report.setGroup(group);
            // submittedBy là students.id của nhóm trưởng
            report.setSubmittedBy(member.getStudent());
            report.setReportTitle(originalName);
            report.setFileName(originalName);
            report.setStoredPath(storedName);
            report.setFileSize(file.getSize());
            report.setVersion(nextVersion);
            report.setStatus("SUBMITTED");
            reportRepository.saveAndFlush(report);

            // Bản cũ (nếu có) chuyển SUPERSEDED để chỉ còn 1 bản hiện lực mỗi nhóm.
            for (Report older : existing) {
                if ("SUBMITTED".equals(older.getStatus())) {
                    older.setStatus("SUPERSEDED");
                    reportRepository.save(older);
                }
            }

            // Nhóm chuyển SUBMITTED khi có báo cáo đầu tiên (đặc tả 5.5).
            if (group.getStatus() == GroupStatus.APPROVED) {
                group.setStatus(GroupStatus.SUBMITTED);
                groupRepository.saveAndFlush(group);
            }
        } catch (IOException e) {
            deleteQuietly(target);
            throw new IllegalStateException("Không lưu được tệp báo cáo trên máy chủ.", e);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
    }

    /**
     * Tải báo cáo của sinh viên: chỉ thành viên trong cùng nhóm mới được.
     * Tham số là {@code registrationId} (id của topic_registrations) như URL đặt tên.
     */
    @Transactional(readOnly = true)
    public ReportDownload getDownload(Long registrationId, String username) {
        TopicRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký đề tài."));
        GroupMember member = memberRepository.findByStudentUsername(username).orElse(null);
        if (member == null || !member.getGroup().getId().equals(reg.getGroup().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ thành viên trong nhóm mới được tải báo cáo.");
        }
        return resolveDownloadByGroup(reg.getGroup().getId());
    }

    /**
     * Tải báo cáo bởi giảng viên: GVHD của đề tài, thành viên hội đồng phụ trách đề tài,
     * Trưởng khoa hoặc quản trị viên đều được (đặc tả 5.5).
     */
    @Transactional(readOnly = true)
    public ReportDownload getDownloadForLecturer(Long registrationId, String username) {
        TopicRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký đề tài."));
        if (!canLecturerViewTopic(reg.getTopic().getId(), username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không được phân công xem báo cáo này.");
        }
        return resolveDownloadByGroup(reg.getGroup().getId());
    }

    /**
     * Giảng viên này có quyền xem báo cáo của đề tài không: là GVHD, hoặc thành viên
     * hội đồng phản biện phụ trách đề tài, hoặc Trưởng khoa / quản trị viên.
     */
    @Transactional(readOnly = true)
    public boolean canLecturerViewTopic(Long topicId, String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        if (user.hasRole(PermissionCatalog.ROLE_ADMIN) || user.hasRole(PermissionCatalog.ROLE_DEAN)) {
            return true;
        }
        if (supervisorRepository.findTopicIdsByLecturerUsername(username).contains(topicId)) {
            return true;
        }
        return councilMemberRepository.findTopicIdsByLecturerUsername(username).contains(topicId);
    }

    /** Cho {@link ReviewerService} - kiểm tra theo topic (GV phản biện tải báo cáo của đề tài). */
    @Transactional(readOnly = true)
    public boolean isDownloadAvailableByTopic(Long topicId) {
        List<Report> reports = reportRepository.search(null, topicId, null);
        if (reports.isEmpty()) return false;
        return isStoredFileAvailable(reports.get(0));
    }

    /** Báo cáo của các nhóm mà giảng viên đang hướng dẫn - màn hình "Báo cáo hướng dẫn". */
    @Transactional(readOnly = true)
    public List<LecturerReportView> getReportsForSupervisor(String username) {
        List<Long> topicIds = supervisorRepository.findTopicIdsByLecturerUsername(username);
        if (topicIds.isEmpty()) return List.of();

        List<LecturerReportView> views = new java.util.ArrayList<>();
        for (Report report : reportRepository.findByTopicIds(topicIds)) {
            StudentGroup group = report.getGroup();
            TopicRegistration reg = registrationRepository.findByGroupIdAndStatusIn(
                    group.getId(), List.of(RegistrationStatus.APPROVED)).orElse(null);
            Long registrationId = reg != null ? reg.getId()
                    : registrationRepository.findByGroupId(group.getId()).stream()
                        .findFirst().map(TopicRegistration::getId).orElse(null);
            if (registrationId == null) continue;
            views.add(new LecturerReportView(
                    registrationId,
                    group.getGroupName(),
                    group.getTopic() != null ? group.getTopic().getTopicName()
                            : (reg != null ? reg.getTopic().getTopicName() : ""),
                    report.getFileName(),
                    isStoredFileAvailable(report)));
        }
        return views;
    }

    private ReportDownload resolveDownloadByGroup(Long groupId) {
        List<Report> reports = reportRepository.findByGroupIdOrderByVersionDesc(groupId);
        Report report = reports.isEmpty() ? null : reports.get(0);
        if (report == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có báo cáo.");
        }
        Path path = resolveStoredPath(report);
        if (path == null || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp báo cáo.");
        }
        return new ReportDownload(path, report.getFileName());
    }

    private Path resolveStoredPath(Report report) {
        if (report.getStoredPath() == null || report.getStoredPath().isBlank()) return null;
        Path path = reportDirectory.resolve(report.getStoredPath()).normalize();
        return path.startsWith(reportDirectory) ? path : null;
    }

    private boolean isStoredFileAvailable(Report report) {
        Path path = resolveStoredPath(report);
        return path != null && Files.isRegularFile(path);
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) throw new IllegalArgumentException("Tên tệp không hợp lệ.");
        String name = Paths.get(originalName).getFileName().toString().trim();
        if (name.length() > 255) throw new IllegalArgumentException("Tên tệp tối đa 255 ký tự.");
        return name;
    }

    private String getAllowedExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 1 || dot == fileName.length() - 1) throw new IllegalArgumentException("Chỉ nhận tệp PDF, DOC hoặc DOCX.");
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Chỉ nhận tệp PDF, DOC hoặc DOCX.");
        return extension;
    }

    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }
}
