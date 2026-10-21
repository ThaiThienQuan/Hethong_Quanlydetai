package vn.edu.demo.topicmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Nội dung và phạm vi của một thông báo mới. */
public class AnnouncementForm {
    @NotBlank(message = "Vui lòng nhập tiêu đề")
    @Size(max = 250, message = "Tiêu đề tối đa 250 ký tự")
    private String title;

    @NotBlank(message = "Vui lòng nhập nội dung")
    private String content;

    /**
     * Phạm vi thông báo - khớp {@code notifications.type}:
     * {@code SCHOOL} (toàn trường) / {@code FACULTY} (khoa).
     */
    @NotNull(message = "Vui lòng chọn phạm vi")
    private String scope = "SCHOOL";

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getScope() { return scope; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setScope(String scope) { this.scope = scope; }
}
