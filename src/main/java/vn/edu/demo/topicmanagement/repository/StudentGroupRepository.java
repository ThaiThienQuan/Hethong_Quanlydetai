package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.StudentGroup;
import vn.edu.demo.topicmanagement.entity.Topic;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    Optional<StudentGroup> findByGroupCode(String groupCode);

    boolean existsByGroupCode(String groupCode);

    /** Nhóm đang thực hiện một đề tài - UNIQUE(topic_id) bảo đảm tối đa một nhóm. */
    Optional<StudentGroup> findByTopicId(Long topicId);

    List<StudentGroup> findByPeriodIdOrderByIdAsc(Long periodId);

    /** Trả về tất cả nhóm sắp xếp theo id giảm dần - dùng cho màn danh sách. */
    default List<StudentGroup> findAllByOrderByIdDesc() {
        return findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
    }

    /**
     * Nhóm của một sinh viên trong một đợt.
     * Nhờ UNIQUE(student_id, period_id) nên truy vấn này trả về tối đa một kết quả.
     *
     * <p>Dùng {@code join} chứ không phải {@code join fetch}: đang select một thuộc tính
     * đơn ({@code gm.group}) nên join fetch không được phép. {@code StudentGroup.period}
     * vẫn được nạp lazily khi service dùng, nên không phát sinh N+1 ở màn hình chi tiết.
     */
    @Query("""
            select gm.group from GroupMember gm
            where gm.student.user.id = :studentUserId and gm.periodId = :periodId
            """)
    Optional<StudentGroup> findByStudentAndPeriod(@Param("studentUserId") Long studentUserId,
                                                  @Param("periodId") Long periodId);

    /** Danh sách nhóm của một sinh viên theo mọi đợt (dùng cho tra cứu kết quả theo đợt). */
    @Query("""
            select distinct gm.group from GroupMember gm
            where gm.student.user.id = :studentUserId
            order by gm.group.id desc
            """)
    List<StudentGroup> findAllByStudent(@Param("studentUserId") Long studentUserId);

    @Query("""
            select distinct g from StudentGroup g
            left join fetch g.topic t
            where g.period.id = :periodId
            order by g.id desc
            """)
    List<StudentGroup> findAllByPeriod(@Param("periodId") Long periodId);

    /** Nhóm thuộc các đề tài đã nộp báo cáo - nguồn để lập hội đồng phản biện. */
    @Query("""
            select distinct g from StudentGroup g
            join fetch g.topic t
            where g.period.id = :periodId and t is not null
            order by g.id
            """)
    List<StudentGroup> findGroupsWithTopicInPeriod(@Param("periodId") Long periodId);

    /** Đếm số đề tài đã có nhóm được chấp thuận, dùng để chặn xóa đề tài. */
    long countByTopic(Topic topic);
}