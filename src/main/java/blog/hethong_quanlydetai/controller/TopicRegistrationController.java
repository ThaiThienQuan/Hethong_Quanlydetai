package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.service.TopicWorkflowService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/registrations")
public class TopicRegistrationController {
    private final TopicWorkflowService workflowService;

    public TopicRegistrationController(TopicWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('REGISTRATION_CREATE', 'REGISTRATION_CONFIRM')")
    public String index(Authentication authentication, Model model) {
        boolean canRegister = hasAuthority(authentication, "REGISTRATION_CREATE")
            && hasAuthority(authentication, "ROLE_SINH_VIEN");
        boolean canConfirm = hasAuthority(authentication, "REGISTRATION_CONFIRM")
            && hasAuthority(authentication, "ROLE_GIANG_VIEN");
        model.addAttribute("canRegister", canRegister);
        model.addAttribute("canConfirm", canConfirm);
        if (!canRegister && !canConfirm) {
            model.addAttribute("info", "Chức năng đăng ký dành cho sinh viên và giảng viên.");
        }
        if (canRegister) {
            model.addAttribute("periods", workflowService.findStudentPeriods());
            model.addAttribute("topics", workflowService.findAvailableTopics());
            model.addAttribute("groups", workflowService.findGroupsForUser(authentication.getName()));
            model.addAttribute("students", workflowService.findAvailableStudents(authentication.getName()));
        }
        if (canConfirm) {
            model.addAttribute("confirmations", workflowService.findPendingConfirmations(authentication.getName()));
        }
        return "registrations";
    }

    @PostMapping("/groups")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String createGroup(@RequestParam String name, @RequestParam String code, @RequestParam Long periodId,
                              @RequestParam(required = false) List<Long> memberIds,
                              Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            workflowService.createGroup(name, code, periodId, memberIds, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Đã tạo nhóm và đăng ký thành viên.");
        } catch (IllegalArgumentException error) {
            redirectAttributes.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/registrations";
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
    public String apply(@RequestParam Long groupId, @RequestParam Long topicId, Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        try {
            workflowService.registerGroup(groupId, topicId, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Đã gửi đăng ký chờ giảng viên xác nhận.");
        } catch (IllegalArgumentException error) {
            redirectAttributes.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/registrations";
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('REGISTRATION_CONFIRM')")
    public String confirm(@PathVariable Long id, @RequestParam boolean approved, Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            workflowService.confirmRegistration(id, approved, authentication.getName());
            redirectAttributes.addFlashAttribute("success",
                    approved ? "Đã xác nhận nhóm thực hiện đề tài." : "Đã từ chối đăng ký đề tài.");
        } catch (IllegalArgumentException error) {
            redirectAttributes.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/registrations";
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}