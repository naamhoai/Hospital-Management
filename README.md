# Hospital Appointment & Medical Service Management System

Ứng dụng học phần SWD392 cho nhóm 4 người: **Khánh, Tráng, Kiên, Nam**. Frontend chung dùng **React + React Bootstrap + Vite**; backend Java 17, Servlet 6, JDBC, Tomcat 10.1 và MySQL 8.4. Giao diện tiếng Việt, đóng gói cùng backend trong `hospital.war`.

Theo yêu cầu đổi frontend ngày 29/10, các trang JSP đã được thay bằng React và Servlet trả JSON qua `/api/*`. Xem [hướng dẫn frontend và những chỗ cần cập nhật trong báo cáo/sơ đồ](docs/REACT_FRONTEND.md).

## Chạy nhanh bằng Docker

Cần Docker Desktop đang hoạt động. Tại thư mục chứa `pom.xml`:

```powershell
docker compose up --build
```

Mở **http://localhost:8080/hospital**. MySQL của Compose dùng cổng máy chủ **3307**, database `hospital`, user `hospital`, password phát triển `change-me-local`. Dữ liệu được giữ trong volume `hospital-db`. Các file SQL chỉ được tự chạy khi volume còn trống. `docker compose down` dừng hệ thống và giữ dữ liệu.

## Chạy bằng IDE và Tomcat

1. Cài JDK 17+, Maven 3.9, **Node.js 22.12+ và npm**, MySQL 8.4 và Tomcat **10.1**. Trong IntelliJ/NetBeans/Eclipse chọn **Open Maven Project** tại `pom.xml`. Node/npm phải có trên PATH của tiến trình Maven/IDE.
2. Trong MySQL Workbench, chạy lần lượt `database/01-schema.sql` → `database/04-operational-extensions.sql` → `database/02-seed.sql` → `database/05-nam-workflows.sql` → `database/03-create-local-user.sql` trên một database mới. Nếu cần tài khoản trưởng khoa demo, chạy thêm `database/06-demo-department-head.sql`. Schema/seed và migration 05 chỉ chạy một lần.
3. Đặt biến môi trường cho tiến trình Tomcat:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/hospital?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh'
$env:DB_USER='hospital'
$env:DB_PASSWORD='change-me-local'
$env:APP_BASE_URL='http://localhost:8080/hospital'
mvn clean package
```

4. Maven tự cài dependency theo frontend/package-lock.json và build React. Copy `target/hospital.war` vào thư mục `webapps` của Tomcat rồi khởi động Tomcat với JVM option `-Duser.timezone=Asia/Ho_Chi_Minh`. Truy cập http://localhost:8080/hospital.

Cũng có thể copy `src/main/resources/application.properties` thành `application-local.properties` trong cùng thư mục và sửa cấu hình. File local đã được `.gitignore`; không commit mật khẩu. Biến môi trường được ưu tiên hơn file cấu hình.

`application-local.properties` chỉ dùng khi phát triển trên máy cá nhân và được loại khỏi WAR. Khi triển khai WAR lên Tomcat, đặt DB_URL/DB_USER/DB_PASSWORD cho tiến trình Tomcat như hướng dẫn; không đóng gói cấu hình riêng của máy mình để gửi cho nhóm.

Nếu đăng nhập báo `Unknown column 'u.password'`, database vẫn dùng schema MVP cũ. Xem [hướng dẫn kiểm tra và cập nhật database](docs/DATABASE_TROUBLESHOOTING.md); chạy `database/00-check-schema.sql` trước khi chọn migration. Không tạo lại database đang có dữ liệu.

Không dùng Tomcat 9 cho source này vì Tomcat 9 dùng namespace `javax.servlet`; source hiện dùng `jakarta.servlet`.

### Chạy Tomcat từ Maven khi phát triển

Sau khi MySQL đã có schema và seed:

```powershell
mvn package
mvn -Pdev -Dfrontend.skip=true test-compile exec:java "-Dexec.mainClass=vn.hospital.dev.DevServer" "-Dexec.classpathScope=test"
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
| Trưởng khoa Nội (script demo 06) | `head@hospital.test` |
| Thu ngân | `cashier@hospital.test` |
| Quản lý | `manager@hospital.test` |
| Quản trị | `admin@hospital.test` |

Seed tạo ca khám ngày chạy SQL, ngày hôm sau và một số ngày kế tiếp. Nếu seed đã cũ, đăng nhập bác sĩ/admin tạo ca mới ở `/schedules`. Lễ tân chỉ check-in lịch của hôm nay. Các tài khoản dùng chung một vai trò; thành viên nhóm không đồng nghĩa vai trò người dùng.

