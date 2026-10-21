package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.TopicForm;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.service.TopicService;

/**
 * Giảng viên đề xuất đề tài - CHỈ giảng viên (/lecturer/** -> LECTURER).
 *   GET  /lecturer/topics            đề tài của tôi
 *   GET  /lecturer/topics/new        form đề xuất
 *   POST /lecturer/topics/new        gửi form đề xuất
 *   GET  /lecturer/topics/{id}/edit  form sửa
 *   POST /lecturer/topics/{id}/edit  gửi form sửa
 *   POST /lecturer/topics/{id}/delete xóa (chỉ khi chưa có đăng ký)
 */
@Controller
@RequestMapping("/lecturer/topics")
@PreAuthorize("hasAuthority('TOPIC_VIEW')")
public class LecturerTopicController {

    private final TopicService topicService;

    public LecturerTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @ModelAttribute("departments")
    public List<Department> departments() { return topicService.getDepartments(); }

    @ModelAttribute("periods")
    public List<RegistrationPeriod> periods() { return topicService.getOpenPeriods(); }

    /**
     * Bộ môn của GV đang đăng nhập - form đề xuất không cho chọn bộ môn khác (A5).
     */
    @ModelAttribute("myDepartment")
    public TopicService.MyDepartment myDepartment(Principal principal) {
        return principal == null ? null : topicService.getMyDepartment(principal.getName());
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("rows", topicService.getMyTopics(principal.getName()));
        return "lecturer/topics/list";
    }

    @GetMapping("/new")
    public String newForm(Principal principal, Model model) {
        TopicForm form = new TopicForm();
        TopicService.MyDepartment dept = topicService.getMyDepartment(principal.getName());
        if (dept != null) form.setDepartmentId(dept.id());
        model.addAttribute("topicForm", form);
        model.addAttribute("lecturers", topicService.getCoSupervisorCandidates(principal.getName(), null, null));
        setFormInfo(model, "/lecturer/topics/new", "Đề xuất đề tài mới");
        return "lecturer/topics/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String create(@Valid @ModelAttribute("topicForm") TopicForm form, BindingResult result,
                         Principal principal, Model model, RedirectAttributes redirect) {
        addRuleErrors(form, principal.getName(), null, result);
        if (result.hasErrors()) {
            model.addAttribute("lecturers",
                    topicService.getCoSupervisorCandidates(principal.getName(), null, form.getPeriodId()));
            setFormInfo(model, "/lecturer/topics/new", "Đề xuất đề tài mới");
            return "lecturer/topics/form";
        }
        try {
            topicService.create(form, principal.getName());
            redirect.addFlashAttribute("message", "Đã gửi đề tài, đang chờ Trưởng khoa duyệt.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/topics/new";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Ràng buộc CHECK/UNIQUE ở tầng DB: trả về form kèm thông báo thay vì lỗi 500.
            model.addAttribute("lecturers",
                    topicService.getCoSupervisorCandidates(principal.getName(), null, form.getPeriodId()));
            result.rejectValue("description", "db.constraint", dbMessage(e));
            setFormInfo(model, "/lecturer/topics/new", "Đề xuất đề tài mới");
            return "lecturer/topics/form";
        }
        return "redirect:/lecturer/topics";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Principal principal, Model model,
                           RedirectAttributes redirect) {
        try {
            model.addAttribute("topicForm", topicService.getFormForEdit(id, principal.getName()));
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/topics";
        }
        model.addAttribute("lecturers",
                topicService.getCoSupervisorCandidates(principal.getName(), id, null));
        setFormInfo(model, "/lecturer/topics/" + id + "/edit", "Sửa đề tài");
        return "lecturer/topics/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("topicForm") TopicForm form,
                         BindingResult result, Principal principal, Model model,
                         RedirectAttributes redirect) {
        addRuleErrors(form, principal.getName(), id, result);
        if (result.hasErrors()) {
            model.addAttribute("lecturers",
                    topicService.getCoSupervisorCandidates(principal.getName(), id, form.getPeriodId()));
            setFormInfo(model, "/lecturer/topics/" + id + "/edit", "Sửa đề tài");
            return "lecturer/topics/form";
        }
        try {
            topicService.update(id, form, principal.getName());
            redirect.addFlashAttribute("message", "Đã cập nhật đề tài và gửi duyệt lại.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("lecturers",
                    topicService.getCoSupervisorCandidates(principal.getName(), id, form.getPeriodId()));
            result.rejectValue("description", "db.constraint", dbMessage(e));
            setFormInfo(model, "/lecturer/topics/" + id + "/edit", "Sửa đề tài");
            return "lecturer/topics/form";
        }
        return "redirect:/lecturer/topics";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('TOPIC_CREATE')")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            topicService.deleteOwnUnsubmitted(id, principal.getName());
            redirect.addFlashAttribute("message", "Đã xóa đề tài.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/topics";
    }

    private void addRuleErrors(TopicForm form, String username, Long topicId, BindingResult result) {
        Map<String, String> errors = topicService.checkRules(form, username, topicId);
        errors.forEach((field, message) -> result.rejectValue(field, "rule", message));
    }

    /**
     * Dịch thông điệp vi phạm ràng buộc DB sang tiếng Việt để hiển thị cho người dùng,
     * tránh lộ trang lỗi 500 khi dữ liệu lọt qua validation của form.
     */
    private String dbMessage(org.springframework.dao.DataIntegrityViolationException e) {
        String msg = String.valueOf(e.getMostSpecificCause().getMessage());
        if (msg.contains("chk_topics_description")) {
            return "Mô tả nội dung đề tài phải có tối thiểu 20 ký tự.";
        }
        if (msg.contains("uq_topics_code")) {
            return "Mã đề tài đã tồn tại, vui lòng chọn mã khác.";
        }
        return "Dữ liệu không hợp lệ, vui lòng kiểm tra lại các trường đã nhập.";
    }

    private void setFormInfo(Model model, String action, String title) {
        model.addAttribute("action", action);
        model.addAttribute("pageTitle", title);
    }
}