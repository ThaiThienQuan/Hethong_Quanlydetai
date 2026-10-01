package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.Permission;
import blog.hethong_quanlydetai.service.PermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/permissions")
@PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public String listPermissions(Model model) {
        model.addAttribute("permissions", permissionService.findAll());
        return "permissions";
    }

    @GetMapping("/{id}")
    public String detailPermission(@PathVariable Long id, Model model) {
        Permission permission = permissionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quyền không tồn tại."));
        model.addAttribute("permission", permission);
        model.addAttribute("roles", permissionService.findRolesUsingPermission(id));
        return "permission-detail";
    }

    @GetMapping("/new")
    public String newPermissionForm(Model model) {
        model.addAttribute("permission", new Permission());
        return "permission-form";
    }

    @GetMapping("/{id}/edit")
    public String editPermissionForm(@PathVariable Long id, Model model) {
        model.addAttribute("permission", permissionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quyền không tồn tại.")));
        return "permission-form";
    }

    @PostMapping("/save")
    public String savePermission(@ModelAttribute Permission permission, RedirectAttributes redirectAttributes) {
        permissionService.save(permission);
        redirectAttributes.addFlashAttribute("message", "Quyền đã được lưu.");
        return "redirect:/permissions";
    }
}
