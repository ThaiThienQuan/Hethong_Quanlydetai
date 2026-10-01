package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.AppUser;
import blog.hethong_quanlydetai.entity.Role;
import blog.hethong_quanlydetai.entity.UserRole;
import blog.hethong_quanlydetai.repository.AppUserRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void changePasswordVerifiesCurrentPasswordAndStoresEncodedPassword() {
        AppUser user = new AppUser();
        user.setUsername("lecturer");
        user.setPassword("{bcrypt}old-hash");
        when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "{bcrypt}old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("{bcrypt}new-hash");
        when(userRepository.save(user)).thenReturn(user);

        userService.changePassword("lecturer", "old-password", "new-password");

        assertEquals("{bcrypt}new-hash", user.getPassword());
        verify(passwordEncoder).encode("new-password");
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRejectsAnIncorrectCurrentPassword() {
        AppUser user = new AppUser();
        user.setUsername("lecturer");
        user.setPassword("{bcrypt}old-hash");
        when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "{bcrypt}old-hash")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword("lecturer", "wrong", "new-password"));

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void saveUserAssignsEverySelectedRole() {
        AppUser user = new AppUser();
        user.setUsername("student");
        user.setPassword("plain-password");
        user.setFullName("Student User");
        user.setEmail("student@example.edu");
        user.setRole(UserRole.SINH_VIEN);
        Role student = new Role();
        student.setId(1L);
        student.setName("SINH_VIEN");
        Role lecturer = new Role();
        lecturer.setId(2L);
        lecturer.setName("GIANG_VIEN");
        when(userRepository.findByUsername("student")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("{bcrypt}encoded");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(student));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(lecturer));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser saved = userService.saveUser(user, Set.of(1L, 2L));

        assertEquals(Set.of(student, lecturer), saved.getRoles());
        assertEquals("{bcrypt}encoded", saved.getPassword());
        assertEquals(UserRole.SINH_VIEN, saved.getRole());
    }

    @Test
    void saveUserRejectsPrimaryRoleNotIncludedInAssignedGroups() {
        AppUser user = new AppUser();
        user.setId(10L);
        user.setUsername("lecturer");
        user.setEmail("lecturer@example.edu");
        user.setRole(UserRole.GIANG_VIEN);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("lecturer")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("lecturer@example.edu")).thenReturn(Optional.of(user));
        Role student = new Role();
        student.setId(1L);
        student.setName("SINH_VIEN");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThrows(IllegalArgumentException.class, () -> userService.saveUser(user, Set.of(1L)));

        verify(userRepository, never()).save(any());
    }
}