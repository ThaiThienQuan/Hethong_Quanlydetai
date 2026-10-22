package vn.edu.demo.topicmanagement.dto;

/**
 * Thẻ số liệu trên trang Tổng quan (mục 5.10 - dành cho Trưởng khoa/admin).
 *
 * @param label nhãn thẻ
 * @param value giá trị hiển thị (đã định dạng sẵn)
 * @param hint  ghi chú nhỏ bên dưới
 * @param tone  màu nhấn: primary / success / warning / danger / info
 */
public record DashboardStatView(
        String label,
        String value,
        String hint,
        String tone) {

    /**
     * Lớp CSS tương ứng với màu nhấn, dùng cho {@code th:classappend="stat.cssClass()"}
     * (Thymeleaf không cho lồng {@code ${...}} bên trong biểu thức ternary).
     */
    public String cssClass() {
        return switch (tone) {
            case "success" -> "is-success";
            case "warning" -> "is-warning";
            case "danger"  -> "is-danger";
            default        -> "is-accent";
        };
    }
}
