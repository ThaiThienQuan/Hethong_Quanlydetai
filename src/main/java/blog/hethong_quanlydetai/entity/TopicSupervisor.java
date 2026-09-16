package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "giang_vien_huong_dan", uniqueConstraints = @UniqueConstraint(name = "uk_topic_supervisor", columnNames = {"topic_id", "teacher_id"}))
@Getter @Setter @NoArgsConstructor
public class TopicSupervisor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private AppUser teacher;
}