# Hospital Management — Servlet + JSP

Ứng dụng demo 4 main workflow bằng **Java 17, Servlet 6, JSP/JSTL, JDBC, MySQL 8.4+ và Tomcat 10.1**. UI chỉ dùng HTML, CSS và JavaScript thuần; không có React, Bootstrap, Node/npm hoặc bộ bài test.

## Người mới pull code: làm đúng 3 bước

### 1. Nạp đúng một file database

Mở **`database/hospital.sql`** trong MySQL Workbench bằng **File → Open SQL Script**, rồi chọn **Query → Execute (All or Selection)** để chạy nguyên file.

File này đã có **CREATE DATABASE, CREATE TABLE và INSERT dữ liệu trình bày**. Không có procedure/DELIMITER, file seed phụ, migration hoặc backup. Tên database thống nhất là **`hospital`**. File không tự xóa hoặc ghi đè database đang có. Chỉ nạp một lần khi `hospital` chưa có bảng. Sau đó mọi thao tác demo thực hiện trên web, không nạp lại SQL để reset.

Có thể dùng PowerShell thay Workbench:

```powershell
.\scripts\setup-database.ps1 -Port 3306 -User root
```

Đổi port/user theo kết nối MySQL trên máy bạn. Script sẽ hỏi mật khẩu MySQL, không sinh thêm file database.

### 2. Điền cấu hình MySQL của máy mình

Copy `src/main/resources/application-local.properties.example` thành **`src/main/resources/application-local.properties`**, rồi sửa:

```properties
DB_URL=jdbc:mysql://127.0.0.1:3306/hospital?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
DB_USER=root
DB_PASSWORD=mat_khau_mysql_cua_may_ban
APP_BASE_URL=http://localhost:3000/hospital
```

`DB_PASSWORD` là mật khẩu **MySQL**, không phải mật khẩu tài khoản web. Đổi `3306` nếu server dùng port khác. File cấu hình riêng này được Git bỏ qua, vì vậy pull code không tự có mật khẩu của máy người khác.

### 3. Chạy web

Cấu hình chung của nhóm là **JDK 17 + Tomcat 10.1**. Cài JDK 17, Maven và MySQL; đặt `JAVA_HOME` tới thư mục JDK. JDK 21/25 cũng có thể build, nhưng Maven luôn xuất bytecode Java 17 nhờ `maven.compiler.release=17` trong `pom.xml`. Không sửa giá trị này theo JDK riêng của từng máy.

Từ thư mục dự án, sau khi dừng server cũ:

```powershell
.\scripts\run-dev.ps1
```

Mở **http://localhost:3000/hospital/login**. Script build WAR rồi chạy Tomcat local; không cần cấu hình Tomcat trong IDE. Nếu dùng Maven trực tiếp:

```powershell
mvn clean package
mvn exec:java
```

Dừng web bằng Ctrl+C. Script tự build sạch rồi chạy Tomcat **10.1.61** đi kèm Maven trên cổng 3000, không cần cài/configure Tomcat riêng. Chỉ chạy một server trên cổng 3000.

Nếu chạy qua IDE/Tomcat cài riêng: chọn **JDK 17** cho Project/Maven và Java chạy Tomcat; dùng Tomcat **10.1**, không dùng Tomcat 9. Undeploy bản cũ qua IDE/Tomcat Manager rồi deploy **`target/hospital.war` vừa build**. IDE có thể giữ cấu hình JDK hoặc thư mục deploy riêng; pull code không tự đổi được các cấu hình đó.

Khi cập nhật code: `git pull` trên nhánh của nhóm, giữ cấu hình MySQL riêng của mình, dừng web và chạy lại `scripts/run-dev.ps1`. **Không nạp lại SQL nếu `hospital` đã có đầy đủ bảng**. Nếu trước đó dùng `hospital_demo`, bản cấu hình mới dùng `hospital`: nạp file SQL vào `hospital` chưa có bảng và đổi URL theo bước 2. Nếu `hospital` đã chứa bảng của phiên bản cũ, không nhập chồng file SQL; cần đối chiếu schema trước khi cập nhật để giữ dữ liệu.

## Tài khoản web

Tất cả tài khoản bên dưới có mật khẩu **`Demo@1234`**.

| Vai trò | Email |
|---|---|
| Bệnh nhân mới để demo từ đầu | `patient@hospital.test` |
| Bệnh nhân đã đặt lịch lúc nạp SQL | `patient2@hospital.test` |
| Bệnh nhân dự phòng | `patient3@hospital.test` |
| Bác sĩ demo từ đầu | `doctor@hospital.test` |
| Bác sĩ có sẵn bệnh án/kết quả | `doctor2@hospital.test` |
| Lễ tân | `reception@hospital.test` |
| Kỹ thuật viên | `lab@hospital.test` |
| Thu ngân | `cashier@hospital.test` |
| Quản lý | `manager@hospital.test` |
| Quản trị | `admin@hospital.test` |
| Trưởng khoa cho chức năng thu hồi kết quả | `head@hospital.test` |

