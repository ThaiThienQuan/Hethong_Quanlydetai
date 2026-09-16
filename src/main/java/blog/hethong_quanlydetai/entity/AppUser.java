package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "nguoi_dung", uniqueConstraints = @UniqueConstraint(name = "uk_nguoi_dung_username", columnNames = "username"))
@Getter @Setter @NoArgsConstructor
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String fullName;

    @Email
    @Column(length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;
}