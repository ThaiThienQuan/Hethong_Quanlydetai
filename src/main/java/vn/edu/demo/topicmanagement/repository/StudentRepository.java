package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByStudentCode(String studentCode);

    boolean existsByStudentCode(String studentCode);

    long countByMajor(String major);

    /**
     * Màn hình hồ sơ SV (mục 5.1b) cần user ngay trong view.
     * open-in-view=false nên phải fetch join trước khi transaction đóng.
     */
    @Query("select s from Student s join fetch s.user u order by s.studentCode")
    List<Student> findAllForProfileList();

    /** Lấy 1 SV kèm user, dùng cho trang chi tiết/sửa (open-in-view=false). */
    @Query("select s from Student s join fetch s.user u where s.id = :id")
    Optional<Student> findByIdForProfile(@Param("id") Long id);

    /**
     * Gợi ý sinh viên chưa thuộc nhóm nào trong đợt đang xét (mục 5.4).
     * Dùng cho ô tìm kiếm khi lập nhóm.
     */
    @Query("""
            select s from Student s
            where (:kw is null
                   or lower(s.studentCode) like :kw
                   or lower(s.user.fullName) like :kw)
              and not exists (
                  select 1 from GroupMember gm
                  where gm.student = s and gm.periodId = :periodId)
            order by s.studentCode
            """)
    List<Student> findStudentsWithoutGroupInPeriod(@Param("periodId") Long periodId,
                                                   @Param("kw") String keyword);

    @Query("""
            select s from Student s
            where s.user.id in :userIds
            """)
    List<Student> findByUserIds(@Param("userIds") List<Long> userIds);
}