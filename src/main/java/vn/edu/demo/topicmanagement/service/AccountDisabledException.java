package vn.edu.demo.topicmanagement.service;

import org.springframework.security.authentication.DisabledException;

/** Tài khoản bị khóa hoặc vô hiệu hóa bởi quản trị viên. */
public class AccountDisabledException extends DisabledException {

    public AccountDisabledException(String message) {
        super(message);
    }
}