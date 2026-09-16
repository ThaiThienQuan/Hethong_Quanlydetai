package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.Topic;
import blog.hethong_quanlydetai.repository.TopicRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    private final TopicRepository repository;

    public TopicController(TopicRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/published")
    public List<Topic> publishedTopics() {
        return repository.findByPublishedTrue();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRUONG_KHOA', 'GIANG_VIEN')")
    public List<Topic> findAll() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GIANG_VIEN')")
    public Topic create(@Valid @RequestBody Topic topic) {
        return repository.save(topic);
    }
}