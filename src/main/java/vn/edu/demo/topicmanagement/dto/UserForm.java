package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.demo.topicmanagement.entity.UserRole;

/** Dữ liệu form Admin tạo / sửa tài khoản. */
public class UserForm {

    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
             message = "Tên đăng nhập gồm 3-50 ký tự: chữ không dấu, số, dấu . _ -")
    private String username;

    // Tạo mới: bắt buộc (>= 6 ký tự). Sửa: để trống nghĩa là giữ mật khẩu cũ. Kiểm tra ở Service.
    private String password;

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @NotNull(message = "Vui lòng chọn vai trò")
    private UserRole role;

    // Mã sinh viên / mã giảng viên KHÔNG nhập tay: hệ thống tự cấp (ProfileCodeService)
    // nên không thể bị trùng. Hai field này không còn được form gửi lên.
    private String className;     // lớp của sinh viên (bắt buộc nếu là sinh viên)
    private String major;         // ngành học của sinh viên (bắt buộc nếu là sinh viên)
    private Long departmentId;    // bắt buộc nếu không phải Admin

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public UserRole getRole() { return role; }
    public String getClassName() { return className; }
    public String getMajor() { return major; }
    public Long getDepartmentId() { return departmentId; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(UserRole role) { this.role = role; }
    public void setClassName(String className) { this.className = className; }
    public void setMajor(String major) { this.major = major; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
