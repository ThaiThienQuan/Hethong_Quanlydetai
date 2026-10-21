package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.UserForm;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.entity.UserRole;
import vn.edu.demo.topicmanagement.service.UserAccountService;

/**
 * Quản lý tài khoản - CHỈ Admin được vào (khai báo trong SecurityConfig: /admin/** -> ADMIN).
 *   GET  /admin/users              danh sách
 *   GET  /admin/users/new          form tạo
 *   POST /admin/users/new          gửi form tạo
 *   GET  /admin/users/{id}/edit    form sửa
 *   POST /admin/users/{id}/edit    gửi form sửa
 *   POST /admin/users/{id}/toggle  khóa / mở khóa
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserAccountService accountService;

    public AdminUserController(UserAccountService accountService) {
        this.accountService = accountService;
    }

    // Dữ liệu cho các ô <select> trên form
    @ModelAttribute("roles")
    public UserRole[] roles() { return UserRole.values(); }

    @ModelAttribute("departments")
    public List<Department> departments() { return accountService.getDepartments(); }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", accountService.findAll());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        setFormInfo(model, "/admin/users/new", "Tạo tài khoản mới", false);
        return "admin/users/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("userForm") UserForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        addRuleErrors(form, null, result);
        if (result.hasErrors()) {
            setFormInfo(model, "/admin/users/new", "Tạo tài khoản mới", false);
            return "admin/users/form";
        }
        accountService.create(form);
        redirect.addFlashAttribute("message", "Đã tạo tài khoản " + form.getUsername().trim() + ".");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("userForm", accountService.getFormForEdit(id));
        setFormInfo(model, "/admin/users/" + id + "/edit", "Sửa tài khoản", true);
        return "admin/users/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("userForm") UserForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        addRuleErrors(form, id, result);
        if (result.hasErrors()) {
            setFormInfo(model, "/admin/users/" + id + "/edit", "Sửa tài khoản", true);
            return "admin/users/form";
        }
        accountService.update(id, form);
        redirect.addFlashAttribute("message", "Đã cập nhật tài khoản.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            accountService.toggleStatus(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã đổi trạng thái tài khoản.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            accountService.delete(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã xóa tài khoản.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error",
                    "Không thể xóa tài khoản vì đang được dữ liệu khác sử dụng. Bạn có thể khóa tài khoản thay thế.");
        }
        return "redirect:/admin/users";
    }

    private void addRuleErrors(UserForm form, Long existingId, BindingResult result) {
        Map<String, String> errors = accountService.checkRules(form, existingId);
        errors.forEach((field, message) -> result.rejectValue(field, "rule", message));
    }

    // editing = true: đang sửa (ô tên đăng nhập và vai trò chỉ xem, không đổi được)
    private void setFormInfo(Model model, String action, String title, boolean editing) {
        model.addAttribute("action", action);
        model.addAttribute("pageTitle", title);
        model.addAttribute("editing", editing);
    }
}
