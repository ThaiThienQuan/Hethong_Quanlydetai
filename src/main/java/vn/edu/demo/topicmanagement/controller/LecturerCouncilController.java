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
import vn.edu.demo.topicmanagement.dto.CouncilResultForm;
import vn.edu.demo.topicmanagement.dto.ReviewScoreForm;
import vn.edu.demo.topicmanagement.service.CouncilScoringService;

/** Trang chấm điểm hội đồng và trang chủ tịch tổng hợp/công bố kết quả. */
@Controller
@RequestMapping("/lecturer/council-work")
public class LecturerCouncilController {
    private final CouncilScoringService scoringService;
    public LecturerCouncilController(CouncilScoringService scoringService) { this.scoringService = scoringService; }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("work", scoringService.getMyWork(principal.getName()));
        return "lecturer/councils/work";
    }

    @PostMapping("/{councilTopicId}/score")
    public String score(@PathVariable Long councilTopicId,
                        @Valid @ModelAttribute("scoreForm") ReviewScoreForm form,
                        BindingResult result, Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getFieldError() == null
                    ? "Điểm chưa hợp lệ." : result.getFieldError().getDefaultMessage());
            return "redirect:/lecturer/council-work";
        }
        try {
            scoringService.submitScore(councilTopicId, principal.getName(), form);
            redirect.addFlashAttribute("message", "Đã lưu điểm hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/council-work";
    }

    @PostMapping("/{councilTopicId}/finalize")
    public String finalizeResult(@PathVariable Long councilTopicId,
                                 @Valid @ModelAttribute("resultForm") CouncilResultForm form,
                                 BindingResult result, Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getFieldError() == null
                    ? "Nhận xét tổng hợp chưa hợp lệ." : result.getFieldError().getDefaultMessage());
            return "redirect:/lecturer/council-work";
        }
        try {
            scoringService.finalizeResult(councilTopicId, principal.getName(), form);
            redirect.addFlashAttribute("message", "Đã tổng hợp điểm trung bình và nhận xét.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/council-work";
    }

}
