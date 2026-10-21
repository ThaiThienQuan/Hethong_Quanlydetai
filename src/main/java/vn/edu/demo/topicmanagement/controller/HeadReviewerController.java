package vn.edu.demo.topicmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.service.ReviewerService;

/** Trưởng khoa phân công GVPB; quyền HEAD được áp dụng tại SecurityConfig cho /head/**. */
@Controller
@RequestMapping("/head/reviewers")
public class HeadReviewerController {
    private final ReviewerService reviewerService;
    public HeadReviewerController(ReviewerService reviewerService) { this.reviewerService = reviewerService; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", reviewerService.getAssignmentsForHead());
        return "head/reviewers/list";
    }

    @PostMapping("/{registrationId}")
    public String assign(@PathVariable Long registrationId, @RequestParam Long lecturerId,
                         RedirectAttributes redirect) {
        try {
            reviewerService.assign(registrationId, lecturerId);
            redirect.addFlashAttribute("message", "Đã phân công giảng viên phản biện.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/reviewers";
    }

    @PostMapping("/{registrationId}/{assignmentId}/remove")
    public String remove(@PathVariable Long registrationId, @PathVariable Long assignmentId,
                         RedirectAttributes redirect) {
        try {
            reviewerService.removeAssignment(registrationId, assignmentId);
            redirect.addFlashAttribute("message", "Đã gỡ phân công phản biện chưa nộp điểm.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/reviewers";
    }
}
