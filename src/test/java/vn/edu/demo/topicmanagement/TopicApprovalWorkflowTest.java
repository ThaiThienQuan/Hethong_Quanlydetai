package vn.edu.demo.topicmanagement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.demo.topicmanagement.dto.HeadSupervisorForm;
import vn.edu.demo.topicmanagement.entity.Lecturer;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicStatus;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.DepartmentRepository;
import vn.edu.demo.topicmanagement.repository.LecturerRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.ReviewAssignmentRepository;
import vn.edu.demo.topicmanagement.repository.StudentGroupRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.TopicRepository;
import vn.edu.demo.topicmanagement.repository.TopicSupervisorRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;
import vn.edu.demo.topicmanagement.service.AuditLogService;
import vn.edu.demo.topicmanagement.service.TopicService;

/**
 * Unit test cho mục 5.3 - Trưởng khoa duyệt / từ chối / công bố / phân công GVHD.
 *
 * <p>Trọng tâm: mỗi hành động chỉ được thực hiện đúng một lần và đúng trạng thái.
 * Người dùng báo: "ấn công bố rồi nhưng vẫn ấn công bố được" và
 * "từ chối rồi vẫn Phân công GVHD được" → các guard state-machine phải chặn.
 */
class TopicApprovalWorkflowTest {

    private TopicRepository topicRepository;
    private TopicSupervisorRepository supervisorRepository;
    private UserRepository userRepository;
    private LecturerRepository lecturerRepository;
    private ReviewAssignmentRepository assignmentRepository;
    private AuditLogService auditLogService;
    private TopicService service;

    /** Trạng thái published thật của topic giả lập (vì Topic là mock). */
    private boolean publishedFlag;

