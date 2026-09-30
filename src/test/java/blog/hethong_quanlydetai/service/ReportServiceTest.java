package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.AppUser;
import blog.hethong_quanlydetai.entity.GroupMember;
import blog.hethong_quanlydetai.entity.Report;
import blog.hethong_quanlydetai.entity.Student;
import blog.hethong_quanlydetai.repository.AppUserRepository;
import blog.hethong_quanlydetai.repository.GroupMemberRepository;
import blog.hethong_quanlydetai.repository.ReportRepository;
import blog.hethong_quanlydetai.repository.StudentRepository;
import blog.hethong_quanlydetai.repository.TopicRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TopicRegistrationRepository registrationRepository;
    @InjectMocks
    private ReportService reportService;

    @Test
    void saveUsesAuthenticatedUserAndRequiresGroupLeader() {
        AppUser user = new AppUser();
        user.setId(12L);
        Student student = new Student();
        student.setId(3L);
        student.setUserId(12L);
        GroupMember member = new GroupMember();
        member.setLeader(true);
        Report report = new Report();
        report.setGroupId(4L);
        report.setSubmittedBy(99L);
        report.setStatus("PUBLISHED");
        report.setVersion(99);
        report.setSubmittedAt(LocalDateTime.MIN);

        when(appUserRepository.findByUsername("student")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(12L)).thenReturn(Optional.of(student));
        when(groupMemberRepository.findByGroupIdAndStudentId(4L, 3L)).thenReturn(Optional.of(member));
        when(registrationRepository.existsByGroupIdAndStatusIn(4L, java.util.List.of("APPROVED")))
            .thenReturn(true);
        when(reportRepository.save(report)).thenReturn(report);

        reportService.save(report, "student");

        assertEquals(3L, report.getSubmittedBy());
        assertEquals("SUBMITTED", report.getStatus());
        assertEquals(1, report.getVersion());
        org.junit.jupiter.api.Assertions.assertNotEquals(LocalDateTime.MIN, report.getSubmittedAt());
        verify(reportRepository).save(report);
    }

    @Test
    void saveRejectsNonLeaderWithoutPersistingReport() {
        AppUser user = new AppUser();
        user.setId(12L);
        Student student = new Student();
        student.setId(3L);
        student.setUserId(12L);
        GroupMember member = new GroupMember();
        member.setLeader(false);
        Report report = new Report();
        report.setGroupId(4L);

        when(appUserRepository.findByUsername("student")).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(12L)).thenReturn(Optional.of(student));
        when(groupMemberRepository.findByGroupIdAndStudentId(4L, 3L)).thenReturn(Optional.of(member));

        assertThrows(IllegalArgumentException.class, () -> reportService.save(report, "student"));

        verify(reportRepository, never()).save(any());
    }
}