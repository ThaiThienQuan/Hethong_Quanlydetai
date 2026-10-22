package vn.edu.demo.topicmanagement.dto;

/**
 * Một bước trên thanh tiến trình quy trình của đợt hiện hành (mục 5.10).
 *
 * @param index       số thứ tự bước (1..6)
 * @param title       tên bước
 * @param description mô tả ngắn
 * @param state       trạng thái: {@code done} (đã xong), {@code current} (đang diễn ra), {@code todo}
 */
public record WorkflowStepView(
        int index,
        String title,
        String description,
        String state) {

    public boolean isDone() {
        return "done".equals(state);
    }

    public boolean isCurrent() {
        return "current".equals(state);
    }

    /**
     * Lớp CSS của bước, dùng cho {@code th:classappend="step.cssClass()"}
     * (Thymeleaf không cho lồng {@code ${...}} bên trong biểu thức ternary).
     */
    public String cssClass() {
        if (isDone()) return "is-done";
        if (isCurrent()) return "is-current";
        return "";
    }
}
