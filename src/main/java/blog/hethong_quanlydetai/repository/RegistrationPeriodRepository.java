package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.RegistrationPeriod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
    List<RegistrationPeriod> findAllByOrderByStudentStartDesc();
}