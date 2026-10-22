import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class CleanupTestCouncils {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/student_topic_management"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh";
        try (Connection c = DriverManager.getConnection(url, "root", "123456")) {
            // Xoa cac hoi dong test vua tao (ten chua "test")
            String del = "DELETE FROM councils WHERE council_name LIKE '%test%'";
            try (PreparedStatement ps = c.prepareStatement(del)) {
                System.out.println("Deleted rows: " + ps.executeUpdate());
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery(
                     "SELECT council_code, council_name FROM councils ORDER BY council_code")) {
                System.out.println("--- Danh sach hoi dong con lai ---");
                while (rs.next()) {
                    System.out.println(rs.getString(1) + "  |  " + rs.getString(2));
                }
            }
        }
    }
}