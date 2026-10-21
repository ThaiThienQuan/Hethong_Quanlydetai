package vn.edu.demo.topicmanagement.controller;

import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.StudentResultView;
import vn.edu.demo.topicmanagement.service.CouncilScoringService;

/**
 * Trang "Kết quả của tôi" cho sinh viên (mục 5.8).
 *
 * <p>Bộ chọn đợt <b>chỉ gồm các đợt SV có nhóm tham gia</b>, lấy từ
 * {@code group_members.period_id} theo {@code student_id} của người đang đăng nhập —
 * không tin tham số {@code periodId} từ client. Truy cập {@code periodId} của đợt
 * SV không tham gia sẽ bị 403, kể cả khi đoán URL hay gọi API.
 */
@Controller
@RequestMapping("/student/results")
@PreAuthorize("hasAuthority('RESULT_VIEW')")
public class StudentResultController {
    private final CouncilScoringService scoringService;

    public StudentResultController(CouncilScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @GetMapping
    public String list(Principal principal, Model model,
                       @RequestParam(required = false) Long periodId) {
        model.addAttribute("periods", scoringService.getPeriodsForStudent(principal.getName()));
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("results", scoringService.getResultsForStudent(principal.getName(), periodId));
        return "student/results/list";
    }

    /** API cho phép sinh viên tra cứu kết quả của chính mình theo đợt. */
    @RestController
    @RequestMapping("/api/my/results")
    @PreAuthorize("hasAuthority('RESULT_VIEW')")
    static class MyResultsApi {

        private final CouncilScoringService scoringService;

        MyResultsApi(CouncilScoringService scoringService) {
            this.scoringService = scoringService;
        }

        @GetMapping
        public ResponseEntity<?> myResults(Principal principal,
                                          @RequestParam(required = false) Long periodId) {
            try {
                List<StudentResultView> results =
                        scoringService.getResultsForStudent(principal.getName(), periodId);
                return ResponseEntity.ok(results);
            } catch (ResponseStatusException e) {
                return ResponseEntity.status(HttpStatus.valueOf(e.getStatusCode().value()))
                        .body(java.util.Map.of("error", e.getReason() == null ? "Không được phép" : e.getReason()));
            }
        }

        /** Danh sách đợt mà sinh viên đang đăng nhập có nhóm tham gia. */
        @GetMapping("/periods")
        public ResponseEntity<?> myPeriods(Principal principal) {
            return ResponseEntity.ok(scoringService.getPeriodsForStudent(principal.getName()).stream()
                    .map(p -> java.util.Map.of(
                            "id", p.getId(),
                            "name", p.getName(),
                            "periodType", p.getTypeLabel()))
                    .toList());
        }
    }
}