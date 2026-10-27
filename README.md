# Hospital Appointment & Medical Service Management System

Ứng dụng học phần SWD392 cho nhóm 4 người: **Khánh, Tráng, Kiên, Nam**. Source dùng Java 17, Servlet 6, JSP/JSTL, JDBC, Tomcat 10.1 và MySQL 8.4. Giao diện tiếng Việt, đóng gói `hospital.war`.

## Chạy nhanh bằng Docker

Cần Docker Desktop đang hoạt động. Tại thư mục chứa `pom.xml`:

```powershell
docker compose up --build
```

Mở **http://localhost:8080/hospital**. MySQL của Compose dùng cổng máy chủ **3307**, database `hospital`, user `hospital`, password phát triển `change-me-local`. Dữ liệu được giữ trong volume `hospital-db`. Các file SQL chỉ được tự chạy khi volume còn trống. `docker compose down` dừng hệ thống và giữ dữ liệu.

## Chạy bằng IDE và Tomcat

1. Cài JDK 17+, Maven 3.9, MySQL 8.4 và Tomcat **10.1**. Trong IntelliJ/NetBeans/Eclipse chọn **Open Maven Project** tại `pom.xml`.
2. Trong MySQL Workbench, chạy lần lượt `database/01-schema.sql`, `02-seed.sql`, `03-create-local-user.sql` trên một database mới. Script không xóa hoặc ghi đè dữ liệu cũ. Seed chỉ chạy một lần.
3. Đặt biến môi trường cho tiến trình Tomcat:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/hospital?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh'
$env:DB_USER='hospital'
$env:DB_PASSWORD='change-me-local'
$env:APP_BASE_URL='http://localhost:8080/hospital'
mvn clean package
```

4. Copy `target/hospital.war` vào thư mục `webapps` của Tomcat rồi khởi động Tomcat với JVM option `-Duser.timezone=Asia/Ho_Chi_Minh`. Truy cập http://localhost:8080/hospital.

Cũng có thể copy `src/main/resources/application.properties` thành `application-local.properties` trong cùng thư mục và sửa cấu hình. File local đã được `.gitignore`; không commit mật khẩu. Biến môi trường được ưu tiên hơn file cấu hình.

Không dùng Tomcat 9 cho source này vì Tomcat 9 dùng namespace `javax.servlet`; source hiện dùng `jakarta.servlet`.

### Chạy Tomcat từ Maven khi phát triển

Sau khi MySQL đã có schema và seed:

```powershell
mvn package
mvn -Pdev test-compile exec:java "-Dexec.mainClass=vn.hospital.dev.DevServer" "-Dexec.classpathScope=test"
```

Tomcat nhúng mặc định cổng 8080, bind localhost; đặt `HTTP_PORT` để đổi cổng. Sau mỗi lần sửa source, dừng tiến trình, chạy lại `mvn package` rồi khởi động lại. Bản triển khai chính vẫn là WAR trên Tomcat.

## Tài khoản demo

Tất cả dùng mật khẩu **`Demo@1234`**. Dữ liệu trong seed là giả lập.

| Vai trò | Email |
|---|---|
| Bệnh nhân | `patient@hospital.test` |
| Bệnh nhân khác | `patient2@hospital.test`, `patient3@hospital.test` |
| Lễ tân | `reception@hospital.test` |
| Bác sĩ Nội | `doctor@hospital.test` |
| Bác sĩ TMH | `doctor2@hospital.test` |
| Kỹ thuật viên | `lab@hospital.test` |
| Thu ngân | `cashier@hospital.test` |
| Quản lý | `manager@hospital.test` |
| Quản trị | `admin@hospital.test` |

Seed tạo ca khám ngày chạy SQL, ngày hôm sau và một số ngày kế tiếp. Nếu seed đã cũ, đăng nhập bác sĩ/admin tạo ca mới ở `/schedules`. Lễ tân chỉ check-in lịch của hôm nay. Các tài khoản dùng chung một vai trò; thành viên nhóm không đồng nghĩa vai trò người dùng.

## Kịch bản demo

1. **Patient:** tìm bác sĩ, vào Lịch khám đặt ca hôm nay còn giờ trống. Ghi lại mã lịch.
2. **Reception:** tìm mã lịch và check-in, hệ thống cấp số chờ và `queueId`.
3. **Doctor:** vào Hàng đợi → Gọi bệnh nhân tiếp theo. Nhập triệu chứng, chọn mã ICD và lưu chẩn đoán.
4. **Doctor:** chỉ định xét nghiệm. **Cashier:** lập hóa đơn theo mã bệnh án, xác nhận tiền mặt.
5. **Lab:** nhập và duyệt kết quả cho chỉ định đã trả tiền. **Doctor:** mở lại bệnh án để xem kết quả.
6. **Doctor:** kê đơn gồm một hoặc nhiều thuốc mẫu, in đơn. **Cashier:** lập hóa đơn mới để thu phần thuốc chưa tính tiền. Chi phí cũ không bị tính lại.
7. **Doctor:** kết thúc khám. **Patient:** đánh giá bệnh án một lần trong 7 ngày.
8. **Manager:** xem lưu lượng hoặc doanh thu, xuất file Excel `.xlsx` hoặc PDF.

## Kiến trúc và source

```text
src/main/java/vn/hospital/
  controller/    Servlet: nhận request, gọi Service, forward JSP
  filter/        UTF-8, HttpSession, phân quyền và CSRF
  service/       Quy tắc nghiệp vụ và ranh giới transaction
  dao/           JDBC PreparedStatement, truy vấn MySQL và audit
  model/         AuthUser và các request/item DTO
  integration/   QueueContextPort, VNPayClient, EmailClient
  listener/      Tác vụ nền, outbox email và nhãn giao diện
  util/          Cấu hình, kiểm tra dữ liệu, băm mật khẩu
