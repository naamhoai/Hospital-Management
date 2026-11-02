# Kết quả kiểm tra — 02/11/2026

Source: Servlet/JSP, tên lớp theo chức năng. Build Maven và WAR thành công. Chạy bằng JDK 21 biên dịch release 17, Tomcat 10.1.61 và MySQL 9.3 local; cấu hình Docker vẫn là MySQL 8.4, chưa chạy Docker lại trong lần này.

## Kiểm thử đã chạy

- **41 JUnit đạt, 0 lỗi, 0 bỏ qua**, trên `hospital_test` riêng.
- **86 HTTP checks đạt**: đăng nhập, CSRF/quyền, đặt lịch, check-in, gọi khám, chẩn đoán, chỉ định/hủy, thu tiền, kết quả/tệp, đơn thuốc, kết thúc, hồ sơ/đánh giá, danh mục và báo cáo/PDF/XLSX; các trang JSP được biên dịch thật bởi Tomcat.
- **36 HTTP checks đạt**: đề nghị/duyệt thu hồi, quyền trưởng khoa, giữ tệp gốc/biên bản, bản kết quả thay thế và thông báo bác sĩ.
- Nạp **01-schema → 02-seed → 04-demo-data** trên database mới: đủ 25 bảng. Nạp lại 04 trong ngày giữ nguyên số dòng, bệnh án, hóa đơn và tồn kho.
- Đổi tên 4 bộ tài khoản/phòng demo trong database local sang `clinic01`–`clinic04`: giữ ID/số lượng lịch hẹn; checksum của 11 bảng bệnh án, xét nghiệm, thuốc, hóa đơn, phản hồi và báo cáo không đổi.

JUnit kiểm tra thêm đặt lịch đồng thời không vượt chỗ, kê đơn rollback khi thiếu kho, kết quả chỉ sau PAID, duyệt đồng thời chỉ một lần, hủy chỉ định không tính lại phí đã hủy, thanh toán cạnh tranh với hủy, quyền sở hữu hồ sơ/tệp và doanh thu không tính hóa đơn chưa trả.

HTTP dùng Tomcat test cổng 3301 và database tách biệt; không tiêu thụ các ca demo chính. Các database test được dọn sau khi hoàn tất. Kết quả Java tại `target/surefire-reports`, HTTP tại `target/http-smoke-results.txt` và `target/http-result-recalls-results.txt`; `target` được Git bỏ qua.

## Giới hạn của kết quả

Không kiểm chứng giao dịch VNPay/SMTP/LIS/dược với nhà cung cấp thật. Chưa đầy đủ mọi nhánh SRS: còn lịch lặp tuần/tháng, Manager duyệt nghỉ, gói khám và các mục trong `SCOPE.md`. Không dùng kết quả kiểm thử này để tuyên bố toàn bộ UC/class/sequence đã khớp 100%.

## Chạy lại

Xem README: unit test bằng `mvn test`; integration bật `HOSPITAL_INTEGRATION=1` với database `hospital_test` vừa tạo bằng schema/seed nền. Script HTTP chỉ chạy trên database test local được chuẩn bị riêng, theo thứ tự `smoke_http.py` rồi `smoke_result_recalls.py`.
