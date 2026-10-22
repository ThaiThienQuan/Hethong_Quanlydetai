package vn.edu.demo.topicmanagement.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.demo.topicmanagement.dto.SupportContactView;
import vn.edu.demo.topicmanagement.service.SupportContactService;

/**
 * Cấp sẵn dữ liệu dùng chung cho mọi trang.
 *
 * <p>Hiện chỉ có {@code supportContact} - thông tin liên hệ quản trị viên - vì nó được
 * nhắc tới ở nhiều nơi: trang đăng nhập, trang đổi mật khẩu, hồ sơ cá nhân, trang hỗ trợ.
 * Khai báo ở {@code @ControllerAdvice} giúp mọi template đều có sẵn biến này mà không
 * phải thêm {@code model.addAttribute} vào từng controller.
 *
 * <p>Đặt ở đây (thay vì trong từng controller) để trang đăng nhập - vốn không đi qua
 * controller nào có thêm thuộc tính riêng - cũng dùng được.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final SupportContactService supportContactService;

    public GlobalModelAttributes(SupportContactService supportContactService) {
        this.supportContactService = supportContactService;
    }

    /**
     * Thông tin liên hệ quản trị viên.
     *
     * @return thông tin liên hệ, hoặc {@code null} nếu không lấy được
     *         (template đã kiểm tra null nên không lỗi)
     */
    @ModelAttribute("supportContact")
    public SupportContactView supportContact() {
        try {
            return supportContactService.getContact();
        } catch (RuntimeException e) {
            // Không để lỗi truy vấn thông tin liên hệ làm hỏng cả trang.
            return null;
        }
    }
}
