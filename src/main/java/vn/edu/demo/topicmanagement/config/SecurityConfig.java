package vn.edu.demo.topicmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình bảo mật (mục 5.1).
 *
 * <p>Bảo mật dữ liệu:
 * <ul>
 *   <li>Mật khẩu băm bằng {@link PasswordEncoderFactories} → BCrypt, không lưu mật khẩu rõ.</li>
 *   <li>CSRF bật cho mọi form POST.</li>
 *   <li>Session cố định khi đăng nhập (chống session fixation).</li>
 *   <li>Đăng xuất bằng POST có CSRF.</li>
 *   <li>Trang lỗi 403/404/500 thân thiện, không lộ stack trace.</li>
 * </ul>
 *
 * <p>Phân quyền chi tiết theo từng quyền được kiểm tra ở controller bằng
 * {@code @PreAuthorize("hasAuthority('...')")} (xem {@code SecurityConfig} + service layer).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final LoginFailureHandler loginFailureHandler;
    private final LoginSuccessHandler loginSuccessHandler;

    public SecurityConfig(LoginFailureHandler loginFailureHandler, LoginSuccessHandler loginSuccessHandler) {
        this.loginFailureHandler = loginFailureHandler;
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Trang đăng nhập và tài nguyên tĩn: ai cũng truy cập được.
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error/**").permitAll()

                // ---------- Phân quyền theo URL (mục 5.2 / 5.3 / ...) ----------
                // Dùng hasAuthority(<PERMISSION>) khớp với bảng role_permissions trong
                // database/HT_QLDT.sql, nên đổi phân quyền trên màn hình "Vai trò &
                // Phân quyền" có hiệu lực ngay mà không phải sửa code.
                // ADMIN nhiều quyền hơn TRUONG_KHOA theo danh mục nên vẫn vào được.

                // Danh mục: bộ môn + hồ sơ GV/SV  (5.1b)
                .requestMatchers("/admin/departments/**", "/admin/profiles/**")
                    .hasAnyAuthority("DEPARTMENT_MANAGE", "PROFILE_MANAGE")

                // Quản trị: tài khoản, vai trò, phân quyền, thông báo nhà trường  (5.1 / 5.9)
                .requestMatchers("/admin/users/**").hasAuthority("USER_MANAGE")
                .requestMatchers("/roles/**").hasAuthority("ROLE_MANAGE")
                .requestMatchers("/admin/announcements/**", "/announcements/manage/**")
                    .hasAuthority("NOTICE_MANAGE")

                // Đợt đăng ký - Trưởng khoa + admin  (5.2, PERIOD_MANAGE)
                .requestMatchers("/periods/**").hasAuthority("PERIOD_MANAGE")

                // Duyệt đề tài (TOPIC_APPROVE) & phân công GVHD (SUPERVISOR_ASSIGN)  (5.3)
                // Hai quyền tách riêng để việc gán quyền trên màn hình "Phân quyền" có hiệu lực.
.requestMatchers("/head/topics/*/approve", "/head/topics/*/reject",
                                 "/head/topics/*/publish", "/head/topics/publish-by-period",
                                 "/head/topics/*/delete")
                    .hasAuthority("TOPIC_APPROVE")
                .requestMatchers("/head/topics/*/supervisors").hasAuthority("SUPERVISOR_ASSIGN")
                .requestMatchers("/head/topics/**").hasAuthority("TOPIC_VIEW")

                // Phân công người chấm  (5.7, ASSIGNMENT_MANAGE)
                .requestMatchers("/head/reviewers/**").hasAuthority("ASSIGNMENT_MANAGE")

                // Lập hội đồng  (5.6, COUNCIL_MANAGE)
                .requestMatchers("/head/councils/**").hasAuthority("COUNCIL_MANAGE")

                // Công bố kết quả  (5.8, RESULT_PUBLISH)
                .requestMatchers("/head/results/**").hasAuthority("RESULT_PUBLISH")

                // Nhật ký email kết quả + gửi lại (7.10) - cùng nhóm quyền RESULT_PUBLISH
                .requestMatchers("/head/emails/**").hasAuthority("RESULT_PUBLISH")

                // Thông báo của khoa  (5.9)
                .requestMatchers("/head/announcements/**").hasAuthority("NOTICE_MANAGE")

                // ---------- Màn hình của từng vai trò ----------
                // GV: đề xuất đề tài (TOPIC_CREATE), xem báo cáo, nhập điểm được giao.
                .requestMatchers("/lecturer/topics/new", "/lecturer/topics/*/edit",
                                 "/lecturer/topics/*/delete")
                    .hasAuthority("TOPIC_CREATE")
.requestMatchers("/lecturer/topics/**").hasAuthority("TOPIC_VIEW")
                .requestMatchers("/lecturer/reports/**").hasAuthority("REPORT_VIEW")
                .requestMatchers("/lecturer/reviews/**", "/lecturer/council-work/**")
                    .hasAuthority("TOPIC_VIEW")
                // GVHD duyệt/từ chối đăng ký của nhóm (5.4) - chỉ GVHD mới thấy.
                .requestMatchers("/lecturer/registrations/**").hasAuthority("REGISTRATION_CONFIRM")

                // SV: nhóm, đăng ký đề tài, nộp báo cáo, xem kết quả.
                .requestMatchers("/student/groups/**", "/student/registrations/**")
                    .hasAuthority("REGISTRATION_CREATE")
                // Nộp báo cáo cần SUBMISSION_CREATE, nhưng tải báo cáo đã nộp thì mọi
                // thành viên trong nhóm đều được (đặc tả 5.5) → REPORT_VIEW.
                .requestMatchers("/student/reports/*/download").hasAuthority("REPORT_VIEW")
                .requestMatchers("/student/reports/**").hasAuthority("SUBMISSION_CREATE")
                .requestMatchers("/student/results/**").hasAuthority("RESULT_VIEW")
                // API tra cứu kết quả của SV - cùng quyền RESULT_VIEW.
                .requestMatchers("/api/my/results/**").hasAuthority("RESULT_VIEW")

                // Danh sách đề tài công bố + thông báo: mọi vai trò đã đăng nhập đều xem được.
                .requestMatchers("/topics/**", "/announcements/**").authenticated()

                // Style Guide (UI-3) - chỉ ADMIN, chỉ tồn tại ở profile "demo".
                .requestMatchers("/ui/**").hasRole("ADMIN")

                // Mọi trang còn lại đều yêu cầu đã đăng nhập.
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                // Bộ đếm đăng nhập sai + khóa tạm 5 lần/15 phút (mục 5.1).
                .failureHandler(loginFailureHandler)
                .successHandler(loginSuccessHandler)
                .permitAll())
            // Đăng xuất bằng POST, có kiểm tra CSRF.
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            // Khóa session cố định khi đăng nhập.
            .sessionManagement(session -> session
                .sessionFixation(sessionFixation -> sessionFixation.changeSessionId()))
            // Giữ mặc định an toàn của Spring Security, chỉ cho phép iframe cùng gốc (devtools).
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}