package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hoi_dong")
@Getter @Setter @NoArgsConstructor
public class Council {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @OneToMany(mappedBy = "council", cascade = CascadeType.ALL, orphanRemoval = true)
    @Size(min = 3, max = 5, message = "Hội đồng phải có từ 03 đến 05 giảng viên")
    private List<CouncilMember> members = new ArrayList<>();
}