Không nhập tài khoản MySQL vào trang đăng nhập web. Đổi vai trò bằng đăng xuất/đăng nhập; nếu cần mở hai vai trò đồng thời, dùng hai trình duyệt/profile riêng vì các tab cùng trình duyệt dùng chung session.

## Bốn main workflow

1. **Khám bệnh:** tìm bác sĩ → đặt lịch → lễ tân check-in → bác sĩ gọi vào khám → chẩn đoán sơ bộ → chỉ định lẻ/gói → thu phí → Lab duyệt kết quả → kê đơn → kết thúc → thu khoản còn lại → xem hồ sơ/đánh giá.
2. **Lịch bác sĩ:** tạo ca đơn hoặc lặp tuần/tháng → kiểm tra trùng → công bố ca → bác sĩ gửi lý do nghỉ → quản lý duyệt/từ chối → cập nhật ca và lịch hẹn bị ảnh hưởng.
3. **Danh mục/giá:** quản lý thêm/sửa/ẩn dịch vụ hoặc gói → kiểm tra thông tin/thành phần → lưu. Giá mới không đổi chi phí đã phát sinh.
4. **Báo cáo:** quản lý chọn lượt khám/doanh thu, ngày/khoa/phương thức → xem bảng và biểu đồ → tùy chọn tải PDF/XLSX. Doanh thu chỉ tính PAID.

Các bước bấm màn hình và chia phần trình bày: [DEMO_MAIN_FLOW.md](docs/DEMO_MAIN_FLOW.md). Hàm tương ứng CD/SQ và dữ liệu vào/ra: [WORKFLOW_CODE_GUIDE.md](docs/WORKFLOW_CODE_GUIDE.md).

## Cấu trúc dễ tìm

`filter → controller → service → dao → MySQL`; Controller chuyển dữ liệu sang JSP để hiển thị. `model` là Java Bean/request/report model; `util` chứa validation/transaction; `integration` chứa kết nối ngoài; `listener` chứa tác vụ nền. `LocalServer` chỉ mở Tomcat local, không xử lý nghiệp vụ.

Các hàm chính có comment tiếng Việt ghi rõ SQ liên quan, đầu vào, điều kiện xử lý và đầu ra. Tên lớp theo chức năng, không theo tên thành viên.

SQL giữ 18 bảng nghiệp vụ của ERD và 8 bảng hỗ trợ triển khai: bộ đếm số, dòng phí, giao dịch, outbox, audit, thu hồi, tổng hợp ngày và thành phần gói. Các cột duyệt nghỉ đặt trong `work_schedule`; gói vẫn dùng entity `MedicalService`, liên kết thành phần qua `service_package_item`.

## Nếu chưa chạy được

- **Không kết nối DB / Access denied:** MySQL phải đang chạy; so lại host, port, `DB_USER`, `DB_PASSWORD` với Workbench.
- **UnsupportedClassVersionError / class file version 63 hoặc 69:** đang deploy file build Java 19/25 vào Java 17. Dừng/undeploy bản cũ, dùng `mvn clean package` của cấu hình mới rồi deploy lại WAR. Kiểm tra `java -version`, `mvn -version` và JDK của Tomcat trong IDE.
- **Unknown database / table / column:** kiểm tra đã nạp `database/hospital.sql` và URL trỏ đúng `hospital` và schema tương ứng với bản code mới.
- **Đã sửa URL nhưng vẫn báo database `hospital_demo`:** kiểm tra biến môi trường `DB_URL` của terminal/IDE/Tomcat; biến môi trường được ưu tiên hơn file properties. Sửa biến đó, build và khởi động lại server.
- **Table already exists:** database đã có bảng; dừng nhập SQL. Nếu đã nạp đúng bản hiện tại thì chỉ cần cấu hình và chạy web. Nếu là schema cũ, cần đối chiếu trước khi cập nhật; không tự xóa dữ liệu.
- **Cổng 3000 đang dùng:** dừng bản web cũ trước khi mở bản mới.
- **Không nhận lịch / không gọi người tiếp theo:** chọn lịch đúng hôm nay; ca phải còn giờ/chỗ; bác sĩ hoàn tất ca đang khám trước khi gọi người khác. Có thể tạo thêm ca bằng màn hình Lịch bác sĩ.

## Phạm vi demo

Dùng tiền mặt để đi hết luồng mà không cần tài khoản dịch vụ bên ngoài. VNPay/email/dược là tùy chọn, chỉ hoạt động khi được cấu hình; không báo giả đã thanh toán hoặc đã gửi ra ngoài. Bản này dùng HttpSession theo kiến trúc Servlet/JSP, không triển khai JWT/Redis. SMS/LIS thật, BHYT/hoàn tiền, QR và các quy tắc vận hành nâng cao không thuộc kịch bản demo cơ bản.

Có thể chạy Docker bằng `docker compose up --build`: web vẫn ở localhost:3000, MySQL host port 3307. Docker chỉ tự nạp cùng file SQL khi volume mới; không xóa volume đang có dữ liệu để cập nhật code.
