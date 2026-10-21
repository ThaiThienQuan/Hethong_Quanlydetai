package vn.edu.demo.topicmanagement.config;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.stereotype.Component;
import vn.edu.demo.topicmanagement.service.LoginAttemptService;

/**
 * Xử lý đăng nhập sai (mục 5.1).
 *
 * <p>Nhiệm vụ:
 * <ul>
 *   <li>Tăng bộ đếm đăng nhập sai theo tài khoản và khóa tạm khi vượt ngưỡng
 *       (xem {@link LoginAttemptService}).</li>
 *   <li>Hiển thị thông báo chung chung, không tiết lộ tài khoản có tồn tại hay không:
 *       "Sai tài khoản hoặc mật khẩu hoặc tài khoản bị khóa".</li>
 * </ul>
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    public LoginFailureHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");

        // Tài khoản đang bị khóa tạm: không tăng thêm bộ đếm (nếu không sẽ khóa lại liên tục).
        if (exception instanceof org.springframework.security.authentication.LockedException) {
            int remaining = loginAttemptService.remainingLockMinutes(username);
            request.getSession().setAttribute("loginError",
                    "Tài khoản đang tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau "
                            + remaining + " phút.");
            response.sendRedirect(request.getContextPath() + "/login?error");
            return;
        }

        loginAttemptService.recordFailure(username);

        int remaining = loginAttemptService.remainingLockMinutes(username);
        String message = remaining > 0
                ? "Tài khoản đang tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau "
                        + remaining + " phút."
                : loginAttemptService.genericFailureMessage(exception);

        request.getSession().setAttribute("loginError", message);
        String target = UrlUtils.buildFullRequestUrl(request)
                .replace(request.getRequestURI(), request.getContextPath() + "/login?error");
        response.sendRedirect(target);
    }
}
