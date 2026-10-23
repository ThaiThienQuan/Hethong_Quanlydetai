package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.CouncilStatus;

public interface CouncilRepository extends JpaRepository<Council, Long> {

    Optional<Council> findByCouncilCode(String councilCode);

    boolean existsByCouncilCode(String councilCode);

    /**
     * Số mã hội đồng đã cấp trong một đợt đăng ký - dùng cấp số thứ tự cho
     * mã tự động ({@code HD-TLCN26-001}, {@code -002}, ...).
     *
     * <p>Đếm theo tiền tố {@code HD-{LoạiĐợt}{Năm}-} để hội đồng đã có sẵn
     * (mã kiểu cũ {@code HD-KLTN-25-A}) không ảnh hưởng tới số thứ tự mới.
     */
    @Query("""
            select count(c) from Council c
            where c.period.id = :periodId
              and c.councilCode like concat(:prefix, '%')
            """)
    long countByPeriodAndCodePrefix(@Param("periodId") Long periodId,
                                    @Param("prefix") String prefix);

    List<Council> findByStatusOrderByIdDesc(CouncilStatus status);

    List<Council> findByPeriodIdOrderByIdDesc(Long periodId);

    long countByStatus(CouncilStatus status);

    /** Số hội đồng thuộc một đợt - dùng để cảnh báo "đợt đã có hội đồng". */
    long countByPeriodId(Long periodId);
}