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
@Table(name = "nhom_sinh_vien")
@Getter @Setter @NoArgsConstructor
public class StudentGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false, length = 150)
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", unique = true)
    private Topic topic;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Size(max = 3, message = "Mỗi nhóm có tối đa 03 thành viên")
    private List<GroupMember> members = new ArrayList<>();

    @Column(length = 500)
    private String reportUrl;
}