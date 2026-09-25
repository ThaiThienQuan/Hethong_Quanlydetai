package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.GroupMember;
import blog.hethong_quanlydetai.entity.Report;
import blog.hethong_quanlydetai.repository.GroupMemberRepository;
import blog.hethong_quanlydetai.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final GroupMemberRepository groupMemberRepository;

    public ReportService(ReportRepository reportRepository,
                         GroupMemberRepository groupMemberRepository) {
        this.reportRepository = reportRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    public List<Report> findAll() {
        return reportRepository.findAll();
    }

    public Report findById(Long id) {
        return reportRepository.findById(id).orElseThrow();
    }

    public Report save(Report report) {
        GroupMember member = groupMemberRepository
                .findByGroupIdAndStudentId(report.getGroupId(), report.getSubmittedBy())
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên không thuộc nhóm này."));

        if (!member.isLeader()) {
            throw new IllegalArgumentException("Chỉ nhóm trưởng mới được nộp báo cáo.");
        }

        if (report.getSubmittedAt() == null) {
            report.setSubmittedAt(LocalDateTime.now());
        }

        if (report.getStatus() == null || report.getStatus().isBlank()) {
            report.setStatus("SUBMITTED");
        }

        if (report.getVersion() == null) {
            report.setVersion(1);
        }

        return reportRepository.save(report);
    }

    public void delete(Long id) {
        reportRepository.deleteById(id);
    }
}