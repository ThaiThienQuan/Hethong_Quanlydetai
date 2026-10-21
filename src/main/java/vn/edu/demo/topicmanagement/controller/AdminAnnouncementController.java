package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
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
import vn.edu.demo.topicmanagement.dto.AnnouncementForm;
import vn.edu.demo.topicmanagement.service.AnnouncementService;

import java.util.List;

/** Admin quản lý thông báo toàn trường tại URL được giới hạn bởi SecurityConfig. */
@Controller
@RequestMapping("/admin/announcements")
@PreAuthorize("hasAuthority('NOTICE_MANAGE')")
public class AdminAnnouncementController {
    private final AnnouncementService announcementService;
    public AdminAnnouncementController(AnnouncementService announcementService) { this.announcementService = announcementService; }

    @ModelAttribute("scopeOptions")
    public List<String> scopeOptions() { return List.of("SCHOOL"); }

    @GetMapping
    public String list(Principal principal, Model model) {
        prepare(model, principal.getName());
        return "announcements/manage";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Principal principal, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("announcementForm", announcementService.getDraftForm(id, principal.getName()));
            model.addAttribute("action", "/admin/announcements/" + id + "/edit");
            model.addAttribute("backUrl", "/admin/announcements");
            model.addAttribute("pageTitle", "Sửa thông báo nháp");
            return "announcements/form";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/announcements";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("announcementForm") AnnouncementForm form,
                         BindingResult result, Principal principal, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("action", "/admin/announcements/" + id + "/edit");
            model.addAttribute("backUrl", "/admin/announcements");
            model.addAttribute("pageTitle", "Sửa thông báo nháp");
            return "announcements/form";
        }
        try {
            announcementService.updateDraft(id, form, principal.getName());
            redirect.addFlashAttribute("message", "Đã cập nhật thông báo nháp.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            announcementService.deleteDraft(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã xóa thông báo nháp.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("announcementForm") AnnouncementForm form,
                         BindingResult result, Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getFieldError() == null
                    ? "Thông tin thông báo chưa hợp lệ." : result.getFieldError().getDefaultMessage());
            return "redirect:/admin/announcements";
        }
        try {
            announcementService.createDraft(form, principal.getName());
            redirect.addFlashAttribute("message", "Đã tạo thông báo nháp.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            announcementService.publish(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã công bố thông báo toàn trường.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    private void prepare(Model model, String username) {
        model.addAttribute("managePath", "/admin/announcements");
        model.addAttribute("announcementForm", new AnnouncementForm());
        model.addAttribute("announcements", announcementService.getManageList(username));
    }
}
