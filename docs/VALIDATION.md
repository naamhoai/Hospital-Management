# Kết quả kiểm tra — 29/10/2026

## Môi trường thực tế

Windows; JDK 21.0.11 biên dịch `--release 17`; Maven 3.9.11; Apache Tomcat 10.1.61; MySQL **9.3.0 cài trên máy** (cổng test riêng 33307). Cấu hình dự án/Compose hướng tới Java 17+, Tomcat 10.1 và MySQL 8.4. Chưa chạy container MySQL 8.4 trong lần kiểm tra này; không coi kiểm tra 9.3 là chứng nhận riêng cho 8.4.

## Java và database

Lệnh: `HOSPITAL_INTEGRATION=1`, `DB_URL` trỏ database riêng **hospital_test**, `mvn test package`.

| Bộ kiểm thử | Số trường hợp | Kết quả |
|---|---:|---|
| CoreTest | 6 | Qua |
| SecurityFilterTest | 7 | Qua |
| DesignContractTest | 7 | Qua |
| ClinicalProgressTest | 3 | Qua |
| NamWorkflowTest | 6 | Qua |
| WorkflowTest | 3 | Qua |
| NamRevisionTest | 9 | Qua |
| **Tổng** | **41** | **Không lỗi, không bỏ qua** |

- Kiểm tra mật khẩu salted hash, HMAC, validation, Excel thực (ZIP/OpenXML), PDF nhiều trang.
- CSRF, session hết hạn không replay POST, role filtering.
- Controller không truy cập DAO, SQL chỉ ở DAO, 18 bảng nghiệp vụ, chữ ký domain typed, Java Bean mapping, PDF giữ dấu tiếng Việt, filter whitelist, tệp sai định dạng/quá lớn/traversal.
- Nam: quyền sở hữu hồ sơ/đơn/tệp; tồn kho; hủy chỉ định và tính hóa đơn; kết quả chỉ sau PAID, khóa sau duyệt; hai kỹ thuật viên duyệt đồng thời chỉ thành công một lần; ngày/khoa/dịch vụ/phương thức/nhóm báo cáo; không có dữ liệu; file xuất hợp lệ.
- Bổ sung: tiến trình khám, JSON escaping; hủy chỉ định đã lên hóa đơn UNPAID; chặn PAID/VNPay đang chờ; callback hết hạn; tranh chấp hủy với lập hóa đơn/thu tiền; thu hồi giữ bản gốc, nhập bản thay thế; quyền đúng khoa, PDF bắt buộc, từ chối và hai lần duyệt đồng thời; thông báo đúng bác sĩ; tổng hợp ngày chạy lại không cộng lặp.
- Toàn viện: đặt → check-in → gọi → chẩn đoán → chỉ định → trả tiền → kết quả → kê đơn → trả thuốc → đóng bệnh án → đánh giá. Rollback tồn kho; tranh suất cuối; hai suất sinh giờ khác nhau.

Tạo database tạm mới: `01-schema.sql` xác nhận **18 bảng**, áp dụng `04-operational-extensions.sql` thành **23 bảng**, seed 10 tài khoản thành công. Áp dụng migration `20261027-sds-alignment.sql` trên schema MVP từ Git HEAD: giữ nguyên user ID/password hash và liên kết bệnh án–queue/đơn/kết quả, xóa queue_id dư trong medical_record. Chỉ các database tạm do kiểm tra tạo mới bị dọn sau kiểm tra.

Ngày 29/10: thử toàn bộ schema mới **01 → 04 → 02 → 05 → 06**, xác nhận **25 bảng**. Sao lưu các database local trước khi áp dụng 05; giữ dữ liệu kết quả cũ. HTTP dùng database demo mới riêng `hospital_nam_http_<mã ngẫu nhiên>`, không ghi vào database chính. Các tệp sao lưu ở `.local/backups` được gitignore.

## HTTP và giao diện

`scripts/smoke_http.py http://127.0.0.1:3000/hospital`: **86 kiểm tra HTTP qua**. Kiểm tra JSP, role/session/CSRF, đầy đủ luồng bệnh viện, upload multipart PNG, download đúng nội dung, chặn bệnh nhân khác tải tệp/hồ sơ/đơn, không sửa kết quả đã duyệt, PDF đơn/phiếu chỉ định, chặn kê đơn khi còn chờ xét nghiệm, tìm thuốc, bộ lọc và báo cáo, Excel hai sheet (parse toàn bộ XML)/PDF; hủy chỉ định sau lập hóa đơn và kiểm tra tổng còn 360.000 VND.

`scripts/smoke_nam_workflows.py http://127.0.0.1:3000/hospital`: **36 kiểm tra HTTP qua**. Đề nghị thu hồi → trưởng khoa duyệt với PDF → đọc biên bản/tệp gốc hai lần → chặn tải kết quả mất hiệu lực → kỹ thuật viên nhập bản thay thế → lịch sử và polling bác sĩ. Tài khoản/bệnh án/biên bản kiểm thử là dữ liệu giả; PDF dùng để thử upload, không phải chứng nhận chuyên môn thật.

Đã quan sát giao diện đăng nhập và dashboard/bệnh án bác sĩ bằng trình duyệt; kiểm tra trước đó có cả desktop và màn hình nhỏ. Ngày 29/10 kiểm tra thêm đăng nhập trưởng khoa và lịch sử duyệt thu hồi/tệp gốc. Kết quả chi tiết tạm ở target/surefire-reports, target/http-smoke-results.txt, target/http-nam-workflows-results.txt; ảnh UI ở target/ui-result-recalls.png, không commit dữ liệu runtime.

## Phạm vi chưa được xác minh ngoài hệ thống

Chưa có merchant VNPay hoặc SMTP/pharmacy provider thật nên không tuyên bố đã đối soát thanh toán, gửi email/SMS hoặc kiểm tra tương tác thuốc thực tế. Không benchmark 100 người dùng, không kiểm thử Azure/PKI/LIS thật. Các yêu cầu còn thiếu ở SCOPE.md; không đánh đồng “41 test qua” với toàn bộ SRS đã hoàn tất.
