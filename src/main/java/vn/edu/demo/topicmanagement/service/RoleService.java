package vn.edu.demo.topicmanagement.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.config.PermissionCatalog;
import vn.edu.demo.topicmanagement.entity.Permission;
import vn.edu.demo.topicmanagement.entity.Role;
import vn.edu.demo.topicmanagement.repository.PermissionRepository;
import vn.edu.demo.topicmanagement.repository.RoleRepository;

/**
 * Nghiệp vụ quản lý vai trò và quyền (chức năng 5.1 - CRUD vai trò + phân quyền).
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    // ======================= ROLE =======================

    @Transactional(readOnly = true)
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Role> findEditableRoles() {
        return roleRepository.findBySystemFalseOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vai trò."));
    }

    @Transactional
    public Role createRole(String code, String name, String description) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Mã vai trò không được để trống.");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Tên vai trò không được để trống.");
        if (roleRepository.existsByCode(code.trim())) {
            throw new IllegalArgumentException("Mã vai trò đã tồn tại.");
        }
        Role role = new Role(code.trim(), name.trim(), false);
        role.setDescription(description == null ? null : description.trim());
        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(Long id, String name, String description) {
        Role role = getRoleById(id);
        if (role.isSystem()) {
            throw new IllegalStateException("Không được sửa vai trò hệ thống.");
        }
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Tên vai trò không được để trống.");
        role.setName(name.trim());
        role.setDescription(description == null ? null : description.trim());
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = getRoleById(id);
        if (role.isSystem()) {
            throw new IllegalStateException("Không được xóa vai trò hệ thống.");
        }
        roleRepository.delete(role);
        roleRepository.flush();
    }

    // ======================= PERMISSION =======================

    @Transactional(readOnly = true)
    public List<Permission> findAllPermissions() {
        return permissionRepository.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public Permission getPermissionById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy quyền."));
    }

    // ======================= PERMISSION ASSIGNMENT =======================

    /**
     * Lấy map permission code -> đã được gán cho vai trò hay chưa.
     * Dùng cho màn hình Phân quyền (checkbox).
     */
    @Transactional(readOnly = true)
    public Map<String, Boolean> getPermissionMapForRole(Long roleId) {
        Role role = getRoleById(roleId);
        Set<String> assignedCodes = role.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        return permissionRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(Permission::getCode, p -> assignedCodes.contains(p.getCode())));
    }

    @Transactional
    public void assignPermissionsByCodes(Long roleId, Set<String> permissionCodes, String changedBy) {
        Role role = getRoleById(roleId);
        if (role.isSystem()) {
            throw new IllegalStateException("Không được thay đổi quyền của vai trò hệ thống.");
        }

        Set<Permission> selected = permissionCodes.stream()
                .map(permissionRepository::findByCode)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toSet());

        role.setPermissions(selected);
        roleRepository.save(role);
    }

    @Transactional
    public void assignPermissions(Long roleId, Set<Long> permissionIds, String changedBy) {
        // Convert IDs to codes for consistency
        Set<String> codes = permissionIds.stream()
                .map(id -> {
                    var perm = permissionRepository.findById(id).orElse(null);
                    return perm != null ? perm.getCode() : null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        assignPermissionsByCodes(roleId, codes, changedBy);
    }

    /** Tên hiển thị của từng quyền. */
    public Map<String, String> getPermissionNames() {
        return PermissionCatalog.permissionNames();
    }
}
