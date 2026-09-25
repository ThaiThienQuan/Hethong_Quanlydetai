package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.service.EvaluationService;
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
    public String index(Model model) {
        model.addAttribute("evaluations", evaluationService.findAll());
        return "evaluations";
    }

    @GetMapping("/new")
    public String createForm() {
        return "evaluation-form";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long assignmentId,
                       @RequestParam BigDecimal score,
                       @RequestParam String comment,
                       RedirectAttributes redirectAttributes) {
        try {
            evaluationService.save(assignmentId, score, comment);
            redirectAttributes.addFlashAttribute("success", "Đã nhập điểm thành công.");
            return "redirect:/evaluations";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/evaluations/new";
        }
    }

    @PostMapping("/calculate")
    public String calculate(@RequestParam Long groupId,
                            @RequestParam Long topicId,
                            @RequestParam Long councilId,
                            RedirectAttributes redirectAttributes) {
        try {
            evaluationService.calculateResult(groupId, topicId, councilId);
            redirectAttributes.addFlashAttribute("success", "Đã tính điểm cuối cùng.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/evaluations";
    }
}