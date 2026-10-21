package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.entity.Role;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Nạp tài khoản kèm quyền cho Spring Security (mục 5.1).
 *
 * <p>Quyền lấy từ bảng {@code role_permissions} qua các vai trò của người dùng, để thay đổi
 * phân quyền trên màn hình Phân quyền có hiệu lực ngay. Trước mỗi quyền thêm tiền tố
 * {@code ROLE_} cho vai trò để {@code hasRole(...)} hoạt động.
 *
 * <p>Tài khoản {@code LOCKED} hoặc {@code enabled = false} không được đăng nhập.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        // Tài khoản bị khóa tạm do đăng nhập sai nhiều lần: chặn ngay.
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new LoginTemporarilyLockedException(
                    "Tài khoản đang tạm khóa do đăng nhập sai nhiều lần.");
        }

        if (!user.isEnabled() || "LOCKED".equals(user.getStatus())) {
            throw new AccountDisabledException("Tài khoản đã bị khóa hoặc vô hiệu hóa.");
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            for (var permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }

        // Không dùng builder của Spring: dựng UserDetails thủ công để tránh xung đột tên lớp.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                true,
                true,
                true,
                true,
                new java.util.ArrayList<>(authorities));
    }
}