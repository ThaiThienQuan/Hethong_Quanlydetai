package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.Report;
import blog.hethong_quanlydetai.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUBMISSION_CREATE', 'ASSIGNMENT_MANAGE', 'ROLE_ADMIN')")
    public String index(Authentication authentication, Model model) {
        boolean canManage = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ASSIGNMENT_MANAGE".equals(authority.getAuthority())
                        || "ROLE_ADMIN".equals(authority.getAuthority()));
        model.addAttribute("reports", reportService.findVisibleToUser(authentication.getName(), canManage));
        return "reports";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('SUBMISSION_CREATE')")
    public String createForm(Model model) {
        model.addAttribute("report", new Report());
        return "report-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('SUBMISSION_CREATE')")
    public String save(@ModelAttribute("report") Report report,
                   Authentication authentication,
                   Model model,
                   RedirectAttributes redirectAttributes) {
    try {
        reportService.save(report, authentication.getName());
        redirectAttributes.addFlashAttribute("success", "Đã nộp báo cáo thành công.");
        return "redirect:/reports";
    } catch (IllegalArgumentException e) {
        model.addAttribute("error", e.getMessage());
        model.addAttribute("report", report);
        return "report-form";
    }
}

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyAuthority('ASSIGNMENT_MANAGE', 'ROLE_ADMIN')")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        reportService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa báo cáo.");
        return "redirect:/reports";
    }
}