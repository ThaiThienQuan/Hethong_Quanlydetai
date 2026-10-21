package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.ReviewScoreForm;
import vn.edu.demo.topicmanagement.service.ReviewerService;

/** Trang dành cho giảng viên xem nhiệm vụ và gửi điểm phản biện. */
@Controller
@RequestMapping("/lecturer/reviews")
public class LecturerReviewController {
    private final ReviewerService reviewerService;
    public LecturerReviewController(ReviewerService reviewerService) { this.reviewerService = reviewerService; }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("tasks", reviewerService.getMyTasks(principal.getName()));
        return "lecturer/reviews/list";
    }

    @PostMapping("/{assignmentId}")
    public String submit(@PathVariable Long assignmentId,
                         @Valid @ModelAttribute("reviewScoreForm") ReviewScoreForm form,
                         BindingResult result, Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getFieldError() == null
                    ? "Thông tin điểm phản biện chưa hợp lệ." : result.getFieldError().getDefaultMessage());
            return "redirect:/lecturer/reviews";
        }
        try {
            reviewerService.submitReview(assignmentId, principal.getName(), form);
            redirect.addFlashAttribute("message", "Đã lưu điểm và nhận xét phản biện.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/reviews";
    }
}
