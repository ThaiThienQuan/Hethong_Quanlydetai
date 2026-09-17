package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "council_members", uniqueConstraints = @UniqueConstraint(name = "uk_council_lecturer", columnNames = {"council_id", "lecturer_id"}))
@Getter @Setter @NoArgsConstructor
public class CouncilMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "council_member_id")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private AppUser teacher;

    @Column(nullable = false)
    private boolean chairman;

    @Column(nullable = false)
    private boolean secretary;
}