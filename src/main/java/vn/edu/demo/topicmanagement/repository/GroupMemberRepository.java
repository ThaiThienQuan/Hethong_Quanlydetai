package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.GroupMember;
import vn.edu.demo.topicmanagement.entity.StudentGroup;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupId(Long groupId);

    List<GroupMember> findByGroup(StudentGroup group);

    /** Kiểm tra SV đã thuộc nhóm nào trong đợt này chưa (ràng buộc UNIQUE(student_id, period_id)). */
    Optional<GroupMember> findByStudentIdAndPeriodId(Long studentId, Long periodId);

    boolean existsByStudentIdAndPeriodId(Long studentId, Long periodId);

    long countByGroupId(Long groupId);

    /** Nhóm trưởng của nhóm. Tên property trong Java là {@code leader} (getter {@code isLeader()}). */
    Optional<GroupMember> findByGroupIdAndLeaderTrue(Long groupId);

    /** Nhóm hiện tại của một sinh viên (theo username) - dùng cho các màn hình sinh viên. */
    Optional<GroupMember> findFirstByStudentUserUsernameOrderByIdDesc(String username);

    /** Tìm group_member mà SV (username) là thành viên. */
    default Optional<GroupMember> findByStudentUsername(String username) {
        return findFirstByStudentUserUsernameOrderByIdDesc(username);
    }

    /** Thành viên kèm hồ sơ sinh viên, dùng khi hiển thị danh sách nhóm. */
    @Query("""
            select gm from GroupMember gm
            join fetch gm.student s
            join fetch s.user u
            where gm.group.id = :groupId
            order by gm.leader desc, s.studentCode
            """)
    List<GroupMember> findByGroupIdWithStudent(@Param("groupId") Long groupId);

    /** Các đợt mà một sinh viên có nhóm tham gia - nguồn cho bộ chọn "Tra cứu theo đợt". */
    @Query("""
            select distinct gm.periodId from GroupMember gm
            where gm.student.user.id = :studentUserId
            order by gm.periodId desc
            """)
    List<Long> findPeriodIdsByStudentUser(@Param("studentUserId") Long studentUserId);
}