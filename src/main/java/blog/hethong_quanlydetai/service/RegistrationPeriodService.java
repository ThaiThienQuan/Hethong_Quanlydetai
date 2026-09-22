package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.RegistrationPeriod;
import blog.hethong_quanlydetai.entity.RegistrationType;
import blog.hethong_quanlydetai.repository.RegistrationPeriodRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RegistrationPeriodService {
    private final RegistrationPeriodRepository repository;

    public RegistrationPeriodService(RegistrationPeriodRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> findAll() {
        return repository.findAllByOrderByStudentStartDesc();
    }

    @Transactional(readOnly = true)
    public RegistrationPeriod findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public RegistrationPeriod save(RegistrationPeriod period) {
        if (period.getId() == null) {
            period.setCreatedBy(1L);
        }
        if (period.getType() != RegistrationType.TLCN && period.getType() != RegistrationType.KLTN) {
            period.setReviewerDeadline(null);
        }
        if (period.getType() != RegistrationType.KLTN) {
            period.setDefenseDate(null);
        }
        if (period.getStatus() == null || period.getStatus().isBlank()) {
            period.setStatus("DRAFT");
        }
        return repository.save(period);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public void publish(Long id) {
        RegistrationPeriod period = findById(id);
        period.setStatus("PUBLISHED");
        repository.save(period);
    }

    public String currentPhase(RegistrationPeriod period, LocalDateTime now) {
        if ("DRAFT".equals(period.getStatus())) {
            return "Bản nháp · Chưa công bố";
        }
        if ("CLOSED".equals(period.getStatus()) || now.isAfter(period.getStudentEnd())) {
            return "Đã kết thúc";
        }
        if (!now.isBefore(period.getStudentStart()) && !now.isAfter(period.getStudentEnd())) {
            return "Giai đoạn 2 · Nhóm sinh viên đăng ký";
        }
        if (!now.isBefore(period.getTeacherStart()) && !now.isAfter(period.getTeacherEnd())) {
            return "Giai đoạn 1 · Xây dựng và công bố đề tài";
        }
        if (now.isBefore(period.getTeacherStart())) {
            return "Sắp mở · Giai đoạn 1";
        }
        return "Chờ giai đoạn 2";
    }
}