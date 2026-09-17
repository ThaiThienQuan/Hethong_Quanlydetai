package blog.hethong_quanlydetai.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_groups")
@Getter @Setter @NoArgsConstructor
public class StudentGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @NotBlank @Column(name = "group_name", nullable = false, length = 100)
    private String name;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "group_code", nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", unique = true)
    private Topic topic;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Size(max = 3, message = "Mỗi nhóm có tối đa 03 thành viên")
    private List<GroupMember> members = new ArrayList<>();

    @Column(length = 500)
    private String reportUrl;
}