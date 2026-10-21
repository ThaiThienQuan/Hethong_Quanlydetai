package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Trang chấm điểm hội đồng và trang chủ tịch tổng hợp (mục 5.7 &amp; 5.8). */
@Controller
@RequestMapping("/lecturer/council-work")
@PreAuthorize("hasAnyAuthority('EVALUATION_CREATE','RESULT_CALCULATE','TOPIC_VIEW')")
public class LecturerCouncilController {
    private final CouncilScoringService scoringService;

    public LecturerCouncilController(CouncilScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("work", scoringService.getMyWork(principal.getName()));
        return "lecturer/councils/work";
    }

    /**
     * Nộp / sửa điểm. {@code assignmentId} là {@code review_assignments.id}
     * (mỗi GV được phân công có một bản chấm riêng).
     */
    @PostMapping("/{assignmentId}/score")
    public String score(@PathVariable Long assignmentId,
                        @Valid @ModelAttribute("scoreForm") ReviewScoreForm form,
                        BindingResult result, Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", result.getFieldError() == null
                    ? "Điểm chưa hợp lệ." : result.getFieldError().getDefaultMessage());
            return "redirect:/lecturer/council-work";
        }
        try {
            scoringService.submitScore(assignmentId, principal.getName(), form);
            redirect.addFlashAttribute("message", "Đã lưu điểm hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException
                 | org.springframework.web.server.ResponseStatusException e) {
            String msg = e.getMessage();
            redirect.addFlashAttribute("error", msg == null ? "Không thể lưu điểm." : msg);
        }
        return "redirect:/lecturer/council-work";
    }

    /**
     * Chủ tịch tổng hợp. {@code councilTopicId} là {@code council_topics.id}
     * (một đề tài trong hội đồng, không phải assignment của từng GV).
     */
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
            redirect.addFlashAttribute("message", "Đã tổng hợp điểm trung bình, kết quả và nhận xét.");
        } catch (IllegalArgumentException | IllegalStateException
                 | org.springframework.web.server.ResponseStatusException e) {
            String msg = e.getMessage();
            redirect.addFlashAttribute("error", msg == null ? "Không thể tổng hợp." : msg);
        }
        return "redirect:/lecturer/council-work";
    }
}