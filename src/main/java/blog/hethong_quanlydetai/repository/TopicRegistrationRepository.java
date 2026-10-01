package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.TopicRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {
    boolean existsByGroupIdAndStatusIn(Long groupId, List<String> statuses);

    List<TopicRegistration> findByStatusOrderByRegisteredAtAsc(String status);

    List<TopicRegistration> findByGroupId(Long groupId);

    List<TopicRegistration> findByTopicIdAndStatus(Long topicId, String status);
}