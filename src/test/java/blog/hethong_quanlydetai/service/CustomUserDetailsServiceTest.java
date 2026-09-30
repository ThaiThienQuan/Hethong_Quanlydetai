package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.Permission;
import blog.hethong_quanlydetai.entity.Role;
import blog.hethong_quanlydetai.repository.AppUserRepository;
import blog.hethong_quanlydetai.repository.PermissionRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void freshInstallAssignsOnlyWorkflowPermissionsToEachRole() {
        when(roleRepository.count()).thenReturn(0L);
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.count()).thenReturn(1L);

        new CustomUserDetailsService(userRepository, roleRepository, permissionRepository, passwordEncoder)
                .initializeDefaultUsers();

        ArgumentCaptor<Iterable<Role>> rolesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(roleRepository).saveAll(rolesCaptor.capture());
        Map<String, Role> roles = StreamSupport.stream(rolesCaptor.getValue().spliterator(), false)
                .collect(Collectors.toMap(Role::getName, role -> role));

        assertEquals(Set.of("TOPIC_VIEW", "PERIOD_MANAGE", "NOTICE_MANAGE", "NOTICE_VIEW"),
                permissionCodes(roles.get("TRUONG_KHOA")));
        assertEquals(Set.of("TOPIC_VIEW", "TOPIC_APPROVE", "ASSIGNMENT_MANAGE", "RESULT_CALCULATE",
                        "RESULT_PUBLISH", "COUNCIL_MANAGE", "NOTICE_MANAGE", "NOTICE_VIEW"),
                permissionCodes(roles.get("CHU_TICH_HOI_DONG")));
        assertEquals(Set.of("TOPIC_VIEW", "TOPIC_CREATE", "REGISTRATION_CONFIRM", "EVALUATION_CREATE",
                        "NOTICE_VIEW"), permissionCodes(roles.get("GIANG_VIEN")));
        assertEquals(Set.of("TOPIC_VIEW", "REGISTRATION_CREATE", "SUBMISSION_CREATE", "RESULT_VIEW",
                        "NOTICE_VIEW"), permissionCodes(roles.get("SINH_VIEN")));
        assertFalse(permissionCodes(roles.get("CHU_TICH_HOI_DONG")).contains("USER_MANAGE"));
        assertFalse(permissionCodes(roles.get("TRUONG_KHOA")).contains("TOPIC_APPROVE"));
    }

    private Set<String> permissionCodes(Role role) {
        return role.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }
}