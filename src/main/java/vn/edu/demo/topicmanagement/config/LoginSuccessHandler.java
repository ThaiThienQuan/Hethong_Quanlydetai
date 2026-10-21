package vn.edu.demo.topicmanagement.config;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import vn.edu.demo.topicmanagement.service.LoginAttemptService;

/**
 * Đăng nhập thành công → xóa bộ đếm đăng nhập sai (mục 5.1).
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;

    public LoginSuccessHandler(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        loginAttemptService.recordSuccess(authentication.getName());
        // Xóa thông báo lỗi còn sót lại trong session từ lần đăng nhập trước.
        request.getSession().removeAttribute("loginError");
        response.sendRedirect(request.getContextPath() + "/");
    }
}
