package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;

/** Thông tin thông báo cần hiển thị; không truyền entity lazy vào template. */
public class AnnouncementView {
    private final Long id;
    private final String title;
    private final String content;
    private final String authorName;
    private final String departmentName;
    /** SCHOOL hoặc FACULTY */
    private final String scope;
    /** DRAFT / PUBLISHED / RETRACTED */
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime publishedAt;

    public AnnouncementView(Long id, String title, String content, String authorName, String departmentName,
                            String scope, String status,
                            LocalDateTime createdAt, LocalDateTime publishedAt) {
        this.id = id; this.title = title; this.content = content; this.authorName = authorName;
        this.departmentName = departmentName; this.scope = scope; this.status = status;
        this.createdAt = createdAt; this.publishedAt = publishedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthorName() { return authorName; }
    public String getDepartmentName() { return departmentName; }
    public String getScope() { return scope; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
}
