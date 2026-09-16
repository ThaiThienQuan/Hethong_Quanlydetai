package blog.hethong_quanlydetai.controller;

import blog.hethong_quanlydetai.entity.RegistrationPeriod;
import blog.hethong_quanlydetai.repository.RegistrationPeriodRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registration-periods")
public class RegistrationPeriodController {
    private final RegistrationPeriodRepository repository;

    public RegistrationPeriodController(RegistrationPeriodRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<RegistrationPeriod> findAll() {
        return repository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TRUONG_KHOA')")
    public RegistrationPeriod create(@Valid @RequestBody RegistrationPeriod period) {
        return repository.save(period);
    }
}