src/main/webapp/WEB-INF/views/   JSP, không truy cập trực tiếp từ URL
database/                      Schema, seed, user MySQL cho máy local
docs/                          Phân công và phạm vi triển khai
```

Truy vấn danh sách nhiều bảng trả về `Map` projection cho JSP; chưa chuyển toàn bộ các bảng thành Java Bean như bản đặc tả entity của báo cáo. Request DTO dùng Java record. `MedicalRecordDAO`, `QueueContextPort`, `EMRService` và các controller giữ vai trò tương ứng với thiết kế; chữ ký hàm có bổ sung `AuthUser` để kiểm tra quyền ở Service.

## Quy tắc đã có

- Băm mật khẩu PBKDF2-HMAC-SHA256 với salt; khóa đăng nhập 15 phút sau nhiều lần sai; đổi session khi đăng nhập, logout hủy session.
- Kiểm tra role ở Filter, quyền sở hữu lịch/bệnh án trong Service, CSRF cho POST và escape nội dung ở JSP.
- Đặt lịch khóa dòng patient và schedule trong transaction; không vượt sức chứa, tối đa 2 lịch chưa hoàn tất khi đặt online; đổi trước 12 giờ, tối đa 2 lần.
- Hủy hơn 3 lần trong tháng tạm chặn đặt online 30 ngày. Hủy do bác sĩ nghỉ không tính như bệnh nhân chủ động hủy.
- Check-in một lần/ngày hẹn, số chờ duy nhất theo ngày/phòng; ưu tiên khẩn cấp, người cao tuổi, sau đó giờ hẹn và số chờ.
- Bác sĩ chỉ sửa bệnh án được phân công và còn OPEN; ICD thuộc danh mục cấu hình; kết thúc khám khóa chỉnh sửa.
- Xét nghiệm cần có chẩn đoán sơ bộ và thanh toán; kết quả duyệt một lần, không sửa lại trên UI.
- Kê nhiều dòng thuốc trong một transaction; kiểm tra danh mục và tồn kho; trừ kho khi phát hành đơn; lỗi một dòng hoàn tác cả đơn.
- Giá được chụp tại lúc đặt lịch/chỉ định/kê đơn; thay giá danh mục không sửa các chi phí đã phát sinh.
- Hóa đơn tính các dòng chưa được lập hóa đơn; thanh toán idempotent, doanh thu chỉ tính PAID, callback VNPay kiểm tra chữ ký và số tiền.
- Email nằm trong outbox transaction; lỗi gửi không rollback nghiệp vụ. Tác vụ nền chạy mỗi 60 giây; sau 5 lần lỗi cần kiểm tra log/outbox.

## Tích hợp ngoài

### VNPay sandbox

Đặt `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` và `APP_BASE_URL` bằng thông tin tài khoản merchant sandbox. Endpoint trả về: `/payment/return`; endpoint IPN: `/payment/ipn`. Cấu hình IPN với VNPay và dùng HTTPS/tunnel khi nhà cung cấp cần gọi vào máy local. Code xác minh HMAC-SHA512, merchant, mã tham chiếu và số tiền; không đánh dấu PAID chỉ vì người dùng bấm nút. Chưa có merchant thật nên chưa đối soát được với VNPay sandbox ngoài mạng. Tiền mặt chạy độc lập để demo.

### Email

Đặt `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`. SMTP yêu cầu STARTTLS. Nếu chưa cấu hình, email giữ PENDING trong `notification_outbox`. Chưa tích hợp SMS; không có thông báo giả rằng SMS/email đã gửi thành công.

## Kiểm thử

```powershell
mvn test
```

13 unit test chạy mặc định, gồm 7 trường hợp hồi quy cho CSRF và phiên đăng nhập hết hạn. 3 integration test trong `WorkflowTest` chỉ chạy khi `HOSPITAL_INTEGRATION=1`, `DB_URL` trỏ vào **database riêng tên `hospital_test`**, đã nạp schema và seed tương tự. Test chủ động ghi dữ liệu demo; không dùng database triển khai. Kiểm thử xác nhận cả rollback tồn kho, tranh chỗ cuối và cấp hai giờ khác nhau khi đặt đồng thời.

`scripts/smoke_http.py` chạy với Python 3 trên app đang chạy và database demo mới; kiểm tra HTTP, CSRF, phân quyền, các JSP, trọn luồng khám và file xuất. Script dùng tài khoản demo, có ghi dữ liệu; không chạy trên dữ liệu thực.

Chi tiết kết quả kiểm tra và những phần còn cần phát triển nằm trong `docs/VALIDATION.md` và `docs/SCOPE.md`.
