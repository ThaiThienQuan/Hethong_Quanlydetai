package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;
import vn.edu.demo.topicmanagement.entity.UserRole;
import vn.edu.demo.topicmanagement.entity.UserStatus;

/** Safe, read-only account details for the currently authenticated user. */
public class AccountProfileView {
    private final String username;
    private final String fullName;
    private final String email;
    private final String phone;
    private final UserRole role;
    private final UserStatus status;
    private final String studentCode;
    private final String lecturerCode;
    private final String departmentName;
    private final LocalDateTime createdAt;

    public AccountProfileView(String username, String fullName, String email, String phone,
                              UserRole role, UserStatus status, String studentCode, String lecturerCode,
                              String departmentName, LocalDateTime createdAt) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.studentCode = studentCode;
        this.lecturerCode = lecturerCode;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public String getStudentCode() { return studentCode; }
    public String getLecturerCode() { return lecturerCode; }
    public String getDepartmentName() { return departmentName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