## Kịch bản demo

1. **Patient:** tìm bác sĩ, vào Lịch khám đặt ca hôm nay còn giờ trống. Ghi lại mã lịch.
2. **Reception:** tìm mã lịch và check-in, hệ thống cấp số chờ và `queueId`.
3. **Doctor:** vào Hàng đợi → Gọi bệnh nhân tiếp theo. Nhập triệu chứng, chọn mã ICD và lưu chẩn đoán.
4. **Doctor:** chỉ định xét nghiệm. **Cashier:** lập hóa đơn theo mã bệnh án, xác nhận tiền mặt.
5. **Lab:** nhập và duyệt kết quả cho chỉ định đã trả tiền. **Doctor:** nhận thông báo trên màn hình (kiểm tra mỗi 15 giây), mở bệnh án để xem kết quả.
6. **Doctor:** kê đơn gồm một hoặc nhiều thuốc mẫu, in đơn. **Cashier:** lập hóa đơn mới để thu phần thuốc chưa tính tiền. Chi phí cũ không bị tính lại.
7. **Doctor:** kết thúc khám. **Patient:** đánh giá bệnh án một lần trong 7 ngày.
8. **Manager:** xem lưu lượng hoặc doanh thu, xuất file Excel `.xlsx` hoặc PDF.

Nhánh bổ sung: bác sĩ/kỹ thuật viên đề nghị thu hồi kết quả → **Trưởng khoa** kiểm tra và duyệt kèm biên bản PDF → **Lab** nhập kết quả thay thế. Kết quả cũ và biên bản được giữ làm lịch sử. Chỉ định đã lên hóa đơn UNPAID vẫn có thể hủy trước khi thực hiện nếu không có giao dịch VNPay đang chờ; hệ thống tính lại hóa đơn.

## Kiến trúc và source

```text
src/main/java/vn/hospital/
  controller/    Servlet: nhận request, gọi Service, trả JSON cho React
  filter/        UTF-8, HttpSession, phân quyền và CSRF
  service/       Quy tắc nghiệp vụ và ranh giới transaction
  dao/           JDBC PreparedStatement, truy vấn MySQL và audit
  model/         17 entity Java Bean trong SDS, Room, request và report models
  integration/   QueueContextPort, VNPayClient, EmailClient
  listener/      Tác vụ nền, outbox email và nhãn giao diện
  util/          DB connection/transaction, cấu hình, validation, băm mật khẩu, file storage
frontend/src/                  React pages, component React Bootstrap, Fetch API và CSS chung
database/                      Schema, seed, user MySQL cho máy local
docs/                          Phân công và phạm vi triển khai
```

