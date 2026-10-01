package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.Announcement;
import blog.hethong_quanlydetai.entity.AppUser;
import blog.hethong_quanlydetai.entity.Role;
import blog.hethong_quanlydetai.repository.AnnouncementRepository;
import blog.hethong_quanlydetai.repository.AppUserRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository, AppUserRepository userRepository, RoleRepository roleRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public List<Announcement> findAll() {
        return announcementRepository.findAllByOrderByPublishedAtDesc();
    }

    public List<Announcement> findVisibleForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return List.of();
        }

        AppUser currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return List.of();
        }

        boolean admin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            return announcementRepository.findByStatusOrderByPublishedAtDesc("PUBLISHED");
        }

        Set<String> allowedRoles = currentUser.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
        if (allowedRoles.isEmpty() && currentUser.getRole() != null) {
            allowedRoles.add(currentUser.getRole().name());
        }
        return announcementRepository.findByStatusOrderByPublishedAtDesc("PUBLISHED").stream()
            .filter(notification -> !notification.getTargetRoles().isEmpty() && notification.getTargetRoles().stream()
                .anyMatch(role -> allowedRoles.contains(role.getName())))
            .toList();
    }

    public List<Announcement> findVisibleForCurrentUserIncludingOwnDrafts() {
        List<Announcement> visibleAnnouncements = findVisibleForCurrentUser();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getName())) {
            return visibleAnnouncements;
        }

        boolean canManage = authentication.getAuthorities().stream()
                .anyMatch(authority -> "NOTICE_MANAGE".equals(authority.getAuthority()));
        if (!canManage) {
            return visibleAnnouncements;
        }

        boolean elevated = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_TRUONG_KHOA".equals(authority.getAuthority())
                || "ROLE_CHU_TICH_HOI_DONG".equals(authority.getAuthority())
                || "ROLE_ADMIN".equals(authority.getAuthority()));
        if (elevated) {
            return announcementRepository.findAllByOrderByPublishedAtDesc();
        }

        Map<Long, Announcement> manageable = new LinkedHashMap<>();
        visibleAnnouncements.forEach(notification -> manageable.put(notification.getId(), notification));
        announcementRepository.findByAuthorOrderByCreatedAtDesc(
                userRepository.findByUsername(authentication.getName()).orElseThrow())
                .forEach(notification -> manageable.put(notification.getId(), notification));
        return List.copyOf(manageable.values());
    }

    public Optional<Announcement> findById(Long id) {
        return announcementRepository.findById(id);
    }

    public Announcement findManageableById(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thông báo không tồn tại."));
        if (authentication == null || !canManage(authentication, announcement)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền quản lý thông báo này.");
        }
        return announcement;
    }

    public Announcement save(Announcement announcement) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("Cần đăng nhập để tạo thông báo.");
        }
        AppUser author = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản người đăng."));
        Announcement savedAnnouncement = announcement;
        if (announcement.getId() == null) {
            announcement.setAuthor(author);
            announcement.setStatus("DRAFT");
            announcement.setCreatedAt(LocalDateTime.now());
        } else {
            savedAnnouncement = announcementRepository.findById(announcement.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Thông báo không tồn tại."));
            if (!canManage(authentication, savedAnnouncement)) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền sửa thông báo này.");
            }
            savedAnnouncement.setTitle(announcement.getTitle());
            savedAnnouncement.setContent(announcement.getContent());
            savedAnnouncement.setTargetRoles(announcement.getTargetRoles());
            savedAnnouncement.setStatus("DRAFT");
        }
        if (announcement.getTargetRoles() == null || announcement.getTargetRoles().isEmpty()) {
            throw new IllegalArgumentException("Thông báo phải có ít nhất một nhóm người nhận.");
        }
        LocalDateTime now = LocalDateTime.now();
        savedAnnouncement.setUpdatedAt(now);
        if (savedAnnouncement.getPublishedAt() == null) {
            savedAnnouncement.setPublishedAt(now);
        }
        return announcementRepository.save(savedAnnouncement);
    }

    public Announcement publish(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thông báo không tồn tại."));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !canManage(authentication, announcement)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền công bố thông báo này.");
        }
        announcement.setStatus("PUBLISHED");
        announcement.setPublishedAt(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    private boolean canManage(Authentication authentication, Announcement announcement) {
        boolean elevated = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_TRUONG_KHOA".equals(authority.getAuthority())
                        || "ROLE_CHU_TICH_HOI_DONG".equals(authority.getAuthority())
                        || "ROLE_ADMIN".equals(authority.getAuthority()));
        return elevated || authentication.getName().equals(announcement.getAuthor().getUsername());
    }
}
