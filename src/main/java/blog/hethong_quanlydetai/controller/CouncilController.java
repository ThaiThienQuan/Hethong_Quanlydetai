package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.Council;
import blog.hethong_quanlydetai.service.CouncilService;
import blog.hethong_quanlydetai.service.RegistrationPeriodService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import blog.hethong_quanlydetai.service.CouncilMemberService;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
@RequestMapping("/councils")
public class CouncilController {

    private final CouncilService councilService;
    private final RegistrationPeriodService periodService;
    private final CouncilMemberService councilMemberService;

    public CouncilController(CouncilService councilService,
                         RegistrationPeriodService periodService,
                         CouncilMemberService councilMemberService) {
    this.councilService = councilService;
    this.periodService = periodService;
    this.councilMemberService = councilMemberService;
}

    @GetMapping
    public String index(Model model) {
        model.addAttribute("councils", councilService.findAll());
        return "councils";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("council", new Council());
        model.addAttribute("periods", periodService.findAll());
        return "council-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("council") Council council,
                   Model model,
                   RedirectAttributes redirectAttributes) {
    try {
        councilService.save(council);
        redirectAttributes.addFlashAttribute("success", "Đã tạo hội đồng phản biện.");
        return "redirect:/councils";
    } catch (DataIntegrityViolationException e) {
        model.addAttribute("error", "Mã hội đồng đã tồn tại hoặc dữ liệu không hợp lệ.");
        model.addAttribute("council", council);
        model.addAttribute("periods", periodService.findAll());
        return "council-form";
    }
}

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                     RedirectAttributes redirectAttributes) {
    try {
        councilService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa hội đồng.");
    } catch (DataIntegrityViolationException e) {
        redirectAttributes.addFlashAttribute("error", "Không thể xóa hội đồng vì đã có giảng viên, phân công chấm hoặc kết quả liên quan.");
    }

    return "redirect:/councils";
}
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
    model.addAttribute("council", councilService.findById(id));
    model.addAttribute("members", councilMemberService.findByCouncilId(id));
    return "council-detail";
}

    @PostMapping("/{id}/members/add")
    public String addMember(@PathVariable Long id,
                        @RequestParam Long lecturerId,
                        @RequestParam String role,
                        RedirectAttributes redirectAttributes) {
    try {
        councilMemberService.addMember(id, lecturerId, role);
        redirectAttributes.addFlashAttribute("success", "Đã thêm giảng viên vào hội đồng.");
    } catch (IllegalArgumentException e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }

    return "redirect:/councils/" + id;
}

    @PostMapping("/{councilId}/members/{memberId}/delete")
    public String deleteMember(@PathVariable Long councilId,
                           @PathVariable Long memberId,
                           RedirectAttributes redirectAttributes) {
    councilMemberService.delete(memberId);
    redirectAttributes.addFlashAttribute("success", "Đã xóa giảng viên khỏi hội đồng.");
    return "redirect:/councils/" + councilId;
}
    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id,
                       RedirectAttributes redirectAttributes) {
    try {
        councilService.completeCouncil(id);
        redirectAttributes.addFlashAttribute("success", "Đã hoàn tất hội đồng phản biện.");
    } catch (IllegalArgumentException e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
    }

    return "redirect:/councils/" + id;
}
}