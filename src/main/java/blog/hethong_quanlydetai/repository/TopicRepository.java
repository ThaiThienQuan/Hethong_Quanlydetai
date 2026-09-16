package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByPublishedTrue();
}