package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    /** Vai trò không phải gốc — cho phép sửa/xóa. Property là "system" (getter isSystem()). */
    List<Role> findBySystemFalseOrderByNameAsc();

    Page<Role> findAllByOrderByNameAsc(Pageable pageable);
}