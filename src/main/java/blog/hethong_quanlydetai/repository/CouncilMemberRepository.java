package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.CouncilMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {
    List<CouncilMember> findByCouncilId(Long councilId);
    long countByCouncilId(Long councilId);
    boolean existsByCouncilIdAndLecturerId(Long councilId, Long lecturerId);
    long countByCouncilIdAndRole(Long councilId, String role);
}