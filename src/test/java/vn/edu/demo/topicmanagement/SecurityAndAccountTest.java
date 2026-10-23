package vn.edu.demo.topicmanagement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.demo.topicmanagement.config.AppProperties;
import vn.edu.demo.topicmanagement.dto.PasswordChangeForm;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.entity.UserRole;
import vn.edu.demo.topicmanagement.entity.UserStatus;
import vn.edu.demo.topicmanagement.repository.DepartmentRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.RoleRepository;
import vn.edu.demo.topicmanagement.repository.StudentRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;
import vn.edu.demo.topicmanagement.service.LoginAttemptService;
import vn.edu.demo.topicmanagement.service.ProfileCodeService;
import vn.edu.demo.topicmanagement.service.UserAccountService;

/**
 * Unit tests cho phần 5.1 - xác thực, tài khoản, phân quyền, bảo mật.
 *
 * <p>Mỗi luật có ít nhất một SUCCESS case và một REJECT case.
 * Tất cả là unit test thuần với repository giả lập (mock), không cần DB.
 */
class SecurityAndAccountTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private DepartmentRepository departmentRepository;
    private StudentRepository studentRepository;
    private LecturerRepository lecturerRepository;
    private UserAccountService service;
    private LoginAttemptService attemptService;
    private AppProperties props;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        studentRepository = mock(StudentRepository.class);
        lecturerRepository = mock(LecturerRepository.class);
        props = new AppProperties();
        service = new UserAccountService(userRepository, roleRepository, departmentRepository,
                studentRepository, lecturerRepository,
                new ProfileCodeService(studentRepository, lecturerRepository),
                mock(org.springframework.security.crypto.password.PasswordEncoder.class));
        attemptService = new LoginAttemptService(userRepository, props);
    }

    private User user(String username, UserRole role) {
        User u = new User();
        u.setId(1L);
        u.setUsername(username);
        u.setFullName("Người dùng " + username);
        u.setEmail(username + "@example.edu.vn");
        u.setEnabled(true);
        u.setStatus(UserStatus.ACTIVE);
        u.setRole(role);
        return u;
    }

    // =========================================================================
    // 5.1 — Giới hạn đăng nhập sai: khóa tạm 5 lần / 15 phút
    // =========================================================================

    @Nested
    @DisplayName("5.1 — Khóa tạm sau nhiều lần đăng nhập sai")
    class LoginAttempts {

        @Test
        @DisplayName("SUCCESS — dưới ngưỡng thì chỉ tăng bộ đếm, chưa khóa")
        void belowThresholdOnlyCounts() {
            User u = user("gv01", UserRole.GIANG_VIEN);
            u.setFailedLoginCount(3);
            when(userRepository.findByUsername("gv01")).thenReturn(Optional.of(u));

            attemptService.recordFailure("gv01");

            assertEquals(4, u.getFailedLoginCount());
            assertTrue(u.getLockedUntil() == null, "Chưa đạt ngưỡng thì không được khóa.");
        }

        @Test
        @DisplayName("SUCCESS — đạt ngưỡng thì khóa tạm đúng số phút cấu hình")
        void atThresholdLocksTemporarily() {
            User u = user("gv01", UserRole.GIANG_VIEN);
            u.setFailedLoginCount(4); // lần sai thứ 5 -> đạt maxFailedAttempts = 5
            when(userRepository.findByUsername("gv01")).thenReturn(Optional.of(u));

            attemptService.recordFailure("gv01");

            assertEquals(5, u.getFailedLoginCount());
            assertNotNull(u.getLockedUntil(), "Phải khóa tạm khi đạt ngưỡng.");
            long minutes = java.time.Duration.between(java.time.LocalDateTime.now(), u.getLockedUntil()).toMinutes();
            assertTrue(minutes > 0 && minutes <= props.getSecurity().getLockDurationMinutes(),
                    "Thời gian khóa phải theo cấu hình (15 phút), thực tế: " + minutes);
        }

        @Test
        @DisplayName("REJECT — tài khoản không tồn tại thì không tạo bản ghi, không lộ thông tin")
        void unknownUserIsIgnored() {
            when(userRepository.findByUsername("khongco")).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> attemptService.recordFailure("khongco"));

            verify(userRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("SUCCESS — đăng nhập thành công thì reset bộ đếm và mở khóa")
        void successResetsCounter() {
            User u = user("sv01", UserRole.SINH_VIEN);
            u.setFailedLoginCount(3);
            u.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(10));
            when(userRepository.findByUsername("sv01")).thenReturn(Optional.of(u));

            attemptService.recordSuccess("sv01");

            assertEquals(0, u.getFailedLoginCount());
            assertTrue(u.getLockedUntil() == null, "Đăng nhập thành công phải mở khóa tạm.");
        }
    }

    // =========================================================================
    // 5.1 — Đổi mật khẩu: mới >= 8 ký tự, có chữ và số
    // =========================================================================

    @Nested
    @DisplayName("5.1 — Đổi mật khẩu cá nhân")
    class ChangeOwnPassword {

        private PasswordChangeForm form(String current, String next, String confirm) {
            PasswordChangeForm f = new PasswordChangeForm();
            f.setCurrentPassword(current);
            f.setNewPassword(next);
            f.setConfirmPassword(confirm);
            return f;
        }

        @Test
        @DisplayName("REJECT — mật khẩu mới ngắn hơn 8 ký tự")
        void rejectShortPassword() {
            User u = user("sv01", UserRole.SINH_VIEN);
            when(userRepository.findByUsername("sv01")).thenReturn(Optional.of(u));
            org.springframework.security.crypto.password.PasswordEncoder enc =
                    mock(org.springframework.security.crypto.password.PasswordEncoder.class);
            when(enc.matches("oldpass", u.getPasswordHash())).thenReturn(true);
            UserAccountService svc = new UserAccountService(userRepository, roleRepository, departmentRepository,
                    studentRepository, lecturerRepository,
                    new ProfileCodeService(studentRepository, lecturerRepository), enc);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> svc.changeOwnPassword("sv01", form("oldpass", "Ab1", "Ab1")));
            assertTrue(ex.getMessage().contains("8 ký tự"));
        }

        @Test
        @DisplayName("REJECT — mật khẩu mới không có số")
        void rejectWithoutDigit() {
            User u = user("sv01", UserRole.SINH_VIEN);
            when(userRepository.findByUsername("sv01")).thenReturn(Optional.of(u));
            org.springframework.security.crypto.password.PasswordEncoder enc =
                    mock(org.springframework.security.crypto.password.PasswordEncoder.class);
            when(enc.matches("oldpass", u.getPasswordHash())).thenReturn(true);
            UserAccountService svc = new UserAccountService(userRepository, roleRepository, departmentRepository,
                    studentRepository, lecturerRepository,
                    new ProfileCodeService(studentRepository, lecturerRepository), enc);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> svc.changeOwnPassword("sv01", form("oldpass", "Abcdefgh", "Abcdefgh")));
            assertTrue(ex.getMessage().contains("số"));
        }

        @Test
        @DisplayName("REJECT — mật khẩu hiện tại sai")
        void rejectWrongCurrentPassword() {
            User u = user("sv01", UserRole.SINH_VIEN);
            when(userRepository.findByUsername("sv01")).thenReturn(Optional.of(u));
            org.springframework.security.crypto.password.PasswordEncoder enc =
                    mock(org.springframework.security.crypto.password.PasswordEncoder.class);
            when(enc.matches("sai", u.getPasswordHash())).thenReturn(false);
            UserAccountService svc = new UserAccountService(userRepository, roleRepository, departmentRepository,
                    studentRepository, lecturerRepository,
                    new ProfileCodeService(studentRepository, lecturerRepository), enc);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> svc.changeOwnPassword("sv01", form("sai", "Abcdef12", "Abcdef12")));
            assertTrue(ex.getMessage().contains("không chính xác"));
        }
    }

    // =========================================================================
    // 5.1 — Không cho admin tự khóa / hạ quyền chính mình; không bỏ quyền admin cuối
    // =========================================================================

    @Nested
    @DisplayName("5.1 — Bảo vệ tài khoản quản trị")
    class AdminProtection {

        @Test
        @DisplayName("REJECT — không thể khóa chính tài khoản đang đăng nhập")
        void rejectSelfLock() {
            User u = user("admin", UserRole.ADMIN);
            when(userRepository.findById(1L)).thenReturn(Optional.of(u));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.toggleStatus(1L, "admin"));
            assertTrue(ex.getMessage().contains("đang đăng nhập"));
        }

        @Test
        @DisplayName("REJECT — không thể bỏ vai trò ADMIN của tài khoản admin cuối cùng")
        void rejectDemotingLastAdmin() {
            User admin = user("admin", UserRole.ADMIN);
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
            // Không có admin nào khác
            when(userRepository.findAll()).thenReturn(java.util.List.of(admin));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.assignRole(1L, UserRole.GIANG_VIEN, "nguoiquanly"));
            assertTrue(ex.getMessage().contains("cuối cùng"));
        }

        @Test
        @DisplayName("REJECT — không thể tự đổi vai trò của chính mình")
        void rejectSelfRoleChange() {
            User admin = user("admin", UserRole.ADMIN);
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.assignRole(1L, UserRole.GIANG_VIEN, "admin"));
            assertTrue(ex.getMessage().contains("đang đăng nhập"));
        }
    }
}
