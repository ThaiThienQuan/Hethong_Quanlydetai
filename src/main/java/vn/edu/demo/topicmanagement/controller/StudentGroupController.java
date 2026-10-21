package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.service.StudentPortalService;

@Controller
@RequestMapping("/student/groups")
@PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
public class StudentGroupController {
    private final StudentPortalService studentService;

    public StudentGroupController(StudentPortalService studentService) { this.studentService = studentService; }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("myGroup", studentService.getMyGroup(principal.getName()));
        model.addAttribute("joinableGroups", studentService.getJoinableGroups(principal.getName()));
        return "student/groups/list";
    }

    @PostMapping
    public String create(@RequestParam String name, Principal principal, RedirectAttributes redirect) {
        try {
            studentService.createGroup(name, principal.getName());
            redirect.addFlashAttribute("message", "Đã tạo nhóm. Bạn là nhóm trưởng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/groups";
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            studentService.joinGroup(id, principal.getName());
            redirect.addFlashAttribute("message", "Bạn đã tham gia nhóm.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/groups";
    }

    @PostMapping("/{id}/leader")
    public String transferLeader(@PathVariable Long id, @RequestParam Long studentId,
                                 Principal principal, RedirectAttributes redirect) {
        try {
            studentService.transferGroupLeadership(id, studentId, principal.getName());
            redirect.addFlashAttribute("message", "Đã chuyển quyền nhóm trưởng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/groups";
    }
}
