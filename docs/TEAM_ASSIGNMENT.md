# Phân công triển khai cho 4 người

Mỗi người phụ trách 3 class diagram và 5 sequence như bảng task assignment trước đó. Cả nhóm dùng chung package gốc `vn.hospital`, schema MySQL và quy ước Controller → Service → DAO. Không tách thành 4 ứng dụng riêng.

| Người | Detailed Design | Class | Sequence | Source chính |
|---|---|---|---|---|
| **1 · Hồ Quốc Khánh** | 3.1 Authentication; 3.2 Search; 3.3 Feedback | CD01–CD03 | SQ01 Login; SQ02 Logout; SQ03 Authorization; SQ04 Search; SQ05 Feedback | `AuthController`, `SecurityFilter`, `AuthService`, `UserDAO`, `SearchController`, `SearchService`, `SearchDAO`, `FeedbackController`, `FeedbackService`, React KhanhPages.jsx |
| **2 · Võ Công Tráng** | 3.4 Appointment; 3.5 Catalog; 3.6 Billing | CD04–CD06 | SQ06 Create; SQ07 Reschedule; SQ08 Cancel; SQ09 Catalog; SQ10 Invoice & Payment | `AppointmentController/Service/DAO`, `ServiceCatalogController`, `CatalogService`, `BillingController/Service`, `InvoiceDAO`, `PaymentService`, `VNPayClient`, `PaymentCallbackController`, React TrangPages.jsx |
| **3 · Nguyễn Đình Kiên** | 3.7 Schedule; 3.8 Reception; 3.9 Queue | CD07–CD09 | SQ11 Setup Schedule; SQ12 Leave; SQ13 Capacity; SQ14 Reception; SQ15 Queue | `ScheduleController/Service/DAO`, `ReceptionController`, `QueueController`, `QueueService`, React KienPages.jsx |
| **4 · Nguyễn Hoài Nam** | 3.10 EMR & Prescription; 3.11 Clinical/Lab; 3.12 Reports | CD10–CD12 | SQ16 Diagnosis; SQ17 Prescription; SQ18 Order Tests; SQ19 Lab Result; SQ20 Reports | `EMRController/Service`, `MedicalRecordDAO`, `QueueContextPort`, `PrescriptionController/Service`, `ClinicalService`, `ClinicalOrderController`, `LabResultController`, `ReportController/DAO`, `AnalyticsService`, `ReportExportService`, React NamEmrPage/NamLabPages/NamRecordPages/NamReportPage |

## Hợp đồng phối hợp

- **Kiên → Nam:** `QueueService.callNext()` chuyển queue sang `IN_CONSULTATION`, tạo bệnh án duy nhất và trả `queueId`. Nam nhận `queueId`, không cấp số chờ trong EMR.
- **Nam → Tráng:** `clinical_order.unit_price` và `prescription_detail.unit_price` là giá chụp lúc phát sinh. Billing chỉ đọc các chi phí chưa xuất hóa đơn, không lấy lại giá mới từ danh mục.
- **Tráng → Nam:** thanh toán cập nhật `clinical_order.payment_status='PAID'`; kỹ thuật viên được duyệt kết quả sau đó.
- **Nam → Khánh:** đóng bệnh án đặt `closed_at`; Feedback kiểm tra cùng bệnh nhân, trạng thái CLOSED, thời hạn 7 ngày và ràng buộc UNIQUE(record_id).
- **Khánh → cả nhóm:** `AuthUser` chứa userId và role trong HttpSession; tất cả POST gửi CSRF. Không tin doctorId/patientId từ form khi đó là thông tin người đang đăng nhập.
- **Cả nhóm:** thay đổi schema phải thêm migration mới và thông báo cho nhóm; không chỉnh script seed rồi nhập lại vào database đã có dữ liệu. File `.env`/`application-local.properties` không commit.

## Chia nhánh Git khi bắt đầu dùng repository

Khánh: `feature/khanh-auth-search-feedback`; Tráng: `feature/trang-appointments-billing`; Kiên: `feature/kien-schedules-queue`; Nam: `feature/nam-emr-reports`. Đây là tên gợi ý khi tách nhánh. Bản refactor hiện làm trên nhánh `nam` của repository nhóm.

Mỗi người chạy `mvn test`, demo một luồng thành công và một luồng bị chặn của phần mình trước khi gộp code. Những hạng mục chưa triển khai đủ theo SRS nằm trong `SCOPE.md`, dùng làm backlog tiếp theo.

## Frontend dùng chung

Cả 4 người dùng React + React Bootstrap trong frontend/. App.jsx quản lý phiên/điều hướng; components.jsx và api.js dùng chung. Xem REACT_FRONTEND.md để build, gọi API và cập nhật sơ đồ View theo yêu cầu mới.
