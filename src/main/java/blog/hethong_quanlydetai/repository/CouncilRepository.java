package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.Council;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouncilRepository extends JpaRepository<Council, Long> {
    List<Council> findAllByOrderByReportDateDesc();
}