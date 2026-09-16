package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bo_mon", uniqueConstraints = @UniqueConstraint(name = "uk_bo_mon_code", columnNames = "code"))
@Getter @Setter @NoArgsConstructor
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false, length = 20)
    private String code;

    @NotBlank @Column(nullable = false, length = 150)
    private String name;
}