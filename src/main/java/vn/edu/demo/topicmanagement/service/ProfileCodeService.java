package vn.edu.demo.topicmanagement.service;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.entity.Lecturer;
import vn.edu.demo.topicmanagement.entity.Student;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.StudentRepository;

/**
 * Cấp mã định danh cho sinh viên và giảng viên (mục 5.1 - tạo tài khoản kèm hồ sơ).
 *
 * <p>Trước đây Admin tự nhập tay mã SV/GV nên dễ trùng. Nay hệ thống tự sinh mã
 * theo dạng {@code SV<5 chữ số>} và {@code GV<3 chữ số>} — mã do hệ thống quản lý
 * nên không thể trùng, người dùng không cần nhớ/quyết định mã.
 *
 * <p>Cơ chế chống trùng gồm 3 lớp:
 * <ol>
 *   <li>Lấy số thứ tự lớn nhất đang có rồi cấp số kế tiếp; nếu mã vừa cấp đã tồn
 *       tại (dữ liệu cũ lệch) thì dò tiếp cho tới khi tìm được mã chưa dùng.</li>
 *   <li>Khoá quy trình cấp mã cho tới khi transaction commit xong, nên hai Admin
 *       tạo tài khoản đồng thời không thể cùng nhận một mã (trong phạm vi 1 tiến trình).</li>
 *   <li>Bảng {@code students} / {@code lecturers} có ràng buộc UNIQUE nên mọi
 *       trường hợp còn lại vẫn bị tầng DB chặn.</li>
 * </ol>
 */
@Service
public class ProfileCodeService {

    private static final String STUDENT_PREFIX = "SV";
    private static final String LECTURER_PREFIX = "GV";
    private static final int STUDENT_INITIAL = 23001;
    private static final int LECTURER_INITIAL = 1;
    private static final int STUDENT_WIDTH = 5;
    private static final int LECTURER_WIDTH = 3;
    private static final int MAX_ATTEMPTS = 1000;

    /** Khoá quy trình cấp mã (đơn giản, hợp lý cho ứng dụng 1 tiến trình). */
    private final ReentrantLock codeLock = new ReentrantLock();

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;

    public ProfileCodeService(StudentRepository studentRepository,
                              LecturerRepository lecturerRepository) {
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
    }

    /**
     * Tạo hồ sơ sinh viên với mã do hệ thống cấp.
     *
     * <p>Phải được gọi trong một transaction đang hoạt động (thường là từ
     * {@code UserAccountService.create}) - khoá cấp mã chỉ được thả sau khi
     * transaction commit để tránh hai luồng tạo tài khoản cùng nhận một mã.
     */
    @Transactional
    public Student createStudentProfile(User user, String className, String major) {
        codeLock.lock();
        boolean released = false;
        try {
            String code = nextStudentCode();
            Student student = studentRepository.save(new Student(user, code, className, major));
            // Thả khoá khi transaction kết thúc (commit/rollback), không phải khi hàm return.
            released = releaseLockAfterTransaction();
            return student;
        } finally {
            if (!released) {
                codeLock.unlock();
            }
        }
    }

    /**
     * Tạo hồ sơ giảng viên với mã do hệ thống cấp. Xem {@link #createStudentProfile}.
     */
    @Transactional
    public Lecturer createLecturerProfile(User user, Department department) {
        codeLock.lock();
        boolean released = false;
        try {
            String code = nextLecturerCode();
            Lecturer lecturer = lecturerRepository.save(new Lecturer(user, code, department));
            released = releaseLockAfterTransaction();
            return lecturer;
        } finally {
            if (!released) {
                codeLock.unlock();
            }
        }
    }

    /**
     * Mã sinh viên dự kiến được cấp tiếp theo (dùng để hiển thị trước trên form Admin).
     */
    @Transactional(readOnly = true)
    public String nextStudentCode() {
        long next = startNumber(studentRepository.findMaxStudentCodeNumber(), STUDENT_INITIAL);
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = STUDENT_PREFIX + String.format("%0" + STUDENT_WIDTH + "d", next);
            if (!studentRepository.existsByStudentCode(code)) {
                return code;
            }
            next++;
        }
        throw new IllegalStateException("Không thể cấp mã sinh viên mới, vui lòng thử lại sau.");
    }

    /**
     * Mã giảng viên dự kiến được cấp tiếp theo (dùng để hiển thị trước trên form Admin).
     */
    @Transactional(readOnly = true)
    public String nextLecturerCode() {
        long next = startNumber(lecturerRepository.findMaxLecturerCodeNumber(), LECTURER_INITIAL);
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = LECTURER_PREFIX + String.format("%0" + LECTURER_WIDTH + "d", next);
            if (!lecturerRepository.existsByLecturerCode(code)) {
                return code;
            }
            next++;
        }
        throw new IllegalStateException("Không thể cấp mã giảng viên mới, vui lòng thử lại sau.");
    }

    /** Chưa có mã nào thì bắt đầu từ số khởi tạo; có rồi thì lấy số kế tiếp. */
    private static long startNumber(Long maxExisting, int initial) {
        if (maxExisting == null || maxExisting < initial) {
            return initial;
        }
        return maxExisting + 1;
    }

    /**
     * Hẹn thả khoá cấp mã tới khi transaction kết thúc.
     *
     * @return true nếu đã hẹn thả khoá (khi đó không cần unlock trong finally)
     */
    private boolean releaseLockAfterTransaction() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                codeLock.unlock();
            }
        });
        return true;
    }
}
