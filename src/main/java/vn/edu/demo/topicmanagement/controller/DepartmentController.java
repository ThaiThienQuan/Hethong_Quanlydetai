package vn.edu.demo.topicmanagement.controller;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.DepartmentForm;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.repository.DepartmentRepository;

/**
 * CRUD bộ môn (mục 5.1b).
 * Yêu cầu: ADMIN hoặc TRUONG_KHOA có quyền DEPARTMENT_MANAGE.
 */
@Controller
@RequestMapping("/admin/departments")
@PreAuthorize("hasAnyRole('ADMIN','TRUONG_KHOA')")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departments", departmentRepository.findAllByOrderByNameAsc());
        return "admin/departments/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("departmentForm")) {
            model.addAttribute("departmentForm", new DepartmentForm());
        }
        model.addAttribute("action", "/admin/departments/new");
        model.addAttribute("pageTitle", "Thêm bộ môn");
        return "admin/departments/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("departmentForm") DepartmentForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (departmentRepository.existsByCode(form.getCode().trim().toUpperCase())) {
            result.rejectValue("code", "exists", "Mã bộ môn đã tồn tại.");
        }
        if (result.hasErrors()) {
            model.addAttribute("action", "/admin/departments/new");
            model.addAttribute("pageTitle", "Thêm bộ môn");
            return "admin/departments/form";
        }

        Department dept = new Department();
        dept.setCode(form.getCode().trim().toUpperCase());
        dept.setName(form.getName().trim());
        dept.setStatus("ACTIVE");
        departmentRepository.save(dept);

        redirect.addFlashAttribute("message", "Đã thêm bộ môn '" + form.getName() + "'.");
        return "redirect:/admin/departments";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy bộ môn."));
        DepartmentForm form = new DepartmentForm();
        form.setCode(dept.getCode());
        form.setName(dept.getName());
        form.setStatus(dept.getStatus());
        model.addAttribute("departmentForm", form);
        model.addAttribute("department", dept);
        model.addAttribute("action", "/admin/departments/" + id + "/edit");
        model.addAttribute("pageTitle", "Sửa bộ môn");
        return "admin/departments/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("departmentForm") DepartmentForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy bộ môn."));

        if (departmentRepository.existsByCodeAndIdNot(form.getCode().trim().toUpperCase(), id)) {
            result.rejectValue("code", "exists", "Mã bộ môn đã tồn tại.");
        }
        if (result.hasErrors()) {
            model.addAttribute("department", dept);
            model.addAttribute("action", "/admin/departments/" + id + "/edit");
            model.addAttribute("pageTitle", "Sửa bộ môn");
            return "admin/departments/form";
        }

        dept.setCode(form.getCode().trim().toUpperCase());
        dept.setName(form.getName().trim());
        dept.setStatus(form.getStatus());
        departmentRepository.save(dept);

        redirect.addFlashAttribute("message", "Đã cập nhật bộ môn.");
        return "redirect:/admin/departments";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirect) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy bộ môn."));
        dept.setStatus("ACTIVE".equals(dept.getStatus()) ? "INACTIVE" : "ACTIVE");
        departmentRepository.save(dept);
        redirect.addFlashAttribute("message", "Đã cập nhật trạng thái bộ môn.");
        return "redirect:/admin/departments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            departmentRepository.deleteById(id);
            redirect.addFlashAttribute("message", "Đã xóa bộ môn.");
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error",
                    "Không thể xóa bộ môn vì đang được sử dụng bởi giảng viên hoặc đề tài. Hãy chuyển trạng thái sang 'Không hoạt động'.");
        }
        return "redirect:/admin/departments";
    }
}
