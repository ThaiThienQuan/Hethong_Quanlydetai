package vn.edu.demo.topicmanagement.service;

import java.util.List;
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
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.repository.CouncilMemberRepository;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.CouncilTopicRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.ReportRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;

/** Nghiệp vụ lập hội đồng phản biện, thêm thành viên/đề tài, kích hoạt. */
@Service
public class CouncilService {

    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final LecturerRepository lecturerRepository;
    private final ReportRepository reportRepository;
    private final RegistrationPeriodRepository periodRepository;

    public CouncilService(CouncilRepository councilRepository,
                          CouncilMemberRepository memberRepository,
                          CouncilTopicRepository councilTopicRepository,
                          TopicRegistrationRepository registrationRepository,
                          LecturerRepository lecturerRepository,
                          ReportRepository reportRepository,
                          RegistrationPeriodRepository periodRepository) {
        this.councilRepository = councilRepository;
        this.memberRepository = memberRepository;
        this.councilTopicRepository = councilTopicRepository;
        this.registrationRepository = registrationRepository;
        this.lecturerRepository = lecturerRepository;
        this.reportRepository = reportRepository;
        this.periodRepository = periodRepository;
    }

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

    @Transactional
    public Long create(CouncilForm form) {
        RegistrationPeriod period = periodRepository.findById(form.getPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký."));
        if (period.getPeriodType() != PeriodType.TLCN
                && period.getPeriodType() != PeriodType.KLTN) {
            throw new IllegalArgumentException("Chỉ tạo hội đồng cho đợt TLCN/KLTN.");
        }
        Council council = new Council();
        council.setPeriod(period);
        council.setCouncilCode(form.getCouncilCode() == null ? "HD" + System.currentTimeMillis() : form.getCouncilCode().trim());
        council.setCouncilName(form.getCouncilName() == null ? "" : form.getCouncilName().trim());
        council.setReportDate(form.getReportDate());
        council.setStatus(CouncilStatus.DRAFT);
        return councilRepository.save(council).getId();
    }

    @Transactional(readOnly = true)
    public CouncilDetailView getDetail(Long councilId) {
        Council council = getCouncil(councilId);
        List<CouncilMember> members = memberRepository.findByCouncilId(councilId);
        List<LecturerOption> lecturers = lecturerRepository.findAll().stream()
                .map(l -> new LecturerOption(l.getId(), l.getUser().getFullName(), l.getLecturerCode())).toList();
        List<TopicRegistration> periodRegistrations = registrationRepository.findByStatusWithGroup(RegistrationStatus.APPROVED);
        List<CouncilTopic> assignments = councilTopicRepository.findByCouncilId(councilId);
        Set<Long> assignedRegIds = assignments.stream()
                .filter(ct -> ct.getTopic() != null)
                .map(ct -> ct.getTopic().getId())
                .collect(Collectors.toSet());

        List<CouncilRegistrationOption> assigned = new java.util.ArrayList<>();
        List<CouncilRegistrationOption> available = new java.util.ArrayList<>();
        for (TopicRegistration r : periodRegistrations) {
            CouncilRegistrationOption opt = new CouncilRegistrationOption(
                    r.getId(), r.getGroup().getGroupName(), r.getTopic().getTopicName());
            if (assignedRegIds.contains(r.getTopic().getId())) assigned.add(opt);
            else if (hasReport(r)) available.add(opt);
        }
        List<CouncilMemberView> memberViews = members.stream().map(m -> new CouncilMemberView(
                m.getId(),
                m.getLecturer().getUser().getFullName(),
                m.getLecturer().getLecturerCode(),
                m.getRole())).toList();
        return new CouncilDetailView(toSummary(council), memberViews, lecturers, available, assigned);
    }

    @Transactional
    public void addMember(Long councilId, CouncilMemberForm form) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        if (memberRepository.countByCouncilId(councilId) >= 5) {
            throw new IllegalStateException("Một hội đồng có tối đa 5 giảng viên.");
        }
        Lecturer lecturer = lecturerRepository.findById(form.getLecturerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên."));
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
    }

    @Transactional
    public void removeMember(Long councilId, Long memberId) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        CouncilMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên hội đồng."));
        if (!member.getCouncil().getId().equals(councilId)) {
            throw new IllegalArgumentException("Thành viên này không thuộc hội đồng đã chọn.");
        }
        memberRepository.delete(member);
    }

    @Transactional
    public void addTopic(Long councilId, Long topicId) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        CouncilTopic ct = new CouncilTopic();
        ct.setCouncil(council);
        TopicRegistration reg = registrationRepository.findById(topicId == null ? 0L : topicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài."));
        ct.setTopic(reg.getTopic());
        // Bảng council_topics có UNIQUE(topic_id) - một đề tài chỉ vào một hội đồng
        councilTopicRepository.save(ct);
    }

    @Transactional
    public void removeTopic(Long councilId, Long topicId) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        CouncilTopic ct = councilTopicRepository.findByCouncilId(councilId).stream()
                .filter(x -> x.getTopic().getId().equals(topicId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Đề tài chưa được xếp vào hội đồng."));
        councilTopicRepository.delete(ct);
    }

    @Transactional
    public void activate(Long councilId) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        long count = memberRepository.countByCouncilId(councilId);
        if (count < 3 || count > 5) {
            throw new IllegalStateException("Hội đồng phải có từ 3 đến 5 giảng viên.");
        }
        if (memberRepository.findByCouncilIdAndRole(councilId, CouncilRole.CHAIRMAN).isEmpty()
                || memberRepository.findByCouncilIdAndRole(councilId, CouncilRole.SECRETARY).isEmpty()) {
            throw new IllegalStateException("Hội đồng phải có đúng một chủ tịch và một thư ký.");
        }
        if (councilTopicRepository.findByCouncilId(councilId).isEmpty()) {
            throw new IllegalStateException("Hãy xếp ít nhất một đề tài vào hội đồng.");
        }
        council.setStatus(CouncilStatus.READY);
        councilRepository.save(council);
    }

    private boolean hasReport(TopicRegistration registration) {
        if (registration.getTopic() == null) return false;
        List<Report> reports = reportRepository.findByGroupIdOrderByVersionDesc(registration.getGroup().getId());
        if (reports.isEmpty()) return false;
        Report latest = reports.get(0);
        return "SUBMITTED".equals(latest.getStatus());
    }

    private CouncilSummaryView toSummary(Council council) {
        List<String> memberNames = memberRepository.findByCouncilId(council.getId()).stream()
                .map(m -> m.getRole().getDisplayName() + ": " + m.getLecturer().getUser().getFullName()).toList();
        List<String> topicNames = councilTopicRepository.findByCouncilId(council.getId()).stream()
                .map(ct -> ct.getTopic().getTopicName()).toList();
        return new CouncilSummaryView(council.getId(), council.getCouncilName(),
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
}
