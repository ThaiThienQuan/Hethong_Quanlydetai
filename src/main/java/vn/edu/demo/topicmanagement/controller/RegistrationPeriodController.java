package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.RegistrationPeriodForm;
import vn.edu.demo.topicmanagement.entity.PeriodType;
import vn.edu.demo.topicmanagement.service.RegistrationPeriodService;

/**
 * Tạo và đóng/mở đợt đăng ký (mục 5.2).
 *
 * <p>Quyền: {@code PERIOD_MANAGE} trong {@code role_permissions} - Trưởng khoa và admin.
 * Kiểm tra ở cả {@code SecurityConfig} (theo URL) và ở đây (theo method) để không phụ thuộc
 * vào một lớp bảo vệ duy nhất.
 */
@Controller
@RequestMapping("/periods")
@PreAuthorize("hasAuthority('PERIOD_MANAGE')")
public class RegistrationPeriodController {

    private final RegistrationPeriodService periodService;

    public RegistrationPeriodController(RegistrationPeriodService periodService) {
        this.periodService = periodService;
    }

    @ModelAttribute("periodTypes")
    public PeriodType[] periodTypes() { return PeriodType.values(); }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("periods", periodService.findAll());
        return "periods/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("periodForm", new RegistrationPeriodForm());
        model.addAttribute("editing", false);
        return "periods/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            model.addAttribute("periodForm", periodService.getFormForEdit(id));
            model.addAttribute("editing", true);
            model.addAttribute("periodId", id);
            return "periods/form";
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/periods";
        }
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("periodForm") RegistrationPeriodForm form,
                         BindingResult result, Model model, Authentication authentication, RedirectAttributes redirect) {
        // Keep the form in create mode if validation fails and Thymeleaf renders it again.
        model.addAttribute("editing", false);
        Map<String, String> errors = periodService.checkRules(form);
        errors.forEach((field, message) -> result.rejectValue(field, "rule", message));
        if (result.hasErrors()) return "periods/form";
        periodService.create(form, authentication.getName());
        redirect.addFlashAttribute("message", "Đã tạo đợt ở trạng thái nháp.");
        return "redirect:/periods";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("periodForm") RegistrationPeriodForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        Map<String, String> errors = periodService.checkRules(form);
        errors.forEach((field, message) -> result.rejectValue(field, "rule", message));
        if (result.hasErrors()) {
            model.addAttribute("editing", true);
            model.addAttribute("periodId", id);
            return "periods/form";
        }
        try {
            periodService.update(id, form);
            redirect.addFlashAttribute("message", "Đã cập nhật đợt đăng ký.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/periods";
    }

    @PostMapping("/{id}/open")
    public String open(@PathVariable Long id, RedirectAttributes redirect) {
        return changeStatus(id, redirect, true);
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            periodService.reopen(id);
            redirect.addFlashAttribute("message", "Đã mở lại đợt đăng ký.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/periods";
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes redirect) {
        return changeStatus(id, redirect, false);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            periodService.deleteDraft(id);
            redirect.addFlashAttribute("message", "Đã xóa đợt đăng ký nháp.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/periods";
    }

    private String changeStatus(Long id, RedirectAttributes redirect, boolean open) {
        try {
            if (open) periodService.open(id); else periodService.close(id);
            redirect.addFlashAttribute("message", open ? "Đã mở đợt đăng ký." : "Đã đóng đợt đăng ký.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/periods";
    }
}
