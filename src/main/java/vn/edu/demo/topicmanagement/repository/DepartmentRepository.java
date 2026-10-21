package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Department> findByStatusOrderByName(String status);

    /** Chỉ trả về bộ môn còn hoạt động. */
    List<Department> findByStatusOrderByNameAsc(String status);

    Page<Department> findAllByOrderByNameAsc(Pageable pageable);

    /** Wrapper cho code gọi không có Pageable. */
    default List<Department> findAllByOrderByNameAsc() {
        return findAllByOrderByNameAsc(Pageable.unpaged()).getContent();
    }
}
