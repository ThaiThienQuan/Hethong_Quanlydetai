package vn.edu.demo.topicmanagement.dto;

import java.nio.file.Path;

/** Đường dẫn nội bộ và tên gốc dùng khi controller trả file cho người dùng đã được cấp quyền. */
public class ReportDownload {
    private final Path path;
    private final String fileName;
    public ReportDownload(Path path, String fileName) { this.path = path; this.fileName = fileName; }
    public Path getPath() { return path; }
    public String getFileName() { return fileName; }
}
