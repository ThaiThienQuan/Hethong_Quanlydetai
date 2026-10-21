package vn.edu.demo.topicmanagement.service;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

/**
 * Tài khoản tạm khóa do đăng nhập sai nhiều lần.
 * Spring Security hiển thị thông báo này qua {@code authenticationFailureHandler}.
 */
public class LoginTemporarilyLockedException extends LockedException {

    public LoginTemporarilyLockedException(String message) {
        super(message);
    }
}