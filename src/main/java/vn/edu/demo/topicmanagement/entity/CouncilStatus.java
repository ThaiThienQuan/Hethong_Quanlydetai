package vn.edu.demo.topicmanagement.entity;

/**
 * Trạng thái hội đồng, khớp {@code chk_councils_status}: DRAFT / READY / FINISHED.
 *
 * <p>Bổ sung {@link #ACTIVE} (alias cho {@link #READY}) và {@link #COMPLETED} (alias cho
 * {@link #FINISHED}) để tương thích code cũ. Khi lưu DB ghi giá trị chuẩn READY/FINISHED.
 */
public enum CouncilStatus {
    DRAFT("Đang lập"),
    READY("Sẵn sàng"),
    ACTIVE("Sẵn sàng"),
    FINISHED("Hoàn tất"),
    COMPLETED("Hoàn tất");

    private final String label;

    CouncilStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String persistedName() {
        if (this == ACTIVE) return "READY";
        if (this == COMPLETED) return "FINISHED";
        return name();
    }

    public static CouncilStatus fromPersisted(String value) {
        if (value == null) return DRAFT;
        return switch (value.toUpperCase()) {
            case "READY", "ACTIVE" -> READY;
            case "FINISHED", "COMPLETED" -> FINISHED;
            default -> DRAFT;
        };
    }
}
