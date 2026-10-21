package vn.edu.demo.topicmanagement.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.EmailLogView;
import vn.edu.demo.topicmanagement.service.EmailLogService;

/**
 * Màn hình nhật ký email kết quả cho Trưởng khoa (mục 7.10).
 * Cho phép lọc theo trạng thái/đợt và gửi lại email lỗi.
 */
@Controller
@RequestMapping("/head/emails")
@PreAuthorize("hasAuthority('RESULT_PUBLISH')")
public class HeadEmailController {
    private final EmailLogService emailLogService;

    public HeadEmailController(EmailLogService emailLogService) {
        this.emailLogService = emailLogService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) Long periodId,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        Page<EmailLogView> logs = emailLogService.search(status, periodId, page, 20);
        model.addAttribute("logs", logs.getContent());
        model.addAttribute("page", logs);
        model.addAttribute("periods", emailLogService.getPeriods());
        model.addAttribute("status", status);
        model.addAttribute("periodId", periodId);
        model.addAttribute("failedCount", emailLogService.countFailed());
        model.addAttribute("mailEnabled", emailLogService.isMailEnabled());
        return "head/emails/list";
    }

    @PostMapping("/{id}/resend")
    public String resend(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            emailLogService.resend(id);
            redirect.addFlashAttribute("message", "Đã gửi lại email.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/emails";
    }

    @PostMapping("/resend-failed")
    public String resendFailed(RedirectAttributes redirect) {
        try {
            int n = emailLogService.resendAllFailed();
            redirect.addFlashAttribute("message", "Đã thử gửi lại " + n + " email lỗi.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/emails";
    }
}