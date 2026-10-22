package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByEnabledTrue();

    long countByStatus(String status);

    /**
     * Danh sách tài khoản có tìm kiếm và lọc theo vai trò/trạng thái (mục 5.1).
     * Trả về Page để phân trang.
     */
    @Query("""
            select distinct u from User u
            left join u.roles r
            where (:kw is null or lower(u.username) like :kw
                   or lower(u.fullName) like :kw
                   or lower(u.email) like :kw)
              and (:roleCode is null or r.code = :roleCode)
              and (:status is null or u.status = :status)
            order by u.id desc
            """)
    Page<User> search(@Param("kw") String keyword,
                      @Param("roleCode") String roleCode,
                      @Param("status") String status,
                      Pageable pageable);

    /** Danh sách giảng viên theo bộ môn, dùng cho ô chọn GVHD/GV chấm. */
    @Query("""
            select u from User u
            where exists (select 1 from u.roles r where r.code = 'GIANG_VIEN')
              and u.enabled = true
            order by u.fullName
            """)
    List<User> findAllEnabledLecturers();

    /**
     * Tài khoản quản trị viên đang hoạt động, dùng làm đầu mối liên hệ cho người dùng.
     * <p>Chỉ lấy tài khoản còn {@code enabled} để tránh hiển thị SĐT của admin đã bị vô hiệu hoá.
     * Nếu có nhiều admin thì lấy tài khoản tạo sớm nhất (đầu danh sách).
     */
    @Query("""
            select u from User u
            where u.enabled = true
              and exists (select 1 from u.roles r where r.code = 'ADMIN')
            order by u.id asc
            """)
    List<User> findEnabledAdmins();
}