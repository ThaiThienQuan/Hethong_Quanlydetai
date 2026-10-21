package vn.edu.demo.topicmanagement.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.demo.topicmanagement.dto.ReportDownload;
import vn.edu.demo.topicmanagement.service.StudentReportService;

/** Provides report downloads only to lecturers assigned as reviewers or council members. */
@Controller
@RequestMapping("/lecturer/reports")
public class LecturerReportController {
    private final StudentReportService reportService;

    public LecturerReportController(StudentReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String listSupervisedReports(Principal principal, Model model) {
        model.addAttribute("reports", reportService.getReportsForSupervisor(principal.getName()));
        return "lecturer/reports/list";
    }

    @GetMapping("/{registrationId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long registrationId, Principal principal) throws IOException {
        ReportDownload download = reportService.getDownloadForLecturer(registrationId, principal.getName());
        Path path = download.getPath();
        String contentType = Files.probeContentType(path);
        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(path));
    }
}
