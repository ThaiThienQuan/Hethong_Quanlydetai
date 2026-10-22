package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.Council;
import vn.edu.demo.topicmanagement.entity.CouncilMember;
import vn.edu.demo.topicmanagement.entity.CouncilRole;

public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {

    List<CouncilMember> findByCouncilId(Long councilId);

    boolean existsByCouncilIdAndLecturerId(Long councilId, Long lecturerId);

    long countByCouncilId(Long councilId);

    long countByCouncilIdAndRole(Long councilId, CouncilRole role);

    Optional<CouncilMember> findByCouncilIdAndRole(Long councilId, CouncilRole role);

    /**
     * Kiểm tra "chủ tịch của đúng hội đồng đó" (mục 3) - không chỉ dựa vào quyền.
     */
    @Query("""
            select cm from CouncilMember cm
            join fetch cm.lecturer l
            join fetch l.user u
            where cm.council.id = :councilId
              and cm.role = :role
              and u.id = :lecturerUserId
            """)
    Optional<CouncilMember> findChairmanOfCouncil(@Param("councilId") Long councilId,
                                                  @Param("role") CouncilRole role,
                                                  @Param("lecturerUserId") Long lecturerUserId);

    /** Thành viên kèm hồ sơ giảng viên, dùng hiển thị trang chi tiết hội đồng. */
    @Query("""
            select cm from CouncilMember cm
            join fetch cm.lecturer l
            join fetch l.user u
            join fetch l.department d
            where cm.council.id = :councilId
            order by cm.role asc, l.lecturerCode
            """)
    List<CouncilMember> findByCouncilIdWithLecturer(@Param("councilId") Long councilId);

    /** Các hội đồng mà một giảng viên tham gia (có thể tham gia nhiều hội đồng). */
    @Query("""
            select cm.council from CouncilMember cm
            where cm.lecturer.user.id = :lecturerUserId
            """)
    List<Council> findCouncilsOfLecturer(@Param("lecturerUserId") Long lecturerUserId);

    /**
     * Danh sách hội đồng mà giảng viên này làm CHỦ TỊCH (mục 5.10 - "việc cần làm"
     * của GV là chủ tịch một hội đồng). "Chủ tịch" là quan hệ theo từng hội đồng.
     */
    @Query("""
            select cm.council from CouncilMember cm
            where cm.lecturer.user.id = :lecturerUserId
              and cm.role = :role
            """)
    List<Council> findCouncilsWhereChairman(@Param("lecturerUserId") Long lecturerUserId,
                                            @Param("role") CouncilRole role);

    /** Các đề tài mà giảng viên này tham gia hội đồng phản biện (tra theo username). */
    @Query("""
            select ct.topic.id from CouncilTopic ct
            where ct.council in (select cm.council from CouncilMember cm
                                 where cm.lecturer.user.username = :username)
            """)
    List<Long> findTopicIdsByLecturerUsername(@Param("username") String username);

    long countByCouncil(Council council);
}