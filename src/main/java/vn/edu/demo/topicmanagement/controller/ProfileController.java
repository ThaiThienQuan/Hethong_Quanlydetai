package vn.edu.demo.topicmanagement.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.entity.Lecturer;
import vn.edu.demo.topicmanagement.entity.Student;
import vn.edu.demo.topicmanagement.service.ProfileManagementService;

/**
 * Quản lý hồ sơ GV và SV (mục 5.1b).
 * Yêu cầu: ADMIN hoặc TRUONG_KHOA có quyền PROFILE_MANAGE.
 */
@Controller
@RequestMapping("/admin/profiles")
@PreAuthorize("hasAnyRole('ADMIN','TRUONG_KHOA')")
public class ProfileController {

    /** Số hồ sơ hiển thị mỗi trang (mục 5.1b: danh sách có phân trang). */
    private static final int PAGE_SIZE = 10;

    private final ProfileManagementService profileService;

    public ProfileController(ProfileManagementService profileService) {
        this.profileService = profileService;
    }

    // ======================= LECTURERS =======================

    @GetMapping("/lecturers")
    public String listLecturers(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        Page<Lecturer> result = (departmentId != null)
                ? profileService.findLecturersByDepartment(departmentId, page, PAGE_SIZE)
                : profileService.findAllLecturers(page, PAGE_SIZE);

        model.addAttribute("lecturers", result.getContent());
        model.addAttribute("page", result);
        model.addAttribute("departments", profileService.getActiveDepartments());
        model.addAttribute("selectedDepartment", departmentId);
        return "admin/profiles/lecturers/list";
    }

    @GetMapping("/lecturers/{id}")
    public String viewLecturer(@PathVariable Long id, Model model) {
        Lecturer lecturer = profileService.getLecturerById(id);
        model.addAttribute("lecturer", lecturer);
        return "admin/profiles/lecturers/detail";
    }

    @GetMapping("/lecturers/{id}/edit")
    public String editLecturerForm(@PathVariable Long id, Model model) {
        Lecturer lecturer = profileService.getLecturerById(id);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("departments", profileService.getActiveDepartments());
        return "admin/profiles/lecturers/form";
    }

    @PostMapping("/lecturers/{id}/edit")
    public String updateLecturer(@PathVariable Long id,
                                @RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String academicDegree,
                                @RequestParam(required = false) Long departmentId,
                                RedirectAttributes redirect) {
        try {
            profileService.updateLecturer(id, fullName, email, phone, academicDegree, departmentId);
            redirect.addFlashAttribute("message", "Đã cập nhật hồ sơ giảng viên.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/profiles/lecturers";
    }

    // ======================= STUDENTS =======================

    @GetMapping("/students")
    public String listStudents(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Student> result = profileService.findAllStudents(page, PAGE_SIZE);
        model.addAttribute("students", result.getContent());
        model.addAttribute("page", result);
        return "admin/profiles/students/list";
    }

    @GetMapping("/students/{id}")
    public String viewStudent(@PathVariable Long id, Model model) {
        Student student = profileService.getStudentById(id);
        model.addAttribute("student", student);
        return "admin/profiles/students/detail";
    }

    @GetMapping("/students/{id}/edit")
    public String editStudentForm(@PathVariable Long id, Model model) {
        Student student = profileService.getStudentById(id);
        model.addAttribute("student", student);
        return "admin/profiles/students/form";
    }

    @PostMapping("/students/{id}/edit")
    public String updateStudent(@PathVariable Long id,
                               @RequestParam(required = false) String fullName,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String className,
                               @RequestParam(required = false) String major,
                               RedirectAttributes redirect) {
        try {
            profileService.updateStudent(id, fullName, email, phone, className, major);
            redirect.addFlashAttribute("message", "Đã cập nhật hồ sơ sinh viên.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/profiles/students";
    }
}
