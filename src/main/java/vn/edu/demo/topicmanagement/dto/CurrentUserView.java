package vn.edu.demo.topicmanagement.dto;

/**
 * Thông tin người dùng đang đăng nhập gọn (tên + chức vụ).
 *
 * <p>Dùng để hiển thị ở thanh trên cùng và ở lời chào của trang Tổng quan.
 * {@code roleCode} là mã vai trò trong DB (ADMIN/TRUONG_KHOA/GIANG_VIEN/SINH_VIEN),
 * dùng để gắn class màu tương ứng trong CSS.
 */
public class CurrentUserView {

    private final String fullName;
    private final String roleCode;
    private final String roleLabel;

    public CurrentUserView(String fullName, String roleCode, String roleLabel) {
        this.fullName = fullName;
        this.roleCode = roleCode;
        this.roleLabel = roleLabel;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getRoleLabel() {
        return roleLabel;
    }
}
