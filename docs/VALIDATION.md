# Kết quả kiểm tra

Ngày kiểm tra: **27/10/2026**, múi giờ Asia/Ho_Chi_Minh.

## Môi trường đã chạy thực tế

- JDK có trên máy: **21.0.11**; Maven compiler dùng `--release 17`, sinh class tương thích Java 17.
- Maven **3.9.11**, Tomcat nhúng **10.1.61**, JSP/JSTL thật.
- MySQL có sẵn trên máy: **9.3.0**, chạy instance thử riêng tại `127.0.0.1:33307`, không dùng dịch vụ hoặc database có sẵn của người dùng.
- Cấu hình bàn giao và Docker Compose nhắm **MySQL 8.4**. Chưa chạy Docker/MySQL 8.4 thực tế trên máy này; schema dùng cú pháp MySQL 8 trở lên.
- HTTP thử nghiệm chỉ bind localhost cổng **18080**.

## Kết quả

**Build WAR thành công. 9 kiểm thử Java qua, 0 lỗi, 0 bị bỏ qua khi bật integration.**

6 unit test kiểm tra:

1. Mật khẩu có salt khác nhau; so khớp đúng/sai và hash hỏng.
2. Kiểm tra số tiền âm, phần thập phân, ngày không tồn tại.
3. Sắp xếp và encode tham số ký VNPay, loại trường chữ ký.
4. HMAC-SHA512 khớp test vector đã biết.
5. File Excel là XLSX ZIP hợp lệ, text công thức được ghi dạng inline string.
6. File PDF đọc lại được và tự chia trang với nhiều dòng.

3 integration test trên `hospital_test` kiểm tra:

- Login đúng/sai; đặt lịch; chặn ca đầy; check-in một lần; gọi bệnh nhân; quyền bác sĩ; mã ICD; lưu chẩn đoán; chỉ định xét nghiệm; chặn kết quả chưa trả tiền; thanh toán; chống thanh toán lại; khóa kết quả duyệt; hoàn tác tồn kho khi đơn nhiều thuốc bị lỗi; xuất đơn hợp lệ; hóa đơn nhiều đợt không tính lại chi phí; đóng bệnh án; chặn sửa bệnh án đóng; đánh giá một lần; chặn người khác đánh giá; doanh thu PAID chính xác.
- Hai luồng tranh chỗ cuối của cùng ca: đúng một luồng đặt được, booked_count bằng 1.
- Hai luồng cùng đặt vào ca còn hai chỗ: cả hai đặt được và được cấp hai giờ khác nhau. Database có UNIQUE theo bác sĩ và giờ hẹn đang chiếm chỗ.

**59 kiểm tra HTTP qua trên Tomcat đang chạy.** Script `scripts/smoke_http.py` xác nhận:

- Chưa login được chuyển về login; bệnh nhân không được xem báo cáo (403); CSRF sai bị chặn (403); logout hủy quyền truy cập.
- Các JSP login, dashboard, search, appointments, reception, queue, schedules, emr, prescription, billing, lab, feedback, services và reports biên dịch và trả trang thành công.
- Luồng POST thực tế từ đặt lịch → check-in → gọi lượt khám → lưu chẩn đoán → chỉ định → tiền mặt → kết quả → đơn thuốc → đóng bệnh án → đánh giá.
- Download `.xlsx` có chữ ký ZIP `PK`; download `.pdf` có chữ ký `%PDF`; nội dung báo cáo doanh thu có loại chi phí thực tế đã thu.
- Nội dung user nhập được escape khi hiển thị qua `c:out`.

Đã xem giao diện login bằng trình duyệt và xác nhận tiếng Việt, form và footer hiển thị đúng. Log HTTP từng bước nằm ở `target/http-smoke-results.txt` sau khi tự chạy script; kết quả Surefire nằm ở `target/surefire-reports`.

## Chưa xác nhận bằng môi trường ngoài

### Hồi quy phiên đăng nhập — 27/10/2026

Đã bổ sung và chạy thành công 7 test cho SecurityFilter, cùng 6 unit test ban đầu (13 test qua, 0 lỗi). Form POST hết hạn khi chưa đăng nhập được chuyển bằng HTTP 303 sang GET trang login mới; dữ liệu cũ không được thực thi lại. Khi đang đăng nhập, POST sai CSRF vẫn bị chặn 403 nhưng hiển thị trang lỗi của ứng dụng. Form login/logout cũ ở tab khác giữ phiên đang đăng nhập và đưa về dashboard. Kiểm tra role vẫn áp dụng sau khi token hợp lệ.

## Tích hợp ngoài chưa xác nhận

- Chưa có tài khoản merchant VNPay để thử giao dịch sandbox với nhà cung cấp và đối soát IPN thật. Unit test ký HMAC không thay thế kiểm thử tích hợp VNPay.
- Chưa có SMTP để xác nhận gửi email ra hộp thư thật. Outbox và cấu hình đã có.
- Chưa kiểm tra tải đồng thời quy mô lớn, nhiều instance, hoặc triển khai public qua HTTPS.
- Không tuyên bố tất cả chi tiết của SRS đã hoàn tất; backlog và khác biệt với UML có trong `SCOPE.md`.
