# Kết quả kiểm tra — 29/10/2026

## Bản React + React Bootstrap hiện tại

- Clean build Maven đã chạy npm ci, Vite build và đóng gói React trong hospital.war; WAR không còn các trang JSP/JSTL cũ.
- **49 test Java/MySQL qua**, không lỗi/không bỏ qua: Core 6, SecurityFilter 10, DesignContract 7, ClinicalProgress 3, FrontendResponse 2, PaymentReturn 3, NamWorkflow 6, Workflow 3, NamRevision 9.
- **11 test React qua**: form/CSRF, giữ dữ liệu khi lỗi, nhiều dòng thuốc, chặn submit trùng, render text an toàn, khóa kê đơn, xử lý lỗi API, tên View đơn thuốc, URL /hospital/ và context ROOT.
- **109 kiểm tra HTTP luồng chính qua** trên database giả lập riêng: luồng khám đầy đủ, tệp/PDF/Excel, phân quyền, CSRF, thanh toán trùng, callback HTML/API/IPN, thêm/sửa dịch vụ, tạo/đổi sức chứa/hủy ca và đặt/đổi/hủy lịch.
- **34 kiểm tra HTTP thu hồi qua** sau sửa JOIN: đề nghị, duyệt, đọc lại biên bản/tệp gốc, chặn tệp mất hiệu lực, nhập bản thay thế, danh sách không trùng và thông báo. Tổng HTTP: **143**.
- Đã thao tác trên trình duyệt React: đăng nhập/đổi vai trò, đặt lịch, check-in, gọi lượt, lưu chẩn đoán, tạo chỉ định, thanh toán tiền mặt demo, nhập kết quả và kê nhiều thuốc. Trang đơn thuốc đã được kiểm tra lại sau sửa ánh xạ tên View.
- Runtime đã chuyển lại database chính **hospital**, cổng HTTP **3000**. GET /login và /api/search trả 200; API đọc được 4 dịch vụ. Các luồng ghi kiểm thử HTTP chạy trên database riêng, không chạy trên database chính.
- Rà trước Git: build thành công từ bản sao chỉ có source đưa lên Git, không có application-local.properties/node_modules. npm audit báo 0 lỗ hổng. WAR cuối có một JS bundle, không có JSP cũ hoặc application-local.properties. Docker Compose config hợp lệ; Docker Engine đang tắt nên chưa chạy kiểm thử container/MySQL 8.4.
- Đã sửa xử lý callback VNPay hai lần khi React tải dữ liệu, điều hướng /hospital/ và cấu hình loại file local khỏi WAR. Chi tiết ở PRE_GIT_CHECK.md.

Hướng dẫn frontend và những chỗ báo cáo/sơ đồ cần đổi từ JSP sang React nằm trong REACT_FRONTEND.md. Các mục dưới đây ghi lại kết quả bản Servlet/JSP trước khi chuyển frontend.

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

## Kiểm tra lỗi database của thành viên nhóm — 29/10

Log nhận được: đăng nhập lỗi MySQL 1054 `Unknown column 'u.password' in 'field list'`. Kết nối đã thành công; schema không khớp code.

Đã tái hiện lỗi trên database thử riêng bằng cột password_hash, chạy migration alignment và kiểm tra lại truy vấn đăng nhập của UserDAO. Kiểm tra giữ nguyên toàn bộ user ID/hash, liên kết bệnh án–queue qua appointment, đơn thuốc và kết quả. Thử cả schema đang cập nhật dở (đã đổi tên password, chưa bỏ cột queue), chạy lại migration, rồi áp dụng 05/06 thành 25 bảng. Script kiểm tra schema chỉ đọc cũng chạy thành công. Tổng **8 kiểm tra qua**, gồm HTTP /search của app local trả 200 và dữ liệu bác sĩ.

Chỉ database ngẫu nhiên tạo cho lần thử bị xóa sau khi thử. Không sửa database chính hoặc database trên máy thành viên. Chi tiết tạm: target/db-team-validation.txt. Hướng dẫn gửi cho nhóm: DATABASE_TROUBLESHOOTING.md.
