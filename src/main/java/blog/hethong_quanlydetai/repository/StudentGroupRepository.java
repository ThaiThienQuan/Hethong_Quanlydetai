package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    boolean existsByCode(String code);

    List<StudentGroup> findByPeriodIdOrderByNameAsc(Long periodId);
}