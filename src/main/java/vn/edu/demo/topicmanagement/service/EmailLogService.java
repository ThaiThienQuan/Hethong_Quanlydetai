package vn.edu.demo.topicmanagement.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.config.AppProperties;
import vn.edu.demo.topicmanagement.dto.EmailLogView;
import vn.edu.demo.topicmanagement.entity.EmailLog;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.repository.EmailLogRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;

/** Nhật ký email gửi kết quả cho sinh viên + nút gửi lại (mục 5.8 &amp; 7.10). */
@Service
public class EmailLogService {

    private final EmailLogRepository emailLogRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final EmailService emailService;
    private final AppProperties props;

    public EmailLogService(EmailLogRepository emailLogRepository,
                           RegistrationPeriodRepository periodRepository,
                           EmailService emailService,
                           AppProperties props) {
        this.emailLogRepository = emailLogRepository;
        this.periodRepository = periodRepository;
        this.emailService = emailService;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public Page<EmailLogView> search(String status, Long periodId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "id"));
        Page<EmailLog> logs = emailLogRepository.search(
                (status == null || status.isBlank()) ? null : status, periodId, pageable);
        return logs.map(this::toView);
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> getPeriods() {
        return periodRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long countFailed() {
        return emailLogRepository.countByStatus("FAILED");
    }

    /** Gửi lại một email (cập nhật đúng dòng cũ, tăng attempt_count). */
    @Transactional
    public void resend(Long emailLogId) {
        EmailLog log = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi email."));
        if ("SENT".equals(log.getStatus())) {
            throw new IllegalStateException("Email này đã gửi thành công, không cần gửi lại.");
        }
        emailService.resend(log);
    }

    /** Gửi lại toàn bộ email đang lỗi. */
    @Transactional
    public int resendAllFailed() {
        List<EmailLog> failed = emailLogRepository.findFailed("FAILED");
        int n = 0;
        for (EmailLog log : failed) {
            emailService.resend(log);
            n++;
        }
        return n;
    }

    public boolean isMailEnabled() {
        return props.getMail().isEnabled();
    }

    private EmailLogView toView(EmailLog e) {
        var student = e.getStudent();
        var user = student == null ? null : student.getUser();
        var group = e.getResult() == null ? null : e.getResult().getGroup();
        var topic = e.getResult() == null ? null : e.getResult().getTopic();
        return new EmailLogView(
                e.getId(),
                e.getResult() == null ? null : e.getResult().getId(),
                user == null ? "" : user.getFullName(),
                student == null ? "" : student.getStudentCode(),
                group == null ? "" : group.getGroupName(),
                topic == null ? "" : topic.getTopicName(),
                e.getToEmail(),
                e.getSubject(),
                e.getStatus(),
                e.getErrorMessage(),
                e.getAttemptCount(),
                e.getCreatedAt(),
                e.getSentAt());
    }
}