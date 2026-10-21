package vn.edu.demo.topicmanagement.dto;

import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.entity.UserRole;
import vn.edu.demo.topicmanagement.entity.UserStatus;

/**
 * Một dòng trong danh sách quản lý tài khoản (mục 5.1).
 *
 * <p>Tách khỏi entity {@link User} vì mã SV/GV và bộ môn nằm ở bảng
 * {@code students}/{@code lecturers} (quan hệ 1-1), còn entity {@code User} chỉ
 * map {@code users} + {@code user_roles}.
 */
public class UserRowView {

    private final Long id;
    private final String username;
    private final String fullName;
    private final String email;
    private final String phone;
    private final UserRole role;
    private final UserStatus status;
    private final String studentCode;
    private final String lecturerCode;
    private final String departmentName;
    private final boolean temporarilyLocked;

    public UserRowView(Long id, String username, String fullName, String email, String phone,
                       UserRole role, UserStatus status, String studentCode, String lecturerCode,
                       String departmentName, boolean temporarilyLocked) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.studentCode = studentCode;
        this.lecturerCode = lecturerCode;
        this.departmentName = departmentName;
        this.temporarilyLocked = temporarilyLocked;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public String getStudentCode() { return studentCode; }
    public String getLecturerCode() { return lecturerCode; }
    public String getDepartmentName() { return departmentName; }
    public boolean isTemporarilyLocked() { return temporarilyLocked; }
}
