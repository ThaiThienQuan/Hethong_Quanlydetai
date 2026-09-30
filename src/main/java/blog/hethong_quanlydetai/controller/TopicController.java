package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.Topic;
import blog.hethong_quanlydetai.service.TopicWorkflowService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/topics")
public class TopicController {
    private final TopicWorkflowService workflowService;

    public TopicController(TopicWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TOPIC_VIEW', 'TOPIC_CREATE', 'TOPIC_APPROVE')")
    public String index(Authentication authentication, Model model) {
        boolean canApprove = hasAuthority(authentication, "TOPIC_APPROVE");
        boolean canCreate = hasAuthority(authentication, "TOPIC_CREATE");
        model.addAttribute("topics", workflowService.findTopics(authentication.getName(), canApprove, canCreate));
        model.addAttribute("canApproveTopics", canApprove);
        model.addAttribute("canCreateTopics", canCreate);
        return "topics";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String createForm(Model model) {
        model.addAttribute("periods", workflowService.findTeacherPeriods());
        model.addAttribute("topic", new Topic());
        return "topic-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String save(@RequestParam String code, @RequestParam String title, @RequestParam String content,
                       @RequestParam Long periodId, Authentication authentication, Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            workflowService.propose(code, title, content, periodId, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Đề tài đã được gửi chủ tịch hội đồng duyệt.");
            return "redirect:/topics";
        } catch (IllegalArgumentException error) {
            model.addAttribute("error", error.getMessage());
            model.addAttribute("periods", workflowService.findTeacherPeriods());
            model.addAttribute("topic", new Topic());
            return "topic-form";
        }
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String review(@PathVariable Long id, @RequestParam boolean approved,
                         RedirectAttributes redirectAttributes) {
        try {
            workflowService.reviewTopic(id, approved);
            redirectAttributes.addFlashAttribute("success",
                    approved ? "Đề tài đã được duyệt và mở đăng ký." : "Đề tài đã bị từ chối.");
        } catch (IllegalArgumentException error) {
            redirectAttributes.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/topics";
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}