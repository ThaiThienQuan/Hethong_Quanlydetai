package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.service.DashboardService;

/**
 * Controller gốc: trang chủ (Tổng quan theo vai trò - mục 5.10) và trang đăng nhập (mục 5.1).
 *
 * <p>Trang chủ yêu cầu đã đăng nhập (SecurityConfig chặn ẩn danh) và hiển thị:
 * thông báo mới, bảng "việc cần làm của tôi" theo vai trò, thanh tiến trình quy trình
 * của đợt hiện hành, số liệu tổng (Trưởng khoa/admin) và bảng đợt đang mở.
 */
@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** Trang chủ - Tổng quan theo vai trò (mục 5.10). */
    @GetMapping("/")
    public String home(Principal principal, Model model) {
        String username = principal == null ? null : principal.getName();
        model.addAttribute("tasks", dashboardService.getMyTasks(username));
        model.addAttribute("notices", dashboardService.getRecentNotices(username));
        model.addAttribute("steps", dashboardService.getWorkflowSteps());
        model.addAttribute("stats", dashboardService.getHeadStats());
        model.addAttribute("openPeriods", dashboardService.getOpenPeriods());
        RegistrationPeriod current = dashboardService.getCurrentPeriod();
        model.addAttribute("currentPeriod", current);
        model.addAttribute("currentPeriodName", current == null ? null : current.getName());
        return "home";
    }

    /** Trang đăng nhập, được {@code SecurityConfig.loginPage("/login")} trỏ tới. */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
