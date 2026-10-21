# Import dữ liệu mẫu — bắt buộc chạy ResetPasswords

File `database/HT_QLDT.sql` chứa 31 tài khoản (`admin`, `truongkhoa`, `gv01..08`, `sv01..21`),
nhưng **SQL không thể sinh BCrypt hash**. Tác giả file SQL chừa sẵn placeholder
`{bcrypt}@@HASH@@`, đòi hỏi một bước sinh hash phía sau khi import.

`SecurityConfig` dùng `PasswordEncoderFactories.createDelegatingPasswordEncoder()`,
nghĩa là mọi hash **bắt buộc phải có prefix** (`{bcrypt}`, `{noop}`, ...). Nếu hash
không hợp lệ, mọi tài khoản đều sẽ nhận lỗi
`IllegalArgumentException: ... must have a password encoding prefix`.

## Chạy nhanh (Windows / PowerShell)

```powershell
cd C:\Users\ducma\Desktop\Web_vhvl\Hethong_Quanlydetai

# 1. Import schema + data (cần mysql client hoặc Workbench)
mysql -h 127.0.0.1 -u root -p123456 student_topic_management < database\HT_QLDT.sql

# 2. Chạy tool sinh hash
.\scripts\reset-passwords.ps1 -Password "123456"
```

Sau bước 2, đăng nhập `admin / 123456` là vào được hệ thống.

## Tool sinh hash

Lớp `tools/ResetPasswords.java` dùng chính `BCryptPasswordEncoder` của Spring +
`DelegatingPasswordEncoder` để sinh ra chuỗi `{bcrypt}$2a$10$...` tương thích `100%`
với cấu hình hiện tại, rồi cập nhật `password_hash` cho toàn bộ bảng `users`.
Mỗi user một salt riêng (`matches()` luôn đúng).

Có thể đổi sang mật khẩu khác bằng cách truyền tham số đầu tiên:

```powershell
.\scripts\reset-passwords.ps1 -Password "Demo@2026"
```
