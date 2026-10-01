package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.Permission;
import blog.hethong_quanlydetai.entity.Role;
import blog.hethong_quanlydetai.repository.PermissionRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    public Role save(Role role) {
        if (role.getId() == null) {
            return roleRepository.save(role);
        }
        Role existing = roleRepository.findById(role.getId())
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại."));
        existing.setName(role.getName());
        existing.setDescription(role.getDescription());
        return roleRepository.save(existing);
    }

    public Role updatePermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại."));

        Set<Permission> permissions = new HashSet<>();
        if (permissionIds != null) {
            permissions = permissionIds.stream()
                    .map(id -> permissionRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Quyền không tồn tại: " + id)))
                    .collect(Collectors.toSet());
        }

        role.setPermissions(permissions);
        return roleRepository.save(role);
    }
}
