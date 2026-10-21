package vn.edu.demo.topicmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.demo.topicmanagement.service.TopicService;

/**
 * CHỨC NĂNG MẪU: xem danh sách đề tài đã công bố.
 * Luồng: Trình duyệt GET /topics -> Controller -> Service -> Repository -> MySQL
 *        rồi dữ liệu quay ngược lại và được đưa vào trang topics/list.html
 */
@Controller
@RequestMapping("/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public String list(Model model) {
        // Model = "cái giỏ" để chuyển dữ liệu sang trang HTML
        model.addAttribute("topics", topicService.getPublishedTopics());
        return "topics/list";   // -> templates/topics/list.html
    }
}
