package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    List<Permission> findAllByOrderByCodeAsc();

    Page<Permission> findAllByOrderByCodeAsc(Pageable pageable);
}