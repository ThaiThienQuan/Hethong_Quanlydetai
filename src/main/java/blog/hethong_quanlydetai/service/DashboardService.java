package blog.hethong_quanlydetai.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    @PersistenceContext
    private EntityManager entityManager;

    public DashboardData load() {
        return new DashboardData(
                count("users"),
                count("departments"),
                count("students"),
                count("lecturers"),
                count("registration_periods"),
                count("topics"),
                count("topic_supervisors"),
                count("student_groups"),
                count("group_members"),
                count("topic_registrations"),
                count("reports"),
                count("councils"),
                count("council_members"),
                count("review_assignments"),
                count("evaluations"),
                count("review_results"),
                count("notifications"),
                periods(),
                topics(),
                notifications(),
                results());
    }

    private long count(String table) {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM " + table)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<PeriodView> periods() {
        return ((List<Object[]>) entityManager.createNativeQuery("""
                SELECT period_name, period_type, status, student_start_at, student_end_at
                FROM registration_periods
                ORDER BY student_start_at DESC
                LIMIT 5
                """).getResultList()).stream()
                .map(row -> new PeriodView(text(row[0]), text(row[1]), text(row[2]), text(row[3]), text(row[4])))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<TopicView> topics() {
        return ((List<Object[]>) entityManager.createNativeQuery("""
                SELECT t.topic_code, t.topic_name, t.status, d.department_name
                FROM topics t
                JOIN departments d ON d.department_id = t.department_id
                ORDER BY t.created_at DESC
                LIMIT 6
                """).getResultList()).stream()
                .map(row -> new TopicView(text(row[0]), text(row[1]), text(row[2]), text(row[3])))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<NoticeView> notifications() {
        return ((List<Object[]>) entityManager.createNativeQuery("""
                SELECT title, content, status
                FROM notifications
                ORDER BY created_at DESC
                LIMIT 4
                """).getResultList()).stream()
                .map(row -> new NoticeView(text(row[0]), text(row[1]), text(row[2])))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<ResultView> results() {
        return ((List<Object[]>) entityManager.createNativeQuery("""
                SELECT result, final_score, published
                FROM review_results
                ORDER BY result_id DESC
                LIMIT 5
                """).getResultList()).stream()
                .map(row -> new ResultView(text(row[0]), text(row[1]), Boolean.TRUE.equals(row[2])))
                .toList();
    }

    private String text(Object value) {
        return value == null ? "-" : value.toString();
    }

    public record DashboardData(
            long users,
            long departments,
            long students,
            long lecturers,
            long periods,
            long topics,
            long supervisors,
            long groups,
            long members,
            long registrations,
            long reports,
            long councils,
            long councilMembers,
            long assignments,
            long evaluations,
            long results,
            long notifications,
            List<PeriodView> periodRows,
            List<TopicView> topicRows,
            List<NoticeView> notificationRows,
            List<ResultView> resultRows) {
    }

    public record PeriodView(String name, String type, String status, String start, String end) {
    }

    public record TopicView(String code, String name, String status, String department) {
    }

    public record NoticeView(String title, String content, String status) {
    }

    public record ResultView(String result, String score, boolean published) {
    }
}
