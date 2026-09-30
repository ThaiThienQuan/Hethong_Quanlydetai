package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.service.DashboardService;
import blog.hethong_quanlydetai.service.AnnouncementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Set;

@Controller
public class HomeController {
    private final DashboardService dashboardService;
    private final AnnouncementService announcementService;

    public HomeController(DashboardService dashboardService, AnnouncementService announcementService) {
        this.dashboardService = dashboardService;
        this.announcementService = announcementService;
    }

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public String trangchu(Authentication authentication, Model model) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(java.util.stream.Collectors.toSet());
        boolean canManageReviews = hasAny(authorities, "RESULT_CALCULATE", "RESULT_PUBLISH", "ROLE_ADMIN");
        boolean canViewOverview = hasAny(authorities, "PERIOD_MANAGE", "COUNCIL_MANAGE", "USER_MANAGE", "ROLE_ADMIN");
        model.addAttribute("dashboard", dashboardService.load(
            announcementService.findVisibleForCurrentUser(), canManageReviews, canViewOverview));
        model.addAttribute("username", authentication.getName());
        model.addAttribute("canViewOverview", canViewOverview);
        model.addAttribute("canManagePeriods", hasAny(authorities, "PERIOD_MANAGE", "ROLE_ADMIN"));
        model.addAttribute("canManageTopics", hasAny(authorities, "TOPIC_VIEW", "TOPIC_CREATE", "TOPIC_APPROVE"));
        model.addAttribute("canRegister", hasAny(authorities, "REGISTRATION_CREATE", "REGISTRATION_CONFIRM"));
        model.addAttribute("canViewReports", hasAny(authorities, "SUBMISSION_CREATE", "ASSIGNMENT_MANAGE", "ROLE_ADMIN"));
        model.addAttribute("canManageCouncils", hasAny(authorities, "COUNCIL_MANAGE", "ROLE_ADMIN"));
        model.addAttribute("canAssignReviews", hasAny(authorities, "ASSIGNMENT_MANAGE", "ROLE_ADMIN"));
        model.addAttribute("canEvaluate", hasAny(authorities, "EVALUATION_CREATE", "RESULT_CALCULATE", "RESULT_PUBLISH", "ROLE_ADMIN"));
        model.addAttribute("canViewResults", hasAny(authorities, "RESULT_VIEW", "ROLE_ADMIN"));
        model.addAttribute("canViewNotifications", authorities.contains("NOTICE_VIEW"));
        model.addAttribute("canManageUsers", authorities.contains("USER_MANAGE"));
        model.addAttribute("canManageRoles", authorities.contains("ROLE_MANAGE"));
        model.addAttribute("canManagePermissions", authorities.contains("PERMISSION_MANAGE"));
        return "dashboard";
    }

    private boolean hasAny(Set<String> authorities, String... required) {
        return java.util.Arrays.stream(required).anyMatch(authorities::contains);
    }
}
