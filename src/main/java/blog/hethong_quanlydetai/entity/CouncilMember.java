package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "thanh_vien_hoi_dong", uniqueConstraints = @UniqueConstraint(name = "uk_council_teacher", columnNames = {"council_id", "teacher_id"}))
@Getter @Setter @NoArgsConstructor
public class CouncilMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private AppUser teacher;

    @Column(nullable = false)
    private boolean chairman;

    @Column(nullable = false)
    private boolean secretary;
}