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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.CouncilDetailView;
import vn.edu.demo.topicmanagement.dto.CouncilForm;
import vn.edu.demo.topicmanagement.dto.CouncilMemberForm;
import vn.edu.demo.topicmanagement.entity.CouncilRole;
import vn.edu.demo.topicmanagement.service.CouncilService;
import vn.edu.demo.topicmanagement.service.ReviewerService;

/** Trang HEAD tạo hội đồng, sửa/xóa, thêm thành viên/đề tài, kích hoạt.
 *
 * <p>Quyền COUNCIL_MANAGE được áp dụng ở SecurityConfig cho {@code /head/councils/**}.
 */
@Controller
@RequestMapping("/head/councils")
@PreAuthorize("hasAuthority('COUNCIL_MANAGE')")
public class HeadCouncilController {
    private final CouncilService councilService;
    private final ReviewerService reviewerService;
    public HeadCouncilController(CouncilService councilService, ReviewerService reviewerService) {
        this.councilService = councilService;
        this.reviewerService = reviewerService;
    }

    @ModelAttribute("councilRoles")
    public CouncilRole[] councilRoles() { return CouncilRole.values(); }

    @GetMapping
    public String list(Model model) {
        addListPage(model, new CouncilForm());
        return "head/councils/list";
    }

    /** Xem trước mã hội đồng sẽ được cấp - form gọi qua fetch khi chọn đợt. */
    @GetMapping("/preview-code")
    @ResponseBody
    public String previewCode(@RequestParam Long periodId) {
        String code = councilService.previewCouncilCode(periodId);
        return code == null ? "" : code;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("councilForm") CouncilForm form, BindingResult result,
                         Principal principal, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            // Trả về thẳng form để hiện lỗi đúng ô, thay vì redirect làm mất
            // lỗi chi tiết và dữ liệu vừa nhập.
            addListPage(model, form);
            return "head/councils/list";
        }
        try {
            Long id = councilService.create(form, principal.getName());
            redirect.addFlashAttribute("message", "Đã tạo hội đồng nháp.");
            return "redirect:/head/councils/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/head/councils";
        }
    }

    /** Nạp lại dữ liệu trang danh sách khi form tạo hội đồng có lỗi. */
    private void addListPage(Model model, CouncilForm form) {
        model.addAttribute("councilForm", form);
        model.addAttribute("periods", councilService.getPeriodsForCouncil());
        model.addAttribute("councils", councilService.getCouncils());
        model.addAttribute("previewCode", councilService.previewCouncilCode(form.getPeriodId()));
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("councilForm") CouncilForm form,
                         BindingResult result, Principal principal, RedirectAttributes redirect) {
        try {
            if (result.hasErrors()) throw new IllegalArgumentException("Vui lòng nhập thông tin hợp lệ.");
            councilService.update(id, form, principal.getName());
            redirect.addFlashAttribute("message", "Đã cập nhật hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            councilService.delete(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã xóa hội đồng.");
            return "redirect:/head/councils";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/head/councils/" + id;
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var detail = councilService.getDetail(id);
        model.addAttribute("detail", detail);
        model.addAttribute("memberForm", new CouncilMemberForm());
        // Pre-fill form Sửa từ dữ liệu hội đồng hiện tại, nếu không form sẽ rỗng và
        // @Valid bắt buộc nhập lại từ đầu (trước đây điền bằng new CouncilForm()).
        model.addAttribute("editForm", toEditForm(detail));
        return "head/councils/detail";
    }

    /** Dựng CouncilForm từ hội đồng để form Sửa hiển thị đúng giá trị đang lưu. */
    private CouncilForm toEditForm(CouncilDetailView detail) {
        var council = detail.getCouncil();
        CouncilForm form = new CouncilForm();
        form.setCouncilCode(council.getCouncilCode());
        form.setCouncilName(council.getName());
        form.setPeriodId(council.getPeriodId());
        form.setReportDate(council.getReportDate());
        return form;
    }

    @PostMapping("/{id}/members")
    public String addMember(@PathVariable Long id, @Valid @ModelAttribute("memberForm") CouncilMemberForm form,
                            BindingResult result, Principal principal, RedirectAttributes redirect) {
        try {
            if (result.hasErrors()) throw new IllegalArgumentException("Chọn giảng viên và vai trò hợp lệ.");
            councilService.addMember(id, form, principal.getName());
            redirect.addFlashAttribute("message", "Đã thêm thành viên hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long id, @PathVariable Long memberId,
                               Principal principal, RedirectAttributes redirect) {
        try {
            councilService.removeMember(id, memberId, principal.getName());
            redirect.addFlashAttribute("message", "Đã gỡ giảng viên khỏi hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/members/{memberId}/role")
    public String updateMemberRole(@PathVariable Long id, @PathVariable Long memberId,
                                  @RequestParam CouncilRole role,
                                  Principal principal, RedirectAttributes redirect) {
        try {
            councilService.updateMemberRole(id, memberId, role, principal.getName());
            redirect.addFlashAttribute("message", "Đã đổi vai trò thành viên.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/topics")
    public String addTopic(@PathVariable Long id, @RequestParam Long registrationId,
                           Principal principal, RedirectAttributes redirect) {
        try {
            councilService.addTopic(id, registrationId, principal.getName());
            redirect.addFlashAttribute("message", "Đã xếp đề tài vào hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/topics/{topicId}/remove")
    public String removeTopic(@PathVariable Long id, @PathVariable Long topicId,
                              Principal principal, RedirectAttributes redirect) {
        try {
            councilService.removeTopic(id, topicId, principal.getName());
            redirect.addFlashAttribute("message", "Đã gỡ đề tài khỏi hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            councilService.activate(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã kích hoạt hội đồng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id;
    }

    /**
     * Trang phân công GVPB cho một đề tài của hội đồng. Chỉ các thành viên hội đồng mới là ứng viên,
     * GVHD/người tạo đề tài đã bị loại ở service.
     */
    @GetMapping("/{id}/topics/{councilTopicId}/assignments")
    public String assignments(@PathVariable Long id, @PathVariable Long councilTopicId,
                             Model model, Principal principal) {
        var existing = reviewerService.listAssignments(councilTopicId);
        var candidates = reviewerService.listReviewerCandidates(councilTopicId);
        var detail = councilService.getDetail(id);
        var topicProgress = detail.getTopics().stream()
                .filter(t -> t.getCouncilTopicId() != null && t.getCouncilTopicId().equals(councilTopicId))
                .findFirst().orElse(null);
        String topicCode = topicProgress == null ? "" : topicProgress.getTopicCode();
        String topicTitle = topicProgress == null ? "" : topicProgress.getTopicTitle();
        String groupName = topicProgress == null ? "" : topicProgress.getGroupName();
        boolean ready = detail.getCouncil().getStatus() == vn.edu.demo.topicmanagement.entity.CouncilStatus.READY;
        model.addAttribute("view",
                new vn.edu.demo.topicmanagement.dto.CouncilTopicAssignmentView(
                        id, councilTopicId, topicProgress == null ? null : topicProgress.getTopicId(),
                        topicCode, topicTitle, groupName, existing, candidates, ready));
        return "head/councils/assignments";
    }

    @PostMapping("/{id}/topics/{councilTopicId}/assignments")
    public String doAssign(@PathVariable Long id, @PathVariable Long councilTopicId,
                           @RequestParam Long lecturerId,
                           Principal principal, RedirectAttributes redirect) {
        try {
            reviewerService.assign(councilTopicId, lecturerId, principal.getName());
            redirect.addFlashAttribute("message", "Đã phân công giảng viên chấm.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id + "/topics/" + councilTopicId + "/assignments";
    }

    @PostMapping("/{id}/topics/{councilTopicId}/assignments/{assignmentId}/remove")
    public String doRemove(@PathVariable Long id, @PathVariable Long councilTopicId,
                           @PathVariable Long assignmentId,
                           Principal principal, RedirectAttributes redirect) {
        try {
            reviewerService.removeAssignment(assignmentId, principal.getName());
            redirect.addFlashAttribute("message", "Đã gỡ phân công chưa có điểm.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/councils/" + id + "/topics/" + councilTopicId + "/assignments";
    }
}
