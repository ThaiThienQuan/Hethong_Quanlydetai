package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByStudentCode(String studentCode);

    boolean existsByStudentCode(String studentCode);

    /**
     * Số thứ tự lớn nhất trong mã sinh viên dạng {@code SV<digits>} (vd SV23001 -> 23001).
     * Dùng để cấp mã mới mà không trùng mã cũ. Trả về null khi chưa có mã dạng này.
     * Native query vì cần SUBSTRING + CAST trong MySQL.
     */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(student_code, 3) AS UNSIGNED)) "
            + "FROM students WHERE student_code REGEXP '^SV[0-9]+$'", nativeQuery = true)
    Long findMaxStudentCodeNumber();

    long countByMajor(String major);

    /**
     * Màn hình hồ sơ SV (mục 5.1b) cần user ngay trong view.
     * open-in-view=false nên phải fetch join trước khi transaction đóng.
     * Có thể đếm trực tiếp (Page) vì join fetch chỉ áp dụng cho OneToOne user -
     * không có quan hệ collection nào làm loãng bản ghi.
     */
    @Query(value = "select s from Student s join fetch s.user u order by s.studentCode",
            countQuery = "select count(s) from Student s")
    Page<Student> findAllForProfileList(Pageable pageable);

    /** Lấy 1 SV kèm user, dùng cho trang chi tiết/sửa (open-in-view=false). */
    @Query("select s from Student s join fetch s.user u where s.id = :id")
    Optional<Student> findByIdForProfile(@Param("id") Long id);

    /**
     * Gợi ý sinh viên chưa thuộc nhóm nào trong đợt đang xét (mục 5.4).
     * Dùng cho ô chọn "Thêm thành viên" của nhóm trưởng.
     *
     * <p>{@code join fetch s.user} vì {@code open-in-view=false}: tên hiển thị và lớp
     * được đọc ngay trong service, không được để lazy load ngoài transaction.
     */
    @Query("""
            select s from Student s
            join fetch s.user u
            where (:kw is null
                   or lower(s.studentCode) like :kw
                   or lower(u.fullName) like :kw)
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