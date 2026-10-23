package vn.edu.demo.topicmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.edu.demo.topicmanagement.dto.RegistrationPeriodForm;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.PeriodType;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.TopicRepository;
import vn.edu.demo.topicmanagement.service.RegistrationPeriodService;

/**
 * Unit tests for {@link RegistrationPeriodService} — section 5.2 business rules.
 *
 * <p>Each rule has at least one SUCCESS case and one REJECT case (rubric section 10).
 * All tests are pure unit tests using mocked repositories + fixed {@link Clock}
 * so they run deterministically regardless of when they are executed.
 */
class RegistrationPeriodServiceTest {

    private RegistrationPeriodRepository periodRepository;
    private TopicRepository topicRepository;
    private CouncilRepository councilRepository;
    private vn.edu.demo.topicmanagement.repository.UserRepository userRepository;
    private RegistrationPeriodService service;
    private Clock fixedClock;

    /** Fixed point-in-time used throughout: 2026-03-10 10:00 UTC+7. */
    private LocalDateTime NOW;

    @BeforeEach
    void setUp() {
        NOW = LocalDateTime.of(2026, 3, 10, 10, 0);
        // Zone must match Asia/Ho_Chi_Minh = UTC+7 so LocalDateTime and Instant align.
        fixedClock = Clock.fixed(
                NOW.toInstant(ZoneId.of("Asia/Ho_Chi_Minh").getRules().getOffset(NOW)),
                ZoneId.of("Asia/Ho_Chi_Minh"));

        periodRepository = mock(RegistrationPeriodRepository.class);
        topicRepository = mock(TopicRepository.class);
        councilRepository = mock(CouncilRepository.class);
        userRepository = mock(vn.edu.demo.topicmanagement.repository.UserRepository.class);
        service = new RegistrationPeriodService(periodRepository, topicRepository,
                councilRepository, userRepository, fixedClock);
    }

    // -------------------------------------------------------------------------
    // Helper: build a fully-populated RegistrationPeriod with id = 20L
    // -------------------------------------------------------------------------

    private RegistrationPeriod period(PeriodType type, PeriodStatus status,
                                     LocalDateTime ls, LocalDateTime le,
                                     LocalDateTime ss, LocalDateTime se,
                                     LocalDateTime review, LocalDateTime council) {
        RegistrationPeriod p = new RegistrationPeriod();
        p.setId(20L);
        p.setName("Test period");
        p.setPeriodType(type);
        p.setStatus(status);
        p.setLecturerStartAt(ls);
        p.setLecturerEndAt(le);
        p.setStudentStartAt(ss);
        p.setStudentEndAt(se);
        p.setReviewDeadline(review);
        p.setCouncilReportDate(council);
        p.setCreatedBy(new User());
        return p;
    }

    // -------------------------------------------------------------------------
    // 5.2 — validateTimes: start < end; SV phase starts after GV phase ends
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("5.2 — Thứ tự thời gian: bắt đầu < kết thúc; SV sau khi GV kết thúc")
    class ValidateTimes {

        @Test
        @DisplayName("SUCCESS — thời gian hợp lệ, publish thành công")
        void validTimesPublishes() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            assertDoesNotThrow(() -> service.publish(20L));
            verify(periodRepository).save(argThat(r -> r.getStatus() == PeriodStatus.PUBLISHED));
        }

