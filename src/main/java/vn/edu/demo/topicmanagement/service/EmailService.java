package vn.edu.demo.topicmanagement.service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.config.AppProperties;
import vn.edu.demo.topicmanagement.entity.EmailLog;
import vn.edu.demo.topicmanagement.entity.GroupMember;
import vn.edu.demo.topicmanagement.entity.ReviewResult;
import vn.edu.demo.topicmanagement.entity.Student;
import vn.edu.demo.topicmanagement.repository.EmailLogRepository;
import vn.edu.demo.topicmanagement.repository.GroupMemberRepository;

/**
 * Gửi email thông báo kết quả cho từng sinh viên khi Khoa công bố (mục 5.8 &amp; 7.10).
 *
 * <p>Quy tắc quan trọng:
 * <ul>
 *   <li>Gửi <b>riêng cho từng SV trong nhóm</b> (kể cả thành viên không phải nhóm trưởng).</li>
 *   <li>{@code email_logs} có {@code UNIQUE (result_id, student_id)} nên mỗi kết quả chỉ gửi
 *       một lần cho mỗi SV; bấm "Gửi lại" sẽ cập nhật đúng dòng đó
 *       ({@code attempt_count} tăng).</li>
 *   <li>{@code app.mail.enabled=false} thì không gửi thật, ghi trạng thái
 *       {@code SKIPPED} để demo vẫn thấy đầy đủ luồng.</li>
 *   <li>Lỗi gửi không làm hỏng thao tác công bố - ghi {@code FAILED} và tiếp tục.</li>
 * </ul>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AppProperties props;

    public EmailService(JavaMailSender mailSender,
                        EmailLogRepository emailLogRepository,
                        GroupMemberRepository groupMemberRepository,
                        AppProperties props) {
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.props = props;
    }

    /**
     * Gửi email kết quả cho toàn bộ thành viên nhóm (kể cả không phải nhóm trưởng).
     *
     * @return số SV đã được ghi nhận trong {@code email_logs}
     */
    @Transactional
    public int sendResultToGroup(ReviewResult result) {
        if (result == null || result.getGroup() == null) return 0;
        List<GroupMember> members = groupMemberRepository
                .findByGroupIdWithStudent(result.getGroup().getId());
        int count = 0;
        for (GroupMember gm : members) {
            Student student = gm.getStudent();
            if (student == null || student.getUser() == null) continue;
            String email = student.getUser().getEmail();
            if (email == null || email.isBlank()) {
                recordFailure(result, student, email, "Sinh viên chưa có email");
                continue;
            }
            sendOne(result, student, email);
            count++;
        }
        return count;
    }

    /**
     * Gửi lại một email đã ghi nhận trước đó (dùng cho dòng {@code FAILED}/{@code SKIPPED}).
     */
    @Transactional
    public void resend(EmailLog emailLog) {
        Student student = emailLog.getStudent();
        String email = student != null && student.getUser() != null ? student.getUser().getEmail() : null;
        if (email == null || email.isBlank()) {
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage("Sinh viên chưa có email");
            emailLog.setAttemptCount(emailLog.getAttemptCount() + 1);
            emailLogRepository.save(emailLog);
            return;
        }
        sendOne(emailLog.getResult(), student, email);
    }

    private void sendOne(ReviewResult result, Student student, String to) {
        String subject = buildSubject(result);
        String body = buildBody(result, student);
        EmailLog entry = emailLogRepository
                .findByResultIdAndStudentId(result.getId(), student.getId())
                .orElseGet(() -> new EmailLog(result, student, to, subject));
        entry.setToEmail(to);
        entry.setSubject(subject);
        entry.setAttemptCount(entry.getAttemptCount() + 1);

        if (!props.getMail().isEnabled()) {
            entry.setStatus("SKIPPED");
            entry.setErrorMessage("Cấu hình app.mail.enabled=false nên không gửi thật");
            entry.setSentAt(null);
            emailLogRepository.save(entry);
            log.info("Email bị bỏ qua (app.mail.enabled=false): {} -> {}", to, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.getMail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            entry.setStatus("SENT");
            entry.setErrorMessage(null);
            entry.setSentAt(LocalDateTime.now(ZONE));
            emailLogRepository.save(entry);
        } catch (MailException e) {
            // Không làm hỏng thao tác công bố: ghi FAILED rồi tiếp tục.
            entry.setStatus("FAILED");
            entry.setErrorMessage(truncate(e.getMessage(), 500));
            entry.setSentAt(null);
            emailLogRepository.save(entry);
            log.warn("Gửi email thất bại tới {}: {}", to, e.getMessage());
        }
    }

    private void recordFailure(ReviewResult result, Student student, String email, String reason) {
        String subject = buildSubject(result);
        EmailLog entry = emailLogRepository
                .findByResultIdAndStudentId(result.getId(), student.getId())
                .orElseGet(() -> new EmailLog(result, student, email == null ? "" : email, subject));
        entry.setStatus("FAILED");
        entry.setErrorMessage(truncate(reason, 500));
        entry.setAttemptCount(entry.getAttemptCount() + 1);
        entry.setSentAt(null);
        emailLogRepository.save(entry);
    }

    private String buildSubject(ReviewResult result) {
        String topicName = result.getTopic() == null ? "" : result.getTopic().getTopicName();
        return "Kết quả phản biện đề tài: " + topicName;
    }

    private String buildBody(ReviewResult result, Student student) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kính gửi ").append(student.getUser().getFullName()).append(",\n\n");
        sb.append("Hệ thống thông báo kết quả phản biện đề tài tốt nghiệp:\n\n");
        sb.append("- Đợt: ").append(result.getTopic().getPeriod() == null
                ? "" : result.getTopic().getPeriod().getName()).append('\n');
        sb.append("- Đề tài: ").append(result.getTopic().getTopicName()).append('\n');
        sb.append("- Điểm cuối cùng: ").append(result.getFinalScore()).append('\n');
        sb.append("- Kết quả: ").append("PASSED".equals(result.getResult()) ? "ĐẠT" : "KHÔNG ĐẠT").append('\n');
        if (result.getSummaryComment() != null && !result.getSummaryComment().isBlank()) {
            sb.append("- Nhận xét tổng hợp: ").append(result.getSummaryComment()).append('\n');
        }
        sb.append("\nĐăng nhập để xem đầy đủ: ").append(loginPath()).append('\n');
        sb.append("\nTrân trọng,\nHệ thống Quản lý Đề tài");
        return sb.toString();
    }

    private String loginPath() {
        return "/login";
    }

    private static String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() <= max ? text : text.substring(0, max);
    }
}