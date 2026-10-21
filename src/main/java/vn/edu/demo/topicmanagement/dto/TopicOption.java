package vn.edu.demo.topicmanagement.dto;

public class TopicOption {
    private final Long id;
    private final String title;
    private final String departmentName;

    public TopicOption(Long id, String title, String departmentName) {
        this.id = id;
        this.title = title;
        this.departmentName = departmentName;
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDepartmentName() { return departmentName; }
}
