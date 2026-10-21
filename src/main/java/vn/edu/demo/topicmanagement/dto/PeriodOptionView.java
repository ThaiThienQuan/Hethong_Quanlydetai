package vn.edu.demo.topicmanagement.dto;

/** Lựa chọn đợt đăng ký cho bộ chọn "Tra cứu theo đợt" (mục 5.8). */
public class PeriodOptionView {
    private final Long id;
    private final String name;
    private final String typeLabel;

    public PeriodOptionView(Long id, String name, String typeLabel) {
        this.id = id;
        this.name = name;
        this.typeLabel = typeLabel;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    /** Nhãn loại đợt: Môn học / NCKH / Tiểu luận chuyên ngành / Khóa luận tốt nghiệp. */
    public String getTypeLabel() { return typeLabel; }
}
