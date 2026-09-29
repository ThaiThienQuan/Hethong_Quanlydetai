package blog.hethong_quanlydetai.entity;

public enum RegistrationType {
    COURSE("Môn học"),
    RESEARCH("Nghiên cứu khoa học"),
    TLCN("Tiểu luận chuyên ngành"),
    KLTN("Khóa luận tốt nghiệp");

    private final String displayName;

    RegistrationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}