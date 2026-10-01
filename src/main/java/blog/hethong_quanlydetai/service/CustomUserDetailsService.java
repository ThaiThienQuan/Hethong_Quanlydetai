package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.AppUser;
import blog.hethong_quanlydetai.entity.Permission;
import blog.hethong_quanlydetai.entity.Role;
import blog.hethong_quanlydetai.entity.UserRole;
import blog.hethong_quanlydetai.repository.AppUserRepository;
import blog.hethong_quanlydetai.repository.PermissionRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.seed-demo-users:false}")
    private boolean seedDemoUsers;

    @Value("${app.security.bootstrap-admin-username:}")
    private String bootstrapAdminUsername;

    @Value("${app.security.bootstrap-admin-password:}")
    private String bootstrapAdminPassword;

    @Autowired
    public CustomUserDetailsService(AppUserRepository userRepository, RoleRepository roleRepository,
                                    PermissionRepository permissionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản không tồn tại: " + username));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || !user.isEnabled()) {
            throw new UsernameNotFoundException("Tài khoản đang bị khóa hoặc vô hiệu hóa.");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }

        if (user.getRoles().isEmpty() && user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }

        return new User(
                user.getUsername(),
                user.getPassword(),
                true,
                true,
                true,
                true,
                authorities
        );
    }

    @PostConstruct
    public void initializeDefaultUsers() {
        boolean freshInstall = roleRepository.count() == 0;
        boolean permissionMigration = freshInstall
            || permissionRepository.findByCode("ROLE_MANAGE").isEmpty()
            || permissionRepository.findByCode("PERMISSION_MANAGE").isEmpty()
            || permissionRepository.findByCode("PERIOD_MANAGE").isEmpty()
            || permissionRepository.findByCode("COUNCIL_MANAGE").isEmpty()
            || permissionRepository.findByCode("RESULT_VIEW").isEmpty();
        Role adminRole = createRoleIfMissing("ADMIN", "Quản trị hệ thống");
        Role chuTichHoiDongRole = createRoleIfMissing("CHU_TICH_HOI_DONG", "Chủ tịch hội đồng");
        Role truongKhoaRole = createRoleIfMissing("TRUONG_KHOA", "Trưởng khoa");
        Role giangVienRole = createRoleIfMissing("GIANG_VIEN", "Giảng viên");
        Role sinhVienRole = createRoleIfMissing("SINH_VIEN", "Sinh viên");

        for (AppUser user : userRepository.findAll()) {
            if (user.getRoles().isEmpty() && user.getRole() != null) {
                roleRepository.findByName(user.getRole().name()).ifPresent(role -> {
                    user.getRoles().add(role);
                    userRepository.save(user);
                });
            }
        }

        Permission userManage = createPermissionIfMissing("USER_MANAGE", "Quản lý tài khoản");
        Permission roleManage = createPermissionIfMissing("ROLE_MANAGE", "Quản lý nhóm người dùng");
        Permission permissionManage = createPermissionIfMissing("PERMISSION_MANAGE", "Quản lý phân quyền");
        Permission topicView = createPermissionIfMissing("TOPIC_VIEW", "Xem đề tài");
        Permission topicCreate = createPermissionIfMissing("TOPIC_CREATE", "Tạo đề tài");
        Permission topicApprove = createPermissionIfMissing("TOPIC_APPROVE", "Duyệt đề tài");
        Permission registrationCreate = createPermissionIfMissing("REGISTRATION_CREATE", "Đăng ký đề tài");
        Permission registrationConfirm = createPermissionIfMissing("REGISTRATION_CONFIRM", "Xác nhận đăng ký");
        Permission submissionCreate = createPermissionIfMissing("SUBMISSION_CREATE", "Nộp bài");
        Permission assignmentManage = createPermissionIfMissing("ASSIGNMENT_MANAGE", "Phân công chấm");
        Permission evaluationCreate = createPermissionIfMissing("EVALUATION_CREATE", "Nhập điểm");
        Permission resultCalculate = createPermissionIfMissing("RESULT_CALCULATE", "Tính điểm trung bình");
        Permission resultPublish = createPermissionIfMissing("RESULT_PUBLISH", "Công bố kết quả");
        Permission resultView = createPermissionIfMissing("RESULT_VIEW", "Xem kết quả đã công bố");
        Permission periodManage = createPermissionIfMissing("PERIOD_MANAGE", "Quản lý đợt đăng ký");
        Permission councilManage = createPermissionIfMissing("COUNCIL_MANAGE", "Quản lý hội đồng");
        Permission noticeManage = createPermissionIfMissing("NOTICE_MANAGE", "Quản lý thông báo");
        Permission noticeView = createPermissionIfMissing("NOTICE_VIEW", "Xem thông báo");

        if (permissionMigration) {
            for (AppUser user : userRepository.findAll()) {
            if (user.getRole() == UserRole.TRUONG_KHOA
                && user.getRoles().remove(chuTichHoiDongRole)) {
                userRepository.save(user);
            }
            }
            replacePermissions(adminRole, userManage, roleManage, permissionManage, topicView, topicCreate,
                topicApprove, registrationCreate, registrationConfirm, submissionCreate, assignmentManage,
                evaluationCreate, resultCalculate, resultPublish, resultView, periodManage, councilManage,
                noticeManage, noticeView);
            replacePermissions(truongKhoaRole, topicView, periodManage, noticeManage, noticeView);
            replacePermissions(chuTichHoiDongRole, topicView, topicApprove, assignmentManage, resultCalculate,
                resultPublish, councilManage, noticeManage, noticeView);
            replacePermissions(giangVienRole, topicView, topicCreate, registrationConfirm, evaluationCreate,
                noticeView);
            replacePermissions(sinhVienRole, topicView, registrationCreate, submissionCreate, resultView,
                noticeView);
        }
        roleRepository.saveAll(Set.of(adminRole, chuTichHoiDongRole, truongKhoaRole, giangVienRole, sinhVienRole));

        if (userRepository.count() == 0) {
            if (seedDemoUsers) {
                createUserIfMissing("admin", "admin123", "Quản trị viên", "admin@khoa.edu.vn", UserRole.ADMIN, adminRole);
                createUserIfMissing("chutich", "chutich123", "Chủ tịch hội đồng", "chutich@khoa.edu.vn", UserRole.CHU_TICH_HOI_DONG, chuTichHoiDongRole);
                createUserIfMissing("truongkhoa", "truongkhoa123", "Trưởng khoa", "truongkhoa@khoa.edu.vn", UserRole.TRUONG_KHOA, truongKhoaRole);
                createUserIfMissing("giangvien", "giangvien123", "Giảng viên", "giangvien@khoa.edu.vn", UserRole.GIANG_VIEN, giangVienRole);
                createUserIfMissing("sinhvien", "sinhvien123", "Sinh viên", "sinhvien@khoa.edu.vn", UserRole.SINH_VIEN, sinhVienRole);
            } else if (bootstrapAdminUsername != null && !bootstrapAdminUsername.isBlank()
                    && bootstrapAdminPassword != null && !bootstrapAdminPassword.isBlank()) {
                createUserIfMissing(bootstrapAdminUsername, bootstrapAdminPassword, "Quản trị viên",
                        "admin@khoa.edu.vn", UserRole.ADMIN, adminRole);
            }
        }
    }

    private Role createRoleIfMissing(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }

    private Permission createPermissionIfMissing(String code, String name) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setCode(code);
                    permission.setName(name);
                    return permissionRepository.save(permission);
                });
    }

    private void replacePermissions(Role role, Permission... permissions) {
        role.getPermissions().clear();
        role.getPermissions().addAll(List.of(permissions));
    }

    private void createUserIfMissing(String username, String rawPassword, String fullName, String email, UserRole role, Role assignedRole) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setEnabled(true);
        user.getRoles().add(assignedRole);
        userRepository.save(user);
    }
}
