package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.Announcement;
import blog.hethong_quanlydetai.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByAuthorOrderByCreatedAtDesc(AppUser author);

    List<Announcement> findByStatusOrderByPublishedAtDesc(String status);

    List<Announcement> findAllByOrderByPublishedAtDesc();
}
