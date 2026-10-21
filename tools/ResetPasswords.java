import java.sql.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ResetPasswords {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/student_topic_management"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh";
        String user = "root";
        String pass = "123456";
        String rawPassword = args.length > 0 ? args[0] : "123456";

        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        // DelegatingPasswordEncoder yeu cau prefix, vi du: {bcrypt}$2a$10$...
        java.util.Map<String, org.springframework.security.crypto.password.PasswordEncoder> encoders =
                new java.util.HashMap<>();
        encoders.put("bcrypt", bcrypt);
        org.springframework.security.crypto.password.DelegatingPasswordEncoder encoder =
                new org.springframework.security.crypto.password.DelegatingPasswordEncoder("bcrypt", encoders);

        // Kiem tra hash co the su dung khong
        String sample = encoder.encode(rawPassword);
        if (!encoder.matches(rawPassword, sample)) {
            throw new IllegalStateException("Bo ma bcrypt khong khop!");
        }
        if (!bcrypt.matches(rawPassword, sample.substring("{bcrypt}".length()))) {
            throw new IllegalStateException("Hash bcrypt thuoc khong khop!");
        }
        System.out.println("Hash mau (khong ghi DB): " + sample);

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            // Moi user mot hash rieng (salt khac nhau)
            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT id, username, password_hash FROM users ORDER BY id");
                 ResultSet rs = select.executeQuery()) {

                int total = 0;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String username = rs.getString("username");
                    String oldHash = rs.getString("password_hash");

                    String newHash = encoder.encode(rawPassword);

                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE users SET password_hash = ? WHERE id = ?")) {
                        update.setString(1, newHash);
                        update.setInt(2, id);
                        update.executeUpdate();
                    }

                    total++;
                    boolean same = oldHash != null && oldHash.equals(newHash);
                    System.out.printf("id=%-3d %-14s cu=[%s] moi=%s%n",
                            id, username, oldHash, same ? "trung" : "da doi");
                }
                System.out.println("--- Da cap nhat " + total + " tai khoan ---");
            }

            // Xac nhan: thu decode lai 3 tai khoan dau
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT username, password_hash FROM users ORDER BY id LIMIT 3");
                 ResultSet rs = check.executeQuery()) {
                System.out.println("--- Xac nhan ---");
                while (rs.next()) {
                    String username = rs.getString("username");
                    String hash = rs.getString("password_hash");
                    boolean ok = encoder.matches(rawPassword, hash);
                    System.out.printf("%-14s len=%d matches=%s%n", username, hash.length(), ok);
                }
            }
        }
    }
}