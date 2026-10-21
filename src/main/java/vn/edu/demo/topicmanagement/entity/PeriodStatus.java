package vn.edu.demo.topicmanagement.entity;

/**
 * Trạng thái đợt đăng ký, khớp {@code chk_period_status}: DRAFT / PUBLISHED / CLOSED.
 *
 * <p>Bổ sung {@link #OPEN} như alias ảo cho {@link #PUBLISHED} - code cũ (dự án gốc bạn)
 * dùng "OPEN". Khi lưu DB ta ghi {@code PUBLISHED}; khi đọc DB, nếu nhận "OPEN" ta
 * coi như PUBLISHED.
 */
public enum PeriodStatus {
    DRAFT("Nháp"),
    PUBLISHED("Đã công bố"),
    OPEN("Đã công bố"),
    CLOSED("Đã đóng");

    private final String label;

    PeriodStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Mã thực sự lưu trong DB: OPEN => PUBLISHED. */
    public String persistedName() {
        return this == OPEN ? "PUBLISHED" : name();
    }

    public static PeriodStatus fromPersisted(String value) {
        if (value == null) return DRAFT;
        return switch (value.toUpperCase()) {
            case "PUBLISHED", "OPEN" -> PUBLISHED;
            case "CLOSED" -> CLOSED;
            default -> DRAFT;
        };
    }
}
