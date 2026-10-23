package vn.edu.demo.topicmanagement.dto;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.demo.topicmanagement.entity.CouncilStatus;

/** Bản trình bày hội đồng với danh sách thành viên/đề tài đã chuyển thành chuỗi. */
public class CouncilSummaryView {
    private final Long id;
    private final Long periodId;
    private final String councilCode;
    private final String name;
    private final String periodName;
    private final LocalDateTime reportDate;
    private final String room;
    private final CouncilStatus status;
    private final List<String> members;
    private final List<String> topics;
    /** Đợt KLTN: ngày báo cáo hội đồng lấy theo ngày của đợt (mục 5.6). */
    private final boolean kltn;

    public CouncilSummaryView(Long id, String name, String periodName, LocalDateTime reportDate, String room,
                              CouncilStatus status, List<String> members, List<String> topics) {
        this(id, null, null, name, periodName, reportDate, room, status, members, topics);
    }

    public CouncilSummaryView(Long id, String councilCode, String name, String periodName,
                              LocalDateTime reportDate, String room, CouncilStatus status,
                              List<String> members, List<String> topics) {
        this(id, null, councilCode, name, periodName, reportDate, room, status, members, topics);
    }

    public CouncilSummaryView(Long id, Long periodId, String councilCode, String name, String periodName,
                              LocalDateTime reportDate, String room, CouncilStatus status,
                              List<String> members, List<String> topics) {
        this(id, periodId, councilCode, name, periodName, reportDate, room, status, members, topics, false);
    }

    public CouncilSummaryView(Long id, Long periodId, String councilCode, String name, String periodName,
                              LocalDateTime reportDate, String room, CouncilStatus status,
                              List<String> members, List<String> topics, boolean kltn) {
        this.id = id; this.periodId = periodId; this.councilCode = councilCode; this.name = name;
        this.periodName = periodName; this.reportDate = reportDate;
        this.room = room; this.status = status;
        this.members = members == null ? List.of() : List.copyOf(members);
        this.topics = topics == null ? List.of() : List.copyOf(topics);
        this.kltn = kltn;
    }

    public Long getId() { return id; }
    public Long getCouncilId() { return id; }
    public Long getPeriodId() { return periodId; }
    public String getCouncilCode() { return councilCode == null ? "" : councilCode; }
    public String getName() { return name; }
    public String getPeriodName() { return periodName; }
    public LocalDateTime getReportDate() { return reportDate; }
    public LocalDateTime getDefenseDate() { return reportDate; } // tương thích cũ
    public String getRoom() { return room; }
    public CouncilStatus getStatus() { return status; }
    /** True khi hội đồng thuộc đợt KLTN - ngày báo cáo lấy theo đợt. */
    public boolean isKltn() { return kltn; }
    public List<String> getMembers() { return members; }
    public List<String> getTopics() { return topics; }
}
