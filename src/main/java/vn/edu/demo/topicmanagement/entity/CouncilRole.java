package vn.edu.demo.topicmanagement.entity;

/** Internal role values stay stable for persistence; displayName is for Vietnamese UI labels. */
public enum CouncilRole {
    CHAIRMAN("Chủ tịch hội đồng"),
    SECRETARY("Thư ký hội đồng"),
    MEMBER("Ủy viên");

    private final String displayName;

    CouncilRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
