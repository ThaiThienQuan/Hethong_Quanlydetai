package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Bảng {@code reports} - báo cáo đề tài do nhóm trưởng nộp, có số phiên bản tăng dần
 * ({@code UNIQUE (group_id, version)}). Tệp lưu ngoài thư mục {@code static}, tên lưu ngẫu nhiên.
 */
@Entity
@Table(name = "reports", uniqueConstraints =
        @UniqueConstraint(name = "uq_reports_group_version", columnNames = {"group_id", "version"}))
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    /** {@code students.id} của nhóm trưởng đã nộp. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false)
    private Student submittedBy;

    @Column(name = "report_title", nullable = false, length = 200)
    private String reportTitle;

    /** Tên tệp gốc đã làm sạch, dùng để hiển thị khi tải về. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Đường dẫn tương đối trong {@code app.storage.dir}. */
    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private int version = 1;

    @Column(length = 500)
    private String description;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private LocalDateTime submittedAt;

    /** SUBMITTED / SUPERSEDED (bản cũ bị thay khi nộp phiên bản mới). */
    @Column(nullable = false, length = 20)
    private String status = "SUBMITTED";

    public Report() {
    }

    public Long getId() {
        return id;
    }

    public StudentGroup getGroup() {
        return group;
    }

    public void setGroup(StudentGroup group) {
        this.group = group;
    }

    public Student getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Student submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public void setStoredPath(String storedPath) {
        this.storedPath = storedPath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}