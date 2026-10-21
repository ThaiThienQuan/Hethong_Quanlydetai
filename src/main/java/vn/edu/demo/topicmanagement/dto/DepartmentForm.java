package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Form cho tạo/sửa bộ môn. */
public class DepartmentForm {
    @NotBlank(message = "Mã bộ môn không được để trống.")
    @Size(min = 2, max = 20, message = "Mã bộ môn 2-20 ký tự.")
    @Pattern(regexp = "[A-Z0-9]+", message = "Mã bộ môn chỉ gồm chữ hoa và số.")
    private String code;

    @NotBlank(message = "Tên bộ môn không được để trống.")
    @Size(min = 3, max = 100, message = "Tên bộ môn 3-100 ký tự.")
    private String name;

    private String status = "ACTIVE";

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
