package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** Bảng {@code council_topics} - các đề tài đưa vào phản biện của một hội đồng. */
@Entity
@Table(name = "council_topics", uniqueConstraints = {
        @UniqueConstraint(name = "uq_council_topics_topic", columnNames = "topic_id"),
        @UniqueConstraint(name = "uq_council_topics_pair", columnNames = {"council_id", "topic_id"})
})
public class CouncilTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "added_at", insertable = false, updatable = false)
    private LocalDateTime addedAt;

    public CouncilTopic() {
    }

    public CouncilTopic(Council council, Topic topic) {
        this.council = council;
        this.topic = topic;
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

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }
}