package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByOrderByPublishedAtDesc();
}