package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Bảng {@code group_members}.
 *
 * <p>Ràng buộc quan trọng: {@code UNIQUE (student_id, period_id)} - mỗi sinh viên chỉ thuộc
 * một nhóm <b>trong mỗi đợt</b>. Nhờ vậy SV đã học đợt TLCN vẫn lập được nhóm ở đợt KLTN và
 * tra cứu kết quả theo từng đợt có ý nghĩa. Nếu đặt {@code UNIQUE (student_id)} toàn cục thì
 * sinh viên sẽ không bao giờ lập được nhóm lần thứ hai.
 *
 * <p>Cột {@code period_id} được sao chép từ nhóm và khớp với nhóm qua ràng buộc khóa ngoại kép
 * {@code (group_id, period_id) -> student_groups(id, period_id)} ở mức CSDL.
 */
@Entity
@Table(name = "group_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_group_members_student_period", columnNames = {"student_id", "period_id"}),
        @UniqueConstraint(name = "uq_group_members_group_student", columnNames = {"group_id", "student_id"})
})
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    /** Sao chép từ nhóm; CSDL ép khớp bằng khóa ngoại kép. */
    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** Đúng 1 thành viên mỗi nhóm là nhóm trưởng. */
    @Column(name = "is_leader", nullable = false)
    private boolean leader = false;

    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    public GroupMember() {
    }

    public GroupMember(StudentGroup group, Student student, boolean leader) {
        this.group = group;
        this.periodId = group.getPeriod().getId();
        this.student = student;
        this.leader = leader;
    }

    public Long getId() {
        return id;
    }

    public StudentGroup getGroup() {
        return group;
    }

    public void setGroup(StudentGroup group) {
        this.group = group;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}