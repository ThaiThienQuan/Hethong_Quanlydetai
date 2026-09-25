package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.Report;
import blog.hethong_quanlydetai.service.ReportService;
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
    public String index(Model model) {
        model.addAttribute("reports", reportService.findAll());
        return "reports";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("report", new Report());
        return "report-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("report") Report report,
                   Model model,
                   RedirectAttributes redirectAttributes) {
    try {
        reportService.save(report);
        redirectAttributes.addFlashAttribute("success", "Đã nộp báo cáo thành công.");
        return "redirect:/reports";
    } catch (IllegalArgumentException e) {
        model.addAttribute("error", e.getMessage());
        model.addAttribute("report", report);
        return "report-form";
    }
}

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        reportService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa báo cáo.");
        return "redirect:/reports";
    }
}