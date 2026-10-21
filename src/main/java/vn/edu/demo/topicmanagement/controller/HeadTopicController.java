package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.HeadSupervisorForm;
import vn.edu.demo.topicmanagement.entity.TopicStatus;
import vn.edu.demo.topicmanagement.service.TopicService;

/**
 * Trưởng khoa duyệt đề tài - CHỉ Trưởng khoa (/head/** -> HEAD).
 *   GET  /head/topics                 hàng đợi duyệt (lọc bộ môn/đợt/trạng thái, phân trang)
 *   POST /head/topics/{id}/approve     duyệt
 *   POST /head/topics/{id}/reject      từ chối (bắt buộc có lý do)
 *   POST /head/topics/{id}/publish     công bố một đề tài
 *   POST /head/topics/publish-by-period công bố danh sách đề tài theo đợt
 *   POST /head/topics/{id}/delete      xóa đề tài (chỉ khi chưa có đăng ký)
 *   GET/POST /head/topics/{id}/supervisors phân công / điều chỉnh GVHD (SUPERVISOR_ASSIGN)
 */
@Controller
@RequestMapping("/head/topics")
@PreAuthorize("hasAuthority('TOPIC_VIEW')")
public class HeadTopicController {

    private final TopicService topicService;

    public HeadTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(required = false) Long periodId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<?> rows = topicService.getApprovalQueue(status, departmentId, periodId, page);
        model.addAttribute("rows", rows);
        model.addAttribute("page", rows);
        model.addAttribute("totalPages", rows.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("departments", topicService.getDepartments());
        model.addAttribute("periods", topicService.getAllPeriodsForFilter());
        model.addAttribute("statuses", TopicStatus.values());
        return "head/topics/list";
    }

    /** Phân công GVHD - quyền riêng SUPERVISOR_ASSIGN (A11), không cần TOPIC_APPROVE. */
    @GetMapping("/{id}/supervisors")
    @PreAuthorize("hasAuthority('SUPERVISOR_ASSIGN')")
    public String supervisorForm(@PathVariable Long id, Model model) {
        var info = topicService.getHeadSupervisorInfo(id);
        HeadSupervisorForm form = new HeadSupervisorForm();
        form.setPrimarySupervisorId(info.primarySupervisorId());
        form.setCoSupervisorId(info.coSupervisorId());
        model.addAttribute("info", info);
        model.addAttribute("id", id);
        model.addAttribute("form", form);
        model.addAttribute("lecturers", topicService.getActiveLecturerOptions());
        return "head/topics/supervisors";
    }

    @PostMapping("/{id}/supervisors")
    @PreAuthorize("hasAuthority('SUPERVISOR_ASSIGN')")
    public String updateSupervisors(@PathVariable Long id,
                                    @ModelAttribute HeadSupervisorForm form,
                                    Principal principal,
                                    RedirectAttributes redirect) {
        try {
            topicService.updateSupervisorsByHead(id, form, principal.getName());
            redirect.addFlashAttribute("message", "Đã cập nhật giảng viên hướng dẫn cho đề tài.");
            return "redirect:/head/topics";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/head/topics/" + id + "/supervisors";
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String approve(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            topicService.approve(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã duyệt đề tài.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/topics";
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String reject(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String reason,
                         Principal principal,
                         RedirectAttributes redirect) {
        try {
            topicService.reject(id, reason, principal.getName());
            redirect.addFlashAttribute("message", "Đã từ chối đề tài.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/topics";
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String publish(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            topicService.publish(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã công bố đề tài.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/topics";
    }

    /** Công bố danh sách đề tài đã duyệt theo từng đợt (giai đoạn 1). */
    @PostMapping("/publish-by-period")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String publishByPeriod(@RequestParam Long periodId,
                                  Principal principal,
                                  RedirectAttributes redirect) {
        int count = topicService.publishAllApproved(periodId, principal.getName());
        redirect.addFlashAttribute("message", "Đã công bố " + count + " đề tài của đợt đã chọn.");
        return "redirect:/head/topics";
    }

    /** Trưởng khoa xóa đề tài khi chưa có đăng ký nào. */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('TOPIC_APPROVE')")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            topicService.deleteByHead(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã xóa đề tài.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/head/topics";
    }
}