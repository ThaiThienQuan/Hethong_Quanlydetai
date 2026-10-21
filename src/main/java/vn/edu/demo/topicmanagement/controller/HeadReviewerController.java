package vn.edu.demo.topicmanagement.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.service.ReviewerService;

/**
 * Trang phân công GVPB mức tổng (giữ lại cho tương thích liên kết cũ).
 *
 * <p>Phiên bản chính thức mục 5.7 nằm ở {@code /head/councils/{id}/topics/{councilTopicId}/assignments}
 * (xem {@link HeadCouncilController}). Trang này chỉ hiển thị danh sách gộp các đề tài
 * và chuyển sang trang chi tiết hội đồng.
 */
@Controller
@RequestMapping("/head/reviewers")
@PreAuthorize("hasAuthority('ASSIGNMENT_MANAGE')")
public class HeadReviewerController {
    private final ReviewerService reviewerService;
    public HeadReviewerController(ReviewerService reviewerService) { this.reviewerService = reviewerService; }

    @GetMapping
    public String list(Model model) {
        // Chuyển sang trang quản lý hội đồng - nơi có phân công GVPB theo đề tài.
        return "redirect:/head/councils";
    }

    @PostMapping("/{registrationId}")
    public String assign(@PathVariable Long registrationId, @RequestParam Long lecturerId,
                         RedirectAttributes redirect) {
        redirect.addFlashAttribute("error",
                "Đường dẫn phân công cũ không còn dùng; hãy vào chi tiết hội đồng để phân công.");
        return "redirect:/head/councils";
    }

    @PostMapping("/{registrationId}/{assignmentId}/remove")
    public String remove(@PathVariable Long registrationId, @PathVariable Long assignmentId,
                         RedirectAttributes redirect) {
        try {
            reviewerService.removeAssignment(assignmentId, null);
            redirect.addFlashAttribute("message", "Đã gỡ phân công phản biện chưa nộp điểm.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils";
    }
}
