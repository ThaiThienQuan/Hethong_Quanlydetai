package vn.edu.demo.topicmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.StudentRepository;
import vn.edu.demo.topicmanagement.service.ProfileCodeService;

/**
 * Unit test cho cơ chế tự cấp mã sinh viên / giảng viên (mục 5.1).
 *
 * <p>Mục tiêu: mã do hệ thống cấp phải KHÔNG BAO GIỜ trùng với mã đang có,
 * kể cả khi dữ liệu cũ lệch (thiếu số, có mã dạng lạ) hoặc có lỗ hổng số.
 */
class ProfileCodeServiceTest {

    private StudentRepository studentRepository;
    private LecturerRepository lecturerRepository;
    private ProfileCodeService service;

    @BeforeEach
    void setUp() {
        studentRepository = mock(StudentRepository.class);
        lecturerRepository = mock(LecturerRepository.class);
        service = new ProfileCodeService(studentRepository, lecturerRepository);
    }

    @Nested
    @DisplayName("Cấp mã sinh viên")
    class StudentCode {

        @Test
        @DisplayName("SUCCESS — chưa có sinh viên nào thì cấp mã khởi tạo")
        void firstCode() {
            when(studentRepository.findMaxStudentCodeNumber()).thenReturn(null);

            assertEquals("SV23001", service.nextStudentCode());
        }

        @Test
        @DisplayName("SUCCESS — cấp tiếp sau mã lớn nhất đang có")
        void nextAfterMax() {
            when(studentRepository.findMaxStudentCodeNumber()).thenReturn(23008L);
            when(studentRepository.existsByStudentCode("SV23009")).thenReturn(false);

            assertEquals("SV23009", service.nextStudentCode());
        }

        @Test
        @DisplayName("SUCCESS — lỗ hổng số vẫn cấp mã chưa dùng")
        void skipExistingCode() {
            when(studentRepository.findMaxStudentCodeNumber()).thenReturn(23001L);
            when(studentRepository.existsByStudentCode("SV23002")).thenReturn(true);
            when(studentRepository.existsByStudentCode("SV23003")).thenReturn(false);

            assertEquals("SV23003", service.nextStudentCode());
        }
    }

    @Nested
    @DisplayName("Cấp mã giảng viên")
    class LecturerCode {

        @Test
        @DisplayName("SUCCESS — chưa có giảng viên nào thì cấp mã khởi tạo")
        void firstCode() {
            when(lecturerRepository.findMaxLecturerCodeNumber()).thenReturn(null);

            assertEquals("GV001", service.nextLecturerCode());
        }

        @Test
        @DisplayName("SUCCESS — cấp tiếp sau mã lớn nhất đang có")
        void nextAfterMax() {
            when(lecturerRepository.findMaxLecturerCodeNumber()).thenReturn(8L);
            when(lecturerRepository.existsByLecturerCode("GV009")).thenReturn(false);

            assertEquals("GV009", service.nextLecturerCode());
        }

        @Test
        @DisplayName("SUCCESS — mã giảng viên và mã sinh viên độc lập, không đụng nhau")
        void independentFromStudentCode() {
            when(lecturerRepository.findMaxLecturerCodeNumber()).thenReturn(1L);
            when(lecturerRepository.existsByLecturerCode("GV002")).thenReturn(false);

            String code = service.nextLecturerCode();

            assertTrue(code.startsWith("GV"));
            assertEquals("GV002", code);
        }
    }
}
