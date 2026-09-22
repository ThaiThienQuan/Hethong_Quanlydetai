package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.RegistrationPeriod;
import blog.hethong_quanlydetai.entity.RegistrationType;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/registration-periods")
public class RegistrationPeriodController {
    private final RegistrationPeriodService service;

    public RegistrationPeriodController(RegistrationPeriodService service) {
        this.service = service;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("periods", service.findAll());
        model.addAttribute("now", LocalDateTime.now());
        model.addAttribute("periodTypes", RegistrationType.values());
        return "registration-periods";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("period", new RegistrationPeriod());
        model.addAttribute("periodTypes", RegistrationType.values());
        return "registration-period-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("period", service.findById(id));
        model.addAttribute("periodTypes", RegistrationType.values());
        return "registration-period-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("period") RegistrationPeriod period,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("periodTypes", RegistrationType.values());
            return "registration-period-form";
        }
        service.save(period);
        redirectAttributes.addFlashAttribute("success", "Đã lưu đợt đăng ký.");
        return "redirect:/registration-periods";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        service.delete(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa đợt đăng ký.");
        return "redirect:/registration-periods";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        service.publish(id);
        redirectAttributes.addFlashAttribute("success", "Đã công bố đợt đăng ký.");
        return "redirect:/registration-periods";
    }
}