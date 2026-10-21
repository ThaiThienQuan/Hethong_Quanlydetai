package vn.edu.demo.topicmanagement.dto;

/** Tên hiển thị an toàn, không đưa entity tài khoản trực tiếp vào trang. */
public class LecturerOption {
    private final Long id;
    private final String fullName;
    private final String lecturerCode;
    public LecturerOption(Long id, String fullName, String lecturerCode) {
        this.id = id;
        this.fullName = fullName;
        this.lecturerCode = lecturerCode;
    }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getLecturerCode() { return lecturerCode; }
}
