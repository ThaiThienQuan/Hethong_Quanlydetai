package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.service.EvaluationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasAnyAuthority('RESULT_VIEW', 'ROLE_ADMIN')")
public class StudentResultController {

    private final EvaluationService evaluationService;

    public StudentResultController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/results")
    public String index(Authentication authentication, Model model) {
        model.addAttribute("results", evaluationService.findPublishedResultsForStudent(authentication.getName()));
        return "student-results";
    }
}