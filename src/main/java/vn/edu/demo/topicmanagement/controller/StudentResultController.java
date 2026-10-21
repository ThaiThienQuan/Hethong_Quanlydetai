package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.demo.topicmanagement.service.CouncilScoringService;

/** Sinh viên chỉ nhận kết quả công khai gắn với đăng ký thuộc nhóm mình. */
@Controller
@RequestMapping("/student/results")
public class StudentResultController {
    private final CouncilScoringService scoringService;
    public StudentResultController(CouncilScoringService scoringService) { this.scoringService = scoringService; }
    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("results", scoringService.getResultsForStudent(principal.getName()));
        return "student/results/list";
    }
}
