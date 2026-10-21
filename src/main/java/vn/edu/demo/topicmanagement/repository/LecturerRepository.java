package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.Lecturer;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {

    Optional<Lecturer> findByUserId(Long userId);

    Optional<Lecturer> findByLecturerCode(String lecturerCode);

    boolean existsByLecturerCode(String lecturerCode);

    long countByDepartmentId(Long departmentId);

    /** Danh sách GV theo bộ môn, dùng cho bộ lọc trong màn hình hồ sơ GV. */
    List<Lecturer> findByDepartmentIdOrderByLecturerCodeAsc(Long departmentId);

    List<Lecturer> findAllByOrderByLecturerCodeAsc();

    /**
     * Màn hình hồ sơ GV (mục 5.1b) cần user + department ngay trong view.
     * open-in-view=false nên phải fetch join trước khi transaction đóng.
     */
    @Query("""
            select l from Lecturer l
            join fetch l.user u
            join fetch l.department d
            where (:departmentId is null or d.id = :departmentId)
            order by l.lecturerCode
            """)
    List<Lecturer> findAllForProfileList(@Param("departmentId") Long departmentId);

    /** Lấy 1 GV kèm user + department, dùng cho trang chi tiết/sửa (open-in-view=false). */
    @Query("select l from Lecturer l join fetch l.user u join fetch l.department d where l.id = :id")
    Optional<Lecturer> findByIdForProfile(@Param("id") Long id);

    /**
     * Bộ môn của một GV theo user id, fetch sẵn department.
     * open-in-view=false nên phải join fetch để đọc được tên bộ môn ngoài transaction.
     */
    @Query("select l from Lecturer l join fetch l.department d where l.user.id = :userId")
    Optional<Lecturer> findByUserIdWithDepartment(@Param("userId") Long userId);

    /**
     * Danh sách GV có thể làm thành viên hội đồng: đang hoạt động, thuộc một bộ môn.
     */
    @Query("""
            select l from Lecturer l
            join fetch l.department d
            where l.user.enabled = true
            order by l.lecturerCode
            """)
    List<Lecturer> findAllActiveWithDepartment();

    @Query("""
            select l from Lecturer l
            join fetch l.user u
            where l.id in :ids
            """)
    List<Lecturer> findByIdsWithUser(@Param("ids") List<Long> ids);
}