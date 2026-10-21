package vn.edu.demo.topicmanagement.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.entity.Lecturer;
import vn.edu.demo.topicmanagement.entity.Student;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.DepartmentRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.StudentRepository;

/**
 * Nghiệp vụ xem/sửa hồ sơ GV và SV (mục 5.1b).
 * Tạo mới được thực hiện qua AdminUserController (tạo tài khoản kèm hồ sơ).
 */
@Service
public class ProfileManagementService {

    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;

    public ProfileManagementService(LecturerRepository lecturerRepository,
                                  StudentRepository studentRepository,
                                  DepartmentRepository departmentRepository) {
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
    }

    // ======================= LECTURER PROFILES =======================

    /**
     * open-in-view=false nên phải fetch join user/department ngay trong service,
     * nếu không view sẽ lỗi LazyInitializationException.
     */
    @Transactional(readOnly = true)
    public List<Lecturer> findAllLecturers() {
        return lecturerRepository.findAllForProfileList(null);
    }

    @Transactional(readOnly = true)
    public List<Lecturer> findLecturersByDepartment(Long departmentId) {
        return lecturerRepository.findAllForProfileList(departmentId);
    }

    @Transactional(readOnly = true)
    public Lecturer getLecturerById(Long id) {
        return lecturerRepository.findByIdForProfile(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ giảng viên."));
    }

    @Transactional
    public void updateLecturer(Long id, String fullName, String email, String phone,
                              String academicDegree, Long departmentId) {
        Lecturer lecturer = getLecturerById(id);
        User user = lecturer.getUser();

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (email != null && email.isBlank()) {
            user.setEmail(null);
        } else if (email != null) {
            user.setEmail(email.trim());
        }
        if (phone != null && phone.isBlank()) {
            user.setPhone(null);
        } else if (phone != null) {
            user.setPhone(phone.trim());
        }
        if (academicDegree != null) {
            lecturer.setAcademicDegree(academicDegree.isBlank() ? null : academicDegree.trim());
        }
        if (departmentId != null) {
            Department dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bộ môn."));
            lecturer.setDepartment(dept);
        }
    }

    // ======================= STUDENT PROFILES =======================

    @Transactional(readOnly = true)
    public List<Student> findAllStudents() {
        return studentRepository.findAllForProfileList();
    }

    @Transactional(readOnly = true)
    public Student getStudentById(Long id) {
        return studentRepository.findByIdForProfile(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ sinh viên."));
    }

    @Transactional
    public void updateStudent(Long id, String fullName, String email, String phone,
                              String className, String major) {
        Student student = getStudentById(id);
        User user = student.getUser();

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (email != null && email.isBlank()) {
            user.setEmail(null);
        } else if (email != null) {
            user.setEmail(email.trim());
        }
        if (phone != null && phone.isBlank()) {
            user.setPhone(null);
        } else if (phone != null) {
            user.setPhone(phone.trim());
        }
        if (className != null && !className.isBlank()) {
            student.setClassName(className.trim());
        }
        if (major != null && !major.isBlank()) {
            student.setMajor(major.trim());
        }
    }

    // ======================= HELPERS =======================

    @Transactional(readOnly = true)
    public List<Department> getActiveDepartments() {
        return departmentRepository.findByStatusOrderByNameAsc("ACTIVE");
    }
}
