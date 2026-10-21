package vn.edu.demo.topicmanagement.entity;

/**
 * Vai trò của tài khoản - khớp với cột {@code roles.code} trong DB.
 * Mỗi {@link UserRole} tương ứng một giá trị {@link Role#getCode()}.
 *
 * <p>Enum này tồn tại để giữ code cũ và Thymeleaf (dùng {@code hasRole('ADMIN')}) trực quan;
 * thực tế lưu trong DB là bảng {@code roles} qua {@code user_roles}.
 */
public enum UserRole {
    ADMIN("Quản trị viên"),
    TRUONG_KHOA("Trưởng khoa"),
    GIANG_VIEN("Giảng viên"),
    SINH_VIEN("Sinh viên");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Tên vai trò khớp {@code roles.code} trong DB. */
    public String code() {
        return name();
    }

    /** Tra cứu theo mã (ADMIN/TRUONG_KHOA/...). Trả null nếu không khớp. */
    public static UserRole fromCode(String code) {
        if (code == null) return null;
        for (UserRole r : values()) {
            if (r.name().equals(code)) return r;
        }
        return null;
    }
}
