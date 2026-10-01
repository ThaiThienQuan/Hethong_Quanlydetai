package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    Optional<Lecturer> findByUserId(Long userId);
}