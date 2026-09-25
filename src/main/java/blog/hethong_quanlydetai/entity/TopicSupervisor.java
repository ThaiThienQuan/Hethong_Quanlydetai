package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "topic_supervisors")
@Getter
@Setter
@NoArgsConstructor
public class TopicSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_supervisor_id")
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "lecturer_id", nullable = false)
    private Long lecturerId;

    @Column(name = "supervisor_order", nullable = false)
    private Integer supervisorOrder;
}