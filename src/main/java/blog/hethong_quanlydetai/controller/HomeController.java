package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String trangchu(Model model) {
        model.addAttribute("dashboard", dashboardService.load());
        return "dashboard";
    }
}
