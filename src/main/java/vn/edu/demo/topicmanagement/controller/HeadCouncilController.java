package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.CouncilForm;
import vn.edu.demo.topicmanagement.dto.CouncilMemberForm;
import vn.edu.demo.topicmanagement.entity.CouncilRole;
import vn.edu.demo.topicmanagement.service.CouncilService;

/** Trang HEAD tạo hội đồng và quản lý thành viên/đề tài trước khi kích hoạt. */
@Controller
@RequestMapping("/head/councils")
public class HeadCouncilController {
    private final CouncilService councilService;
    public HeadCouncilController(CouncilService councilService) { this.councilService = councilService; }

    @ModelAttribute("councilRoles")
    public CouncilRole[] councilRoles() { return CouncilRole.values(); }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("periods", councilService.getPeriodsForCouncil());
        model.addAttribute("councils", councilService.getCouncils());
        model.addAttribute("councilForm", new CouncilForm());
        return "head/councils/list";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("councilForm") CouncilForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("error", "Vui lòng nhập tên và chọn đợt hợp lệ.");
            return "redirect:/head/councils";
        }
        try {
            Long id = councilService.create(form);
            redirect.addFlashAttribute("message", "Đã tạo hội đồng nháp.");
            return "redirect:/head/councils/" + id;
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/head/councils";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("detail", councilService.getDetail(id));
        model.addAttribute("memberForm", new CouncilMemberForm());
        return "head/councils/detail";
    }

    @PostMapping("/{id}/members")
    public String addMember(@PathVariable Long id, @Valid @ModelAttribute("memberForm") CouncilMemberForm form,
                            BindingResult result, RedirectAttributes redirect) {
        try {
            if (result.hasErrors()) throw new IllegalArgumentException("Chọn giảng viên và vai trò hợp lệ.");
            councilService.addMember(id, form);
            redirect.addFlashAttribute("message", "Đã thêm thành viên hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long id, @PathVariable Long memberId, RedirectAttributes redirect) {
        try {
            councilService.removeMember(id, memberId);
            redirect.addFlashAttribute("message", "Đã gỡ giảng viên khỏi hội đồng nháp.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/topics")
    public String addTopic(@PathVariable Long id, @RequestParam Long registrationId, RedirectAttributes redirect) {
        try {
            councilService.addTopic(id, registrationId);
            redirect.addFlashAttribute("message", "Đã xếp đề tài vào hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/topics/{registrationId}/remove")
    public String removeTopic(@PathVariable Long id, @PathVariable Long registrationId,
                              RedirectAttributes redirect) {
        try {
            councilService.removeTopic(id, registrationId);
            redirect.addFlashAttribute("message", "Đã gỡ đề tài khỏi hội đồng nháp.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            councilService.activate(id);
            redirect.addFlashAttribute("message", "Đã kích hoạt hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }
}
