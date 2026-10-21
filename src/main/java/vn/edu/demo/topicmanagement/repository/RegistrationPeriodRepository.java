package vn.edu.demo.topicmanagement.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {

    List<RegistrationPeriod> findAllByOrderByIdDesc();

    /** Các đợt đã công bố - sinh viên chỉ đăng ký trong đợt này. */
    List<RegistrationPeriod> findByStatusOrderByIdDesc(PeriodStatus status);

    /** Đợt đang mở cho sinh viên: PUBLISHED và đang trong khung giai đoạn 2. */
    @Query("""
            select p from RegistrationPeriod p
            where p.status = :published
              and :now between p.studentStartAt and p.studentEndAt
            order by p.id desc
            """)
    List<RegistrationPeriod> findPeriodsOpenForStudents(@Param("now") LocalDateTime now,
                                                         @Param("published") PeriodStatus published);

    /** Đợt đang mở cho giảng viên: PUBLISHED và đang trong khung giai đoạn 1. */
    @Query("""
            select p from RegistrationPeriod p
            where p.status = :published
              and :now between p.lecturerStartAt and p.lecturerEndAt
            order by p.id desc
            """)
    List<RegistrationPeriod> findPeriodsOpenForLecturers(@Param("now") LocalDateTime now,
                                                           @Param("published") PeriodStatus published);

    Optional<RegistrationPeriod> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}