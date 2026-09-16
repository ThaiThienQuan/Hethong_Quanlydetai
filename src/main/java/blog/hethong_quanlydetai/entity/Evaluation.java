package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "danh_gia", uniqueConstraints = @UniqueConstraint(name = "uk_evaluation_topic_teacher", columnNames = {"topic_id", "teacher_id"}))
@Getter @Setter @NoArgsConstructor
public class Evaluation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private AppUser teacher;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @NotNull @DecimalMin("0.0") @DecimalMax("10.0")
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String comment;
}