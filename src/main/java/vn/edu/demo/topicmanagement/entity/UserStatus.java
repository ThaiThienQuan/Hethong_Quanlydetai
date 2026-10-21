package vn.edu.demo.topicmanagement.entity;

/**
 * Trạng thái tài khoản - khớp với {@code users.status} trong DB (ACTIVE / LOCKED).
 *
 * <p>Enum cũ có {@code INACTIVE}; với DB mới chỉ ACTIVE / LOCKED - tuy nhiên helper
 * {@link #fromCode(String)} vẫn chấp nhận "INACTIVE" để tương thích code cũ (coi như ACTIVE).
 */
public enum UserStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động"),
    LOCKED("Bị khóa");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Cột {@code users.status} lưu ACTIVE / LOCKED - map về enum. */
    public static UserStatus fromCode(String code) {
        if (code == null) return ACTIVE;
        return switch (code.toUpperCase()) {
            case "LOCKED" -> LOCKED;
            case "INACTIVE" -> INACTIVE;
            default -> ACTIVE;
        };
    }
}
