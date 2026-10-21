package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.AnnouncementForm;
import vn.edu.demo.topicmanagement.dto.AnnouncementView;
import vn.edu.demo.topicmanagement.entity.Notification;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.NotificationRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/** Nghiệp vụ thông báo - dựa trên bảng {@code notifications} theo schema {@code HT_QLDT}. */
@Service
public class AnnouncementService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public AnnouncementService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /** Thông báo đã đăng mà người đọc được phép thấy (theo vai trò). */
    @Transactional(readOnly = true)
    public List<AnnouncementView> getVisible(String username) {
        User reader = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        List<String> roleCodes = reader.getRoles().stream()
                .map(r -> r.getCode()).collect(Collectors.toList());
        List<Notification> published = notificationRepository.findVisibleForRoles(
                roleCodes.isEmpty() ? List.of("_NONE_") : roleCodes, PUBLISHED);
        List<AnnouncementView> views = new ArrayList<>();
        for (Notification n : published) views.add(toView(n));
        return views;
    }

    /** Tất cả thông báo do người dùng hiện tại tạo. */
    @Transactional(readOnly = true)
    public List<AnnouncementView> getManageList(String username) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return List.of();
        List<Notification> mine = notificationRepository.findAll().stream()
                .filter(n -> n.getCreatedBy() != null
                        && n.getCreatedBy().getId().equals(me.getId()))
                .sorted((a, b) -> {
                    if (a.getPublishedAt() == null && b.getPublishedAt() == null)
                        return Long.compare(b.getId(), a.getId());
                    if (a.getPublishedAt() == null) return 1;
                    if (b.getPublishedAt() == null) return -1;
                    return b.getPublishedAt().compareTo(a.getPublishedAt());
                })
                .toList();
        List<AnnouncementView> views = new ArrayList<>();
        for (Notification n : mine) views.add(toView(n));
        return views;
    }

    @Transactional
    public Long createDraft(AnnouncementForm form, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        String type = (form.getScope() == null || form.getScope().isBlank())
                ? "SCHOOL" : form.getScope().toUpperCase();
        if (!"SCHOOL".equals(type) && !"FACULTY".equals(type)) {
            throw new IllegalArgumentException("Phạm vi thông báo không hợp lệ (SCHOOL/FACULTY).");
        }
        if (!"ADMIN".equals(creator.getPrimaryRoleCode())
                && !"TRUONG_KHOA".equals(creator.getPrimaryRoleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin/Trưởng khoa được tạo thông báo.");
        }
        if ("ADMIN".equals(creator.getPrimaryRoleCode()) && !"SCHOOL".equals(type)) {
            throw new IllegalArgumentException("Admin chỉ tạo được thông báo toàn trường (SCHOOL).");
        }
        Notification n = new Notification();
        n.setCreatedBy(creator);
        n.setStatus(DRAFT);
        applyForm(n, form, type);
        return notificationRepository.save(n).getId();
    }

    @Transactional(readOnly = true)
    public AnnouncementForm getDraftForm(Long id, String username) {
        Notification n = getOwnedDraft(id, username);
        AnnouncementForm form = new AnnouncementForm();
        form.setTitle(n.getTitle());
        form.setContent(n.getContent());
        form.setScope(n.getType() == null ? "SCHOOL" : n.getType());
        return form;
    }

    @Transactional
    public void updateDraft(Long id, AnnouncementForm form, String username) {
        Notification n = getOwnedDraft(id, username);
        String type = (form.getScope() == null || form.getScope().isBlank())
                ? n.getType() : form.getScope().toUpperCase();
        if (!"SCHOOL".equals(type) && !"FACULTY".equals(type)) {
            throw new IllegalArgumentException("Phạm vi thông báo không hợp lệ (SCHOOL/FACULTY).");
        }
        applyForm(n, form, type);
    }

    @Transactional
    public void deleteDraft(Long id, String username) {
        Notification n = getOwnedDraft(id, username);
        notificationRepository.delete(n);
    }

    @Transactional
    public void publish(Long id, String username) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));
        if (n.getCreatedBy() == null || !n.getCreatedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không tạo thông báo này.");
        }
        if (!DRAFT.equals(n.getStatus())) {
            throw new IllegalStateException("Chỉ công bố thông báo đang ở trạng thái nháp.");
        }
        n.setStatus(PUBLISHED);
        n.setPublishedAt(LocalDateTime.now(ZONE));
    }

    private AnnouncementView toView(Notification n) {
        return new AnnouncementView(
                n.getId(),
                n.getTitle(),
                n.getContent(),
                n.getCreatedBy() == null ? "" : n.getCreatedBy().getFullName(),
                null,
                n.getType(),
                n.getStatus(),
                n.getCreatedAt(),
                n.getPublishedAt());
    }

    private Notification getOwnedDraft(Long id, String username) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));
        if (n.getCreatedBy() == null || !n.getCreatedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không quản lý thông báo này.");
        }
        if (!DRAFT.equals(n.getStatus())) {
            throw new IllegalStateException("Chỉ có thể sửa hoặc xóa thông báo đang ở trạng thái nháp.");
        }
        return n;
    }

    private void applyForm(Notification n, AnnouncementForm form, String type) {
        n.setTitle(form.getTitle() == null ? "" : form.getTitle().trim());
        n.setContent(form.getContent() == null ? "" : form.getContent().trim());
        n.setType(type);
    }
}
