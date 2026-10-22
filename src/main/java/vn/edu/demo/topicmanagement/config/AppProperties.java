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
    private final Support support = new Support();
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

    /**
     * Thông tin liên hệ quản trị viên (mục 5.1).
     *
     * <p>Đọc từ {@code app.support.*} trong {@code application.properties} để không phải
     * sửa mã nguồn mỗi khi đổi đầu mối liên hệ. Tài khoản quản trị cụ thể
     * (tên, email, điện thoại) lấy từ bảng {@code users} - xem {@code SupportContactService}.
     */
    public static class Support {
        /** Tên đơn vị chịu trách nhiệm hỗ trợ người dùng. */
        private String unitName = "Khoa Công nghệ Thông tin";
        /** Địa chỉ trụ sở (nơi nhận thư từ / đến trực để liên hệ). */
        private String address = "Phòng Hành chính - Tầng 2, Tòa nhà A, Trường Đại học";
        /** Số điện thoại của bộ phận hỗ trợ. */
        private String phone = "0900000001";
        /** Email của bộ phận hỗ trợ. */
        private String email = "admin@demo.local";
        /** Giờ làm việc để người dùng biết khi nào gọi được. */
        private String officeHours = "Sáng: 8:00 - 11:30, Chiều: 14:00 - 17:00 (thứ 2 đến thứ 6)";
        /** Ghi chú hướng dẫn cách liên hệ (ví dụ nội dung tin nhắn nên ghi gì). */
        private String instruction = "Vui lòng cung cấp tên đăng nhập và mã sinh viên / mã giảng viên "
                + "để Admin tra cứu nhanh hơn.";

        public String getUnitName() {
            return unitName;
        }

        public void setUnitName(String unitName) {
            this.unitName = unitName;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getOfficeHours() {
            return officeHours;
        }

        public void setOfficeHours(String officeHours) {
            this.officeHours = officeHours;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
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

    public Support getSupport() {
        return support;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}