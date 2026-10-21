package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.demo.topicmanagement.service.AnnouncementService;

/** Danh sách thông báo đã công bố mà người dùng hiện tại được phép xem. */
@Controller
@RequestMapping("/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;
    public AnnouncementController(AnnouncementService announcementService) { this.announcementService = announcementService; }
    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("announcements", announcementService.getVisible(principal.getName()));
        return "announcements/list";
    }
}
