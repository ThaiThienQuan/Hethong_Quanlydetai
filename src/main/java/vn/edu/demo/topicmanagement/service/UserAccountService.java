package vn.edu.demo.topicmanagement.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.AccountProfileView;
import vn.edu.demo.topicmanagement.dto.PasswordChangeForm;
import vn.edu.demo.topicmanagement.dto.UserForm;
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
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository,
                              RoleRepository roleRepository,
                              DepartmentRepository departmentRepository,
                              StudentRepository studentRepository,
                              LecturerRepository lecturerRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
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
        if (form.getNewPassword() == null || form.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới nhập lại không khớp.");
        }
        u.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(u);
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
        // Hồ sơ đi kèm: sinh viên cần mã SV + lớp + ngành; giảng viên cần mã GV + bộ môn.
        if (creating && role == UserRole.SINH_VIEN) {
            if (isBlank(f.getStudentCode())) {
                errors.put("studentCode", "Vui lòng nhập mã sinh viên");
            } else if (studentRepository.existsByStudentCode(f.getStudentCode().trim())) {
                errors.put("studentCode", "Mã sinh viên đã tồn tại");
            }
            if (isBlank(f.getClassName())) {
                errors.put("className", "Vui lòng nhập lớp của sinh viên");
            }
            if (isBlank(f.getMajor())) {
                errors.put("major", "Vui lòng nhập ngành học của sinh viên");
            }
        }
        if (creating && (role == UserRole.GIANG_VIEN || role == UserRole.TRUONG_KHOA)) {
            if (isBlank(f.getLecturerCode())) {
                errors.put("lecturerCode", "Vui lòng nhập mã giảng viên");
            } else if (lecturerRepository.existsByLecturerCode(f.getLecturerCode().trim())) {
                errors.put("lecturerCode", "Mã giảng viên đã tồn tại");
            }
            if (f.getDepartmentId() == null) {
                errors.put("departmentId", "Vui lòng chọn bộ môn");
            }
        }
        String email = blankToNull(f.getEmail());
        if (email != null) {
            boolean duplicated = creating
                    ? userRepository.existsByEmail(email)
                    : userRepository.existsByEmailAndIdNot(email, existingId);
            if (duplicated) errors.put("email", "Email đã được dùng cho tài khoản khác");
        }
        return errors;
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
        if (f.getRole() == UserRole.SINH_VIEN) {
            studentRepository.save(new Student(u, f.getStudentCode().trim(),
                    f.getClassName().trim(), f.getMajor().trim()));
        } else if (f.getRole() == UserRole.GIANG_VIEN || f.getRole() == UserRole.TRUONG_KHOA) {
            Department dept = departmentRepository.findById(f.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Bộ môn không tồn tại"));
            lecturerRepository.save(new Lecturer(u, f.getLecturerCode().trim(), dept));
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
        userRepository.save(u);
    }

    @Transactional
    public void toggleStatus(Long id, String currentUsername) {
        User u = getById(id);
        if (u.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("Không thể khóa chính tài khoản đang đăng nhập.");
        }
        u.setStatus("ACTIVE".equals(u.getRawStatus()) ? UserStatus.LOCKED : UserStatus.ACTIVE);
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
