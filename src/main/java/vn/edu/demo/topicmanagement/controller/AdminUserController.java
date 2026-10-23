package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.UserForm;
import vn.edu.demo.topicmanagement.dto.UserRowView;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.entity.UserRole;
import vn.edu.demo.topicmanagement.entity.UserStatus;
import vn.edu.demo.topicmanagement.service.AuditLogService;
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
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class AdminUserController {

    private final UserAccountService accountService;
    private final AuditLogService auditLogService;

    public AdminUserController(UserAccountService accountService, AuditLogService auditLogService) {
        this.accountService = accountService;
        this.auditLogService = auditLogService;
    }

    // Dữ liệu cho các ô <select> trên form
    @ModelAttribute("roles")
    public UserRole[] roles() { return UserRole.values(); }

    /** Số trang mỗi lần (mục 5.1 - danh sách có phân trang). */
    private static final int pageSize = 10;

    @ModelAttribute("departments")
    public List<Department> departments() { return accountService.getDepartments(); }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String role,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<UserRowView> users = accountService.searchWithProfiles(keyword, role, status, page, pageSize);
        model.addAttribute("users", users.getContent());
        model.addAttribute("page", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        // Luôn gán danh sách trạng thái để template không phụ thuộc ModelAttribute.
        model.addAttribute("statuses", UserStatus.values());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        setFormInfo(model, "/admin/users/new", "Tạo tài khoản mới", false);
        addCodePreviews(model);
        return "admin/users/form";
    }

    /**
     * Mã SV/GV do hệ thống cấp tự động -> hiển thị trước mã sẽ được cấp cho Admin xem.
     * Luôn gán lại (kể cả khi form có lỗi) để ô mã không bị trống.
     */
    private void addCodePreviews(Model model) {
        model.addAttribute("nextStudentCode", accountService.previewStudentCode());
        model.addAttribute("nextLecturerCode", accountService.previewLecturerCode());
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("userForm") UserForm form, BindingResult result,
                         Model model, Principal principal, RedirectAttributes redirect) {
        addRuleErrors(form, null, result);
        if (result.hasErrors()) {
            setFormInfo(model, "/admin/users/new", "Tạo tài khoản mới", false);
            addCodePreviews(model);
            return "admin/users/form";
        }
        accountService.create(form);
        auditLogService.record(principal.getName(), AuditLogService.USER_CREATE,
                "User", null, "Tạo tài khoản " + form.getUsername().trim());
        redirect.addFlashAttribute("message", "Đã tạo tài khoản " + form.getUsername().trim() + ".");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("userForm", accountService.getFormForEdit(id));
        model.addAttribute("userId", id);
        setFormInfo(model, "/admin/users/" + id + "/edit", "Sửa tài khoản", true);
        addCodePreviews(model);
        return "admin/users/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("userForm") UserForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        addRuleErrors(form, id, result);
        if (result.hasErrors()) {
            setFormInfo(model, "/admin/users/" + id + "/edit", "Sửa tài khoản", true);
            addCodePreviews(model);
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
            auditLogService.record(principal.getName(), AuditLogService.USER_LOCK,
                    "User", id, "Đổi trạng thái tài khoản");
            redirect.addFlashAttribute("message", "Đã đổi trạng thái tài khoản.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Admin đặt lại mật khẩu cho tài khoản về mật khẩu mặc định.
     * Không cần nhập mật khẩu - người dùng muốn đổi thì tự đổi sau khi đăng nhập
     * tại trang /account/password. Mật khẩu mặc định KHÔNG hiển thị ra giao diện.
     */
    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                Principal principal,
                                RedirectAttributes redirect) {
        try {
            accountService.resetPasswordToDefault(id, principal.getName());
            auditLogService.record(principal.getName(), AuditLogService.PASSWORD_RESET,
                    "User", id, "Admin đặt lại mật khẩu về mặc định");
            redirect.addFlashAttribute("message",
                    "Đã đặt lại mật khẩu về mặc định. Người dùng có thể tự đổi sau khi đăng nhập.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /** Admin gán lại vai trò cho tài khoản (mục 5.1). */
    @PostMapping("/{id}/assign-role")
    public String assignRole(@PathVariable Long id,
                             @RequestParam vn.edu.demo.topicmanagement.entity.UserRole role,
                             Principal principal,
                             RedirectAttributes redirect) {
        try {
            accountService.assignRole(id, role, principal.getName());
            auditLogService.record(principal.getName(), AuditLogService.USER_ROLE_CHANGE,
                    "User", id, "Gán vai trò " + role.name());
            redirect.addFlashAttribute("message", "Đã gán vai trò mới cho tài khoản.");
        } catch (IllegalArgumentException | IllegalStateException e) {
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
