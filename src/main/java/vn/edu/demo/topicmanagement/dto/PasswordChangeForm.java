package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Form fields for an authenticated user's own password change. */
public class PasswordChangeForm {
    @NotBlank(message = "Nhập mật khẩu hiện tại")
    private String currentPassword;

    @NotBlank(message = "Nhập mật khẩu mới")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
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
