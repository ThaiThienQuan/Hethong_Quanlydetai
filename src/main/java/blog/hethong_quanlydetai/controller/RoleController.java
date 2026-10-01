package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.Permission;
import blog.hethong_quanlydetai.entity.Role;
import blog.hethong_quanlydetai.service.PermissionService;
import blog.hethong_quanlydetai.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/roles")
@PreAuthorize("hasAuthority('ROLE_MANAGE')")
public class RoleController {
    private final RoleService roleService;
    private final PermissionService permissionService;

    public RoleController(RoleService roleService, PermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public String listRoles(Model model) {
        model.addAttribute("roles", roleService.findAll());
        return "roles";
    }

    @GetMapping("/{id}")
    public String detailRole(@PathVariable Long id, Model model) {
        Role role = roleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại."));

        model.addAttribute("role", role);
        model.addAttribute("permissions", permissionService.findAll());
        model.addAttribute("assignedPermissionIds", role.getPermissions().stream()
                .map(Permission::getId)
                .collect(Collectors.toSet()));
        return "role-detail";
    }

    @GetMapping("/new")
    public String newRoleForm(Model model) {
        model.addAttribute("role", new Role());
        return "role-form";
    }

    @GetMapping("/{id}/edit")
    public String editRoleForm(@PathVariable Long id, Model model) {
        model.addAttribute("role", roleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại.")));
        return "role-form";
    }

    @PostMapping("/save")
    public String saveRole(@ModelAttribute Role role, RedirectAttributes redirectAttributes) {
        roleService.save(role);
        redirectAttributes.addFlashAttribute("message", "Vai trò đã được lưu.");
        return "redirect:/roles";
    }

    @PostMapping("/{id}/permissions")
    public String updatePermissions(@PathVariable Long id,
                                   @RequestParam(required = false) java.util.List<Long> permissionIds,
                                   RedirectAttributes redirectAttributes) {
        roleService.updatePermissions(id, permissionIds);
        redirectAttributes.addFlashAttribute("message", "Phân quyền cho vai trò đã được cập nhật.");
        return "redirect:/roles/" + id;
    }
}
