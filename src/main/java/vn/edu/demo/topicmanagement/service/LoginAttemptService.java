package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Bộ đếm đăng nhập sai và khóa tạm theo tài khoản (mục 5.1 - "giới hạn đăng nhập sai
 * liên tiếp, ví dụ 5 lần/15 phút, bằng cơ chế tự viết nhỏ, không thêm thư viện").
 *
 * <p>Quy tắc:
 * <ul>
 *   <li>Mỗi lần đăng nhập sai (sai tên đăng nhập hoặc sai mật khẩu) tăng
 *       {@code users.failed_login_count} của tài khoản đó.</li>
 *   <li>Khi số lần sai đạt {@code app.security.max-failed-attempts} (mặc định 5),
 *       đặt {@code users.locked_until = now + app.security.lock-duration-minutes}
 *       (mặc định 15 phút) → tài khoản tạm khóa.</li>
 *   <li>Đăng nhập thành công → reset bộ đếm về 0 và xóa {@code locked_until}.</li>
 * </ul>
 *
 * <p>Không log mật khẩu ở bất kỳ đâu. Chỉ log tên đăng nhập và số lần sai.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final UserRepository userRepository;
    private final vn.edu.demo.topicmanagement.config.AppProperties props;

    public LoginAttemptService(UserRepository userRepository, vn.edu.demo.topicmanagement.config.AppProperties props) {
        this.userRepository = userRepository;
        this.props = props;
    }

    /**
     * Ghi nhận một lần đăng nhập sai và khóa tạm nếu đã vượt ngưỡng.
     * Tài khoản không tồn tại thì bỏ qua (không rò rỉ thông tin tài khoản tồn tại hay không).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username) {
        if (username == null || username.isBlank()) return;
        User user = userRepository.findByUsername(username.trim()).orElse(null);
        if (user == null) return;

        int attempts = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(attempts);
        int max = props.getSecurity().getMaxFailedAttempts();

        if (attempts >= max) {
            LocalDateTime until = LocalDateTime.now()
                    .plusMinutes(props.getSecurity().getLockDurationMinutes());
            user.setLockedUntil(until);
            log.info("Tài khoản {} bị khóa tạm đến {} sau {} lần đăng nhập sai.",
                    user.getUsername(), until, attempts);
        }
        userRepository.saveAndFlush(user);
    }

    /** Đăng nhập thành công → xóa bộ đếm và mở khóa tạm. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String username) {
        if (username == null || username.isBlank()) return;
        userRepository.findByUsername(username.trim()).ifPresent(user -> {
            if (user.getFailedLoginCount() > 0 || user.getLockedUntil() != null) {
                user.setFailedLoginCount(0);
                user.setLockedUntil(null);
                userRepository.saveAndFlush(user);
            }
        });
    }

    /** Số phút còn lại bị khóa tạm (0 = không bị khóa). Dùng để hiển thị thông báo. */
    public int remainingLockMinutes(String username) {
        if (username == null) return 0;
        User user = userRepository.findByUsername(username.trim()).orElse(null);
        if (user == null || user.getLockedUntil() == null) return 0;
        long seconds = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).getSeconds();
        if (seconds <= 0) return 0;
        return (int) Math.ceil(seconds / 60.0);
    }

    /** Thông báo lỗi chuẩn, không lộ thông tin tài khoản có tồn tại hay không. */
    public String genericFailureMessage(AuthenticationException e) {
        return "Sai tài khoản hoặc mật khẩu hoặc tài khoản bị khóa.";
    }
}
