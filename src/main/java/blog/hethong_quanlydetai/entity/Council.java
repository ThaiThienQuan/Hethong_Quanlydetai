package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "councils")
@Getter @Setter @NoArgsConstructor
public class Council {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "council_id")
    private Long id;

    @Column(name = "council_name", nullable = false, length = 150)
    private String name;

    @Column(name = "council_code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "report_date", nullable = false)
    private java.time.LocalDateTime reportDate;

    @Column(nullable = false, length = 20)
    private String status = "PLANNED";

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @OneToMany(mappedBy = "council", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouncilMember> members = new ArrayList<>();
}