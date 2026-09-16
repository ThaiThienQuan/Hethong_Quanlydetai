package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "de_tai")
@Getter @Setter @NoArgsConstructor
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false, length = 250)
    private String title;

    @NotBlank @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean published;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;
}