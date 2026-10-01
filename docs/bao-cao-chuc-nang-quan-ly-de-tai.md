# Báo cáo chức năng hệ thống quản lý đề tài khoa CNTT

## 1. Mục tiêu hệ thống
Hệ thống quản lý đề tài khoa CNTT hỗ trợ toàn bộ quy trình từ khi giảng viên đề xuất và duyệt đề tài, sinh viên đăng ký, thực hiện, nộp bài, phản biện, chấm điểm, tính điểm trung bình và công bố kết quả. Mỗi vai trò trong hệ thống chỉ được truy cập các chức năng phù hợp với nhiệm vụ của mình, không cho phép thao tác vượt quyền.

## 2. Vai trò và quyền truy cập

### 2.1 Admin
- Quản lý người dùng
- Quản lý vai trò và quyền
- Cấu hình hệ thống
- Nhật ký và báo cáo tổng quan
- Gửi thông báo hệ thống

### 2.2 Chủ tịch hội đồng
- Duyệt đề tài do giảng viên đề xuất
- Phân công giảng viên chấm cho đề tài
- Tính điểm trung bình
- Công bố kết quả cho sinh viên
- Quản lý đợt đăng ký, hồ sơ và báo cáo

### 2.3 Giảng viên
- Đăng ký đề tài mới
- Xác nhận sinh viên được nhận đề tài
- Chấm điểm đề tài được giao
- Xem danh sách đề tài và sinh viên đang quản lý

### 2.4 Sinh viên
- Đăng ký đề tài phù hợp
- Theo dõi trạng thái đề tài
- Nộp báo cáo / sản phẩm đề tài
- Xem kết quả công bố của bản thân

### 2.5 Quy tắc loại bỏ quyền không cần thiết
- Mỗi menu, API và button chỉ xuất hiện khi người dùng có permission tương ứng.
- Không hiển thị chức năng nếu không thuộc vai trò hoặc không có quyền.
- Backend thực hiện kiểm tra quyền quyết định bằng `@PreAuthorize` hoặc kiểm tra permission trước khi thực thi thao tác.

## 3. Luồng nghiệp vụ chi tiết

### Giai đoạn 1: Đề xuất và duyệt đề tài
1. Giảng viên đăng nhập hệ thống.
2. Giảng viên tạo đề tài mới với thông tin: tên đề tài, mô tả, lĩnh vực, mức độ, giảng viên hướng dẫn.
3. Hệ thống lưu đề tài ở trạng thái `PENDING`.
4. Chủ tịch hội đồng xem danh sách đề tài chờ duyệt.
5. Chủ tịch hội đồng duyệt hoặc từ chối đề tài.
6. Nếu duyệt, đề tài chuyển sang trạng thái `APPROVED` và có thể mở cho sinh viên đăng ký.

### Giai đoạn 2: Đăng ký đề tài
1. Sinh viên truy cập vào danh sách đề tài được phép đăng ký.
2. Sinh viên gửi đăng ký theo đề tài mong muốn.
3. Hệ thống lưu bản ghi đăng ký ở trạng thái `WAITING_CONFIRMATION`.
4. Giảng viên xác nhận hoặc từ chối sinh viên cho đề tài đó.
5. Nếu xác nhận, bản ghi thành `CONFIRMED` và sinh viên chính thức được nhận đề tài.

### Giai đoạn 3: Thực hiện và nộp đề tài
1. Sinh viên tiến hành làm đề tài theo hướng dẫn của giảng viên.
2. Sinh viên nộp bài báo cáo hoặc sản phẩm đề tài đến hệ thống.
3. Hệ thống lưu trạng thái `SUBMITTED`.
4. Chủ tịch hội đồng kiểm tra và chuẩn bị phân công giảng viên phản biện/chấm điểm.