        @Test
        @DisplayName("REJECT — GV: kết thúc không sau bắt đầu")
        void rejectGVEndBeforeStart() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.DRAFT,
                    NOW.plusDays(1), NOW.minusDays(1), // LE < LS
                    NOW.plusDays(2), NOW.plusDays(8),
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publish(20L));
            assertTrue(ex.getMessage().contains("bắt đầu"));
        }

        @Test
        @DisplayName("REJECT — SV: kết thúc không sau bắt đầu")
        void rejectSVEndBeforeStart() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(8), NOW.plusDays(1), // SE < SS
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publish(20L));
            assertTrue(ex.getMessage().contains("bắt đầu"));
        }

        @Test
        @DisplayName("REJECT — SV bắt đầu trước khi GV kết thúc")
        void rejectSVPhaseBeforeGVPhase() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.plusDays(1),  // GV ends tomorrow
                    NOW, NOW.plusDays(8),               // SV starts today, before tomorrow
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publish(20L));
            assertTrue(ex.getMessage().contains("sinh viên"));
        }
    }

    // -------------------------------------------------------------------------
    // 5.2 — validateOptionalDeadlines: GVPB / council date type rules
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("5.2 — Hạn chót GVPB & ngày hội đồng: chỉ TLCN/KLTN; sau thời gian SV")
    class ValidateOptionalDeadlines {

        @Test
        @DisplayName("SUCCESS — MON_HOC không cần GVPB hay ngày hội đồng")
        void monHocNoOptionalFields() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            assertDoesNotThrow(() -> service.publish(20L));
        }

        @Test
        @DisplayName("SUCCESS — TLCN với hạn chót GVPB hợp lệ (sau ngày kết thúc SV)")
        void TLCNWithValidReviewDeadline() {
            RegistrationPeriod p = period(PeriodType.TLCN, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),    // SV ends +8
                    NOW.plusDays(10), null);              // review +10 > +8
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            assertDoesNotThrow(() -> service.publish(20L));
        }

        @Test
        @DisplayName("SUCCESS — KLTN với hạn chót GVPB và ngày hội đồng hợp lệ")
        void KLTNWithValidDeadlines() {
            RegistrationPeriod p = period(PeriodType.KLTN, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),    // SV ends +8
                    NOW.plusDays(10), NOW.plusDays(15));  // review +10 > +8; council +15 > +10
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            assertDoesNotThrow(() -> service.publish(20L));
        }

        @Test
        @DisplayName("REJECT — MON_HOC có hạn chót GVPB (chỉ TLCN/KLTN được phép)")
        void rejectReviewDeadlineOnMonHoc() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),
                    NOW.plusDays(10), null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publish(20L));
            assertTrue(ex.getMessage().contains("TLCN") || ex.getMessage().contains("KLTN"));
        }

        @Test
        @DisplayName("REJECT — TLCN có ngày hội đồng (chỉ KLTN được phép)")
        void rejectCouncilDateOnTLCN() {
            RegistrationPeriod p = period(PeriodType.TLCN, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),
                    NOW.plusDays(10), NOW.plusDays(15));
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publish(20L));
            assertTrue(ex.getMessage().contains("KLTN"));
        }

        @Test
        @DisplayName("REJECT — TLCN hạn chót GVPB không sau ngày kết thúc SV")
        void rejectReviewBeforeStudentEnd() {
            RegistrationPeriod p = period(PeriodType.TLCN, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),  // SV ends +8
                    NOW.plusDays(5), null);            // review +5 < +8
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publish(20L));
            assertTrue(ex.getMessage().contains("sau ngày kết thúc đăng ký"));
        }

        @Test
        @DisplayName("REJECT — KLTN ngày hội đồng không sau hạn chót GVPB")
        void rejectCouncilBeforeReview() {
            RegistrationPeriod p = period(PeriodType.KLTN, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),
                    NOW.plusDays(10), NOW.plusDays(9)); // council +9 < review +10
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.publish(20L));
            assertTrue(ex.getMessage().contains("sau hạn chót"));
        }
    }

    // -------------------------------------------------------------------------
    // 5.2 — requireLecturerPhaseOpen: correct time window
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("5.2 — requireLecturerPhaseOpen: kiểm tra khung giảng viên")
    class RequireLecturerPhaseOpen {

        @Test
        @DisplayName("SUCCESS — PUBLISHED, đang trong khung GV (10 Mar 2026, giả định NOW = 10 Mar)")
        void lecturerPhaseIsOpen() {
            // NOW = 2026-03-10T10:00. GV window: 2026-03-08 to 2026-03-11.
            // NOW is between them, so the phase is open.
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.PUBLISHED,
                    NOW.minusDays(2), NOW.plusDays(1),   // 08 to 11
                    NOW.plusDays(2), NOW.plusDays(10),
                    null, null);
            assertDoesNotThrow(() -> service.requireLecturerPhaseOpen(p));
        }

        @Test
        @DisplayName("REJECT — CLOSED")
        void rejectClosed() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.CLOSED,
                    NOW.minusDays(2), NOW.plusDays(1),
                    NOW.plusDays(2), NOW.plusDays(10),
                    null, null);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.requireLecturerPhaseOpen(p));
            assertTrue(ex.getMessage().contains("đã đóng"));
        }

        @Test
        @DisplayName("REJECT — DRAFT")
        void rejectDraft() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.plusDays(1),
                    NOW.plusDays(2), NOW.plusDays(10),
                    null, null);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.requireLecturerPhaseOpen(p));
            assertTrue(ex.getMessage().contains("chưa được công bố"));
        }

        @Test
        @DisplayName("REJECT — chưa tới khung GV (NOW = 10 Mar, GV bắt đầu 11 Mar)")
        void rejectBeforeGVWindow() {
            // NOW = 2026-03-10T10:00, GV starts 2026-03-11.
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.PUBLISHED,
                    NOW.plusDays(1), NOW.plusDays(3),   // starts tomorrow
                    NOW.plusDays(4), NOW.plusDays(10),
                    null, null);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.requireLecturerPhaseOpen(p));
            assertTrue(ex.getMessage().contains("Chưa tới khung"));
        }

        @Test
        @DisplayName("REJECT — đã hết khung GV (NOW = 10 Mar, GV kết thúc 09 Mar)")
        void rejectAfterGVWindow() {
            // NOW = 2026-03-10T10:00, GV ended 2026-03-09.
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.PUBLISHED,
                    NOW.minusDays(3), NOW.minusDays(1), // ended yesterday
                    NOW.plusDays(1), NOW.plusDays(10),
                    null, null);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.requireLecturerPhaseOpen(p));
            assertTrue(ex.getMessage().contains("Đã hết khung"));
        }
    }

    // -------------------------------------------------------------------------
    // 5.2 — requireStudentPhaseOpen: correct time window
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("5.2 — requireStudentPhaseOpen: kiểm tra khung sinh viên")
    class RequireStudentPhaseOpen {

        @Test
        @DisplayName("SUCCESS — PUBLISHED, đang trong khung SV")
        void studentPhaseIsOpen() {
            // NOW = 2026-03-10T10:00. SV window: 2026-03-09 to 2026-03-15.
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.PUBLISHED,
                    NOW.minusDays(5), NOW.minusDays(1),   // GV ended
                    NOW.minusDays(1), NOW.plusDays(5),    // SV open now
                    null, null);
            assertDoesNotThrow(() -> service.requireStudentPhaseOpen(p));
        }

        @Test
        @DisplayName("REJECT — CLOSED")
        void rejectClosed() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.CLOSED,
                    NOW.minusDays(5), NOW.minusDays(1),
                    NOW.minusDays(1), NOW.plusDays(5),
                    null, null);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.requireStudentPhaseOpen(p));
            assertTrue(ex.getMessage().contains("đã đóng"));
        }

        @Test
        @DisplayName("REJECT — chưa tới khung SV (NOW = 10 Mar, SV bắt đầu 12 Mar)")
        void rejectBeforeSVWindow() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.PUBLISHED,
                    NOW.minusDays(5), NOW.minusDays(1),
                    NOW.plusDays(2), NOW.plusDays(10), // starts in 2 days
                    null, null);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.requireStudentPhaseOpen(p));
            assertTrue(ex.getMessage().contains("Chưa tới khung"));
        }

        @Test
        @DisplayName("REJECT — đã hết khung SV (NOW = 10 Mar, SV kết thúc 09 Mar)")
        void rejectAfterSVWindow() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.PUBLISHED,
                    NOW.minusDays(5), NOW.minusDays(2),
                    NOW.minusDays(1), NOW.minusDays(0), // bắt đầu hôm qua, kết thúc 09 Mar
                    null, null);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.requireStudentPhaseOpen(p));
            assertTrue(ex.getMessage().contains("Đã hết khung"));
        }
    }

    // -------------------------------------------------------------------------
    // 5.2 — publish: DRAFT -> PUBLISHED
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("5.2 — publish: DRAFT -> PUBLISHED")
    class Publish {

        @Test
        @DisplayName("SUCCESS — DRAFT chuyển PUBLISHED")
        void draftToPublished() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.DRAFT,
                    NOW.minusDays(2), NOW.plusDays(1),
                    NOW.plusDays(2), NOW.plusDays(10),
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            assertDoesNotThrow(() -> service.publish(20L));
            verify(periodRepository).save(argThat(r -> r.getStatus() == PeriodStatus.PUBLISHED));
        }

        @Test
        @DisplayName("REJECT — đợt đã PUBLISHED")
        void rejectAlreadyPublished() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.PUBLISHED,
                    NOW.minusDays(2), NOW.plusDays(1),
                    NOW.plusDays(2), NOW.plusDays(10),
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.publish(20L));
            assertTrue(ex.getMessage().contains("nháp"));
        }

        @Test
        @DisplayName("REJECT — đợt đã CLOSED")
        void rejectClosed() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.CLOSED,
                    NOW.minusDays(2), NOW.plusDays(1),
                    NOW.plusDays(2), NOW.plusDays(10),
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.publish(20L));
            assertTrue(ex.getMessage().contains("nháp"));
        }
    }

    // -------------------------------------------------------------------------
    // 5.2 — close: -> CLOSED
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("5.2 — close: -> CLOSED")
    class Close {

        @Test
        @DisplayName("SUCCESS — PUBLISHED -> CLOSED")
        void publishedToClosed() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.PUBLISHED,
                    NOW.minusDays(5), NOW.minusDays(1),
                    NOW.minusDays(1), NOW.plusDays(2),
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            assertDoesNotThrow(() -> service.close(20L));
            verify(periodRepository).save(argThat(r -> r.getStatus() == PeriodStatus.CLOSED));
        }

        @Test
        @DisplayName("REJECT — đợt đã CLOSED")
        void rejectAlreadyClosed() {
            RegistrationPeriod p = period(PeriodType.MON_HOC, PeriodStatus.CLOSED,
                    NOW.minusDays(5), NOW.minusDays(1),
                    NOW.minusDays(1), NOW.plusDays(2),
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(p));
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.close(20L));
            assertTrue(ex.getMessage().contains("đã đóng"));
        }
    }

    // -------------------------------------------------------------------------
    // 5.2 — update: PUBLISHED only changes time, not type
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("5.2 — update: PUBLISHED chỉ sửa mốc thời gian, không đổi loại đợt")
    class UpdateTypeChange {

        @Test
        @DisplayName("REJECT — PUBLISHED đổi loại đợt")
        void rejectTypeChange() {
            RegistrationPeriod existing = period(PeriodType.MON_HOC, PeriodStatus.PUBLISHED,
                    NOW.minusDays(5), NOW.minusDays(1),
                    NOW.minusDays(1), NOW.plusDays(2),
                    null, null);
            when(periodRepository.findById(20L)).thenReturn(Optional.of(existing));
            when(topicRepository.countByPeriodId(20L)).thenReturn(0L);

            RegistrationPeriod updated = period(PeriodType.KLTN, PeriodStatus.PUBLISHED,
                    NOW.minusDays(5), NOW.minusDays(1),
                    NOW.minusDays(1), NOW.plusDays(2),
                    NOW.plusDays(5), NOW.plusDays(10));
            updated.setId(20L);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.update(updated));
            assertTrue(ex.getMessage().contains("mốc thời gian"));
        }
    }

    // -------------------------------------------------------------------------
    // 5.2 — checkRules: lỗi phải được gán đúng ô trên form
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("5.2 — checkRules: gán lỗi đúng field")
    class CheckRulesFieldMapping {

        private RegistrationPeriodForm form(PeriodType type, LocalDateTime ls, LocalDateTime le,
                                            LocalDateTime ss, LocalDateTime se,
                                            LocalDateTime review, LocalDateTime council) {
            RegistrationPeriodForm f = new RegistrationPeriodForm();
            f.setName("Đợt test");
            f.setType(type);
            f.setLecturerStart(ls);
            f.setLecturerEnd(le);
            f.setStudentStart(ss);
            f.setStudentEnd(se);
            f.setReviewDeadline(review);
            f.setDefenseDate(council);
            return f;
        }

        @Test
        @DisplayName("REJECT — GV kết thúc trước bắt đầu -> lỗi ở lecturerEnd")
        void errorOnLecturerEnd() {
            Map<String, String> errors = service.checkRules(form(PeriodType.MON_HOC,
                    NOW.plusDays(1), NOW.minusDays(1),
                    NOW.plusDays(2), NOW.plusDays(8),
                    null, null));
            assertTrue(errors.containsKey("lecturerEnd"));
            assertFalse(errors.containsKey("name"));
        }

        @Test
        @DisplayName("REJECT — SV bắt đầu trước khi GV kết thúc -> lỗi ở studentStart")
        void errorOnStudentStart() {
            Map<String, String> errors = service.checkRules(form(PeriodType.MON_HOC,
                    NOW.minusDays(2), NOW.plusDays(1),
                    NOW, NOW.plusDays(8),
                    null, null));
            assertTrue(errors.containsKey("studentStart"));
        }

        @Test
        @DisplayName("REJECT — MON_HOC có hạn GVPB -> lỗi ở reviewDeadline")
        void errorOnReviewDeadline() {
            Map<String, String> errors = service.checkRules(form(PeriodType.MON_HOC,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),
                    NOW.plusDays(10), null));
            assertTrue(errors.containsKey("reviewDeadline"));
        }

        @Test
        @DisplayName("REJECT — TLCN có ngày báo cáo hội đồng -> lỗi ở defenseDate")
        void errorOnDefenseDate() {
            Map<String, String> errors = service.checkRules(form(PeriodType.TLCN,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),
                    NOW.plusDays(10), NOW.plusDays(15)));
            assertTrue(errors.containsKey("defenseDate"));
        }

        @Test
        @DisplayName("SUCCESS — KLTN hợp lệ -> không có lỗi")
        void noErrorForValidKltn() {
            Map<String, String> errors = service.checkRules(form(PeriodType.KLTN,
                    NOW.minusDays(2), NOW.minusDays(1),
                    NOW.plusDays(1), NOW.plusDays(8),
                    NOW.plusDays(10), NOW.plusDays(15)));
            assertTrue(errors.isEmpty(), "Lỗi không mong đợi: " + errors);
        }
    }

    // -------------------------------------------------------------------------
    // 5.2 — delete: only when no topics exist
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("5.2 — delete: chỉ xóa được khi chưa có đề tài")
    class Delete {

        @Test
        @DisplayName("REJECT — đợt có đề tài")
        void rejectDeleteWithTopics() {
            when(topicRepository.countByPeriodId(20L)).thenReturn(3L);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.delete(20L));
            assertTrue(ex.getMessage().contains("đã có đề tài"));
        }
    }
}
