package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.AppUser;
import blog.hethong_quanlydetai.entity.Role;
import blog.hethong_quanlydetai.entity.UserRole;
import blog.hethong_quanlydetai.repository.AppUserRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.HashSet;

@Service
public class UserService {
    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> findAllUsers() {
        return userRepository.findAllByOrderByFullNameAsc();
    }

    public Optional<AppUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<AppUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public AppUser updateProfile(String username, String fullName, String email, String phone) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        userRepository.findByEmail(email).filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> { throw new IllegalArgumentException("Email đã được sử dụng."); });
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        return userRepository.save(user);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public AppUser saveUser(AppUser user) {
        UserRole selectedRole = user.getRole() == null ? UserRole.SINH_VIEN : user.getRole();
        Role role = roleRepository.findByName(selectedRole.name())
            .orElseThrow(() -> new IllegalStateException("Vai trò không tồn tại: " + selectedRole.name()));
        return saveUser(user, List.of(role.getId()));
        }

        public AppUser saveUser(AppUser user, Collection<Long> roleIds) {
        boolean isNewUser = user.getId() == null;
            UserRole primaryRole = user.getRole();
            if (primaryRole == null && roleIds != null && roleIds.size() == 1) {
                Role selectedRole = roleRepository.findById(roleIds.iterator().next())
                        .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại."));
                try {
                    primaryRole = UserRole.valueOf(selectedRole.getName());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Vui lòng chọn một vai trò chính hợp lệ.");
                }
            }
            if (primaryRole == null) {
                throw new IllegalArgumentException("Vui lòng chọn vai trò chính của tài khoản.");
            }
            UserRole selectedPrimaryRole = primaryRole;

        AppUser savedUser = isNewUser
                ? new AppUser()
                : userRepository.findById(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));

        userRepository.findByUsername(user.getUsername()).filter(existing -> !existing.getId().equals(savedUser.getId()))
            .ifPresent(existing -> { throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng."); });
        userRepository.findByEmail(user.getEmail()).filter(existing -> !existing.getId().equals(savedUser.getId()))
            .ifPresent(existing -> { throw new IllegalArgumentException("Email đã được sử dụng."); });

        savedUser.setUsername(user.getUsername());
        savedUser.setFullName(user.getFullName());
        savedUser.setEmail(user.getEmail());
        savedUser.setPhone(user.getPhone());

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            if (user.getPassword().length() < 8) {
                throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự.");
            }
            savedUser.setPassword(passwordEncoder.encode(user.getPassword()));
        } else if (isNewUser) {
            throw new IllegalArgumentException("Mật khẩu là bắt buộc khi tạo tài khoản.");
        }

        String status = user.getStatus() == null || user.getStatus().isBlank()
                ? "ACTIVE"
                : user.getStatus().toUpperCase();
        if (!"ACTIVE".equals(status) && !"LOCKED".equals(status)) {
            throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
        }
        savedUser.setStatus(status);
        savedUser.setEnabled("ACTIVE".equals(status));

        if (roleIds == null || roleIds.isEmpty()) {
            throw new IllegalArgumentException("Tài khoản phải được gán ít nhất một vai trò.");
        }
        HashSet<Role> assignedRoles = new HashSet<>();
        for (Long roleId : roleIds) {
            assignedRoles.add(roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại: " + roleId)));
        }
        if (assignedRoles.stream().noneMatch(role -> selectedPrimaryRole.name().equals(role.getName()))) {
            throw new IllegalArgumentException("Vai trò chính phải được chọn trong các nhóm người dùng.");
        }
        savedUser.setRoles(assignedRoles);
        savedUser.setRole(selectedPrimaryRole);

        return userRepository.save(savedUser);
    }

    public AppUser updateStatus(Long id, String status) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        String normalizedStatus = status == null ? "" : status.toUpperCase();
        if (!"ACTIVE".equals(normalizedStatus) && !"LOCKED".equals(normalizedStatus)) {
            throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
        }
        user.setStatus(normalizedStatus);
        user.setEnabled("ACTIVE".equals(normalizedStatus));
        return userRepository.save(user);
    }
}
