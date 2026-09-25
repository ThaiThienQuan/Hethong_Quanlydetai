package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.TopicSupervisor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicSupervisorRepository extends JpaRepository<TopicSupervisor, Long> {
    boolean existsByTopicIdAndLecturerId(Long topicId, Long lecturerId);
}