package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.AccountProfileView;
import vn.edu.demo.topicmanagement.dto.PasswordChangeForm;
import vn.edu.demo.topicmanagement.dto.UserForm;
import vn.edu.demo.topicmanagement.dto.UserRowView;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.entity.Lecturer;
import vn.edu.demo.topicmanagement.entity.Role;
import vn.edu.demo.topicmanagement.entity.Student;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.entity.UserRole;
import vn.edu.demo.topicmanagement.entity.UserStatus;
import vn.edu.demo.topicmanagement.repository.DepartmentRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.RoleRepository;
import vn.edu.demo.topicmanagement.repository.StudentRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Nghiệp vụ quản lý tài khoản (chức năng của Admin).
 *
 * <p>Vai trò lưu qua bảng nối {@code user_roles} - code dùng enum {@link UserRole}
 * để tiện thao tác; helper {@link User#setRole(UserRole)} đồng bộ vào {@code Set<Role>}.
 */
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final ProfileCodeService profileCodeService;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository,
                              RoleRepository roleRepository,
                              DepartmentRepository departmentRepository,
                              StudentRepository studentRepository,
                              LecturerRepository lecturerRepository,
                              ProfileCodeService profileCodeService,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.profileCodeService = profileCodeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Danh sách tài khoản có tìm kiếm + lọc theo vai trò/trạng thái + phân trang
     * (mục 5.1: "danh sách có tìm kiếm (username, họ tên, email), lọc theo vai trò/trạng thái, phân trang").
     */
    @Transactional(readOnly = true)
    public Page<User> search(String keyword, String roleCode, String status, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim().toLowerCase() + "%";
        String st = (status == null || status.isBlank()) ? null : status.trim();
        return userRepository.search(kw, (roleCode == null || roleCode.isBlank()) ? null : roleCode.trim(), st,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
    }

    /**
     * Cùng với {@link #search} nhưng trả về DTO đã gộp hồ sơ SV/GV (mã, bộ môn).
     * Vì {@code open-in-view=false}, mã SV/GV và bộ môn phải được nối sẵn trong transaction.
     */
    @Transactional(readOnly = true)
    public Page<UserRowView> searchWithProfiles(String keyword, String roleCode, String status, int page, int size) {
        Page<User> raw = search(keyword, roleCode, status, page, size);
        List<UserRowView> views = new ArrayList<>();
        for (User u : raw.getContent()) {
            views.add(toRowView(u));
        }
        return new PageImpl<>(views, raw.getPageable(), raw.getTotalElements());
    }

    private UserRowView toRowView(User u) {
        String studentCode = null;
        String lecturerCode = null;
        String deptName = null;
        Student s = studentRepository.findByUserId(u.getId()).orElse(null);
        if (s != null) {
            studentCode = s.getStudentCode();
        } else {
            Lecturer l = lecturerRepository.findByUserId(u.getId()).orElse(null);
            if (l != null) {
                lecturerCode = l.getLecturerCode();
                deptName = l.getDepartment() == null ? null : l.getDepartment().getName();
            }
        }
        boolean locked = u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now());
        return new UserRowView(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getStatus(), studentCode, lecturerCode, deptName, locked);
    }

    @Transactional(readOnly = true)
    public List<Department> getDepartments() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public AccountProfileView getOwnProfile(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        return new AccountProfileView(u.getUsername(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getStatus(), u.getStudentCode(), u.getLecturerCode(),
                u.getDepartment() == null ? null : u.getDepartment().getName(), u.getCreatedAt());
    }

    @Transactional
    public void changeOwnPassword(String username, PasswordChangeForm form) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (!passwordEncoder.matches(form.getCurrentPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        validateNewPassword(form.getNewPassword());
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới nhập lại không khớp.");
        }
        u.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        // Đổi mật khẩu thành công thì xóa bộ đếm đăng nhập sai và mở khóa tạm.
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        userRepository.save(u);
    }

    /**
     * Quy tắc mật khẩu mới (mục 5.1): tối thiểu 8 ký tự, phải có cả chữ và số.
     */
    private void validateNewPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
        if (!password.matches(".*[A-Za-z].*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất một chữ cái.");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất một chữ số.");
        }
    }

    public Map<String, String> checkRules(UserForm f, Long existingId) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean creating = (existingId == null);
        UserRole role = f.getRole();

        String pw = f.getPassword();
        if (creating && (isBlank(pw) || pw.length() < 6)) {
            errors.put("password", "Mật khẩu tối thiểu 6 ký tự");
        } else if (!creating && !isBlank(pw) && pw.length() < 6) {
            errors.put("password", "Mật khẩu mới tối thiểu 6 ký tự (hoặc để trống nếu không đổi)");
        }
        if (creating && f.getUsername() != null && userRepository.existsByUsername(f.getUsername().trim())) {
            errors.put("username", "Tên đăng nhập đã tồn tại");
        }
        // Hồ sơ đi kèm: sinh viên cần lớp + ngành; giảng viên cần bộ môn.
        // Mã sinh viên / mã giảng viên do hệ thống tự cấp (ProfileCodeService)
        // nên Admin không nhập tay và không thể tạo mã trùng.
        if (creating && role == UserRole.SINH_VIEN) {
            if (isBlank(f.getClassName())) {
                errors.put("className", "Vui lòng nhập lớp của sinh viên");
            }
            if (isBlank(f.getMajor())) {
                errors.put("major", "Vui lòng nhập ngành học của sinh viên");
            }
        }
        if (creating && (role == UserRole.GIANG_VIEN || role == UserRole.TRUONG_KHOA)) {
            if (f.getDepartmentId() == null) {
                errors.put("departmentId", "Vui lòng chọn bộ môn");
            }
        }
        String email = blankToNull(f.getEmail());
        if (email == null) {
            // users.email NOT NULL + UNIQUE -> để trống sẽ gây lỗi 500 khi insert.
            errors.put("email", "Vui lòng nhập email");
        } else {
            boolean duplicated = creating
                    ? userRepository.existsByEmail(email)
                    : userRepository.existsByEmailAndIdNot(email, existingId);
            if (duplicated) errors.put("email", "Email đã được dùng cho tài khoản khác");
        }
        return errors;
    }

    /**
     * Mã sinh viên sẽ được hệ thống cấp cho tài khoản mới (xem trước trên form).
     */
    @Transactional(readOnly = true)
    public String previewStudentCode() {
        return profileCodeService.nextStudentCode();
    }

    /**
     * Mã giảng viên sẽ được hệ thống cấp cho tài khoản mới (xem trước trên form).
     */
    @Transactional(readOnly = true)
    public String previewLecturerCode() {
        return profileCodeService.nextLecturerCode();
    }

    @Transactional
    public Long create(UserForm f) {
        Map<String, String> errors = checkRules(f, null);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.values().iterator().next());
        }
        User u = new User();
        u.setUsername(f.getUsername().trim());
        u.setPasswordHash(passwordEncoder.encode(f.getPassword()));
        u.setFullName(f.getFullName() == null ? "" : f.getFullName().trim());
        u.setEmail(blankToNull(f.getEmail()));
        u.setPhone(blankToNull(f.getPhone()));
        u.setEnabled(true);
        // Gán vai trò qua bảng nối user_roles — phải dùng thực thể Role managed
        // mới được (User.setRole(...) sẽ tự khởi tạo Role detached, gây TransientPropertyValueException).
        if (f.getRole() != null) {
            Role roleEntity = roleRepository.findByCode(f.getRole().name()).orElse(null);
            if (roleEntity != null) {
                u.getRoles().add(roleEntity);
            }
        }
        userRepository.save(u);

        // Tạo hồ sơ đi kèm theo vai trò: sinh viên -> students, giảng viên/TK -> lecturers.
        // Thiếu hồ sơ khiến các màn hình của vai trò đó báo lỗi khi người dùng thao tác.
        // Mã SV/GV được hệ thống tự cấp để không bao giờ trùng.
        if (f.getRole() == UserRole.SINH_VIEN) {
            profileCodeService.createStudentProfile(u, f.getClassName().trim(), f.getMajor().trim());
        } else if (f.getRole() == UserRole.GIANG_VIEN || f.getRole() == UserRole.TRUONG_KHOA) {
            Department dept = departmentRepository.findById(f.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Bộ môn không tồn tại"));
            profileCodeService.createLecturerProfile(u, dept);
        }
        return u.getId();
    }

    @Transactional(readOnly = true)
    public UserForm getFormForEdit(Long id) {
        User u = getById(id);
        UserForm f = new UserForm();
        f.setUsername(u.getUsername());
        f.setFullName(u.getFullName());
        f.setEmail(u.getEmail());
        f.setPhone(u.getPhone());
        f.setRole(u.getRole());
        return f;
    }

    @Transactional
    public void update(Long id, UserForm f) {
        User u = getById(id);
        u.setFullName(f.getFullName() == null ? u.getFullName() : f.getFullName().trim());
        u.setEmail(blankToNull(f.getEmail()));
        u.setPhone(blankToNull(f.getPhone()));
        if (!isBlank(f.getPassword())) {
            u.setPasswordHash(passwordEncoder.encode(f.getPassword()));
        }
        // Gán lại vai trò nếu được chỉ định và khác vai trò hiện tại (mục 5.1 - "gán vai trò").
        if (f.getRole() != null && u.getRole() != f.getRole()) {
            applyRole(u, f.getRole());
        }
        userRepository.save(u);
    }

    /**
     * Gán vai trò cho tài khoản (mục 5.1).
     * Không cho hạ quyền chính mình và không cho bỏ vai trò ADMIN của tài khoản admin cuối cùng.
     */
    @Transactional
    public void assignRole(Long id, UserRole newRole, String performedBy) {
        User u = getById(id);
        if (newRole == null) {
            throw new IllegalArgumentException("Vui lòng chọn vai trò.");
        }
        if (u.getUsername().equals(performedBy)) {
            throw new IllegalStateException("Không thể tự đổi vai trò của tài khoản đang đăng nhập.");
        }
        if (u.getRole() == UserRole.ADMIN && newRole != UserRole.ADMIN && isLastAdmin(u)) {
            throw new IllegalStateException(
                    "Không thể bỏ vai trò Quản trị của tài khoản quản trị cuối cùng.");
        }
        applyRole(u, newRole);
        userRepository.save(u);
    }

    /** Còn tài khoản ADMIN nào khác không? (dùng để chặn bỏ quyền admin cuối cùng) */
    private boolean isLastAdmin(User user) {
        long admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .filter(u -> !u.getId().equals(user.getId()))
                .count();
        return admins == 0;
    }

    private void applyRole(User u, UserRole role) {
        Role roleEntity = roleRepository.findByCode(role.name()).orElse(null);
        if (roleEntity == null) {
            throw new IllegalStateException("Vai trò " + role.name() + " chưa được tạo trong hệ thống.");
        }
        u.getRoles().clear();
        u.getRoles().add(roleEntity);
    }

    /**
     * Mật khẩu mặc định khi Admin đặt lại cho sinh viên / giảng viên / trưởng khoa.
     * <p>Chỉ tồn tại ở phía server, KHÔNG hiển thị ra giao diện cho người dùng khác.
     * Người dùng muốn đổi thì tự đổi ở trang /account/password sau khi đăng nhập.
     */
    public static final String DEFAULT_PASSWORD = "123456";

    /**
     * Admin đặt lại mật khẩu cho một tài khoản về mật khẩu mặc định {@link #DEFAULT_PASSWORD}.
     *
     * <p>Mật khẩu luôn được băm bằng BCrypt, không bao giờ lưu dạng rõ.
     * Đặt lại xong thì mở khóa tạm và xóa bộ đếm đăng nhập sai để tài khoản đăng nhập lại được ngay.
     */
    @Transactional
    public void resetPasswordToDefault(Long id, String performedBy) {
        User u = getById(id);
        u.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        // Cho tài khoản đăng nhập lại được ngay.
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        userRepository.save(u);
    }

    /** Ghi đè cũ - không còn dùng, giữ lại cho tương thích. */
    @Deprecated
    @Transactional
    public void resetPassword(Long id, String newPassword, String performedBy) {
        User u = getById(id);
        if (isBlank(newPassword)) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu mới.");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới tối thiểu 6 ký tự.");
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải có cả chữ và số.");
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        userRepository.save(u);
    }

    @Transactional
    public void toggleStatus(Long id, String currentUsername) {
        User u = getById(id);
        if (u.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("Không thể khóa chính tài khoản đang đăng nhập.");
        }
        // Không khóa tài khoản quản trị cuối cùng của hệ thống.
        boolean locking = "ACTIVE".equals(u.getRawStatus());
        if (locking && u.getRole() == UserRole.ADMIN && isLastAdmin(u)) {
            throw new IllegalStateException(
                    "Không thể khóa tài khoản quản trị cuối cùng của hệ thống.");
        }
        u.setStatus(locking ? UserStatus.LOCKED : UserStatus.ACTIVE);
        userRepository.save(u);
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        User u = getById(id);
        if (u.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("Không thể xóa tài khoản đang đăng nhập.");
        }
        userRepository.delete(u);
        userRepository.flush();
    }

    private User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