Controller nhận HTTP, gọi Service và trả dữ liệu JSON; React hiển thị dữ liệu. ApiController dùng lại các Servlet nghiệp vụ qua /api/*, không sao chép Service. Toàn bộ SQL nghiệp vụ nằm trong DAO; Service điều phối transaction và kiểm tra quyền. DAO ánh xạ domain sang Java Bean qua `BeanMapper`; các danh sách JOIN cho màn hình vẫn là projection `Map`, không thay thế domain model.

Chi tiết đối chiếu báo cáo, CD10–CD12/SQ16–SQ20, quan hệ ERD và những điểm tài liệu tự mâu thuẫn nằm trong [docs/DESIGN_TRACEABILITY.md](docs/DESIGN_TRACEABILITY.md). `model` chính là package `bean (model)` trong bảng mô tả package; `integration` và `listener` chứa adapter/tác vụ khởi động kỹ thuật.

### Thay đổi trên nhánh Nam

- EMR nhận `queueId`, kiểm tra phân công bác sĩ, lưu `DiagnosisRequest`, lịch sử bệnh nhân, khóa bệnh án khi kết thúc.
- Kê đơn sau khi có đủ kết quả cận lâm sàng (nếu có), tìm theo tên/hoạt chất; kê nhiều thuốc bằng `PrescriptionRequest`, kiểm tra tồn kho, chụp giá và lưu chi tiết trong cùng transaction; xem/in/tải PDF đơn thuốc có tiếng Việt.
- In phiếu chỉ định PDF; tạo chỉ định bằng `ClinicalOrderRequest`, hủy trước thực hiện/thanh toán, kể cả khi đã lên hóa đơn UNPAID; xóa đúng dòng phí và tính lại tổng trong cùng transaction. Chặn thay đổi khi giao dịch VNPay còn hiệu lực.
- Kết quả chỉ duyệt sau thanh toán. Quy trình thu hồi cần trưởng khoa đúng chuyên khoa và biên bản PDF; bản thay thế có ID mới, giữ bản gốc. Phần mềm không xác minh chữ ký số trong PDF.
- Hiển thị tiến trình khám suy ra từ bệnh án/chỉ định; thông báo bác sĩ bằng polling 15 giây. Tác vụ nền tổng hợp ngày trước vào `report_daily_summary`, chạy lại không cộng lặp.
- Tải PDF/PNG/JPEG tối đa 5 MB; tệp nằm ngoài webroot, tải qua endpoint kiểm tra quyền theo bệnh án.
- Bệnh nhân xem hồ sơ, kết quả, đơn thuốc và hóa đơn của chính mình tại `/my-records`; đăng nhập bằng `patient@hospital.test`.
- `ReportFilter` lọc ngày/khoa/dịch vụ chỉ định/phương thức, nhóm DAY/MONTH/QUARTER; `PatientFlowReport`/`RevenueReport`/`ChartDataPoint` phục vụ bảng và biểu đồ. Excel có sheet chi tiết + thông tin bộ lọc, PDF nhúng Noto Sans có dấu tiếng Việt.
- Kết thúc khám tự lập/cập nhật hóa đơn cho chi phí còn chưa tính. Hóa đơn đang có giao dịch VNPay được giữ nguyên cho đến khi giao dịch hết hạn/xử lý xong.
- Giao diện chung nền sáng, điều hướng theo vai trò, danh sách chọn bệnh nhân và trạng thái tiếng Việt, bố cục đáp ứng desktop/mobile.

Đọc [hướng dẫn code phần Nam](docs/NAM_CODE_GUIDE.md) để xem từng Controller → Service → DAO, transaction và 5 sequence đồng bộ với code. Các hàm nghiệp vụ quan trọng có comment tiếng Việt.

### Database mới và database cũ

`01-schema.sql` chứa **18 bảng nghiệp vụ** của báo cáo. `04-operational-extensions.sql` thêm **5 bảng kỹ thuật**. `05-nam-workflows.sql` thêm **2 bảng** thu hồi kết quả/tổng hợp ngày và ràng buộc duy nhất cho kết quả đang có hiệu lực. Tổng schema thực thi là **25 bảng**; bảng bổ sung được liệt kê riêng trong tài liệu đối chiếu.

Bản cũ có `users.password_hash` và `medical_record.queue_id`: dùng `database/migrations/20261027-sds-alignment.sql` sau khi sao lưu. Migration giữ dữ liệu, đổi tên cột mật khẩu và bỏ liên kết queue dư; queue được tìm qua `appointment_id`. **Không chạy lại schema/seed/extensions trên database đã có dữ liệu**, và không áp dụng mọi file migrations một cách máy móc. Chọn migration đúng phiên bản nguồn.

Database đã SDS-aligned nhưng chưa có `test_result.status`: sao lưu rồi chạy `database/05-nam-workflows.sql` **một lần** bằng tài khoản có quyền ALTER/CREATE. Không chạy lại 01/02/04. Script 06 chỉ dành cho dữ liệu demo. Compose cũng chỉ tự nạp các file này khi volume trống.

Để chạy cổng 3000 khi phát triển:

```powershell
./scripts/run-dev.ps1 -Port 3000
```

Cấu hình DB qua biến môi trường hoặc file local như hướng dẫn phía trên. Script cần Maven trên PATH và MySQL đã chạy. Không phụ thuộc thư mục `.local` của máy tác giả.

## Quy tắc đã có

- Băm mật khẩu PBKDF2-HMAC-SHA256 với salt; khóa đăng nhập 15 phút sau nhiều lần sai; đổi session khi đăng nhập, logout hủy session.
- Kiểm tra role ở Filter, quyền sở hữu lịch/bệnh án trong Service, CSRF cho POST; React hiển thị nội dung người dùng dưới dạng text và API loại bỏ password/hash/secret.
- Đặt lịch khóa dòng patient và schedule trong transaction; không vượt sức chứa, tối đa 2 lịch chưa hoàn tất khi đặt online; đổi trước 12 giờ, tối đa 2 lần.
- Hủy hơn 3 lần trong tháng tạm chặn đặt online 30 ngày. Hủy do bác sĩ nghỉ không tính như bệnh nhân chủ động hủy.
- Check-in một lần/ngày hẹn, số chờ duy nhất theo ngày/phòng; ưu tiên khẩn cấp, người cao tuổi, sau đó giờ hẹn và số chờ.
- Bác sĩ chỉ sửa bệnh án được phân công và còn OPEN; ICD thuộc danh mục cấu hình; kết thúc khám khóa chỉnh sửa.
- Xét nghiệm cần có chẩn đoán sơ bộ và thanh toán; kết quả đã duyệt không sửa nội dung. Nếu thu hồi được duyệt, nhập một bản thay thế mới và giữ nguyên bản gốc.
- Kê nhiều dòng thuốc trong một transaction; kiểm tra danh mục và tồn kho; trừ kho khi phát hành đơn; lỗi một dòng hoàn tác cả đơn.
- Giá được chụp tại lúc đặt lịch/chỉ định/kê đơn; thay giá danh mục không sửa các chi phí đã phát sinh.
- Hóa đơn tính các dòng chưa được lập hóa đơn; thanh toán idempotent, doanh thu chỉ tính PAID, callback VNPay kiểm tra chữ ký và số tiền.
- Email nằm trong outbox transaction; lỗi gửi không rollback nghiệp vụ. Tác vụ nền chạy mỗi 60 giây, gồm tổng hợp ngày trước theo giờ Việt Nam; sau 5 lần lỗi gửi cần kiểm tra log/outbox.

## Tích hợp ngoài

### VNPay sandbox

Đặt `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET` và `APP_BASE_URL` bằng thông tin tài khoản merchant sandbox. Endpoint trả về: `/payment/return`; endpoint IPN: `/payment/ipn`. Cấu hình IPN với VNPay và dùng HTTPS/tunnel khi nhà cung cấp cần gọi vào máy local. Code xác minh HMAC-SHA512, merchant, mã tham chiếu và số tiền; không đánh dấu PAID chỉ vì người dùng bấm nút. Chưa có merchant thật nên chưa đối soát được với VNPay sandbox ngoài mạng. Tiền mặt chạy độc lập để demo.

### Hệ thống dược (tùy chọn)

`PHARMACY_BASE_URL` bật adapter HTTPS. Hợp đồng tích hợp hiện là POST `/interactions` với các dòng `medicationId,quantity`, trả về cảnh báo dạng từng dòng; POST `/prescriptions` nhận mã đơn để thông báo đồng bộ. Đây là adapter theo hợp đồng của dự án, chưa phải API đã xác nhận với nhà cung cấp bệnh viện. Khi chưa cấu hình, giao diện nói rõ không kiểm tra tương tác tự động; không báo giả đã đồng bộ. Lỗi đồng bộ sau commit được ghi log, không tạo lại đơn hoặc trừ kho lần hai.

`ICD_CATALOG_FILE` có thể trỏ đến danh mục CSV đã được nhóm duyệt (`code,description`). Mặc định chỉ có 4 mã minh họa trong resource, không phải bộ ICD đầy đủ.

### Email

Đặt `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`. SMTP yêu cầu STARTTLS. Nếu chưa cấu hình, email giữ PENDING trong `notification_outbox`. Chưa tích hợp SMS; không có thông báo giả rằng SMS/email đã gửi thành công.

## Kiểm thử

```powershell
mvn test
```

31 test Java chạy mặc định, gồm kiến trúc, Bean, PDF Unicode, tiến trình khám/JSON, bảo vệ dữ liệu API, callback VNPay và 10 trường hợp CSRF/session/phân quyền. 18 integration test trong `WorkflowTest`, `NamWorkflowTest`, `NamRevisionTest` chỉ chạy khi `HOSPITAL_INTEGRATION=1`, `DB_URL` trỏ vào **database riêng tên `hospital_test`**, đã nạp đủ schema/seed/05/06. Test chủ động ghi dữ liệu demo; không dùng database triển khai.

Kiểm thử React: vào frontend, chạy `npm ci` rồi `npm test`. Bộ kiểm thử kiểm tra form/CSRF, giữ nội dung khi lỗi, chặn submit trùng, hiển thị text an toàn, khóa kê đơn khi đang chờ và xử lý lỗi API.

`scripts/smoke_http.py` chạy với Python 3 trên app và database demo mới; kiểm tra hợp đồng JSON React, session/CSRF/phân quyền, trọn luồng khám, file xuất và HTML shell. Script dùng tài khoản demo, có ghi dữ liệu; không chạy trên dữ liệu thực.

Sau script trên, chạy `python scripts/smoke_nam_workflows.py http://localhost:3000/hospital` để kiểm tra thu hồi, tệp lưu lâu dài, nhập kết quả thay thế và thông báo. Cần tài khoản demo của script 06.

Có kiểm thử riêng cho quyền đọc hồ sơ/tệp, kết quả khóa sau duyệt, hủy chỉ định, bộ lọc/nhóm doanh thu và báo cáo không dữ liệu.

Chi tiết kết quả kiểm tra và những phần còn cần phát triển nằm trong `docs/VALIDATION.md` và `docs/SCOPE.md`.
