package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.LinkedHashSet;
import java.util.Set;

/** Bảng {@code users}. Mật khẩu luôn lưu ở dạng BCrypt ({@code password_hash}). */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private java.time.LocalDateTime lockedUntil;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private java.time.LocalDateTime updatedAt;

    /** Bảng nối {@code user_roles}. Một tài khoản có thể mang nhiều vai trò. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @OrderBy("code asc")
    private Set<Role> roles = new LinkedHashSet<>();

    /** Hồ sơ sinh viên (1-1) - không map FK trực tiếp, dùng service để nạp. */
    @Transient
    private Student student;

    /** Hồ sơ giảng viên (1-1) - không map FK trực tiếp, dùng service để nạp. */
    @Transient
    private Lecturer lecturer;

    public User() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** Alias cho code cũ gọi setPassword. */
    public void setPassword(String password) { this.passwordHash = password; }
    public String getPassword() { return passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    /** DB lưu String (ACTIVE/LOCKED). Code cũ thường xem như enum. */
    public UserStatus getStatus() { return UserStatus.fromCode(status); }
    public void setStatus(UserStatus s) {
        this.status = (s == UserStatus.LOCKED) ? "LOCKED" : "ACTIVE";
    }
    public String getRawStatus() { return status; }
    public void setRawStatus(String status) { this.status = status; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(int v) { this.failedLoginCount = v; }

    public java.time.LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(java.time.LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Lecturer getLecturer() { return lecturer; }
    public void setLecturer(Lecturer lecturer) { this.lecturer = lecturer; }

    /** Mã vai trò chính (mã đầu tiên trong tập roles) - có thể null. */
    @Transient
    public String getPrimaryRoleCode() {
        if (roles == null || roles.isEmpty()) return null;
        return roles.iterator().next().getCode();
    }

    /** Vai trò chính dưới dạng enum {@link UserRole}, hoặc null. */
    @Transient
    public UserRole getRole() { return UserRole.fromCode(getPrimaryRoleCode()); }

    @Transient
    public void setRole(UserRole role) {
        if (roles == null) roles = new LinkedHashSet<>();
        roles.clear();
        if (role != null) {
            Role r = new Role();
            r.setCode(role.name());
            r.setName(role.getLabel());
            roles.add(r);
        }
    }

    /** Kiểm tra user có một trong các mã vai trò. */
    @Transient
    public boolean hasAnyRole(String... codes) {
        if (roles == null || codes == null) return false;
        for (Role r : roles) {
            for (String c : codes) {
                if (c != null && c.equalsIgnoreCase(r.getCode())) return true;
            }
        }
        return false;
    }

    @Transient
    public boolean hasRole(String code) { return hasAnyRole(code); }

    /** Mã sinh viên (lookup từ {@link Student#studentCode}). */
    @Transient
    public String getStudentCode() {
        return student == null ? null : student.getStudentCode();
    }

    /** Mã giảng viên (lookup từ {@link Lecturer#lecturerCode}). */
    @Transient
    public String getLecturerCode() {
        return lecturer == null ? null : lecturer.getLecturerCode();
    }

    /** Bộ môn: lấy từ lecturer. */
    @Transient
    public Department getDepartment() {
        return lecturer == null ? null : lecturer.getDepartment();
    }

    @Transient
    public void setDepartment(Department department) {
        if (this.lecturer != null) this.lecturer.setDepartment(department);
    }
}
