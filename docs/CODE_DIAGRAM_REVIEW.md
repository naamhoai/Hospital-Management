# Đối chiếu code với class và sequence — 31/10/2026

## Kết luận

Luồng nghiệp vụ chính đã chạy được qua Servlet + JSP, nhưng source chưa khớp từng lớp/từng lời gọi với toàn bộ hình trong SDS. Không thể kết luận “đã khớp 100%”. CD10–12 gần code nhất; CD01–09 còn nhiều tên lớp, công nghệ và bước xử lý khác implementation.

Code kiểm tra: nhánh `main`, commit `0d32856` (đã merge hoàn tác React). Chưa sửa source Java/JSP hoặc sơ đồ trong lần kiểm tra này. File này ghi nhận hiện trạng, không thay thế SRS/SDS.

Nguồn đối chiếu trực tiếp:

- [SRS — Report 1](https://docs.google.com/document/d/1wGyYYJdDnR7CLNjlmsUIBLMdyoLoOzIs/edit).
- [SDS — Report 2 v3.0](https://docs.google.com/document/d/1zj8LAZlByDeXaiw5AxKfLHPY45NiM_uS/edit), bản sửa 30/10/2026. Đã tải DOCX và xem các hình CD01–12 và SQ01–20, không chỉ đọc tên mục.
- Có hai hình check-in và walk-in trong cùng mục 3.8.2.1; ở bảng dưới gọi chung SQ14. Mục 3.13 là đặc tả entity, không phải một feature/class diagram thứ 13.

## 1. Cách đọc luồng chung

```text
Người dùng thao tác trên JSP
  → HTTP request
  → SecurityFilter: phiên đăng nhập, vai trò và CSRF
  → Servlet.doGet/doPost
  → Service: kiểm tra dữ liệu, quyền, quy tắc nghiệp vụ, transaction
  → DAO: SQL/JDBC
  → MySQL
  → kết quả về Service và Servlet
  → forward JSP hoặc redirect rồi GET lại JSP
```

Class diagram mô tả cấu trúc các lớp, dữ liệu, trách nhiệm và phụ thuộc. Sequence mô tả thứ tự tương tác của một kịch bản. Một class diagram có thể phục vụ nhiều sequence. Servlet dùng `doGet/doPost` là đúng cơ chế framework; không cần tạo các hàm Controller giả chỉ để có tên giống UML. Tuy nhiên tên đối tượng trên lifeline và các lời gọi Service/DAO vẫn phải phản ánh implementation hoặc được ghi rõ là thiết kế khái niệm.

## 2. Toàn bộ 12 class / 20 sequence tương ứng với code nào

| Người | Mục / Class | Sequence trong báo cáo | Đường xử lý thực tế | Đánh giá |
|---|---|---|---|---|
| Khánh | 3.1 / CD01 Authentication | SQ01 Login; SQ02 Logout; SQ03 Authorization | `login.jsp` → `AuthController` → `AuthService.login` → `UserDAO.findByEmail`; đăng xuất dùng `HttpSession.invalidate`; phân quyền ở `SecurityFilter` | Có chức năng nhưng khác thiết kế JWT/Redis và vị trí kiểm tra quyền. |
| Khánh | 3.2 / CD02 Search | SQ04 Search Doctors and Services | `search.jsp` → `SearchController` → `SearchService.searchDoctorsAndServices` → `SearchDAO.doctors/services` | Có tìm kiếm cơ bản; hình còn `BookingController/Service/Repository`, Redis hold và `SearchCriteria/SearchResult` chưa có trong đường xử lý. |
| Khánh | 3.3 / CD03 Feedback | SQ05 Submit Patient Feedback | `feedback.jsp` → `FeedbackController` → `FeedbackService.submitFeedback` → `FeedbackDAO` + `DoctorDAO.recalculateRating` | Quy tắc đánh giá đã có; hình vẫn dùng Booking và `bookingId`, code nhận `recordId`. |
| Tráng | 3.4 / CD04 Appointment | SQ06 Create; SQ07 Reschedule; SQ08 Cancel | `appointments.jsp` → `AppointmentController` → `AppointmentService.createAppointment/rescheduleAppointment/cancelAppointment` → `AppointmentDAO` + `ScheduleDAO` + `PatientDAO` | Các thao tác chạy được. Không có TicketHold/Redis; giờ trống được tự cấp. Nhánh hủy thiếu hạn hủy và lý do. |
| Tráng | 3.5 / CD05 Catalog | SQ09 Manage Service Catalog and Pricing | `services.jsp` → `ServiceCatalogController` → `CatalogService.save` → `DepartmentDAO` + `MedicalServiceDAO` + `AuditDAO` | Luồng lưu/đổi giá có. Hình gọi `ServiceCatalogService` và `AuditLogger`, khác tên lớp code. |
| Tráng | 3.6 / CD06 Billing | SQ10 Create Invoice and Process Payment | `billing.jsp` → `BillingController` → `BillingService.createInvoice/processCashPayment` → `InvoiceDAO`, `ClinicalOrderDAO`, `PrescriptionDAO`; online tách `PaymentService`, `VNPayClient`, `PaymentCallbackController` | Tiền mặt chạy được. Thiết kế cần cập nhật lớp xử lý online, dòng hóa đơn, payment attempt và trạng thái UNPAID. |
| Kiên | 3.7 / CD07 Schedule | SQ11 Setup Periodic Schedule; SQ12 Leave; SQ13 Capacity | `schedules.jsp` → `ScheduleController` → `ScheduleService.createSchedule/requestLeave/adjustCapacity` → `ScheduleDAO`, `AppointmentDAO`, `DoctorDAO` | Có tạo/hủy ca, chỉnh sức chứa. Chưa có lịch định kỳ theo khoảng ngày, entity/bảng TimeSlot hoặc quy trình duyệt nghỉ. |
| Kiên | 3.8 / CD08 Reception | SQ14 Patient Reception: check-in và walk-in | `reception.jsp` → `ReceptionController` → `ReceptionService` → `QueueService.checkIn/registerWalkIn` → `AppointmentDAO`, `QueueDAO`, `DoctorDAO`; walk-in gọi `AppointmentService.book` | Check-in chạy được. Walk-in chọn bệnh nhân đã có tài khoản; chưa tạo hồ sơ bệnh nhân mới, chưa quét QR/WebSocket. |
| Kiên | 3.9 / CD09 Queue | SQ15 Auto Queue and Routing | `queue.jsp` → `QueueController` → `QueueService.routePatient/callNext` → `QueueDAO`, `RoomDAO`, `MedicalRecordDAO` | Có chuyển phòng thủ công, cấp số và tiếp nhận khám. Chưa có thuật toán chọn phòng ít bận và WebSocket như hình. |
| Nam | 3.10 / CD10 EMR & Prescription | SQ16 Clinical Exam and Diagnosis; SQ17 Issue E-Prescription | `emr.jsp` → `EMRController` → `EMRService`; `prescription.jsp` → `PrescriptionController` → `PrescriptionService`; DAO bệnh án, thuốc, đơn và chi tiết | Khớp phần domain và nghiệp vụ chính, còn khác vị trí kiểm tra queue, chữ ký request, bước GET workspace và các điều kiện bổ sung. |
| Nam | 3.11 / CD11 Clinical/Lab | SQ18 Order Clinical Tests; SQ19 Update Lab Results | `ClinicalOrderController` / `LabResultController` → `ClinicalService` → `MedicalRecordDAO`, `MedicalServiceDAO`, `ClinicalOrderDAO`, `TestResultDAO` | Khớp tạo chỉ định, điều kiện PAID, lưu kết quả và COMPLETED. Cần đồng bộ lịch sử thu hồi và sửa trùng dòng danh sách sau thay thế. |
| Nam | 3.12 / CD12 Reports | SQ20 Patient Flow and Revenue Reports | `reports.jsp` → `ReportController` → `AnalyticsService.generateReport/exportReport` → `ReportDAO` / `ReportExportService` | Class khá sát. Sequence hiện chủ yếu vẽ flow, chưa thể hiện đủ revenue, nhánh xuất tùy chọn và trường hợp dữ liệu rỗng. |

## 3. Thứ tự demo liên tục và sơ đồ cần mở

| Bước | Ai thao tác / màn hình | Thay đổi / kết quả | Class / Sequence |
|---|---|---|---|
| 1 | Bác sĩ hoặc Admin → Ca làm việc | Có ca khám trong ngày, còn giờ và sức chứa | CD07 / SQ11; SQ13 nếu chỉnh sức chứa |
| 2 | Bệnh nhân → Đăng nhập | Xác thực; session chứa `AuthUser` và CSRF | CD01 / SQ01; SQ03 đi kèm các request được bảo vệ |
| 3 | Bệnh nhân → Tìm bác sĩ & dịch vụ | Danh sách bác sĩ/dịch vụ phù hợp | CD02 / SQ04 |
| 4 | Bệnh nhân → Lịch khám → Đặt lịch | `Appointment=BOOKED`; nhận mã lịch và giờ được cấp | CD04 / SQ06 |
| 5 | Lễ tân → Tiếp nhận → Check-in | `Appointment=CHECKED_IN`, `Queue=WAITING`; có số và phòng | CD08 / SQ14; có sử dụng code cấp số thuộc CD09 |
| 6 | Bác sĩ → Hàng đợi → Gọi bệnh nhân tiếp theo | `Queue=IN_CONSULTATION`; tạo bệnh án OPEN; redirect `/emr?queueId=...` | CD09 nối CD10. Cần thể hiện rõ bước `callNext/createDraftForQueue` trên sequence; hình SQ15 hiện chủ yếu mô tả tự phân phòng. |
| 7 | Bác sĩ → Bệnh án → Lưu chẩn đoán | Lưu triệu chứng, ICD, chẩn đoán, ghi chú | CD10 / SQ16 |
| 8 | Bác sĩ → Chỉ định dịch vụ | `ClinicalOrder=PENDING`, `paymentStatus=PENDING_PAYMENT`; chụp giá | CD11 / SQ18 |
| 9 | Thu ngân → Lập hóa đơn → Tiền mặt | `Invoice=PAID`; các chỉ định thuộc hóa đơn chuyển PAID | CD06 / SQ10 |
| 10 | Kỹ thuật viên → Xét nghiệm → Lưu & duyệt | Tạo TestResult APPROVED; order COMPLETED; khóa sửa trực tiếp | CD11 / SQ19 |
| 11 | Bác sĩ → Mở lại bệnh án, xem kết quả → Kê đơn | Lưu Prescription + Details, trừ kho, xem/in/PDF | CD10 / SQ17 |
| 12 | Thu ngân → Lập hóa đơn phần thuốc mới → Thanh toán | Không thu lại chi phí đã nằm trên hóa đơn trước | CD06 / SQ10 lặp lại |
| 13 | Bác sĩ → Kết thúc khám | MedicalRecord CLOSED; Queue và Appointment COMPLETED | Mở rộng CD10/CD09; chưa có sequence riêng trong 20 SQ |
| 14 | Bệnh nhân → Đánh giá | Một đánh giá/bệnh án, trong 7 ngày; cập nhật điểm bác sĩ | CD03 / SQ05 |
| 15 | Quản lý → Báo cáo → Lọc/xuất | Lượt khám hoặc doanh thu PAID; biểu đồ, XLSX/PDF | CD12 / SQ20 |

SQ07–08 là nhánh thay đổi lịch, SQ09 là chuẩn bị danh mục/giá, SQ12–13 là vận hành ca. Không phải mọi SQ đều xảy ra trong cùng một lượt khám. Nếu không có chỉ định, bỏ bước xét nghiệm; kê đơn sau chẩn đoán hợp lệ. Khi có chỉ định, phải đủ kết quả hoặc hủy chỉ định chưa thực hiện trước khi kê đơn/kết thúc.

## 4. Phần Nam: đường gọi thực sự để mở code khi thuyết trình

### CD10 / SQ16 — Chẩn đoán

- Đọc màn hình: `EMRController.doGet` gọi `EMRService.workspace(queueId,q)`. Bên trong đọc `QueueContextPort.getContext`, `MedicalRecordDAO.findByQueueIdRead`, lịch sử đã đóng và thông tin bệnh nhân. HTTP GET hiện không gọi trực tiếp `getRecord/getPatientHistory`, dù các API domain đó vẫn tồn tại.
- Lưu: `EMRController.doPost(action=diagnosis)` tạo `DiagnosisRequest` → `EMRService.recordClinicalDiagnosis(queueId,request)`.
- Service kiểm tra trường bắt buộc/ICD, lấy QueueContext, kiểm tra đúng bác sĩ và IN_CONSULTATION, kiểm tra bệnh án OPEN, rồi `MedicalRecordDAO.save` trong transaction.
- Lỗi trả 422 và giữ dữ liệu form chẩn đoán. Thành công redirect rồi GET lại `emr.jsp`.
- Hình SQ16 đang đặt `verifyQueueContext` ở Controller; code và CD10 đặt tại Service bằng `getContext`. Cần sửa lifeline/lời gọi của sequence cho thống nhất.

### CD10 / SQ17 — Kê đơn

- Tìm thuốc hiện đi qua GET `/emr?queueId=...&medicationKeyword=...`: `EMRController` → `PrescriptionService.searchMedication` → `MedicationDAO`.
- POST `/prescriptions`: `PrescriptionController` gom nhiều dòng thuốc thành `PrescriptionRequest` → `PrescriptionService.issuePrescription`.
- Kiểm tra đúng bác sĩ, OPEN, có chẩn đoán, không còn xét nghiệm chưa xong, dòng thuốc/liều/số lượng/danh mục/tồn kho. Khóa thuốc, lưu `PrescriptionDAO.saveWithDetails`, `MedicationDAO.deductStock` trong cùng transaction.
- `PharmacyClient` chỉ gọi nếu được cấu hình; chưa cấu hình không đồng nghĩa đã kiểm tra tương tác thuốc thực tế.
- GET đơn trả `prescription.jsp`; `format=pdf` gọi `printPrescription` rồi stream file. Sequence cần dùng `PrescriptionRequest`, bổ sung MedicalRecordDAO/ClinicalOrderDAO cho các guard quan trọng, và thống nhất `saveWithDetails` với CD10.

### CD11 / SQ18 — Chỉ định

- POST `/clinical-orders`: `ClinicalOrderController` tạo `ClinicalOrderRequest` → `ClinicalService.createClinicalOrders`.
- Kiểm tra bệnh án OPEN/có chẩn đoán/đúng bác sĩ; kiểm tra dịch vụ ACTIVE, không trùng và lấy giá qua `MedicalServiceDAO` → `ClinicalOrderDAO.saveAll`.
- Bệnh án/đơn thuốc không bị đưa vào phần vận hành; queueId chỉ dùng để quay về đúng màn hình bệnh án.
- Cần bổ sung `MedicalServiceDAO` và guard chẩn đoán vào sequence. Hủy chỉ định là một nhánh bổ sung; khi hóa đơn UNPAID đã chứa phí đó, code tính lại hóa đơn cùng transaction.

### CD11 / SQ19 — Kết quả xét nghiệm

- `LabResultController.doPost` tạo TestResult và đọc tệp tùy chọn → `ClinicalService.recordTestResult`.
- Chỉ kỹ thuật viên, chỉ định PAID và PENDING/RESULT_RECALLED, chưa có kết quả hiệu lực mới được duyệt.
- `TestResultDAO.save` → `ClinicalOrderDAO.updateExecutionStatus(...,COMPLETED)`; audit/outbox cùng transaction. Không sửa bản đã APPROVED trực tiếp.
- Thu hồi qua `ResultRecallController/Service`, trưởng khoa duyệt kèm PDF, bảo tồn bản gốc và tạo bản thay thế. Đây là luồng mở rộng từ BR14, chưa có một SQ riêng trong báo cáo.
- Class/ERD nên ghi: một chỉ định có 0..1 kết quả **hiệu lực**, và có thể 0..* kết quả lịch sử.

### CD12 / SQ20 — Báo cáo

- `ReportController.doGet` tạo ReportFilter → `AnalyticsService.generateReport(type,filter)`.
- `type=flow`: `getPatientFlowReport` → `ReportDAO.queryPatientFlowMetrics`, tính tỷ lệ và ChartDataPoint. `type=revenue`: `getRevenueReport` → `queryRevenueMetrics`, chỉ tổng hợp hóa đơn PAID.
- Mặc định trả `reports.jsp`. Chỉ khi có `format=pdf/xlsx` mới gọi `exportReport` → `ReportExportService`.
- Ngày sai trả 422. Không có dữ liệu vẫn trả báo cáo rỗng hợp lệ, không coi là lỗi nghiệp vụ. Đã kiểm tra cả màn hình rỗng và xuất XLSX rỗng.
- SQ20 cần `alt flow/revenue`, `opt export`, tách nhánh lỗi filter khỏi nhánh không có dữ liệu. Batch tổng hợp ngày trước là luồng nền riêng, không phải mỗi lần xem báo cáo đều ghi dữ liệu.

## 5. Những điểm cần sửa để đồng bộ

### A. Hai vấn đề đã tái hiện khi chạy

1. **SQ08 / hủy lịch:** `AppointmentService.cancelAppointment` chỉ kiểm tra sở hữu và BOOKED, chưa kiểm tra giờ/hạn hủy; form không nhận lý do. Đã thử một lịch BOOKED giả thuộc ngày hôm qua, booked_count hợp lệ: POST trả 302 và trạng thái thành CANCELLED. Hình SQ08 có nhánh `verifyCancellationDeadline`, danh sách UC04 yêu cầu lý do/hạn hủy. Cần thống nhất hạn hủy rồi triển khai; không tự lấy quy tắc 12 giờ của **đổi lịch** áp sang hủy. Đặc tả UC04 chi tiết trong SRS còn bị chép nhầm nội dung đăng ký ca bác sĩ, cũng cần sửa.
2. **CD11 / danh sách xét nghiệm sau thu hồi:** `ClinicalOrderDAO.worklist` LEFT JOIN mọi dòng TestResult theo order_id. Sau một bản RECALLED và một bản APPROVED, một order xuất hiện hai dòng/link trên `/lab`. Đã xác nhận cả kết quả SQL lẫn HTML. Nên JOIN chỉ bản APPROVED cho danh sách hiện hành; lịch sử xem riêng.

### B. Các khác biệt thiết kế cần quyết định

| Phạm vi | Hình đang vẽ | Implementation hiện tại / hướng đồng bộ |
|---|---|---|
| CD01 / SQ01–03 | JwtProvider, TokenClaims, RedisSessionService, AuthSession; authorization qua AuthController/AuthService | HttpSession + AuthUser; PBKDF2 qua Passwords; user/role đọc bởi UserDAO; SecurityFilter kiểm tra quyền trước Servlet. Chọn cập nhật hình theo code hoặc triển khai thật JWT/Redis; không chỉ đổi nhãn. |
| CD02–03 / SQ04–05 | BookingController/BookingService/BookingRepository dùng cho cả search và feedback | Tách SearchController/Service/DAO và FeedbackController/Service/DAO. Feedback dùng recordId. Search chưa có specialtyId riêng và Redis hold. |
| CD04 / SQ06–08 | TicketHoldService, WorkScheduleDAO, chọn appointmentTime, NotificationClient gọi trực tiếp | ScheduleDAO + khóa SQL/transaction; tự cấp giờ trống; notification_outbox → MaintenanceService → EmailClient. Thứ tự INSERT/cập nhật booked_count cùng transaction khác hình nhưng vẫn nguyên tử. |
| CD05 / SQ09 | ServiceCatalogService, AuditLogger | CatalogService và AuditDAO. Quyền code: ADMIN/MANAGER; SRS còn khác nhau giữa Manager và Admin/Accountant, cần chốt. |
| CD06 / SQ10 | BillingDAO, BillableItem, PaymentResult; BillingService xử lý toàn bộ online; ảnh ghi invoice PENDING | Code gom phí qua InvoiceDAO/invoice_line; invoice UNPAID; online tách PaymentService và PaymentCallbackController; payment_attempt bảo vệ callback/lặp. Cập nhật thanh toán các khoản thuộc **invoice**, không toàn bộ record. Chưa kiểm thử VNPay với merchant thật. |
| CD07 / SQ11 | Tạo nhiều lịch định kỳ và TimeSlot | Code tạo từng WorkSchedule, chưa có TimeSlot/entity/bảng riêng, chưa có lịch tuần tự động. |
| CD07 / SQ12 | LeaveRequest theo khoảng ngày/lý do, ON_LEAVE, khóa TimeSlot | Code hủy một ca ngay, CANCELLED, hủy lịch BOOKED liên quan, chặn nếu đã tiếp nhận; chưa có duyệt nghỉ. |
| CD07 / SQ13 | Cập nhật TimeSlot khi newCapacity >= bookedCount | Code đổi max_slots của WorkSchedule chỉ khi chưa có booking. Chặt hơn hình nhưng chưa cùng chức năng. |
| CD08–09 / SQ14–15 | TimeSlot/QueueTicket; quét QR; tạo MedicalRecord khi walk-in; WebSocket và tự chọn phòng ít bận | Appointment/WorkSchedule/Queue; tìm mã/SĐT; walk-in chọn bệnh nhân có sẵn; tạo draft khi bác sĩ callNext; chuyển phòng thủ công cùng khoa; chưa có WebSocket/tối ưu phòng. Thứ tự gọi hiện ưu tiên cấp độ rồi appointment_time/queue_number, chưa biểu diễn riêng “đặt trước đúng giờ > walk-in” như BR09. |
| CD10–12 / SQ16–20 | Các lớp domain phần lớn đã có | Đồng bộ các guard/lời gọi mô tả tại phần 4; thêm kết thúc khám, lịch sử thu hồi, batch và thông báo nếu đưa vào phạm vi trình bày. |

Đây là khác biệt giữa thiết kế được vẽ và code, không phải tất cả đều là lỗi chạy. Ví dụ dùng HttpSession hoặc DAO là lựa chọn hợp lệ cho Servlet/JSP, nhưng phải được báo cáo mô tả đúng. Ngược lại, thiếu quy tắc hạn hủy là thiếu nghiệp vụ; không nên chỉ xóa nhánh kiểm tra khỏi sequence để che thiếu sót.

## 6. Kiểm chứng thực tế trong lần review

- Build độc lập từ snapshot Git HEAD: `mvn -o -B clean package` thành công.
- 23 test không cần DB đạt. 18 test tích hợp của bộ JUnit được skip vì không bật HOSPITAL_INTEGRATION; không tính là đã đạt.
- 86 kiểm tra HTTP/JSP của hành trình chính đạt.
- 36 kiểm tra HTTP thu hồi, tệp gốc/biên bản và thông báo đạt.
- 41 kiểm tra HTTP bổ sung đạt: tạo ca/đổi sức chứa, đổi/hủy lịch hợp lệ, hủy ca, tạo/sửa giá, walk-in, chuyển phòng, callNext, báo cáo rỗng và chặn thanh toán trùng.
- Tổng: **163 kiểm tra HTTP đạt**, cộng hai phép thử riêng đã xác nhận hai vấn đề ở phần 5A. Bộ kiểm thử xanh không có nghĩa hình đã đồng bộ hoặc mọi quy tắc SRS đã hoàn thành.
- Kiểm tra bổ sung xác nhận walk-in chưa tạo MedicalRecord; sau callNext có một draft duy nhất. Hóa đơn thuốc phát sinh sau hóa đơn đầu cũng đã được thanh toán ở bước kiểm tra riêng.
- Chỉ dùng database giả lập mới `hospital_audit_ec2b863d`, máy chủ QA cổng 3001, source snapshot dưới `.local/design-audit-20261031`. Không reset/seed lại database `hospital` và không chạy các script có ghi dữ liệu trên bản demo của người dùng.
- Chưa kiểm thử VNPay merchant thật, SMTP/LIS/pharmacy provider thật hoặc mọi kịch bản đồng thời trong lượt review này.

Chứng cứ kiểm tra ban đầu nằm dưới `.local/design-audit-20261031`: ảnh gốc, báo cáo tải về, snapshot code, kết quả HTTP và `gap-probe.json`. Thư mục tạm này đã được dọn ngày 02/11/2026 theo yêu cầu; tài liệu trên giữ lại kết quả và các bước tái hiện.
