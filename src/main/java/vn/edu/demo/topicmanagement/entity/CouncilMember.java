package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Bảng {@code council_members} - thành viên hội đồng. CSDL bảo đảm mỗi hội đồng có
 * đúng một CHAIRMAN và đúng một SECRETARY qua các cột sinh ra.
 *
 * <p>Lưu ý: "chủ tịch hội đồng" là quan hệ theo từng hội đồng, không phải vai trò tài khoản.
 * Mọi thao tác của chủ tịch phải kiểm tra {@code council_members.role = CHAIRMAN}
 * của đúng hội đồng đó.
 */
@Entity
@Table(name = "council_members", uniqueConstraints =
        @UniqueConstraint(name = "uq_council_members_lecturer", columnNames = {"council_id", "lecturer_id"}))
public class CouncilMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    /** CHAIRMAN / SECRETARY / MEMBER. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouncilRole role = CouncilRole.MEMBER;

    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    public CouncilMember() {
    }

    public CouncilMember(Council council, Lecturer lecturer, CouncilRole role) {
        this.council = council;
        this.lecturer = lecturer;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Council getCouncil() {
        return council;
    }

    public void setCouncil(Council council) {
        this.council = council;
    }

    public Lecturer getLecturer() {
        return lecturer;
    }

    public void setLecturer(Lecturer lecturer) {
        this.lecturer = lecturer;
    }

    public CouncilRole getRole() {
        return role;
    }

    public void setRole(CouncilRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}