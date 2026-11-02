# Software Architecture và Package Diagram theo bản JSP

File chỉnh sửa trực tiếp: `E:/SWD392/DESIGN/Hospital_Software_Architecture.vpp`. Cùng một project chứa **1.1 Software Architecture** và **1.2 Package Diagram**. Các hình/lớp/đường nối là thành phần Visual Paradigm có thể chỉnh sửa, không phải ảnh nhúng.

## Luồng chạy

Browser → `SecurityFilter.doFilter()` → `FilterChain.doFilter()` → Servlet → Service → DAO → MySQL. Servlet đặt request attributes, `BaseController.view()` forward tới `/WEB-INF/views/*.jsp`; JSP/JSTL đọc model và render HTML về Browser. CSS/JavaScript ở `webapp/assets` bổ sung giao diện/tương tác. Một số endpoint redirect hoặc tải PDF/XLSX/tệp thay vì forward JSP.

Filter kiểm tra HttpSession, vai trò và CSRF trước Servlet. AuthUser được giữ trong session; không có JWT/Redis trong bản này. Đường Browser → Filter → Controller ở Architecture mô tả thứ tự runtime.

Service xử lý quy tắc nghiệp vụ, giao dịch qua `Database`, gọi DAO và adapter. DAO dùng JDBC PreparedStatement, ánh xạ Java Beans/DTO; một số màn hình JOIN dùng Map projection. EMR nhận `queueId`; `QueueContextPort` là adapter nội bộ gọi QueueDAO, không phải một dịch vụ triển khai độc lập.

`BackgroundJobs` là listener khởi tạo scheduler, gọi MaintenanceService và tổng hợp báo cáo ngày trước. Service ghi email vào `notification_outbox`, tác vụ nền gửi qua EmailClient khi có SMTP. VNPayClient và PharmacyClient gọi nhà cung cấp bên ngoài khi được cấu hình. Pharmacy là tùy chọn; code hiện chưa có adapter SMS.

Architecture: nét liền biểu diễn lời gọi/giao tiếp; nét đứt biểu diễn sử dụng model/helper. Không thể hiện toàn bộ giá trị trả về/callback để giữ sơ đồ dễ đọc.

## Package phụ thuộc gì

| Package thật trong `src/main/java/vn/hospital` | Package ứng dụng được tham chiếu trực tiếp |
|---|---|
| controller | service, model, util |
| service | dao, integration, model, util |
| dao | model, util |
| filter | model |
| integration | dao, model, util |
| listener | service |
| util | model |
| model | Không phụ thuộc package ứng dụng khác |

Tổng 15 quan hệ tham chiếu trực tiếp giữa 8 Java package. Sơ đồ có thêm web resources JSP/JSTL/CSS/JS; đây là thư mục tài nguyên web, không phải Java package. Controller phụ thuộc JSP qua đường dẫn forward; JSP đọc model qua EL/JSTL.

**Package Diagram thể hiện phụ thuộc source, không thể hiện thứ tự HTTP.** `filter` không import `controller`: Tomcat điều phối `FilterChain` tới Servlet theo mapping. Vì vậy không thêm một Java dependency giả từ filter sang controller; quan hệ chạy trước được thể hiện rõ ở Architecture và phần mô tả của package filter trong `.vpp`.

Các thành phần cũ React, React Bootstrap, ApiController và FrontendResponse đã được thay bằng cấu trúc JSP/Servlet hiện có. Không sửa class/sequence nghiệp vụ trong lần cập nhật này.
