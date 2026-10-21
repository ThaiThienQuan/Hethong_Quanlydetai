package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.entity.Permission;
import vn.edu.demo.topicmanagement.entity.Role;
import vn.edu.demo.topicmanagement.service.AuditLogService;
import vn.edu.demo.topicmanagement.service.RoleService;

/**
 * Quản lý vai trò và phân quyền (mục 5.1).
 * Yêu cầu: ADMIN hoặc TRUONG_KHOA có quyền ROLE_MANAGE.
 */
@Controller
@RequestMapping("/roles")
@PreAuthorize("hasAnyRole('ADMIN','TRUONG_KHOA')")
public class RoleController {

    private final RoleService roleService;
    private final AuditLogService auditLogService;

    public RoleController(RoleService roleService, AuditLogService auditLogService) {
        this.roleService = roleService;
        this.auditLogService = auditLogService;
    }

    // ======================= ROLE CRUD =======================

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", roleService.findAllRoles());
        return "admin/roles/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("role", new RoleForm());
        model.addAttribute("action", "/roles/new");
        model.addAttribute("pageTitle", "Tạo vai trò mới");
        return "admin/roles/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("role") RoleForm form, RedirectAttributes redirect) {
        try {
            roleService.createRole(form.getCode(), form.getName(), form.getDescription());
            redirect.addFlashAttribute("message", "Đã tạo vai trò '" + form.getName() + "'.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/roles/new";
        }
        return "redirect:/roles";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Role role = roleService.getRoleById(id);
        RoleForm form = new RoleForm();
        form.setId(role.getId());
        form.setCode(role.getCode());
        form.setName(role.getName());
        form.setDescription(role.getDescription());
        model.addAttribute("role", form);
        model.addAttribute("action", "/roles/" + id + "/edit");
        model.addAttribute("pageTitle", "Sửa vai trò");
        model.addAttribute("readonly", role.isSystem());
        return "admin/roles/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("role") RoleForm form,
                          RedirectAttributes redirect) {
        try {
            roleService.updateRole(id, form.getName(), form.getDescription());
            redirect.addFlashAttribute("message", "Đã cập nhật vai trò.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/roles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            roleService.deleteRole(id);
            redirect.addFlashAttribute("message", "Đã xóa vai trò.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "Không thể xóa vai trò vì đang được sử dụng.");
        }
        return "redirect:/roles";
    }

    // ======================= PERMISSION ASSIGNMENT =======================

    @GetMapping("/{id}/permissions")
    public String permissions(@PathVariable Long id, Model model) {
        Role role = roleService.getRoleById(id);
        model.addAttribute("role", role);
        model.addAttribute("permissionMap", roleService.getPermissionMapForRole(id));
        model.addAttribute("permissionNames", roleService.getPermissionNames());
        return "admin/roles/permissions";
    }

    @PostMapping("/{id}/permissions")
    public String savePermissions(@PathVariable Long id,
                                  @RequestParam(value = "permissions", required = false) List<String> permissionCodes,
                                  Principal principal, RedirectAttributes redirect) {
        try {
            var codes = permissionCodes == null ? java.util.Set.<String>of() : new java.util.HashSet<>(permissionCodes);
            roleService.assignPermissionsByCodes(id, codes, principal.getName());
            auditLogService.record(principal.getName(), AuditLogService.ROLE_PERMISSION_CHANGE,
                    "Role", id, "Cập nhật quyền cho vai trò ID " + id);
            redirect.addFlashAttribute("message", "Đã lưu phân quyền cho vai trò.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/roles";
    }

    // ======================= HELPER FORM =======================

    public static class RoleForm {
        private Long id;
        private String code;
        private String name;
        private String description;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
