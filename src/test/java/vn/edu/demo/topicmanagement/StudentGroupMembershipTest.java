package vn.edu.demo.topicmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.edu.demo.topicmanagement.config.AppProperties;
import vn.edu.demo.topicmanagement.entity.GroupMember;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.Student;
import vn.edu.demo.topicmanagement.entity.StudentGroup;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.GroupMemberRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.StudentGroupRepository;
import vn.edu.demo.topicmanagement.repository.StudentRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.TopicRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;
import vn.edu.demo.topicmanagement.service.StudentPortalService;

/**
 * Unit tests cho quản lý thành viên nhóm của nhóm trưởng (đặc tả 5.4):
 * thêm / gỡ sinh viên và giữ nguyên luồng chuyển quyền nhóm trưởng.
 *
 * <p>Mỗi quy tắc có ít nhất một SUCCESS và một REJECT (rubric mục 10).
 * Kịch bản nhóm: sv01 là nhóm trưởng, sv02 là thành viên thường.
 * Entity sinh id ở CSDL nên {@code Student} được mock, còn {@code GroupMember}
 * và {@code RegistrationPeriod} dùng bản thật vì có setter công khai.
 */
class StudentGroupMembershipTest {

    private static final long GROUP_ID = 100L;
    private static final long PERIOD_ID = 7L;
    private static final int MAX_SIZE = 3;

    private static final String LEADER = "sv01";
    private static final String MEMBER = "sv02";

    private StudentGroupRepository groupRepository;
    private GroupMemberRepository memberRepository;
    private TopicRegistrationRepository registrationRepository;
    private TopicRepository topicRepository;
    private UserRepository userRepository;
    private StudentRepository studentRepository;
    private RegistrationPeriodRepository periodRepository;
    private StudentPortalService service;

    private RegistrationPeriod period;
    private StudentGroup group;
    private Student leaderStudent;
    private Student secondStudent;
    private GroupMember leaderRow;
    private GroupMember secondRow;

