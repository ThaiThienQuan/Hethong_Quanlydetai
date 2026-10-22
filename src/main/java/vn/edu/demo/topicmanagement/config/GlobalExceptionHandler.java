package vn.edu.demo.topicmanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Xử lý lỗi tập trung (mục 7.8 - "trang lỗi 403/404/500 thân thiện, không lộ stack trace").
 *
 * <p>Nguyên tắc:
 * <ul>
 *   <li>Lỗi nghiệp vụ ({@code IllegalArgumentException}, {@code IllegalStateException},
 *       {@code ResponseStatusException}) → trả về thông báo tiếng Việt rõ ràng, không lộ SQL/stack trace.</li>
 *   <li>Lỗi không mong đợi → ghi log server-side, trả về thông báo chung chung cho người dùng.</li>
 *   <li>Không bao giờ hiển thị stack trace cho người dùng cuối.</li>
 * </ul>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Lỗi nghiệp vụ có thông điệp tiếng Việt sẵn → chuyển thành flash message rồi quay lại trang đang gọi.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleBusinessError(Exception e, RedirectAttributes redirect, HttpServletRequest request) {
        redirect.addFlashAttribute("error", safeMessage(e.getMessage()));
        return "redirect:" + refererPath(request);
    }

    /** Lỗi có mã HTTP cụ thể (403, 404...) → trả về trang lỗi tương ứng, kèm mã HTTP đúng. */
    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatus(ResponseStatusException e, HttpServletRequest request,
                                      jakarta.servlet.http.HttpServletResponse response) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        String reason = safeMessage(e.getReason());
        if (status == HttpStatus.NOT_FOUND) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "error/404";
        }
        if (status == HttpStatus.FORBIDDEN) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return "error/403";
        }
        log.warn("Lỗi nghiệp vụ HTTP {} tại {}: {}", e.getStatusCode().value(), request.getRequestURI(), reason);
        response.setStatus(e.getStatusCode().value());
        return "error/500";
    }

    /** Không đủ quyền → trang 403. */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException e) {
        return "error/403";
    }

    /**
     * Đường dẫn không tồn tại (gõ sai URL) → trang 404 thay vì trang lỗi chung 500.
     * Chỉ đổi mã HTTP + template trả về, không đụng nghiệp vụ.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResource(NoResourceFoundException e) {
        return "error/404";
    }

    /** Sai thông tin đăng nhập → về trang đăng nhập. */
    @ExceptionHandler(AuthenticationException.class)
    public String handleAuthentication(AuthenticationException e, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "Sai tài khoản hoặc mật khẩu hoặc tài khoản bị khóa.");
        return "redirect:/login";
    }

    /** Lỗi không mong đợi: log đầy đủ server-side, trả thông báo chung chung. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Lỗi hệ thống tại {}: {}", request.getRequestURI(), e.getMessage(), e);
        return "error/500";
    }

    /**
     * Đường dẫn quay lại: dùng Referer nếu có và là đường dẫn nội bộ, ngược lại về trang chủ.
     * Tránh open-redirect (không redirect ra ngoài hệ thống).
     */
    private String refererPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) return "/";
        try {
            java.net.URI uri = java.net.URI.create(referer);
            String path = uri.getPath();
            if (path == null || path.isBlank()) return "/";
            String ctx = request.getContextPath();
            if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
                path = path.substring(ctx.length());
            }
            if (path.isEmpty()) return "/";
            if (!path.startsWith("/")) return "/";
            // Chặn open-redirect: chỉ cho đường dẫn nội bộ.
            if (path.contains("//") || path.contains(":")) return "/";
            return path;
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }

    /**
     * Lọc thông điệp lỗi: bỏ thông tin kỹ thuật (stack trace, câu SQL, tên lớp) và giới hạn độ dài.
     */
    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Đã xảy ra lỗi. Vui lòng thử lại hoặc liên hệ quản trị viên.";
        }
        String m = message.trim();
        if (m.length() > 300) m = m.substring(0, 300) + "...";
        // Loại bỏ dấu vết của lỗi SQL kỹ thuật.
        if (m.contains("SQL") || m.contains("Constraint") || m.contains("org.hibernate")
                || m.contains("java.lang.") || m.contains("Exception:")) {
            return "Thao tác không thực hiện được do vi phạm ràng buộc dữ liệu. Vui lòng kiểm tra lại thông tin.";
        }
        return m;
    }
}
