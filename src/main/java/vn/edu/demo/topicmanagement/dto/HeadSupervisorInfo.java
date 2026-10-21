package vn.edu.demo.topicmanagement.dto;

/** Chỉ chứa thông tin cần hiển thị ở trang phân công GVHD, không trả entity JPA cho view. */
public record HeadSupervisorInfo(
        String topicTitle,
        String periodName,
        String statusLabel,
        String currentSupervisors,
        Long primarySupervisorId,
        Long coSupervisorId) { }