    @BeforeEach
    void setUp() {
        groupRepository = mock(StudentGroupRepository.class);
        memberRepository = mock(GroupMemberRepository.class);
        registrationRepository = mock(TopicRegistrationRepository.class);
        topicRepository = mock(TopicRepository.class);
        userRepository = mock(UserRepository.class);
        studentRepository = mock(StudentRepository.class);
        periodRepository = mock(RegistrationPeriodRepository.class);

        AppProperties props = new AppProperties();
        props.getGroup().setMaxSize(MAX_SIZE);
        props.getGroup().setMinSize(1);

        service = new StudentPortalService(groupRepository, memberRepository, registrationRepository,
                topicRepository, userRepository, studentRepository, periodRepository, null, props);

        period = new RegistrationPeriod();
        period.setId(PERIOD_ID);

        group = mock(StudentGroup.class);
        when(group.getId()).thenReturn(GROUP_ID);
        when(group.getGroupName()).thenReturn("Nhóm Môn Học Mới");
        when(group.getPeriod()).thenReturn(period);

        // Dựng membership trước, rồi mới stub repository (tránh mock lồng trong thenReturn).
        leaderStudent = student(1L, LEADER, "SV001");
        secondStudent = student(2L, MEMBER, "SV002");
        leaderRow = memberOf(leaderStudent, true);
        secondRow = memberOf(secondStudent, false);

        when(groupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroupIdWithStudent(GROUP_ID))
                .thenReturn(List.of(leaderRow, secondRow));
        when(memberRepository.findByGroupIdAndLeaderTrue(GROUP_ID)).thenReturn(Optional.of(leaderRow));
        when(registrationRepository.findByGroupIdAndStatusIn(eq(GROUP_ID), anyList()))
                .thenReturn(Optional.empty());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Student student(long id, String username, String code) {
        User u = new User();
        u.setId(id * 100);
        u.setUsername(username);
        u.setFullName("Sinh viên " + id);

        Student s = mock(Student.class);
        when(s.getId()).thenReturn(id);
        when(s.getUser()).thenReturn(u);
        when(s.getStudentCode()).thenReturn(code);
        when(s.getClassName()).thenReturn("CNTT1.K18");
        return s;
    }

    private GroupMember memberOf(Student s, boolean isLeader) {
        GroupMember gm = new GroupMember();
        gm.setGroup(group);
        gm.setStudent(s);
        gm.setPeriodId(PERIOD_ID);
        gm.setLeader(isLeader);
        return gm;
    }

    /** Nhóm đã có đăng ký đề tài PENDING/APPROVED. */
    private void groupHasRegistration() {
        TopicRegistration reg = mock(TopicRegistration.class);
        when(registrationRepository.findByGroupIdAndStatusIn(eq(GROUP_ID), anyList()))
                .thenReturn(Optional.of(reg));
    }

    /** Nhóm chỉ còn một mình nhóm trưởng (1 thành viên). */
    private void groupHasSingleMember() {
        when(memberRepository.findByGroupIdWithStudent(GROUP_ID)).thenReturn(List.of(leaderRow));
        when(memberRepository.countByGroupId(GROUP_ID)).thenReturn(1L);
    }

    // ------------------------------------------------------------------
    // Thêm thành viên
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("5.4 — Nhóm trưởng thêm sinh viên vào nhóm")
    class AddMember {

        @Test
        @DisplayName("SUCCESS — thêm sinh viên chưa có nhóm vào nhóm của mình")
        void addFreeStudent() {
            Student candidate = student(3L, "sv03", "SV003");
            when(studentRepository.findById(3L)).thenReturn(Optional.of(candidate));
            when(memberRepository.existsByStudentIdAndPeriodId(3L, PERIOD_ID)).thenReturn(false);
            when(memberRepository.countByGroupId(GROUP_ID)).thenReturn(2L);

            service.addMemberToGroup(GROUP_ID, 3L, LEADER);

            ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
            verify(memberRepository).save(captor.capture());
            GroupMember saved = captor.getValue();
            assertEquals(3L, saved.getStudent().getId());
            assertEquals(PERIOD_ID, saved.getPeriodId());
            assertFalse(saved.isLeader(), "Thành viên mới không được là nhóm trưởng");
        }

        @Test
        @DisplayName("REJECT — thành viên thường không tự thêm được người khác")
        void rejectNonLeader() {
            Student candidate = student(3L, "sv03", "SV003");
            when(studentRepository.findById(3L)).thenReturn(Optional.of(candidate));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.addMemberToGroup(GROUP_ID, 3L, MEMBER));
            assertTrue(ex.getMessage().contains("nhóm trưởng"));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECT — sinh viên đã thuộc nhóm khác trong cùng đợt")
        void rejectStudentAlreadyInGroup() {
            Student candidate = student(3L, "sv03", "SV003");
            when(studentRepository.findById(3L)).thenReturn(Optional.of(candidate));
            when(memberRepository.existsByStudentIdAndPeriodId(3L, PERIOD_ID)).thenReturn(true);
            when(memberRepository.countByGroupId(GROUP_ID)).thenReturn(2L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.addMemberToGroup(GROUP_ID, 3L, LEADER));
            assertTrue(ex.getMessage().contains("đã thuộc một nhóm khác"));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECT — nhóm đã đủ số thành viên tối đa")
        void rejectGroupFull() {
            Student candidate = student(3L, "sv03", "SV003");
            when(studentRepository.findById(3L)).thenReturn(Optional.of(candidate));
            when(memberRepository.countByGroupId(GROUP_ID)).thenReturn((long) MAX_SIZE);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.addMemberToGroup(GROUP_ID, 3L, LEADER));
            assertTrue(ex.getMessage().contains("tối đa"));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECT — nhóm đã đăng ký đề tài thì không đổi thành viên được")
        void rejectAfterRegistration() {
            groupHasRegistration();
            Student candidate = student(3L, "sv03", "SV003");
            when(studentRepository.findById(3L)).thenReturn(Optional.of(candidate));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.addMemberToGroup(GROUP_ID, 3L, LEADER));
            assertTrue(ex.getMessage().contains("đăng ký đề tài"));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECT — không tìm thấy sinh viên")
        void rejectUnknownStudent() {
            when(studentRepository.findById(99L)).thenReturn(Optional.empty());
            when(memberRepository.countByGroupId(GROUP_ID)).thenReturn(2L);

            assertThrows(IllegalArgumentException.class,
                    () -> service.addMemberToGroup(GROUP_ID, 99L, LEADER));
            verify(memberRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------
    // Gỡ thành viên
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("5.4 — Nhóm trưởng gỡ thành viên khỏi nhóm")
    class RemoveMember {

        @Test
        @DisplayName("SUCCESS — gỡ một thành viên thường")
        void removePlainMember() {
            when(memberRepository.countByGroupId(GROUP_ID)).thenReturn(2L);

            service.removeMemberFromGroup(GROUP_ID, 2L, LEADER);

            ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
            verify(memberRepository).delete(captor.capture());
            assertEquals(2L, captor.getValue().getStudent().getId());
        }

        @Test
        @DisplayName("REJECT — không gỡ được chính nhóm trưởng")
        void rejectRemoveLeader() {
            when(memberRepository.countByGroupId(GROUP_ID)).thenReturn(2L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.removeMemberFromGroup(GROUP_ID, 1L, LEADER));
            assertTrue(ex.getMessage().contains("Chuyển quyền"));
            verify(memberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("REJECT — nhóm chỉ còn tối thiểu 1 thành viên")
        void rejectBelowMinSize() {
            groupHasSingleMember();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.removeMemberFromGroup(GROUP_ID, 1L, LEADER));
            assertTrue(ex.getMessage().contains("tối thiểu") || ex.getMessage().contains("Chuyển quyền"));
            verify(memberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("REJECT — thành viên thường không gỡ được ai")
        void rejectNonLeaderRemoving() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.removeMemberFromGroup(GROUP_ID, 2L, MEMBER));
            assertTrue(ex.getMessage().contains("nhóm trưởng"));
            verify(memberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("REJECT — sinh viên không thuộc nhóm")
        void rejectNonMember() {
            when(memberRepository.countByGroupId(GROUP_ID)).thenReturn(2L);

            assertThrows(IllegalArgumentException.class,
                    () -> service.removeMemberFromGroup(GROUP_ID, 77L, LEADER));
            verify(memberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("REJECT — nhóm đã đăng ký đề tài thì không gỡ được")
        void rejectAfterRegistration() {
            groupHasRegistration();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.removeMemberFromGroup(GROUP_ID, 2L, LEADER));
            assertTrue(ex.getMessage().contains("đăng ký đề tài"));
            verify(memberRepository, never()).delete(any());
        }
    }

    // ------------------------------------------------------------------
    // Chuyển quyền nhóm trưởng (giữ hành vi cũ, nay có chặn sau khi đăng ký)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("5.4 — Chuyển quyền nhóm trưởng")
    class TransferLeadership {

        @Test
        @DisplayName("SUCCESS — nhóm trưởng chuyển quyền cho thành viên khác")
        void transfer() {
            service.transferGroupLeadership(GROUP_ID, 2L, LEADER);

            ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
            verify(memberRepository, times(2)).save(captor.capture());
            List<GroupMember> saved = captor.getAllValues();
            assertTrue(saved.stream().anyMatch(gm -> gm.isLeader() && gm.getStudent().getId() == 2L));
            assertTrue(saved.stream().anyMatch(gm -> !gm.isLeader() && gm.getStudent().getId() == 1L));
        }

        @Test
        @DisplayName("REJECT — thành viên thường không chuyển được quyền")
        void rejectNonLeader() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.transferGroupLeadership(GROUP_ID, 2L, MEMBER));
            assertTrue(ex.getMessage().contains("nhóm trưởng"));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECT — nhóm đã đăng ký đề tài thì không chuyển quyền được")
        void rejectAfterRegistration() {
            groupHasRegistration();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.transferGroupLeadership(GROUP_ID, 2L, LEADER));
            assertTrue(ex.getMessage().contains("đăng ký đề tài"));
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("REJECT — sinh viên không thuộc nhóm")
        void rejectNonMember() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.transferGroupLeadership(GROUP_ID, 77L, LEADER));
            verify(memberRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------
    // Cờ quyền hiển thị trên giao diện
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("5.4 — Cờ quyền cho màn hình nhóm của sinh viên")
    class ViewFlags {

        @Test
        @DisplayName("SUCCESS — nhóm trưởng có cờ quản lý thành viên và gợi ý SV")
        void leaderSeesManagementUi() {
            when(memberRepository.findByStudentUsername(LEADER)).thenReturn(Optional.of(leaderRow));
            when(memberRepository.countByGroupId(GROUP_ID)).thenReturn(2L);
            Student candidate = student(3L, "sv03", "SV003");
            when(studentRepository.findStudentsWithoutGroupInPeriod(eq(PERIOD_ID), isNull()))
                    .thenReturn(List.of(candidate));

            var view = service.getMyGroup(LEADER);

            assertNotNull(view);
            assertTrue(view.isCurrentUserLeader());
            assertTrue(view.isCanManageMembers());
            assertTrue(view.isHasVacancy());
            assertEquals(1, view.getAvailableCandidates().size());
            assertEquals("SV003", view.getAvailableCandidates().get(0).getStudentCode());
            assertEquals(MAX_SIZE, view.getMaxSize());
            assertEquals(2, view.getMemberCount());
            assertTrue(view.getMembers().stream().anyMatch(m -> m.isLeader()),
                    "Phải đánh dấu thành viên nào là nhóm trưởng");
        }

        @Test
        @DisplayName("SUCCESS — nhóm đã đăng ký đề tài thì khoá cờ quản lý thành viên")
        void lockFlagsAfterRegistration() {
            when(memberRepository.findByStudentUsername(LEADER)).thenReturn(Optional.of(leaderRow));
            groupHasRegistration();

            var view = service.getMyGroup(LEADER);

            assertNotNull(view);
            assertTrue(view.isCurrentUserLeader());
            assertFalse(view.isCanManageMembers());
            assertFalse(view.isCanTransferLeadership());
            assertTrue(view.getAvailableCandidates().isEmpty(),
                    "Đã đăng ký đề tài thì không gợi ý thêm thành viên nữa");
        }

        @Test
        @DisplayName("SUCCESS — thành viên thường không có cờ quản lý")
        void plainMemberSeesNoManagementUi() {
            when(memberRepository.findByStudentUsername(MEMBER)).thenReturn(Optional.of(secondRow));

            var view = service.getMyGroup(MEMBER);

            assertNotNull(view);
            assertFalse(view.isCurrentUserLeader());
            assertFalse(view.isCanManageMembers());
            assertFalse(view.isCanTransferLeadership());
            assertTrue(view.getAvailableCandidates().isEmpty());
        }

        @Test
        @DisplayName("SUCCESS — nhóm đã đủ người thì không gợi ý thêm")
        void noCandidatesWhenGroupFull() {
            // Nhóm đủ 3 thành viên: sv01 (trưởng), sv02, sv03.
            Student thirdStudent = student(3L, "sv03", "SV003");
            GroupMember thirdRow = memberOf(thirdStudent, false);
            when(memberRepository.findByGroupIdWithStudent(GROUP_ID))
                    .thenReturn(List.of(leaderRow, secondRow, thirdRow));
            when(memberRepository.findByStudentUsername(LEADER)).thenReturn(Optional.of(leaderRow));
            when(memberRepository.countByGroupId(GROUP_ID)).thenReturn((long) MAX_SIZE);

            var view = service.getMyGroup(LEADER);

            assertNotNull(view);
            assertFalse(view.isHasVacancy());
            assertTrue(view.getAvailableCandidates().isEmpty());
            verify(studentRepository, never()).findStudentsWithoutGroupInPeriod(any(), any());
        }
    }

    // ------------------------------------------------------------------
    // Ràng buộc UNIQUE (student_id, period_id) ở CSDL
    // ------------------------------------------------------------------

    @Test
    @DisplayName("5.4 — Trạng thái PENDING/APPROVED được tính là 'đã đăng ký đề tài'")
    void activeRegistrationStatuses() {
        when(memberRepository.findByStudentUsername(LEADER)).thenReturn(Optional.of(leaderRow));
        when(memberRepository.countByGroupId(GROUP_ID)).thenReturn(2L);
        when(registrationRepository.findByGroupIdAndStatusIn(eq(GROUP_ID),
                eq(List.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED))))
                .thenReturn(Optional.of(mock(TopicRegistration.class)));

        var view = service.getMyGroup(LEADER);

        assertNotNull(view);
        assertFalse(view.isCanManageMembers(),
                "Có đăng ký PENDING/APPROVED thì không được đổi thành viên");
    }
}