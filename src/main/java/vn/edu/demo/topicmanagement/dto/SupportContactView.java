package vn.edu.demo.topicmanagement.dto;

/**
 * Thông tin liên hệ quản trị viên, dùng để hiển thị cho người dùng khi cần trợ giúp
 * (quên mật khẩu, tài khoản bị khoá, cần đặt lại mật khẩu...).
 *
 * <p>Gồm hai phần:
 * <ul>
 *   <li>Thông tin tài khoản quản trị lấy từ bảng {@code users} (tên, email, điện thoại).</li>
 *   <li>Thông tin đầu mối hỗ trợ của đơn vị lấy từ cấu hình {@code app.support.*}
 *       (địa chỉ, giờ làm việc, hướng dẫn liên hệ).</li>
 * </ul>
 *
 * @param adminFullName  họ tên quản trị viên (null nếu chưa có tài khoản admin nào)
 * @param adminEmail     email quản trị viên
 * @param adminPhone     điện thoại quản trị viên
 * @param unitName       tên đơn vị chịu trách nhiệm hỗ trợ
 * @param address        địa chỉ trụ sở
 * @param phone          điện thoại bộ phận hỗ trợ
 * @param email          email bộ phận hỗ trợ
 * @param officeHours    giờ làm việc
 * @param instruction    hướng dẫn cách liên hệ
 */
public record SupportContactView(
        String adminFullName,
        String adminEmail,
        String adminPhone,
        String unitName,
        String address,
        String phone,
        String email,
        String officeHours,
        String instruction) {
}
