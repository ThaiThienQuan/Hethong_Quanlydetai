package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.Announcement;
import blog.hethong_quanlydetai.service.AnnouncementService;
import blog.hethong_quanlydetai.service.RoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;

import java.util.HashSet;
import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    private final AnnouncementService announcementService;
    private final RoleService roleService;

    public NotificationController(AnnouncementService announcementService, RoleService roleService) {
        this.announcementService = announcementService;
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('NOTICE_VIEW')")
    public String listNotifications(Model model, Authentication authentication) {
        model.addAttribute("notifications", announcementService.findVisibleForCurrentUserIncludingOwnDrafts());
        model.addAttribute("canManageNotifications", authentication.getAuthorities().stream()
                .anyMatch(authority -> "NOTICE_MANAGE".equals(authority.getAuthority())));
        return "notifications";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String newNotificationForm(Model model) {
        model.addAttribute("notification", new Announcement());
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("selectedTargetRoleIds", roleService.findAll().stream()
            .filter(role -> "SINH_VIEN".equals(role.getName())).map(role -> role.getId()).toList());
        return "notification-form";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String editNotificationForm(@PathVariable Long id, Model model) {
        Announcement notification = announcementService.findManageableById(id);
        model.addAttribute("notification", notification);
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("selectedTargetRoleIds", notification.getTargetRoles().stream()
            .map(role -> role.getId()).toList());
        return "notification-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String saveNotification(@ModelAttribute Announcement announcement,
                                   @RequestParam(required = false) List<Long> targetRoleIds,
                                   RedirectAttributes redirectAttributes) {
        announcement.setTargetRoles(new HashSet<>());
        if (targetRoleIds != null) {
            targetRoleIds.stream()
                    .map(id -> roleService.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Nhóm người dùng không tồn tại.")))
                    .forEach(announcement.getTargetRoles()::add);
        }
        announcementService.save(announcement);
        redirectAttributes.addFlashAttribute("message", "Thông báo đã được lưu.");
        return "redirect:/notifications";
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public String publishNotification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        announcementService.publish(id);
        redirectAttributes.addFlashAttribute("message", "Thông báo đã được công bố.");
        return "redirect:/notifications";
    }
}
