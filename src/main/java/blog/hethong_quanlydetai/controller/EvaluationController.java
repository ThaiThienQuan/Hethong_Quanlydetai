package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.service.EvaluationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVALUATION_CREATE', 'RESULT_CALCULATE', 'RESULT_PUBLISH', 'ROLE_ADMIN')")
    public String index(Authentication authentication, Model model) {
        boolean admin = isAdmin(authentication);
        model.addAttribute("evaluations", evaluationService.findAll());
        model.addAttribute("results", evaluationService.findResults());
        model.addAttribute("calculationOptions",
                evaluationService.findReadyCalculations(authentication.getName(), admin));
        return "evaluations";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyAuthority('EVALUATION_CREATE', 'ROLE_ADMIN')")
    public String createForm(Authentication authentication, Model model) {
        model.addAttribute("assignments", evaluationService.findPendingAssignments(
                authentication.getName(), isAdmin(authentication)));
        return "evaluation-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyAuthority('EVALUATION_CREATE', 'ROLE_ADMIN')")
    public String save(@RequestParam Long assignmentId,
                       @RequestParam BigDecimal score,
                       @RequestParam String comment,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        try {
            evaluationService.save(assignmentId, score, comment, authentication.getName(), isAdmin(authentication));
            redirectAttributes.addFlashAttribute("success", "Đã nhập điểm thành công.");
            return "redirect:/evaluations";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/evaluations/new";
        }
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyAuthority('RESULT_CALCULATE', 'ROLE_ADMIN')")
        public String calculate(@RequestParam String selection,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            ScoreTarget target = parseSelection(selection);
            evaluationService.calculateResult(
                target.groupId(), target.topicId(), target.councilId(),
                authentication.getName(), isAdmin(authentication));
            redirectAttributes.addFlashAttribute("success", "Đã tính điểm cuối cùng.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/evaluations";
    }

    private ScoreTarget parseSelection(String selection) {
        String[] ids = selection == null ? new String[0] : selection.split(":", -1);
        if (ids.length != 3) {
            throw new IllegalArgumentException("Vui lòng chọn nhóm, đề tài và hội đồng cần tổng hợp.");
        }
        try {
            return new ScoreTarget(Long.valueOf(ids[0]), Long.valueOf(ids[1]), Long.valueOf(ids[2]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Lựa chọn tổng hợp không hợp lệ.");
        }
    }

    @PostMapping("/results/{id}/publish")
    @PreAuthorize("hasAnyAuthority('RESULT_PUBLISH', 'ROLE_ADMIN')")
    public String publish(@PathVariable Long id,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            evaluationService.publishResult(id, authentication.getName(), isAdmin(authentication));
            redirectAttributes.addFlashAttribute("success", "Đã công bố kết quả cho sinh viên.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/evaluations";
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private record ScoreTarget(Long groupId, Long topicId, Long councilId) {
    }
}