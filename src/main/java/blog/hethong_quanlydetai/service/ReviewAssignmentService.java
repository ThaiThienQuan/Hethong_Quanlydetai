package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.entity.ReviewAssignment;
import blog.hethong_quanlydetai.repository.ReviewAssignmentRepository;
import blog.hethong_quanlydetai.repository.TopicSupervisorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReviewAssignmentService {

    private final ReviewAssignmentRepository reviewAssignmentRepository;
    private final TopicSupervisorRepository topicSupervisorRepository;

    public ReviewAssignmentService(ReviewAssignmentRepository reviewAssignmentRepository,
                                   TopicSupervisorRepository topicSupervisorRepository) {
        this.reviewAssignmentRepository = reviewAssignmentRepository;
        this.topicSupervisorRepository = topicSupervisorRepository;
    }

    public List<ReviewAssignment> findAll() {
        return reviewAssignmentRepository.findAllByOrderByAssignedAtDesc();
    }

    public void assign(Long councilId, Long topicId, Long lecturerId) {
        if (topicSupervisorRepository.existsByTopicIdAndLecturerId(topicId, lecturerId)) {
            throw new IllegalArgumentException("Giảng viên hướng dẫn không được chấm đề tài mình hướng dẫn.");
        }

        if (reviewAssignmentRepository.existsByCouncilIdAndTopicIdAndLecturerId(councilId, topicId, lecturerId)) {
            throw new IllegalArgumentException("Giảng viên này đã được phân công chấm đề tài này trong hội đồng.");
        }

        ReviewAssignment assignment = new ReviewAssignment();
        assignment.setCouncilId(councilId);
        assignment.setTopicId(topicId);
        assignment.setLecturerId(lecturerId);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setStatus("ASSIGNED");

        reviewAssignmentRepository.save(assignment);
    }

    public void delete(Long id) {
        reviewAssignmentRepository.deleteById(id);
    }
}