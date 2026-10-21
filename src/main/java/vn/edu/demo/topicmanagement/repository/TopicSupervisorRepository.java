package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.TopicSupervisor;

public interface TopicSupervisorRepository extends JpaRepository<TopicSupervisor, Long> {

    List<TopicSupervisor> findByTopicId(Long topicId);

    Optional<TopicSupervisor> findByTopicIdAndLecturerId(Long topicId, Long lecturerId);

    void deleteByTopicId(Long topicId);

    boolean existsByTopicIdAndLecturerId(Long topicId, Long lecturerId);

    long countByTopicId(Long topicId);

    /** Danh sách GVHD của một đề tài. */
    @Query("""
            select ts from TopicSupervisor ts
            join fetch ts.lecturer l
            join fetch l.user u
            where ts.topic.id = :topicId
            order by ts.supervisorOrder
            """)
    List<TopicSupervisor> findByTopicIdWithLecturer(@Param("topicId") Long topicId);

    /**
     * Các đề tài mà giảng viên này đang hướng dẫn - dùng để chặn
     * "GV không được chấm đề tài mình hướng dẫn" (mục 5.7).
     */
    @Query("""
            select ts from TopicSupervisor ts
            join fetch ts.topic t
            where ts.lecturer.user.id = :lecturerUserId
            """)
    List<TopicSupervisor> findByLecturerUserId(@Param("lecturerUserId") Long lecturerUserId);

    /** Danh sách đề tài mà giảng viên này làm GVHD, để kiểm tra GVHD khi phân công chấm. */
    @Query("select ts.topic.id from TopicSupervisor ts where ts.lecturer.user.id = :lecturerUserId")
    List<Long> findTopicIdsByLecturerUserId(@Param("lecturerUserId") Long lecturerUserId);

    /**
     * Danh sách id giảng viên đang làm GVHD của các đề tài thuộc một đợt.
     * Dùng để loại các GV đã bận khỏi danh sách chọn "GVHD thứ hai".
     */
    @Query("select distinct ts.lecturer.id from TopicSupervisor ts where ts.topic.period.id = :periodId")
    List<Long> findLecturerIdsByPeriodId(@Param("periodId") Long periodId);
}