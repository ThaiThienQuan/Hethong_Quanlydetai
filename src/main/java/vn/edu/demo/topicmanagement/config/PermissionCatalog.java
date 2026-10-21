package vn.edu.demo.topicmanagement.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Danh mục quyền và phân quyền mặc định (mục 3 của đề bài).
 *
 * <p>Danh mục được nạp khi khởi động một cách idempotent: quyền đã tồn tại thì giữ nguyên,
 * không ghi đè phân quyền mà Trưởng khoa đã chỉnh trên màn hình Phân quyền.
 *
 * <p>Quyền là chuỗi, kiểm tra bằng {@code @PreAuthorize("hasAuthority('...')")}.
 * "Chủ tịch hội đồng" không nằm ở đây - đó là quan hệ theo từng hội đồng
 * ({@code council_members.role}), không phải vai trò tài khoản.
 */
public final class PermissionCatalog {

    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String ROLE_MANAGE = "ROLE_MANAGE";
    public static final String PERMISSION_MANAGE = "PERMISSION_MANAGE";
    public static final String DEPARTMENT_MANAGE = "DEPARTMENT_MANAGE";
    public static final String PROFILE_MANAGE = "PROFILE_MANAGE";
    public static final String PERIOD_MANAGE = "PERIOD_MANAGE";
    public static final String TOPIC_VIEW = "TOPIC_VIEW";
    public static final String TOPIC_CREATE = "TOPIC_CREATE";
    public static final String TOPIC_APPROVE = "TOPIC_APPROVE";
    public static final String SUPERVISOR_ASSIGN = "SUPERVISOR_ASSIGN";
    public static final String REGISTRATION_CREATE = "REGISTRATION_CREATE";
    public static final String REGISTRATION_CONFIRM = "REGISTRATION_CONFIRM";
    public static final String SUBMISSION_CREATE = "SUBMISSION_CREATE";
    public static final String REPORT_VIEW = "REPORT_VIEW";
    public static final String COUNCIL_MANAGE = "COUNCIL_MANAGE";
    public static final String ASSIGNMENT_MANAGE = "ASSIGNMENT_MANAGE";
    public static final String EVALUATION_CREATE = "EVALUATION_CREATE";
    public static final String RESULT_CALCULATE = "RESULT_CALCULATE";
    public static final String RESULT_PUBLISH = "RESULT_PUBLISH";
    public static final String RESULT_VIEW = "RESULT_VIEW";
    public static final String NOTICE_VIEW = "NOTICE_VIEW";
    public static final String NOTICE_MANAGE = "NOTICE_MANAGE";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DEAN = "TRUONG_KHOA";
    public static final String ROLE_LECTURER = "GIANG_VIEN";
    public static final String ROLE_STUDENT = "SINH_VIEN";

    private PermissionCatalog() {
    }

    /** Tên hiển thị tiếng Việt của từng quyền, dùng cho màn hình Phân quyền. */
    public static Map<String, String> permissionNames() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(USER_MANAGE, "Quản lý tài khoản");
        map.put(ROLE_MANAGE, "Quản lý vai trò");
        map.put(PERMISSION_MANAGE, "Quản lý quyền");
        map.put(DEPARTMENT_MANAGE, "Quản lý bộ môn");
        map.put(PROFILE_MANAGE, "Quản lý hồ sơ GV/SV");
        map.put(PERIOD_MANAGE, "Quản lý đợt đăng ký");
        map.put(TOPIC_VIEW, "Xem danh sách đề tài");
        map.put(TOPIC_CREATE, "Đề xuất đề tài");
        map.put(TOPIC_APPROVE, "Duyệt và công bố đề tài");
        map.put(SUPERVISOR_ASSIGN, "Phân công GV hướng dẫn");
        map.put(REGISTRATION_CREATE, "Lập nhóm và đăng ký đề tài");
        map.put(REGISTRATION_CONFIRM, "Chấp thuận đăng ký của nhóm");
        map.put(SUBMISSION_CREATE, "Nộp báo cáo đề tài");
        map.put(REPORT_VIEW, "Xem và tải báo cáo");
        map.put(COUNCIL_MANAGE, "Lập hội đồng phản biện");
        map.put(ASSIGNMENT_MANAGE, "Phân công GV phản biện");
        map.put(EVALUATION_CREATE, "Nhập điểm phản biện");
        map.put(RESULT_CALCULATE, "Tổng hợp kết quả");
        map.put(RESULT_PUBLISH, "Công bố kết quả và gửi email");
        map.put(RESULT_VIEW, "Xem kết quả đề tài của mình");
        map.put(NOTICE_VIEW, "Xem thông báo");
        map.put(NOTICE_MANAGE, "Soạn và đăng thông báo");
        return map;
    }

    /** Bốn vai trò gốc của hệ thống, không được xóa. */
    public static List<String[]> systemRoles() {
        return List.of(
                new String[]{ROLE_ADMIN, "Quản trị hệ thống"},
                new String[]{ROLE_DEAN, "Trưởng khoa"},
                new String[]{ROLE_LECTURER, "Giảng viên"},
                new String[]{ROLE_STUDENT, "Sinh viên"});
    }

    /** Quyền mặc định của từng vai trò, theo bảng ở mục 3. */
    public static Set<String> defaultPermissionsFor(String roleCode) {
        return switch (roleCode) {
            case ROLE_ADMIN -> Set.of(
                    USER_MANAGE, ROLE_MANAGE, PERMISSION_MANAGE,
                    DEPARTMENT_MANAGE, PROFILE_MANAGE,
                    TOPIC_VIEW, TOPIC_APPROVE, SUPERVISOR_ASSIGN,
                    REPORT_VIEW,
                    COUNCIL_MANAGE, ASSIGNMENT_MANAGE,
                    RESULT_CALCULATE, RESULT_PUBLISH,
                    NOTICE_VIEW, NOTICE_MANAGE);
            case ROLE_DEAN -> Set.of(
                    DEPARTMENT_MANAGE, PROFILE_MANAGE, PERIOD_MANAGE,
                    TOPIC_VIEW, TOPIC_APPROVE, SUPERVISOR_ASSIGN,
                    REPORT_VIEW,
                    COUNCIL_MANAGE, ASSIGNMENT_MANAGE,
                    RESULT_PUBLISH, NOTICE_VIEW, NOTICE_MANAGE);
            case ROLE_LECTURER -> Set.of(
                    TOPIC_VIEW, TOPIC_CREATE,
                    REGISTRATION_CONFIRM, REPORT_VIEW,
                    EVALUATION_CREATE, RESULT_CALCULATE, NOTICE_VIEW);
            case ROLE_STUDENT -> Set.of(
                    TOPIC_VIEW, REGISTRATION_CREATE, SUBMISSION_CREATE,
                    REPORT_VIEW, RESULT_VIEW, NOTICE_VIEW);
            default -> Set.of();
        };
    }
}