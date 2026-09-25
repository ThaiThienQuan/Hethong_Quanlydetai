package blog.hethong_quanlydetai.repository;

import blog.hethong_quanlydetai.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupIdAndStudentId(Long groupId, Long studentId);
}