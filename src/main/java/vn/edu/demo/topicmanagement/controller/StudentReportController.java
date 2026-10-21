package vn.edu.demo.topicmanagement.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.demo.topicmanagement.dto.ReportDownload;
import vn.edu.demo.topicmanagement.service.StudentReportService;

/** Trang nộp báo cáo dành cho nhóm sinh viên; Spring Security bảo vệ toàn bộ URL /student/**. */
@Controller
@RequestMapping("/student/reports")
public class StudentReportController {
    private final StudentReportService reportService;

    public StudentReportController(StudentReportService reportService) { this.reportService = reportService; }

    @GetMapping
    public String page(Principal principal, Model model) {
        model.addAttribute("reportView", reportService.getOverview(principal.getName()));
        return "student/reports/page";
    }

    @PostMapping
    public String submit(@RequestParam("file") MultipartFile file, Principal principal, RedirectAttributes redirect) {
        try {
            reportService.submit(principal.getName(), file);
            redirect.addFlashAttribute("message", "Đã nộp báo cáo.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/reports";
    }

    @GetMapping("/{registrationId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long registrationId, Principal principal) throws IOException {
        ReportDownload download = reportService.getDownload(registrationId, principal.getName());
        Path path = download.getPath();
        Resource resource = new FileSystemResource(path);
        String contentType = java.nio.file.Files.probeContentType(path);
        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }
}
