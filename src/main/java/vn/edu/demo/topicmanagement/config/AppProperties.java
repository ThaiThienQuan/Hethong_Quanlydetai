package vn.edu.demo.topicmanagement.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình nghiệp vụ đọc từ {@code application.properties} (mục 7.4).
 * Giá trị mặc định ở đây khớp với giả định A1–A14 của đề bài, đổi được bằng
 * file cấu hình mà không phải sửa mã nguồn.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String locale = "vi";
    private String timeZone = "Asia/Ho_Chi_Minh";
    private String datePattern = "dd/MM/yyyy";
    private String dateTimePattern = "dd/MM/yyyy HH:mm";

    private final Group group = new Group();
    private BigDecimal passingScore = new BigDecimal("5.00");
    private String storageDir = "./storage/reports";
    private final Report report = new Report();
    private final Security security = new Security();
    private final Mail mail = new Mail();
    private int pageSize = 10;

    /** Quy tắc kích thước nhóm (A9): mặc định theo đề bài là tối đa 3 thành viên. */
    public static class Group {
        private int minSize = 1;
        private int maxSize = 3;

        public int getMinSize() {
            return minSize;
        }

        public void setMinSize(int minSize) {
            this.minSize = minSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }

    /** Giới hạn tệp báo cáo (A7). */
    public static class Report {
        private List<String> allowedExtensions = List.of("pdf", "doc", "docx");
        private long maxSizeBytes = 20L * 1024 * 1024;

        public List<String> getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(List<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }

        public long getMaxSizeBytes() {
            return maxSizeBytes;
        }

        public void setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
        }
    }

    /** Khóa tạm sau nhiều lần đăng nhập sai. */
    public static class Security {
        private int maxFailedAttempts = 5;
        private int lockDurationMinutes = 15;

        public int getMaxFailedAttempts() {
            return maxFailedAttempts;
        }

        public void setMaxFailedAttempts(int maxFailedAttempts) {
            this.maxFailedAttempts = maxFailedAttempts;
        }

        public int getLockDurationMinutes() {
            return lockDurationMinutes;
        }

        public void setLockDurationMinutes(int lockDurationMinutes) {
            this.lockDurationMinutes = lockDurationMinutes;
        }
    }

    /** Cấu hình gửi email (mục 7.10). */
    public static class Mail {
        private boolean enabled = false;
        private String from = "no-reply@topicmanagement.local";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    public String getDateTimePattern() {
        return dateTimePattern;
    }

    public void setDateTimePattern(String dateTimePattern) {
        this.dateTimePattern = dateTimePattern;
    }

    public Group getGroup() {
        return group;
    }

    public BigDecimal getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(BigDecimal passingScore) {
        this.passingScore = passingScore;
    }

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public Report getReport() {
        return report;
    }

    public Security getSecurity() {
        return security;
    }

    public Mail getMail() {
        return mail;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}