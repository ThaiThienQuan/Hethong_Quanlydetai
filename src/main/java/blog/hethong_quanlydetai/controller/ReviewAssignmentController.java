package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.service.ReviewAssignmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
@RequestMapping("/review-assignments")
public class ReviewAssignmentController {

    private final ReviewAssignmentService reviewAssignmentService;

    public ReviewAssignmentController(ReviewAssignmentService reviewAssignmentService) {
        this.reviewAssignmentService = reviewAssignmentService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("assignments", reviewAssignmentService.findAll());
        return "review-assignments";
    }

    @GetMapping("/new")
    public String createForm() {
        return "review-assignment-form";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long councilId,
                       @RequestParam Long topicId,
                       @RequestParam Long lecturerId,
                       RedirectAttributes redirectAttributes) {
        try {
            reviewAssignmentService.assign(councilId, topicId, lecturerId);
            redirectAttributes.addFlashAttribute("success", "Đã phân công giảng viên chấm đề tài.");
            return "redirect:/review-assignments";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/review-assignments/new";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                     RedirectAttributes redirectAttributes) {
    try {
        reviewAssignmentService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa phân công.");
    } catch (DataIntegrityViolationException e) {
        redirectAttributes.addFlashAttribute("error", "Không thể xóa phân công vì đã có điểm đánh giá liên quan.");
    }

    return "redirect:/review-assignments";
}
}