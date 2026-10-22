package vn.edu.demo.topicmanagement.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang Style Guide (UI-3) — chỉ dùng cho demo/thiết kế, KHÔNG thuộc nghiệp vụ.
 *
 * <ul>
 *   <li>Chỉ tồn tại khi chạy với profile {@code demo} (spring.profiles.active=demo).</li>
 *   <li>Chỉ ADMIN được truy cập.</li>
 *   <li>Không đụng entity / repository / service / database.</li>
 * </ul>
 */
@Profile("demo")
@Controller
public class StyleGuideController {

    @GetMapping("/ui/style-guide")
    @PreAuthorize("hasRole('ADMIN')")
    public String styleGuide() {
        return "ui/style-guide";
    }
}
