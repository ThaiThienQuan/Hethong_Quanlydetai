package vn.edu.demo.topicmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang trợ giúp &amp; liên hệ.
 *
 * <p>Trang này cố tình cho phép xem <b>kể cả khi chưa đăng nhập</b> (xem
 * {@code SecurityConfig} → {@code requestMatchers("/support/**").permitAll()}):
 * người quên mật khẩu hoặc bị khoá tài khoản không đăng nhập được, nếu chặn trang này
 * thì họ không có cách nào biết phải liên hệ ai, ở đâu, số điện thoại gì.
 *
 * <p>Trang chỉ hiển thị thông tin liên hệ công khai - không có dữ liệu nghiệp vụ nào
 * cần đăng nhập mới xem được.
 */
@Controller
public class SupportController {

    /** Trang trợ giúp: thông tin liên hệ Admin + hướng dẫn xử lý tình huống thường gặp. */
    @GetMapping("/support")
    public String support() {
        return "support/index";
    }
}
