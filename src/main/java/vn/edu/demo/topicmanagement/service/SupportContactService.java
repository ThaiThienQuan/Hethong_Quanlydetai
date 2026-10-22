package vn.edu.demo.topicmanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.config.AppProperties;
import vn.edu.demo.topicmanagement.dto.SupportContactView;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Lấy thông tin liên hệ quản trị viên để hiển thị cho người dùng.
 *
 * <p>Vì sao cần service riêng: thông tin liên hệ xuất hiện ở nhiều nơi (trang đăng nhập,
 * trang đổi mật khẩu, hồ sơ cá nhân, trang trợ giúp). Gom vào một chỗ để:
 * <ul>
 *   <li>không lặp lại logic "tìm tài khoản ADMIN" ở từng controller;</li>
 *   <li>chỉ truy vấn DB một lần mỗi lần render, thay vì mỗi template tự gọi repository;</li>
 *   <li>thông tin cấu hình (địa chỉ, giờ làm việc) đổi được trong {@code application.properties}.</li>
 * </ul>
 *
 * <p>Thông tin tài khoản admin lấy từ bảng {@code users} nên luôn khớp với dữ liệu thật;
 * nếu chưa có tài khoản admin nào thì trả về {@code null} cho phần tài khoản và
 * vẫn hiển thị được thông tin đầu mối từ cấu hình.
 */
@Service
public class SupportContactService {

    private final UserRepository userRepository;
    private final AppProperties props;

    public SupportContactService(UserRepository userRepository, AppProperties props) {
        this.userRepository = userRepository;
        this.props = props;
    }

    /**
     * Thông tin liên hệ đầy đủ: tài khoản quản trị viên (nếu có) + đầu mối hỗ trợ từ cấu hình.
     */
    @Transactional(readOnly = true)
    public SupportContactView getContact() {
        AppProperties.Support s = props.getSupport();
        User admin = findPrimaryAdmin();
        return new SupportContactView(
                admin == null ? null : admin.getFullName(),
                admin == null ? s.getEmail() : admin.getEmail(),
                admin == null ? s.getPhone() : admin.getPhone(),
                s.getUnitName(),
                s.getAddress(),
                s.getPhone(),
                s.getEmail(),
                s.getOfficeHours(),
                s.getInstruction());
    }

    /**
     * Tài khoản quản trị viên chính: tài khoản ADMIN đang hoạt động, ưu tiên tài khoản có
     * điện thoại (vì người dùng cần gọi) rồi đến tài khoản tạo sớm nhất.
     */
    @Transactional(readOnly = true)
    public User findPrimaryAdmin() {
        List<User> admins = userRepository.findEnabledAdmins();
        if (admins.isEmpty()) {
            return null;
        }
        return admins.stream()
                .filter(u -> u.getPhone() != null && !u.getPhone().isBlank())
                .findFirst()
                .orElse(admins.get(0));
    }
}
