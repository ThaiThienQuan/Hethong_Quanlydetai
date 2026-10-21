package vn.edu.demo.topicmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller gốc: trang chủ và trang đăng nhập (mục 5.1).
 *
 * <p>Chỉ xử lý hai đường dẫn công khai. Các trang nghiệp vụ theo vai trò sẽ được bổ sung
 * ở các nhóm tiếp theo.
 */
@Controller
public class HomeController {

    /** Trang chủ - yêu cầu đã đăng nhập (SecurityConfig chặn ẩn danh). */
    @GetMapping("/")
    public String home() {
        return "home";
    }

    /** Trang đăng nhập, được {@code SecurityConfig.loginPage("/login")} trỏ tới. */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}