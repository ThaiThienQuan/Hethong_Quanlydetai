package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.TopicRegistrationForm;
import vn.edu.demo.topicmanagement.service.StudentPortalService;

@Controller
@RequestMapping("/student/registrations")
@PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
public class StudentRegistrationController {
    private final StudentPortalService studentService;

    public StudentRegistrationController(StudentPortalService studentService) { this.studentService = studentService; }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("myGroup", studentService.getMyGroup(principal.getName()));
        model.addAttribute("canRegister", studentService.canRegister(principal.getName()));
        model.addAttribute("registrations", studentService.getMyRegistrations(principal.getName()));
        model.addAttribute("topics", studentService.getAvailableTopics(principal.getName()));
        model.addAttribute("registrationForm", new TopicRegistrationForm());
        return "student/registrations/list";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("registrationForm") TopicRegistrationForm form,
                           BindingResult result, Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", "Vui lòng chọn đề tài.");
            return "redirect:/student/registrations";
        }
        try {
            studentService.registerTopic(form.getTopicId(), principal.getName());
            redirect.addFlashAttribute("message", "Đã gửi đăng ký đề tài, đang chờ giảng viên hướng dẫn xác nhận.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/registrations";
    }

    /** Hủy đăng ký khi còn PENDING - chỉ nhóm trưởng của nhóm đó. */
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            studentService.cancelRegistration(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã hủy đăng ký đề tài.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/registrations";
    }
}
