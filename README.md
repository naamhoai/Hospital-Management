# Hospital Appointment & Medical Service Management

Dự án Java 17, Servlet 6, JSP/JSTL, JDBC, Tomcat 10.1 và MySQL 8.4. Giao diện dùng JSP, CSS và JavaScript thuần. Maven đóng gói thành `hospital.war`.

## Cấu trúc code

| Thư mục | Trách nhiệm |
|---|---|
| `controller` | Nhận request, đọc tham số, gọi Service và chọn JSP hoặc file tải về |
| `service` | Kiểm tra quyền, điều kiện nghiệp vụ và transaction |
| `dao` | Câu SQL, PreparedStatement và ánh xạ dữ liệu |
| `model` | Entity, request và dữ liệu báo cáo |
| `filter` | Phiên đăng nhập, vai trò và CSRF |
| `integration` | Queue context, VNPay, email và kết nối dược tùy chọn |
| `listener` | Tác vụ nền và nhãn giao diện |
| `util` | Kết nối DB, validation, mật khẩu và xử lý tệp |
| `src/main/webapp/WEB-INF/views` | Các trang JSP |

Tên lớp theo chức năng, không theo tên người làm: `AppointmentController`, `ServiceCatalogService`, `ClinicalService`, `EMRService`, `AnalyticsService`. Test dùng `HospitalWorkflowTest`, `ClinicalWorkflowTest`, `ClinicalSafetyTest`.

## Tạo database mới

Thư mục `database` chỉ còn 4 file:

1. `01-schema.sql`: toàn bộ 25 bảng, gồm 18 bảng nghiệp vụ và 7 bảng hỗ trợ.
2. `02-seed.sql`: tài khoản theo vai trò, khoa/phòng, thuốc/dịch vụ và ca khám mẫu.
3. `03-create-local-user.sql`: tùy chọn, cấp quyền cho user kết nối local; sửa mật khẩu trước khi dùng.
4. `04-demo-data.sql`: dữ liệu các bước đặt lịch, tiếp nhận, khám, xét nghiệm, thanh toán và báo cáo.

Trên database mới, chạy **01 → 02 → 04** bằng MySQL Workbench; chạy thêm 03 nếu cần user ứng dụng. Không chạy lại 01/02 trên database đang có dữ liệu. Database hiện tại đã đủ 25 bảng thì giữ nguyên; 04 có thể bổ sung lại dữ liệu demo mà không reset các ca đã xử lý trong ngày.

Các file extension/migration và seed demo rời đã được gộp vào 01/02/04; không cần tìm hay chạy các file cũ. Docker cũng nạp 3 file này khi khởi tạo volume mới, không tự nạp lại khi volume đã có dữ liệu.

```bash
docker compose up --build
```

Mở `http://localhost:8080/hospital`. Xem [tài khoản và dữ liệu demo](docs/DEMO_MAIN_FLOW.md).

## Chạy local cổng 3000

Cần JDK 17 trở lên, Maven trên PATH và MySQL đang chạy. Tạo `src/main/resources/application-local.properties` (file này được Git bỏ qua):

```properties
DB_URL=jdbc:mysql://127.0.0.1:3306/hospital?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
DB_USER=hospital
DB_PASSWORD=your-local-password
APP_BASE_URL=http://localhost:3000/hospital
```

Đổi host/port và tài khoản theo máy đang dùng; biến môi trường cùng tên được ưu tiên hơn file cấu hình.

```powershell
./scripts/run-dev.ps1 -Port 3000
```

Hoặc `mvn clean package`, chép `target/hospital.war` vào `webapps` của Tomcat 10.1. Không commit cấu hình mật khẩu, thư mục `.local` hay `target`.

## Luồng demo liên tục cho cả nhóm

**Bệnh nhân tìm bác sĩ → đặt lịch → lễ tân check-in/cấp số → bác sĩ gọi vào khám → lưu chẩn đoán → chỉ định nếu cần → thu phí → kỹ thuật viên duyệt kết quả → bác sĩ kê đơn/kết thúc → thu khoản còn lại → bệnh nhân xem hồ sơ/đánh giá → quản lý xem báo cáo.**

Servlet gọi Service; Service kiểm tra quyền/điều kiện rồi gọi DAO trong transaction. DAO dùng JDBC đến MySQL. Controller chuyển dữ liệu sang JSP để hiển thị; không viết SQL trong Controller hoặc Service.

Chẩn đoán sơ bộ phải có trước chỉ định. Kết quả chỉ duyệt sau PAID. Đơn thuốc kiểm tra tồn kho và chỉ định chưa hoàn tất. Hóa đơn chỉ lấy các phí chưa lập hóa đơn; đóng bệnh án và trả hết tiền là hai trạng thái riêng.

Xem [đường đi từng chức năng](docs/DEMO_MAIN_FLOW.md), [hướng dẫn khám và báo cáo](docs/CLINICAL_REPORTING_GUIDE.md), [phân công thuyết trình](docs/TEAM_ASSIGNMENT.md), [đối chiếu sơ đồ](docs/CODE_DIAGRAM_REVIEW.md) và [phạm vi còn thiếu](docs/SCOPE.md).

## Kiểm thử

```bash
mvn clean test
mvn package
```

Test có ghi database chỉ bật khi `HOSPITAL_INTEGRATION=1` và `DB_URL` trỏ tới **hospital_test** dùng riêng cho test. Chuẩn bị database này bằng 01/02, đổi tên database trong `CREATE DATABASE`/`USE` sang `hospital_test`. Không nạp 04 trước JUnit vì các test tự tạo lượt khám.

```powershell
$env:HOSPITAL_INTEGRATION='1'
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/hospital_test?serverTimezone=Asia/Ho_Chi_Minh'
# Cấu hình DB_USER và DB_PASSWORD trên máy của bạn.
mvn test
```

Kiểm thử HTTP trên database test riêng: `python scripts/smoke_http.py URL`, sau đó `python scripts/smoke_result_recalls.py URL`. Các script tạo và xử lý dữ liệu thử; không dùng để kiểm tra chỉ đọc trên database đang demo.

## Tích hợp tùy chọn và giới hạn

VNPay cần `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `APP_BASE_URL` và cấu hình callback `/payment/return`, `/payment/ipn`. Code kiểm tra chữ ký/số tiền; chưa cấu hình merchant thì dùng tiền mặt để demo. Email gửi từ outbox khi có SMTP; SMS/LIS chưa có adapter thực tế.

Kết nối dược bật qua `PHARMACY_BASE_URL`, theo hợp đồng của dự án; chưa cấu hình thì không báo đã kiểm tra tương tác hay đồng bộ thành công. `ICD_CATALOG_FILE` có thể trỏ tới CSV nhóm duyệt; resource mặc định chỉ chứa mã minh họa.

Lịch lặp tuần/tháng, Manager duyệt nghỉ và gói khám vẫn còn thiếu so với toàn bộ SRS. Việc dọn code không thay thế việc hoàn thiện các yêu cầu này; chi tiết nằm trong `docs/SCOPE.md`.
