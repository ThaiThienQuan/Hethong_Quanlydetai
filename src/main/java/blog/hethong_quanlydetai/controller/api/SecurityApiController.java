package blog.hethong_quanlydetai.controller.api;

import blog.hethong_quanlydetai.entity.Announcement;
import blog.hethong_quanlydetai.entity.AppUser;
import blog.hethong_quanlydetai.entity.Permission;
import blog.hethong_quanlydetai.entity.Role;
import blog.hethong_quanlydetai.entity.UserRole;
import blog.hethong_quanlydetai.service.AnnouncementService;
import blog.hethong_quanlydetai.service.PermissionService;
import blog.hethong_quanlydetai.service.RoleService;
import blog.hethong_quanlydetai.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SecurityApiController {
    private final UserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AnnouncementService announcementService;

    public SecurityApiController(UserService userService, RoleService roleService,
                                PermissionService permissionService, AnnouncementService announcementService) {
        this.userService = userService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.announcementService = announcementService;
    }

    @GetMapping("/auth/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> currentUser(Principal principal) {
        AppUser user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        return Map.of(
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "role", user.getRole(),
                "status", user.getStatus(),
                "permissions", user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getCode)
                        .distinct()
                        .toList()
        );
    }

    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public UserResponse currentProfile(Principal principal) {
        return userService.findByUsername(principal.getName()).map(SecurityApiController::toUserResponse)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateProfile(Principal principal, @Valid @RequestBody ProfileRequest request) {
        return toUserResponse(userService.updateProfile(principal.getName(), request.fullName(),
                request.email(), request.phone()));
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(Principal principal, @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(principal.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public List<UserResponse> getUsers() {
        return userService.findAllUsers().stream().map(SecurityApiController::toUserResponse).toList();
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.findById(id).map(SecurityApiController::toUserResponse)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setStatus(request.status());
        user.setRole(request.role());
        return ResponseEntity.ok(toUserResponse(userService.saveUser(user, request.roleIds())));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        AppUser user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại."));
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setStatus(request.status());
        user.setRole(request.role());
        return toUserResponse(userService.saveUser(user, request.roleIds()));
    }

    @PostMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String status = payload.getOrDefault("status", "ACTIVE");
        return ResponseEntity.ok(toUserResponse(userService.updateStatus(id, status)));
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<UserResponse> patchUserStatus(@PathVariable Long id,
                                                        @RequestBody Map<String, String> payload) {
        String status = payload.getOrDefault("status", "ACTIVE");
        return ResponseEntity.ok(toUserResponse(userService.updateStatus(id, status)));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public List<RoleResponse> getRoles() {
        return roleService.findAll().stream().map(SecurityApiController::toRoleResponse).toList();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public RoleResponse createRole(@Valid @RequestBody RoleRequest request) {
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        return toRoleResponse(roleService.save(role));
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public RoleResponse getRole(@PathVariable Long id) {
        Role role = roleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại."));
        return toRoleResponse(role);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public RoleResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        Role role = roleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại."));
        role.setName(request.name());
        role.setDescription(request.description());
        return toRoleResponse(roleService.save(role));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public RoleResponse updateRolePermissions(@PathVariable Long id,
                                              @RequestBody PermissionAssignmentRequest request) {
        Role role = roleService.updatePermissions(id, request.permissionIds());
        return toRoleResponse(role);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public List<PermissionResponse> getPermissions() {
        List<Role> roles = roleService.findAll();
        return permissionService.findAll().stream()
            .map(permission -> toPermissionResponse(permission, roles)).toList();
    }

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public PermissionResponse createPermission(@Valid @RequestBody PermissionRequest request) {
        Permission permission = new Permission();
        permission.setCode(request.code());
        permission.setName(request.name());
        permission.setDescription(request.description());
        return toPermissionResponse(permissionService.save(permission), roleService.findAll());
    }

    @GetMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public PermissionResponse getPermission(@PathVariable Long id) {
        Permission permission = permissionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quyền không tồn tại."));
        return toPermissionResponse(permission, roleService.findAll());
    }

    @PutMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
    public PermissionResponse updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        Permission permission = permissionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quyền không tồn tại."));
        permission.setCode(request.code());
        permission.setName(request.name());
        permission.setDescription(request.description());
        return toPermissionResponse(permissionService.save(permission), roleService.findAll());
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAuthority('NOTICE_VIEW')")
    public List<NotificationResponse> getNotifications() {
        return announcementService.findVisibleForCurrentUser().stream()
                .map(SecurityApiController::toNotificationResponse).toList();
    }

    @GetMapping("/notifications/{id}")
    @PreAuthorize("hasAuthority('NOTICE_VIEW')")
    public NotificationResponse getNotification(@PathVariable Long id) {
        return announcementService.findVisibleForCurrentUser().stream()
                .filter(notification -> notification.getId().equals(id))
                .findFirst().map(SecurityApiController::toNotificationResponse)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Bạn không có quyền xem thông báo này."));
    }

    @PostMapping("/notifications")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationRequest request) {
        Announcement announcement = toAnnouncement(request);
        return ResponseEntity.ok(toNotificationResponse(announcementService.save(announcement)));
    }

    @PutMapping("/notifications/{id}")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public NotificationResponse updateNotification(@PathVariable Long id,
                                                  @Valid @RequestBody NotificationRequest request) {
        Announcement announcement = toAnnouncement(request);
        announcement.setId(id);
        return toNotificationResponse(announcementService.save(announcement));
    }

    @PostMapping("/notifications/{id}/publish")
    @PreAuthorize("hasAuthority('NOTICE_MANAGE')")
    public NotificationResponse publishNotification(@PathVariable Long id) {
        return toNotificationResponse(announcementService.publish(id));
    }

    private Announcement toAnnouncement(NotificationRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setTargetRoles(new HashSet<>());
        if (request.targetRoleIds() != null) {
            request.targetRoleIds().stream()
                    .map(id -> roleService.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Nhóm người dùng không tồn tại.")))
                    .forEach(announcement.getTargetRoles()::add);
        }
        return announcement;
    }

    private static UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getRole(), user.getStatus(),
                user.getRoles().stream().map(Role::getName).toList());
    }

    private static RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(),
                role.getPermissions().stream().map(Permission::getCode).toList());
    }

        private static PermissionResponse toPermissionResponse(Permission permission, List<Role> roles) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getName(),
            permission.getDescription(), roles.stream()
                .filter(role -> role.getPermissions().stream()
                    .anyMatch(rolePermission -> permission.getId().equals(rolePermission.getId())))
                .map(Role::getName).toList());
    }

    private static NotificationResponse toNotificationResponse(Announcement announcement) {
        return new NotificationResponse(announcement.getId(), announcement.getTitle(), announcement.getContent(),
                announcement.getStatus(), announcement.getPublishedAt(), announcement.getCreatedAt(),
                announcement.getAuthor().getFullName(),
                announcement.getTargetRoles().stream().map(Role::getName).toList());
    }

    public record UserResponse(Long id, String username, String fullName, String email, String phone,
                               blog.hethong_quanlydetai.entity.UserRole role, String status, List<String> roles) { }

    public record RoleResponse(Long id, String name, String description, List<String> permissions) { }

    public record PermissionResponse(Long id, String code, String name, String description, List<String> roles) { }

    public record NotificationResponse(Long id, String title, String content, String status,
                                       java.time.LocalDateTime publishedAt, java.time.LocalDateTime createdAt,
                                       String author, List<String> targetRoles) { }

    public record NotificationRequest(@NotBlank String title, @NotBlank String content, List<Long> targetRoleIds) { }

    public record PermissionAssignmentRequest(List<Long> permissionIds) { }

    public record UserRequest(@NotBlank String username, String password, @NotBlank String fullName,
                              @Email @NotBlank String email, String phone, List<Long> roleIds, String status,
                              UserRole role) { }

    public record ProfileRequest(@NotBlank String fullName, @Email @NotBlank String email, String phone) { }

    public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank String newPassword) { }

    public record RoleRequest(@NotBlank String name, String description) { }

    public record PermissionRequest(@NotBlank String code, @NotBlank String name, String description) { }
}
