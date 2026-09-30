package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.GroupMember;
import blog.hethong_quanlydetai.entity.Report;
import blog.hethong_quanlydetai.entity.Student;
import blog.hethong_quanlydetai.repository.AppUserRepository;
import blog.hethong_quanlydetai.repository.GroupMemberRepository;
import blog.hethong_quanlydetai.repository.ReportRepository;
import blog.hethong_quanlydetai.repository.StudentRepository;
import blog.hethong_quanlydetai.repository.TopicRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AppUserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TopicRegistrationRepository registrationRepository;

    public ReportService(ReportRepository reportRepository,
                         GroupMemberRepository groupMemberRepository,
                         AppUserRepository userRepository,
                         StudentRepository studentRepository,
                         TopicRegistrationRepository registrationRepository) {
        this.reportRepository = reportRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
    }

    public List<Report> findAll() {
        return reportRepository.findAll();
    }

    public Report findById(Long id) {
        return reportRepository.findById(id).orElseThrow();
    }

    public Report save(Report report, String username) {
        Student student = userRepository.findByUsername(username)
                .flatMap(user -> studentRepository.findByUserId(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Hồ sơ sinh viên không tồn tại."));
        GroupMember member = groupMemberRepository
                .findByGroupIdAndStudentId(report.getGroupId(), student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên không thuộc nhóm này."));

        if (!member.isLeader()) {
            throw new IllegalArgumentException("Chỉ nhóm trưởng mới được nộp báo cáo.");
        }
        if (!registrationRepository.existsByGroupIdAndStatusIn(report.getGroupId(), List.of("APPROVED"))) {
            throw new IllegalArgumentException("Nhóm chưa được xác nhận đề tài nên chưa thể nộp báo cáo.");
        }

        report.setSubmittedBy(student.getId());
        report.setSubmittedAt(LocalDateTime.now());
        report.setStatus("SUBMITTED");
        report.setVersion(1);

        return reportRepository.save(report);
    }

    public List<Report> findVisibleToUser(String username, boolean canManage) {
        if (canManage) {
            return findAll();
        }
        Student student = userRepository.findByUsername(username)
                .flatMap(user -> studentRepository.findByUserId(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Hồ sơ sinh viên không tồn tại."));
        java.util.Set<Long> groupIds = groupMemberRepository.findByStudentId(student.getId()).stream()
                .map(member -> member.getGroup().getId())
                .collect(java.util.stream.Collectors.toSet());
        return reportRepository.findAll().stream()
                .filter(report -> groupIds.contains(report.getGroupId()))
                .toList();
    }

    public void delete(Long id) {
        reportRepository.deleteById(id);
    }
}