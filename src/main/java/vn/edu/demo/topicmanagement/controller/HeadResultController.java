package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.service.CouncilScoringService;

/**
 * Trang Trưởng khoa công bố kết quả (mục 5.8).
 *
 * <p>Có thể công bố từng đề tài hoặc cả hội đồng. Sau khi công bố, hệ thống gửi email
 * riêng cho từng SV trong nhóm và ghi vào {@code email_logs}.
 */
@Controller
@RequestMapping("/head/results")
@PreAuthorize("hasAuthority('RESULT_PUBLISH')")
public class HeadResultController {
    private final CouncilScoringService scoringService;

    public HeadResultController(CouncilScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("results", scoringService.getResultsAwaitingPublication());
        model.addAttribute("councils", scoringService.getCouncilsAwaitingPublication());
        return "head/results/list";
    }

    @PostMapping("/{councilTopicId}/publish")
    public String publish(@PathVariable Long councilTopicId, Principal principal, RedirectAttributes redirect) {
        try {
            scoringService.publishResultByHead(councilTopicId, principal.getName());
            redirect.addFlashAttribute("message", "Đã công bố kết quả và gửi email cho sinh viên.");
        } catch (IllegalArgumentException | IllegalStateException
                 | org.springframework.web.server.ResponseStatusException e) {
            redirect.addFlashAttribute("error", message(e));
        }
        return "redirect:/head/results";
    }

    /** Công bố toàn bộ kết quả đang chờ của một hội đồng. */
    @PostMapping("/council/{councilId}/publish-all")
    public String publishAll(@PathVariable Long councilId, Principal principal, RedirectAttributes redirect) {
        try {
            int n = scoringService.publishWholeCouncil(councilId, principal.getName());
            redirect.addFlashAttribute("message", "Đã công bố " + n + " kết quả của hội đồng và gửi email cho sinh viên.");
        } catch (IllegalArgumentException | IllegalStateException
                 | org.springframework.web.server.ResponseStatusException e) {
            redirect.addFlashAttribute("error", message(e));
        }
        return "redirect:/head/results";
    }

    private String message(RuntimeException e) {
        String msg = e.getMessage();
        return msg == null ? "Không thể công bố kết quả." : msg;
    }
}