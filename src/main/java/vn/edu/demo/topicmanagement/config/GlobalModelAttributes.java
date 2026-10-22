package vn.edu.demo.topicmanagement.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.demo.topicmanagement.dto.CurrentUserView;
import vn.edu.demo.topicmanagement.dto.SupportContactView;
import vn.edu.demo.topicmanagement.service.DashboardService;
import vn.edu.demo.topicmanagement.service.SupportContactService;

/**
 * Cấp sẵn dữ liệu dùng chung cho mọi trang.
 *
 * <p>Có hai thuộc tính chung:
 * <ul>
 *   <li>{@code supportContact} - thông tin liên hệ quản trị viên (dùng ở trang đăng nhập,
 *       trang đổi mật khẩu, hồ sơ cá nhân, trang hỗ trợ);</li>
 *   <li>{@code currentUser} - họ tên + chức vụ của người đang đăng nhập, dùng ở thanh trên
 *       cùng và ở lời chào của trang Tổng quan.</li>
 * </ul>
 * Khai báo ở {@code @ControllerAdvice} giúp mọi template đều có sẵn mà không phải thêm
 * {@code model.addAttribute} vào từng controller.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final SupportContactService supportContactService;
    private final DashboardService dashboardService;

    public GlobalModelAttributes(SupportContactService supportContactService,
                                 DashboardService dashboardService) {
        this.supportContactService = supportContactService;
        this.dashboardService = dashboardService;
    }

    /**
     * Thông tin liên hệ quản trị viên.
     *
     * @return thông tin liên hệ, hoặc {@code null} nếu không lấy được
     *         (template đã kiểm tra null nên không lỗi)
     */
    @ModelAttribute("supportContact")
    public SupportContactView supportContact() {
        try {
            return supportContactService.getContact();
        } catch (RuntimeException e) {
            // Không để lỗi truy vấn thông tin liên hệ làm hỏng cả trang.
            return null;
        }
    }

    /** Tên + chức vụ của người đang đăng nhập (rỗng nếu chưa đăng nhập). */
    @ModelAttribute("currentUser")
    public CurrentUserView currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            username = auth.getName();
        }
        try {
            return dashboardService.getCurrentUser(username);
        } catch (RuntimeException e) {
            return new CurrentUserView(username == null ? "" : username, "", "");
        }
    }
}