### Giai đoạn 4: Phân công chấm điểm
1. Chủ tịch hội đồng phân công giảng viên chấm cho từng đề tài.
2. Mỗi giảng viên nhận được danh sách đề tài được giao.
3. Giảng viên nhập điểm và nhận xét theo thang điểm thống nhất.
4. Hệ thống lưu từng bản chấm điểm riêng biệt.

### Giai đoạn 5: Tính điểm và công bố
1. Chủ tịch hội đồng tổng hợp điểm từ các giảng viên chấm.
2. Hệ thống tính điểm trung bình theo công thức chuẩn.
3. Hệ thống lưu kết quả đánh giá có trạng thái `PUBLISHED`.
4. Sinh viên truy cập vào giao diện kết quả để xem điểm và trạng thái hoàn thành.

## 4. Yêu cầu chức năng hệ thống

### 4.1 Quản lý tài khoản
- Tạo tài khoản mới
- Cập nhật thông tin cá nhân
- Kích hoạt / vô hiệu hóa tài khoản
- Gán vai trò cho người dùng
- Khóa tài khoản nếu vi phạm quy chế

### 4.2 Quản lý vai trò và quyền
- Tạo vai trò mới
- Gán permission cho vai trò
- Cập nhật quyền theo từng chức năng
- Chỉ admin có thao tác quản trị hệ thống

### 4.3 Quản lý thông báo
- Gửi thông báo tới người dùng hoặc nhóm vai trò
- Chỉ hiển thị thông báo phù hợp với quyền và vai trò người nhận
- Cho phép quản trị/điều phối gửi thông báo công khai

### 4.4 Quản lý đề tài
- Theo dõi trạng thái từng đề tài
- Lưu lịch sử duyệt, xác nhận, nộp bài và chấm điểm
- Hệ thống quy chuẩn trạng thái đề tài rõ ràng

### 4.5 Quản lý hội đồng phản biện
- Tạo hội đồng
- Chọn chủ tịch, thư ký, giảng viên phản biện
- Phân công đề tài theo hội đồng

### 4.6 Quản lý báo cáo và kết quả
- Tạo báo cáo
- Lưu file nộp bài
- Tính điểm trung bình
- Xuất kết quả công bố cho sinh viên

## 5. Cấu trúc quyền theo màn hình

| Màn hình | Admin | Chủ tịch hội đồng | Giảng viên | Sinh viên |
|---|---|---|---|---|
| Dashboard | Có | Có | Có | Có |
| Quản lý tài khoản | Có | Không | Không | Không |
| Quản lý vai trò | Có | Không | Không | Không |
| Quản lý quyền | Có | Không | Không | Không |
| Đề xuất đề tài | Không | Không | Có | Không |
| Duyệt đề tài | Không | Có | Không | Không |
| Đăng ký đề tài | Không | Không | Không | Có |
| Xác nhận sinh viên | Không | Không | Có | Không |
| Nộp đề tài | Không | Không | Không | Có |
| Phân công chấm | Không | Có | Không | Không |
| Chấm điểm | Không | Không | Có | Không |
| Tính điểm & công bố | Không | Có | Không | Không |
| Xem kết quả | Có | Có | Có | Có (chỉ của mình) |

## 6. Yêu cầu kỹ thuật chính
- Spring Boot 4.x
- Spring Security với phân quyền theo `ROLE_*` và `PERMISSION_*`
- Thymeleaf để render giao diện web
- MySQL làm cơ sở dữ liệu chính
- JPA/Hibernate để mapping entity
- Dashboard tối ưu cho thao tác nhanh, hiển thị các khối thống kê và menu theo quyền

## 7. Kết luận
Hệ thống quản lý đề tài cần được xây dựng theo mô hình quyền chính xác, luồng nghiệp vụ rõ ràng và dashboard thân thiện với người dùng. Việc giới hạn chức năng theo vai trò giúp hệ thống dễ quản lý, an toàn và phù hợp với mô hình quản lý khoa CNTT.
