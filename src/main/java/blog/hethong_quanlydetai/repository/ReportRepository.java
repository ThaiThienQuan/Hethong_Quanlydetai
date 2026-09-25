package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByGroupIdOrderBySubmittedAtDesc(Long groupId);
}