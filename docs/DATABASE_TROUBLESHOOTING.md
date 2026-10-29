# Sửa lỗi database trên máy thành viên nhóm

## Lỗi đã gặp ngày 29/10/2026

```text
Database operation failed
Caused by: SQLSyntaxErrorException: Unknown column 'u.password' in 'field list'
```

MySQL đã nhận kết nối và chạy truy vấn đăng nhập. Lỗi này là **schema cũ không khớp code mới**, không phải mất kết nối hoặc sai mật khẩu MySQL. UserDAO hiện đọc `users.password`; bản MVP trước đây dùng tên `users.password_hash`. Cả hai đều lưu hash PBKDF2, không lưu mật khẩu rõ.

## Cập nhật database cũ và giữ dữ liệu

1. Trong MySQL Workbench, dùng đúng host/port/database mà Tomcat đang cấu hình ở DB_URL. Chạy `database/00-check-schema.sql` để xem cấu trúc. Script này chỉ đọc, không in mật khẩu/hash.
2. Sao lưu schema và dữ liệu bằng **Server → Data Export** trước khi chạy migration. Dừng ứng dụng trong lúc cập nhật.
3. Nếu `users` chỉ có `password_hash`, chạy `database/migrations/20261027-sds-alignment.sql` bằng tài khoản quản trị database. Script đổi tên cột, giữ nguyên giá trị hash và user ID, cập nhật diagnosis và bỏ liên kết queue cũ; liên kết lượt khám vẫn qua appointment_id. Script kiểm tra trước khi đổi/xóa các cột để có thể chạy tiếp sau một lần cập nhật dở.
4. Nếu `test_result` chưa có cột `status`, chạy thêm `database/05-nam-workflows.sql` **một lần**. Cần có sẵn các bảng kỹ thuật của bản MVP như invoice_line/audit_log. Nếu thiếu cả các bảng đó, cần kiểm tra phiên bản schema trước khi chọn migration; không chạy lại toàn bộ 04 một cách máy móc.
5. Tùy chọn cho database demo: `database/06-demo-department-head.sql` tạo tài khoản trưởng khoa.
6. Clean/build WAR mới, triển khai lại Tomcat và đăng nhập bằng tài khoản cũ.

Nếu bảng có **cả password và password_hash**, hoặc không có cả hai, dừng để kiểm tra schema thực tế. Không tự xóa/chuyển đè dữ liệu cột. Không chạy lại 01-schema/02-seed lên database đang có dữ liệu.

## Database mới hoàn toàn

Chỉ dùng khi database trống: **01-schema → 04-operational-extensions → 02-seed → 05-nam-workflows → 03-create-local-user**. Script 06 là tùy chọn. Cấu trúc đầy đủ có 25 bảng. Không import theo thứ tự số tên file vì seed cần các cột từ 04.

## Kiểm tra cổng và cấu hình

| Cách chạy | Địa chỉ MySQL dùng trong DB_URL |
|---|---|
| IDE/Tomcat + MySQL cài trên máy | localhost:3306, hoặc cổng MySQL thực tế của máy đó |
| IDE/Tomcat + MySQL từ Compose | localhost:3307 |
| Web và MySQL đều trong Compose | db:3306 (đã có trong compose.yaml) |

File application-local.properties được gitignore nên mỗi máy cần tự tạo hoặc đặt DB_URL/DB_USER/DB_PASSWORD cho tiến trình Tomcat. Cổng 33307 chỉ là cấu hình thử riêng trên máy tác giả. Biến môi trường được ưu tiên hơn file properties. Nếu dùng Tomcat do IDE hoặc Windows Service khởi động, biến môi trường phải được đặt cho đúng tiến trình đó và cần khởi động lại.

Docker chỉ tự import SQL khi volume trống. Pull code/build lại image không tự nâng cấp database trong volume cũ; cần chạy migration trên đúng database. Không dùng `docker compose down -v` để sửa lỗi vì sẽ xóa volume dữ liệu.

## Phân biệt lỗi

| Lỗi gốc trong Caused by | Cần kiểm tra |
|---|---|
| Unknown column / Table doesn't exist | Phiên bản schema và migration |
| Access denied for user | DB_USER/DB_PASSWORD và quyền MySQL trên đúng database |
| Unknown database | Tên database trong DB_URL và bước tạo database |
| Communications link failure / Connection refused | MySQL đã chạy chưa, host/port trong DB_URL |

Luôn gửi dòng **Caused by** cuối cùng khi báo lỗi, không chỉ gửi dòng Database operation failed. Không gửi mật khẩu hoặc nội dung hash.
