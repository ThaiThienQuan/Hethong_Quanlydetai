package vn.edu.demo.topicmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.entity.AuditLog;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.AuditLogRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Ghi nhật ký các hành động quan trọng (mục 7.7): tạo/duyệt đề tài, chấp thuận đăng ký,
 * phân công, nhập điểm, tổng hợp, công bố, đổi quyền, khóa tài khoản.
 *
 * <p>Dùng {@code REQUIRES_NEW} để nhật ký được ghi kể cả khi transaction nghiệp vụ
 * bị rollback, và để việc ghi log không làm hỏng thao tác chính.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    public static final String TOPIC_CREATE = "TOPIC_CREATE";
    public static final String TOPIC_APPROVE = "TOPIC_APPROVE";
    public static final String TOPIC_REJECT = "TOPIC_REJECT";
    public static final String TOPIC_PUBLISH = "TOPIC_PUBLISH";
    public static final String SUPERVISOR_ASSIGN = "SUPERVISOR_ASSIGN";
    public static final String REGISTRATION_CONFIRM = "REGISTRATION_CONFIRM";
    public static final String REGISTRATION_REJECT = "REGISTRATION_REJECT";
    public static final String REPORT_SUBMIT = "REPORT_SUBMIT";
    public static final String COUNCIL_CREATE = "COUNCIL_CREATE";
    public static final String ASSIGNMENT_CREATE = "ASSIGNMENT_CREATE";
    public static final String EVALUATION_SUBMIT = "EVALUATION_SUBMIT";
    public static final String RESULT_CALCULATE = "RESULT_CALCULATE";
    public static final String RESULT_PUBLISH = "RESULT_PUBLISH";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_LOCK = "USER_LOCK";
    public static final String ROLE_PERMISSION_CHANGE = "ROLE_PERMISSION_CHANGE";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /** Ghi nhật ký theo username của người thực hiện. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, String action, String entity, Long entityId, String detail) {
        try {
            User user = username == null ? null : userRepository.findByUsername(username).orElse(null);
            auditLogRepository.save(new AuditLog(user, action, entity, entityId, detail));
        } catch (RuntimeException e) {
            // Nhật ký không được làm hỏng thao tác nghiệp vụ chính.
            log.warn("Không ghi được nhật ký {} cho {}: {}", action, entity, e.getMessage());
        }
    }
}