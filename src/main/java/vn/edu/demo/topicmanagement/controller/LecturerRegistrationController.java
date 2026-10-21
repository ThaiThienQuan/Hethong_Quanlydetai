package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.PendingRegistrationView;
import vn.edu.demo.topicmanagement.service.StudentPortalService;

/**
 * GVHD xem và duyệt/từ chối đăng ký đề tài của các nhóm SV.
 * Đúng đặc tả 5.4 (A10): cả hai GVHD đều có quyền.
 * URL: /lecturer/registrations
 */
@Controller
@RequestMapping("/lecturer/registrations")
@PreAuthorize("hasAuthority('REGISTRATION_CONFIRM')")
public class LecturerRegistrationController {

    private final StudentPortalService studentService;

    public LecturerRegistrationController(StudentPortalService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("registrations", studentService.getPendingForSupervisor(principal.getName()));
        return "lecturer/registrations/list";
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('REGISTRATION_CONFIRM')")
    public String approve(@PathVariable Long id, Principal principal,
                          RedirectAttributes redirect) {
        try {
            studentService.approveRegistration(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã duyệt đăng ký đề tài.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/registrations";
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('REGISTRATION_CONFIRM')")
    public String reject(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String reason,
                         Principal principal,
                         RedirectAttributes redirect) {
        try {
            studentService.rejectRegistration(id, reason, principal.getName());
            redirect.addFlashAttribute("message", "Đã từ chối đăng ký đề tài.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/registrations";
    }
}
