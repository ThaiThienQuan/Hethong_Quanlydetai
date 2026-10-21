package vn.edu.demo.topicmanagement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.CouncilMember;
import vn.edu.demo.topicmanagement.entity.CouncilTopic;
import vn.edu.demo.topicmanagement.entity.GroupMember;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.Report;
import vn.edu.demo.topicmanagement.entity.StudentGroup;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.repository.CouncilMemberRepository;
import vn.edu.demo.topicmanagement.repository.CouncilTopicRepository;
import vn.edu.demo.topicmanagement.repository.GroupMemberRepository;
import vn.edu.demo.topicmanagement.repository.ReportRepository;
import vn.edu.demo.topicmanagement.repository.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final Path reportDirectory;

    public StudentReportService(GroupMemberRepository memberRepository,
                                TopicRegistrationRepository registrationRepository,
                                ReportRepository reportRepository,
                                UserRepository userRepository,
                                StudentRepository studentRepository,
                                TopicSupervisorRepository supervisorRepository,
                                CouncilMemberRepository councilMemberRepository,
                                CouncilTopicRepository councilTopicRepository,
                                @Value("${app.upload.report-dir:uploads/reports}") String reportDirectory) {
        this.memberRepository = memberRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.supervisorRepository = supervisorRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.councilTopicRepository = councilTopicRepository;
        this.reportDirectory = Paths.get(reportDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public StudentReportView getOverview(String username) {
        GroupMember member = memberRepository.findByStudentUsername(username).orElse(null);
        if (member == null) return new StudentReportView(null, null, null, false, null, null, false);

        StudentGroup group = member.getGroup();
        // Giả định: "leader" dựa trên cột is_leader ở GroupMember (chưa có leader riêng trên group)
        boolean leader = member.isLeader();
        TopicRegistration reg = registrationRepository.findByGroupIdAndStatusIn(
                group.getId(), List.of(RegistrationStatus.APPROVED, RegistrationStatus.PENDING)).orElse(null);
        if (reg == null) return new StudentReportView(null, null, null, leader, null, null, false);

        Report report = reportRepository.findByGroupIdOrderByVersionDesc(group.getId()).stream().findFirst().orElse(null);
        return new StudentReportView(reg.getId(), reg.getTopic().getTopicName(), reg.getStatus(),
                leader, report == null ? null : report.getFileName(), report == null ? null : report.getSubmittedAt(),
                report != null && isStoredFileAvailable(report));
    }

    @Transactional
    public void submit(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp báo cáo.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp báo cáo không được vượt quá 10 MB.");

        GroupMember member = memberRepository.findByStudentUsername(username)
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
            vn.edu.demo.topicmanagement.entity.Student s = studentRepository.findById(member.getStudent().getId()).orElse(null);
            if (s != null) report.setSubmittedBy(s);
            report.setReportTitle(originalName);
            report.setFileName(originalName);
            report.setStoredPath(storedName);
            report.setFileSize(file.getSize());
            report.setVersion(nextVersion);
            report.setStatus("SUBMITTED");
            reportRepository.saveAndFlush(report);
        } catch (IOException e) {
            deleteQuietly(target);
            throw new IllegalStateException("Không lưu được tệp báo cáo trên máy chủ.", e);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public ReportDownload getDownload(Long groupId, String username) {
        GroupMember member = memberRepository.findByStudentUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (!member.getGroup().getId().equals(groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ thành viên trong nhóm mới được tải báo cáo.");
        }
        return resolveDownloadByGroup(groupId);
    }

    @Transactional(readOnly = true)
    public ReportDownload getDownloadForLecturer(Long groupId, String username) {
        boolean assignedReviewer = false;
        boolean assignedSupervisor = false;
        boolean councilMember = false;
        try {
            // GVHD: topic của group hiện tại
            groupRepository(groupId).ifPresent(g -> {
                if (g.getTopic() != null) {
                    // Lấy các phân công chấm cho GV hiện tại
                    // assignedReviewer / supervisor: giả định true nếu username thuộc danh sách GVHD/GVPB
                }
            });
        } catch (Exception ignored) {}
        if (!assignedReviewer && !assignedSupervisor && !councilMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không được phân công xem báo cáo này.");
        }
        return resolveDownloadByGroup(groupId);
    }

    /** Truy vấn đơn giản bằng cách duyệt topic gắn với group. */
    private java.util.Optional<StudentGroup> groupRepository(Long groupId) {
        // dùng groupRepository đã có? - chưa inject. Trả Optional.empty tạm thời.
        return java.util.Optional.empty();
    }

    @Transactional(readOnly = true)
    public boolean isDownloadAvailable(Long registrationId) {
        // Stub cũ - giữ cho code compile, không có ý nghĩa ở DB mới
        return false;
    }

    /** Cho {@link ReviewerService} - kiểm tra theo topic (mới hơn theo DB). */
    @Transactional(readOnly = true)
    public boolean isDownloadAvailableByTopic(Long topicId) {
        // Tìm group có topic_id = topicId
        List<Report> reports = reportRepository.search(null, topicId, null);
        if (reports.isEmpty()) return false;
        return isStoredFileAvailable(reports.get(0));
    }

    @Transactional(readOnly = true)
    public List<LecturerReportView> getReportsForSupervisor(String username) {
        List<LecturerReportView> views = new ArrayList<>();
        // giữ chỗ - chưa có liên kết trực tiếp
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

    private static class ArrayList<T> extends java.util.ArrayList<T> { }
}
