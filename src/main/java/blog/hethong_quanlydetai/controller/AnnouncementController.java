package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.Announcement;
import blog.hethong_quanlydetai.repository.AnnouncementRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementRepository repository;

    public AnnouncementController(AnnouncementRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Announcement> findAll() {
        return repository.findAllByOrderByPublishedAtDesc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TRUONG_KHOA')")
    public Announcement create(@Valid @RequestBody Announcement announcement) {
        return repository.save(announcement);
    }
}