    @BeforeEach
    void setUp() {
        topicRepository = mock(TopicRepository.class);
        supervisorRepository = mock(TopicSupervisorRepository.class);
        userRepository = mock(UserRepository.class);
        lecturerRepository = mock(LecturerRepository.class);
        assignmentRepository = mock(ReviewAssignmentRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new TopicService(topicRepository, supervisorRepository, userRepository,
                lecturerRepository, mock(DepartmentRepository.class),
                mock(RegistrationPeriodRepository.class),
                mock(TopicRegistrationRepository.class),
                mock(StudentGroupRepository.class),
                assignmentRepository, auditLogService);
        publishedFlag = false;
    }

    private Topic topic(Long id, TopicStatus status, boolean published) {
        publishedFlag = published;
        Topic t = mock(Topic.class);
        when(t.getId()).thenReturn(id);
        when(t.getStatus()).thenReturn(status);
        when(t.getTopicCode()).thenReturn("DA-MH-26-00" + id);
        when(t.getTopicName()).thenReturn("Đề tài " + id);
        when(t.isPublished()).thenAnswer(inv -> publishedFlag);
        doAnswer(inv -> {
            publishedFlag = inv.getArgument(0);
            return null;
        }).when(t).setPublished(anyBoolean());
        RegistrationPeriod p = mock(RegistrationPeriod.class);
        when(p.getId()).thenReturn(1L);
        when(p.getName()).thenReturn("Đợt demo");
        when(t.getPeriod()).thenReturn(p);
        return t;
    }

    private Lecturer lecturer(Long id) {
        Lecturer l = mock(Lecturer.class);
        // User.getFullName() là final → không stub được trên mock, dùng instance thật.
        User u = new User();
        u.setId(id);
        u.setFullName("GV " + id);
        when(l.getId()).thenReturn(id);
        when(l.getUser()).thenReturn(u);
        return l;
    }

    @Nested
    @DisplayName("5.3 - Công bố đề tài")
    class Publish {

        @Test
        @DisplayName("Công bố đề tài APPROVED chưa công bố → thành công, cờ published = true")
        void publishApprovedTopic() {
            Topic t = topic(1L, TopicStatus.APPROVED, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            service.publish(1L, "truongkhoa");

            assertTrue(publishedFlag);
            verify(topicRepository).save(t);
        }

        @Test
        @DisplayName("Công bố đề tài ĐÃ công bố → bị chặn, không ghi lại log")
        void publishTwiceIsRejected() {
            Topic t = topic(1L, TopicStatus.APPROVED, true);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.publish(1L, "truongkhoa"));

            assertTrue(ex.getMessage().contains("đã được công bố"));
            verify(topicRepository, never()).save(any());
            verify(auditLogService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Công bố đề tài chưa duyệt (PENDING) → bị chặn")
        void publishPendingTopicIsRejected() {
            Topic t = topic(1L, TopicStatus.PENDING, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            assertThrows(IllegalStateException.class, () -> service.publish(1L, "truongkhoa"));

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Công bố đề tài bị từ chối → bị chặn")
        void publishRejectedTopicIsRejected() {
            Topic t = topic(1L, TopicStatus.REJECTED, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            assertThrows(IllegalStateException.class, () -> service.publish(1L, "truongkhoa"));

            verify(topicRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("5.3 - Từ chối đề tài")
    class Reject {

        @Test
        @DisplayName("Từ chối đề tài PENDING → thành công, cờ published bị tắt")
        void rejectPendingTopic() {
            Topic t = topic(1L, TopicStatus.PENDING, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            service.reject(1L, "Mã đề tài sai", "truongkhoa");

            verify(t).setStatus(TopicStatus.REJECTED);
            verify(t).setRejectReason("Mã đề tài sai");
            verify(t).setPublished(false);
        }

        @Test
        @DisplayName("Từ chối đề tài đã duyệt → bị chặn")
        void rejectApprovedTopicIsRejected() {
            Topic t = topic(1L, TopicStatus.APPROVED, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            assertThrows(IllegalStateException.class,
                    () -> service.reject(1L, "lý do", "truongkhoa"));

            verify(topicRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("5.3 - Trưởng khoa phân công GVHD")
    class AssignSupervisors {

        private HeadSupervisorForm form() {
            HeadSupervisorForm f = new HeadSupervisorForm();
            f.setPrimarySupervisorId(10L);
            return f;
        }

        @Test
        @DisplayName("Phân công GVHD cho đề tài PENDING → thành công")
        void assignForPendingTopic() {
            Topic t = topic(1L, TopicStatus.PENDING, false);
            // Tạo lecturer trước: không được gọi stubbing lồng trong when(...).
            Lecturer primary = lecturer(10L);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));
            when(lecturerRepository.findById(10L)).thenReturn(Optional.of(primary));
            when(assignmentRepository.countByTopicId(1L)).thenReturn(0L);
            when(assignmentRepository.findByTopicId(1L)).thenReturn(List.of());

            assertDoesNotThrow(() -> service.updateSupervisorsByHead(1L, form(), "truongkhoa"));
        }

        @Test
        @DisplayName("Phân công GVHD cho đề tài BỊ TỪ CHỐI → bị chặn (lỗi người dùng gặp)")
        void assignForRejectedTopicIsRejected() {
            Topic t = topic(1L, TopicStatus.REJECTED, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.updateSupervisorsByHead(1L, form(), "truongkhoa"));

            assertTrue(ex.getMessage().contains("bị từ chối"));
            verify(supervisorRepository, never()).save(any());
            verify(auditLogService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Phân công GVHD cho đề tài ĐÃ CÔNG BỐ → bị chặn")
        void assignForPublishedTopicIsRejected() {
            Topic t = topic(1L, TopicStatus.APPROVED, true);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.updateSupervisorsByHead(1L, form(), "truongkhoa"));

            assertTrue(ex.getMessage().contains("đã công bố"));
            verify(supervisorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Mở form phân công GVHD cho đề tài bị từ chối → bị chặn")
        void openFormForRejectedTopicIsRejected() {
            Topic t = topic(1L, TopicStatus.REJECTED, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            assertThrows(IllegalStateException.class, () -> service.getHeadSupervisorInfo(1L));
        }
    }

    @Nested
    @DisplayName("5.3 - Duyệt đề tài")
    class Approve {

        @Test
        @DisplayName("Duyệt đề tài PENDING có GVHD → thành công")
        void approvePendingTopicWithSupervisor() {
            Topic t = topic(1L, TopicStatus.PENDING, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));
            when(supervisorRepository.countByTopicId(1L)).thenReturn(1L);
            when(userRepository.findByUsername("truongkhoa")).thenReturn(Optional.empty());

            service.approve(1L, "truongkhoa");

            verify(t).setStatus(TopicStatus.APPROVED);
        }

        @Test
        @DisplayName("Duyệt đề tài chưa có GVHD → bị chặn")
        void approveWithoutSupervisorIsRejected() {
            Topic t = topic(1L, TopicStatus.PENDING, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));
            when(supervisorRepository.countByTopicId(1L)).thenReturn(0L);

            assertThrows(IllegalStateException.class, () -> service.approve(1L, "truongkhoa"));

            verify(topicRepository, never()).save(any());
        }

        @Test
        @DisplayName("Duyệt đề tài đã từ chối → bị chặn")
        void approveRejectedTopicIsRejected() {
            Topic t = topic(1L, TopicStatus.REJECTED, false);
            when(topicRepository.findById(1L)).thenReturn(Optional.of(t));

            assertThrows(IllegalStateException.class, () -> service.approve(1L, "truongkhoa"));

            verify(topicRepository, never()).save(any());
        }
    }
}
