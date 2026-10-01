package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}