package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.service.CouncilScoringService;

/** Lets the faculty publish results after the council chair has completed consolidation. */
@Controller
@RequestMapping("/head/results")
public class HeadResultController {
    private final CouncilScoringService scoringService;

    public HeadResultController(CouncilScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("results", scoringService.getResultsAwaitingPublication());
        return "head/results/list";
    }

    @PostMapping("/{councilTopicId}/publish")
    public String publish(@PathVariable Long councilTopicId, Principal principal, RedirectAttributes redirect) {
        try {
            scoringService.publishResultByHead(councilTopicId, principal.getName());
            redirect.addFlashAttribute("message", "Đã công bố kết quả cho sinh viên.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/results";
    }
}
