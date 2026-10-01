package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.AppUser;
import blog.hethong_quanlydetai.entity.UserRole;
import blog.hethong_quanlydetai.service.RoleService;
import blog.hethong_quanlydetai.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.security.Principal;

@Controller
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final RoleService roleService;

    public UserController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "users";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String newUserForm(Model model) {
        AppUser user = new AppUser();
        user.setRole(UserRole.SINH_VIEN);
        model.addAttribute("user", user);
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("userRoles", UserRole.values());
        model.addAttribute("selectedRoleIds", roleService.findAll().stream()
            .filter(role -> "SINH_VIEN".equals(role.getName())).map(role -> role.getId()).toList());
        return "user-form";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String editUserForm(@PathVariable Long id, Model model) {
        AppUser user = userService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));
        model.addAttribute("user", user);
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("userRoles", UserRole.values());
        model.addAttribute("selectedRoleIds", user.getRoles().stream().map(role -> role.getId()).toList());
        return "user-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String saveUser(@ModelAttribute AppUser user, @RequestParam(required = false) java.util.List<Long> roleIds,
                           Model model, RedirectAttributes redirectAttributes) {
        try {
            userService.saveUser(user, roleIds);
            redirectAttributes.addFlashAttribute("message", "Tài khoản đã được lưu.");
            return "redirect:/users";
        } catch (IllegalArgumentException | IllegalStateException error) {
            model.addAttribute("error", error.getMessage());
            model.addAttribute("roles", roleService.findAll());
            model.addAttribute("userRoles", UserRole.values());
            model.addAttribute("selectedRoleIds", roleIds == null ? java.util.List.of() : roleIds);
            return "user-form";
        }
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        AppUser user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));
        model.addAttribute("user", user);
        return "user-profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute ProfileRequest request, Principal principal,
                                RedirectAttributes redirectAttributes) {
        userService.updateProfile(principal.getName(), request.fullName(), request.email(), request.phone());
        redirectAttributes.addFlashAttribute("message", "Thông tin hồ sơ đã được cập nhật.");
        return "redirect:/users/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 Principal principal, RedirectAttributes redirectAttributes) {
        userService.changePassword(principal.getName(), currentPassword, newPassword);
        redirectAttributes.addFlashAttribute("message", "Mật khẩu đã được cập nhật.");
        return "redirect:/users/profile";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes redirectAttributes) {
        userService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Trạng thái tài khoản đã cập nhật.");
        return "redirect:/users";
    }

    public record ProfileRequest(@NotBlank String fullName, @NotBlank @Email String email, String phone) { }
}
