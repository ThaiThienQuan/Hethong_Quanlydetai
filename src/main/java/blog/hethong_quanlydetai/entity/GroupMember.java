package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "thanh_vien_nhom", uniqueConstraints = @UniqueConstraint(name = "uk_student_one_group", columnNames = "student_id"))
@Getter @Setter @NoArgsConstructor
public class GroupMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private AppUser student;

    @Column(nullable = false)
    private boolean leader;
}