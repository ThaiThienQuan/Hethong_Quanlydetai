package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form fields for an authenticated user's own password change (mục 5.1). */
public class PasswordChangeForm {
    @NotBlank(message = "Nhập mật khẩu hiện tại")
    private String currentPassword;

    // Mật khẩu mới: tối thiểu 8 ký tự, có cả chữ và số (mục 5.1).
    @NotBlank(message = "Nhập mật khẩu mới")
    @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu mới phải có cả chữ và số")
    private String newPassword;

    @NotBlank(message = "Nhập lại mật khẩu mới")
    private String confirmPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
