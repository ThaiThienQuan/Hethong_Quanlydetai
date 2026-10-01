package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.Permission;
import blog.hethong_quanlydetai.entity.Role;
import blog.hethong_quanlydetai.repository.PermissionRepository;
import blog.hethong_quanlydetai.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissionService(PermissionRepository permissionRepository, RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    public Optional<Permission> findById(Long id) {
        return permissionRepository.findById(id);
    }

    public Permission save(Permission permission) {
        if (permission.getId() == null) {
            return permissionRepository.save(permission);
        }
        Permission existing = permissionRepository.findById(permission.getId())
                .orElseThrow(() -> new IllegalArgumentException("Quyền không tồn tại."));
        existing.setCode(permission.getCode());
        existing.setName(permission.getName());
        existing.setDescription(permission.getDescription());
        return permissionRepository.save(existing);
    }

    public List<Role> findRolesUsingPermission(Long permissionId) {
        return roleRepository.findAll().stream()
                .filter(role -> role.getPermissions().stream()
                        .anyMatch(permission -> permission.getId() != null && permission.getId().equals(permissionId)))
                .toList();
    }
}
