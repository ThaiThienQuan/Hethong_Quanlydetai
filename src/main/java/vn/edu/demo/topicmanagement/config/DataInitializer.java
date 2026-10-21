package vn.edu.demo.topicmanagement.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.entity.Permission;
import vn.edu.demo.topicmanagement.entity.Role;
import vn.edu.demo.topicmanagement.repository.PermissionRepository;
import vn.edu.demo.topicmanagement.repository.RoleRepository;

/**
 * Nạp danh mục quyền và 4 vai trò gốc khi khởi động (mục 5.1).
 *
 * <p>Idempotent: quyền đã có thì giữ nguyên; vai trò gốc chỉ được tạo nếu chưa có.
 * Phân quyền mặc định chỉ gán cho vai trò vừa tạo, nên không ghi đè phân quyền
 * mà Trưởng khoa đã chỉnh trên màn hình Phân quyền.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public DataInitializer(PermissionRepository permissionRepository, RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        var names = PermissionCatalog.permissionNames();

        // 1. Nạp danh mục quyền.
        for (var entry : names.entrySet()) {
            permissionRepository.findByCode(entry.getKey())
                    .orElseGet(() -> {
                        Permission p = new Permission(entry.getKey(), entry.getValue());
                        return permissionRepository.save(p);
                    });
        }
        log.info("Đã nạp {} quyền vào danh mục.", names.size());

        // 2. Tạo 4 vai trò gốc nếu chưa có, kèm phân quyền mặc định.
        for (String[] role : PermissionCatalog.systemRoles()) {
            String code = role[0];
            if (roleRepository.existsByCode(code)) {
                continue;
            }
            Role r = new Role(code, role[1], true);
            Set<Permission> granted = new LinkedHashSet<>();
            for (String permCode : PermissionCatalog.defaultPermissionsFor(code)) {
                permissionRepository.findByCode(permCode).ifPresent(granted::add);
            }
            r.setPermissions(granted);
            roleRepository.save(r);
            log.info("Đã tạo vai trò {} với {} quyền mặc định.", code, granted.size());
        }
    }
}