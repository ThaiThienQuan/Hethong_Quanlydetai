package vn.edu.demo.topicmanagement.entity;

/** Loại đợt đăng ký, khớp CHECK {@code chk_period_type}. */
public enum PeriodType {
    MON_HOC("Đề tài môn học"),
    NCKH("Nghiên cứu khoa học (NCKH)"),
    TLCN("Tiểu luận chuyên ngành (TLCN)"),
    KLTN("Khóa luận tốt nghiệp (KLTN)");

    private final String displayName;

    PeriodType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Hạn chót GVPB chỉ thiết lập được cho TLCN và KLTN (mục 5.2). */
    public boolean allowsReviewDeadline() {
        return this == TLCN || this == KLTN;
    }

    /** Ngày báo cáo hội đồng chỉ thiết lập được cho KLTN. */
    public boolean allowsCouncilReportDate() {
        return this == KLTN;
    }
}