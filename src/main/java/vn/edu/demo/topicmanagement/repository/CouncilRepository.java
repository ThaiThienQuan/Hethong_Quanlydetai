package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.CouncilStatus;

public interface CouncilRepository extends JpaRepository<Council, Long> {

    Optional<Council> findByCouncilCode(String councilCode);

    boolean existsByCouncilCode(String councilCode);

    List<Council> findByStatusOrderByIdDesc(CouncilStatus status);

    List<Council> findByPeriodIdOrderByIdDesc(Long periodId);

    long countByStatus(CouncilStatus status);
}