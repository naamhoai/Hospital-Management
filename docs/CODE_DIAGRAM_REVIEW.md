# Đối chiếu code, main workflow, UC, class và sequence — 02/11/2026

## Kết luận

Luồng nghiệp vụ chính đã chạy được qua Servlet + JSP, nhưng source chưa khớp từng lớp/từng lời gọi với toàn bộ hình trong SDS. Không thể kết luận “đã khớp 100%”. CD10–12 gần code nhất; CD01–09 còn nhiều tên lớp, công nghệ và bước xử lý khác implementation.

Code kiểm tra lại: nhánh `main`, commit `24d101c` (`update dữ liệu db`). `git diff 0d32856 HEAD -- src` không có khác biệt: commit dữ liệu mới không thay đổi logic Java/JSP đã kiểm tra trước đó. Sau đối chiếu đã sửa 4 main workflow và gom các project Visual Paradigm local vào `E:/SWD392/DESIGN/Hospital_All_Diagrams.vpp`. Không sửa Java/JSP, database hoặc hai báo cáo trên Drive; các khoảng thiếu implementation trong bảng dưới vẫn còn.

Nguồn đối chiếu trực tiếp:

- [SRS — Report 1](https://docs.google.com/document/d/1wGyYYJdDnR7CLNjlmsUIBLMdyoLoOzIs/edit).
- Metadata đọc ngày 02/11/2026: SRS sửa gần nhất 02/11/2026; [SDS — Report 2 v3.0](https://docs.google.com/document/d/1zj8LAZlByDeXaiw5AxKfLHPY45NiM_uS/edit) sửa gần nhất 31/10/2026. Đã đọc lại raw DOCX, 4 main workflow, 6 hình UC, Architecture/Package, 12 class và 21 hình sequence (SQ14 có 2 hình).
- Có hai hình check-in và walk-in trong cùng mục 3.8.2.1; ở bảng dưới gọi chung SQ14. Mục 3.13 là đặc tả entity, không phải một feature/class diagram thứ 13.

## 0. Kết quả đọc lại theo SRS hiện tại

### 0.1 Bốn main workflow

| Main workflow trong SRS | Code hiện tại | Kết luận |
|---|---|---|
| 1.2.1 Patient Appointment & Medical Examination | Có tìm kiếm, đặt lịch, check-in, queue, bệnh án, chỉ định, thu phí, kết quả, kê đơn, hoàn tất và đánh giá | Chạy được các bước nghiệp vụ chính, nhưng thứ tự trên hình chưa khớp điều kiện của UC và code. |
| 1.2.2 Doctor Schedule Management | Tạo từng ca; chỉnh sức chứa khi chưa có booking; `requestLeave` hủy ngay ca và booking chưa tiếp nhận | Thiếu lịch lặp tuần/tháng và bước Manager xét duyệt nghỉ như hình. |
| 1.2.3 Service Catalog and Pricing | Có thêm/sửa/ẩn dịch vụ CONSULTATION/LAB/IMAGING, giá được chụp khi phát sinh chi phí | Khớp phần dịch vụ đơn lẻ; chưa có gói khám và trường mô tả/code dịch vụ như workflow/UC. |
| 1.2.4 Reporting and Analytics | Có flow/revenue, ngày/khoa/dịch vụ/phương thức, bảng/biểu đồ, xuất tùy chọn PDF/XLSX | Khớp nghiệp vụ chính khá sát; SQ20 cần vẽ đủ revenue và nhánh xuất tùy chọn. |

### 0.2 Chỗ cần thống nhất trước khi nói “code khớp main workflow”

Hình SRS 1.2.1 vẽ: khám → chỉ định → làm xét nghiệm/kết quả → ghi chẩn đoán → kê đơn → lập hóa đơn/thanh toán một lần cuối. Tuy nhiên chính SRS quy định:

- UC-13 và BR-13: có chẩn đoán sơ bộ trước khi chỉ định; postcondition UC-13 yêu cầu bệnh nhân trả phí rồi làm xét nghiệm.
- UC-14: đã thanh toán dịch vụ là precondition của nhập/duyệt kết quả.
- UC-12: có chẩn đoán và đủ kết quả cận lâm sàng (nếu có) trước khi kê đơn.
- UC-16 lại ghi precondition đã hoàn tất khám/xét nghiệm/kê đơn, nên nếu hiểu đây là lần thanh toán duy nhất thì không đi được đến UC-14.

Code đang phù hợp với các điều kiện UC-11/12/13/14 theo chuỗi:

```text
Tìm kiếm → Đặt lịch → Tiếp nhận → Cấp số/phòng → Bác sĩ gọi vào khám
→ Lưu chẩn đoán sơ bộ
→ [Nếu cần xét nghiệm] Chỉ định → Lập/thu hóa đơn phí khám + xét nghiệm
  → Lab nhập/duyệt kết quả → Bác sĩ xem kết quả, cập nhật chẩn đoán khi cần
→ Kê đơn nếu cần → Thu phần thuốc/chi phí mới chưa thu
→ Kết thúc khám → Xem hồ sơ → Đánh giá
```

`ClinicalService.recordTestResult` chặn chưa PAID; `PrescriptionService` chặn còn chỉ định chưa hoàn tất; `EMRService.closeRecord` tạo hóa đơn cho chi phí còn chưa tính. Vì vậy thứ tự thanh toán hai đợt là có chủ đích, không phải hiển thị thiếu dữ liệu. Hệ thống cũng cho kết thúc rồi thu phần còn lại; CLOSED là trạng thái lâm sàng, không tự đồng nghĩa PAID.

Đề xuất đồng bộ: giữ điều kiện trả phí trước xét nghiệm, bổ sung chẩn đoán sơ bộ và bước thu phí cận lâm sàng vào main workflow; UC-16 mô tả thanh toán theo từng đợt chi phí. Không bỏ kiểm tra PAID chỉ để đi theo hình đang thiếu bước.

### 0.3 UC ↔ CD ↔ SQ ↔ chức năng code

“Có lõi” nghĩa là đã có đường xử lý nghiệp vụ chính, không có nghĩa tất cả nhánh/tích hợp/màn hình trong UC đều hoàn tất.

| UC trong SRS | Class / sequence trong SDS | Đối chiếu implementation |
|---|---|---|
| UC-01 Search | CD02 / SQ04 | SearchController → SearchService → SearchDAO. Có keyword/ngày/ca còn giờ; chưa lọc riêng kinh nghiệm/phí/specialtyId, không có Redis hold. |
| UC-02 Book | CD04 / SQ06 | AppointmentService.createAppointment; khóa patient/schedule, kiểm tra tối đa 2 booking và capacity. Chọn ca, tự cấp giờ đầu còn trống; chưa cho chọn appointmentTime, chưa TicketHoldService/QR. |
| UC-03 Reschedule | CD04 / SQ07 | Có giới hạn 2 lần và trước đầu ca 12 giờ; chọn ca mới, tự cấp giờ. Khác DTO/TicketHoldService trên hình. |
| UC-04 Cancel | CD04 / SQ08 | Có hủy, trả capacity và khóa online nếu hủy nhiều; thiếu lý do và kiểm tra hạn hủy như danh sách UC/SQ08. |
| UC-05 Recurring schedule | CD07 / SQ11 | Chỉ tạo từng ngày/ca bằng createSchedule; chưa tạo lặp tuần/tháng, chưa rule chốt thứ Sáu/duyệt lịch. |
| UC-06 Leave | CD07 / SQ12 | requestLeave hủy trực tiếp một ca và booking chưa check-in; chưa lưu phiếu/lý do, chưa Manager duyệt/Needs Processing/SMS như các phần SRS. |
| UC-07 Capacity | CD07 / SQ13 | Có kiểm tra quyền và sức chứa; code chỉ đổi khi booked_count=0, chặt hơn nhánh newMaxCapacity>=bookedCount trên SQ13. Không có TimeSlot riêng. |
| UC-08 Check-in | CD08 / SQ14 check-in | ReceptionController/Service → QueueService.checkIn, mã lịch/điện thoại, chỉ hôm nay, chặn trùng. Không có QR/TimeSlotRepository/WebSocket; Appointment=CHECKED_IN, Queue=WAITING. |
| UC-09 Walk-in | CD08 / SQ14 walk-in | Chọn bệnh nhân đã có tài khoản, book chỗ chưa chiếm rồi check-in. Chưa có tạo user/patient mới tại quầy. Draft EMR sinh khi callNext, không ở reception như hình. |
| UC-10 Routing | CD09 / SQ15 | Có số duy nhất theo ngày/phòng, phòng bác sĩ và chuyển phòng thủ công cùng khoa. Thiếu gợi ý phòng ít người, bảng gọi số realtime và đúng thứ tự booked-on-time trước walk-in của BR-09. |
| UC-11 Diagnosis | CD10 / SQ16 | Có queue context, quyền bác sĩ, ICD, lịch sử và lưu chẩn đoán. SQ16 gọi verifyQueueContext tại Controller nhưng code gọi getContext tại Service; thiếu bước tạo draft/callNext và GET workspace. |
| UC-12 Prescription | CD10 / SQ17 | Có tìm thuốc, nhiều dòng, đủ kết quả, check tồn, saveWithDetails/trừ kho cùng transaction và PDF. SQ17 cần PrescriptionRequest + MedicalRecordDAO/ClinicalOrderDAO; adapter dược cần cấu hình provider, chưa chứng minh tích hợp thật. |
| UC-13 Orders | CD11 / SQ18 | Có chẩn đoán sơ bộ, service ACTIVE, chụp giá, tránh trùng/hủy trước thanh toán. SQ18 chưa vẽ MedicalServiceDAO và dùng tham số rời thay ClinicalOrderRequest. |
| UC-14 Results | CD11 / SQ19 | Có PAID, duyệt một lần, upload/file có quyền, cập nhật COMPLETED, polling/outbox; thu hồi theo trưởng khoa. Chưa tích hợp máy LIS thật; class cần bổ sung lịch sử result/recall. |
| UC-15 Catalog | CD05 / SQ09 | Có service/giá/trạng thái, giá không hồi tố. Thiếu health package; ServiceCatalogService đã thống nhất tên; AuditLogger trên hình tương ứng AuditDAO trong code. Actor SRS list/workflow là Manager nhưng spec/SQ09 còn Admin/Accountant, cần thống nhất. |
| UC-16 Billing | CD06 / SQ10 | Có invoice_line, phí khám/xét nghiệm/thuốc, tiền mặt, VNPay callback. Thiếu BHYT/discount/refund/Momo/biên lai riêng có PDF. Online ở PaymentService/PaymentCallbackController; cập nhật PAID theo invoice, không cả record. |
| UC-17 Flow report | CD12 / SQ20 | Có đếm/tỷ lệ, lọc, biểu đồ và PDF/XLSX. Dữ liệu hợp lệ rỗng trả 200, không lỗi như SQ20. |
| UC-18 Revenue report | CD12 / SQ20 | Có DAY/MONTH/QUARTER, department/service/payment/sourceType, chỉ PAID. SQ20 chủ yếu flow, cần nhánh revenue và opt export. |
| UC-19 Feedback | CD03 / SQ05 | Có đúng bệnh nhân, CLOSED, một lần/7 ngày, điểm trung bình. Code dùng recordId và FeedbackController/Service/DAO; hình còn Booking*/bookingId. Chưa có link mời đánh giá có token SMS/Zalo. |

Authentication (CD01/SQ01–03) là chức năng chung, không phải UC-01 Search. Code dùng HttpSession/AuthUser/SecurityFilter/Passwords/UserDAO; hình đang vẽ JWT/Redis/AuthSession/UserService/Repository. SRS non-UI cho phép token/session nên không cần coi JWT là điều kiện bắt buộc của SRS; SDS phải mô tả đúng lựa chọn kỹ thuật đã thống nhất.

### 0.4 Các hình UC có phạm vi rộng hơn code và bảng 19 UC

Không thể nói đã triển khai mọi ellipse trong mục 1.3.3:

- Manager còn health package, CRUD khoa, bảng giá riêng và feedback list/details; code chưa có các màn hình/quy trình đầy đủ này.
- Administrator vẽ CRUD user/role/permission, reset mật khẩu, cấu hình và màn xem/filter audit log. Có role filter và ghi audit không đồng nghĩa đã có các màn hình quản trị đó.
- Patient còn cập nhật feedback, mua gói khám và tải toàn bộ bệnh án PDF; code có gửi feedback, xem bệnh án và PDF đơn thuốc, chưa phải toàn bộ các tính năng trên.
- Doctor còn sửa đơn thuốc/sửa chi tiết chỉ định; code hiện thiên về phát hành đơn, tạo/hủy chỉ định và khóa dữ liệu theo trạng thái.
- Receptionist còn cập nhật/hủy đăng ký walk-in; chưa có toàn bộ đường xử lý riêng như hình.
- Cashier còn CRUD/delete invoice và receipt riêng; code quản lý phát sinh phí và thanh toán, chưa triển khai đầy đủ các ellipse.

### 0.5 Tài liệu đang tự mâu thuẫn và ảnh cloud chưa được cập nhật

1. Main workflow 1.2.1 thiếu thu phí trước Lab, khác UC-13/14 và BR-13; nên bổ sung bước thay vì bỏ business guard trong code.
2. Bảng đặc tả **UC-04 và UC-05 bị đảo nội dung**: UC-04 Cancel chứa đăng ký lịch bác sĩ, UC-05 Schedule chứa hủy lịch của bệnh nhân. Phải sửa đúng ID/actor/description/steps; không dùng bảng bị đảo để kết luận code sai feature.
3. Workflow 1.2.2 và UC list có Manager duyệt nghỉ; UC-06 chi tiết/SQ12 lại gần xử lý nghỉ trực tiếp. Chốt quy trình phê duyệt rồi sửa thống nhất UC/class/sequence/code.
4. **Ảnh SDS mục 1.1 và 1.2 trên Drive vẫn ghi React + React Bootstrap, HTTP API/JSON.** Code hiện JSP; file local `E:/SWD392/DESIGN/Hospital_All_Diagrams.vpp` và hai PNG cùng thư mục đã được sửa JSP. Sửa file local không tự thay ảnh trong báo cáo cloud.
5. SRS bảng công nghệ còn SQL Server; triển khai MySQL theo lựa chọn người dùng. SDS CD07–09 còn TimeSlot/slotId/QueueTicket trong khi entity/ERD và code dùng WorkSchedule/Appointment/Queue. Cần thống nhất một mô hình dữ liệu.
6. Một số hình class có chữ bị cắt/biểu tượng giao diện chồng lên tên hàm; cần xuất lại hình từ VPP thay vì chụp khi đang chọn/chỉnh sửa đối tượng.

### 0.6 Việc nên ưu tiên

- Sửa lỗi/thiếu kiểm tra trong code: hạn hủy + lý do; JOIN Lab chỉ chọn kết quả APPROVED đang hiệu lực, tách lịch sử thu hồi để tránh nhân đôi dòng.
- Chốt 3 quy trình còn khác: thu phí trước Lab, phê duyệt nghỉ, tiếp nhận người bệnh chưa có tài khoản. Chức năng SRS đã yêu cầu nhưng chưa triển khai cần code bổ sung hoặc được nhóm/thầy thống nhất phạm vi; không chỉ đổi tên diagram để coi là hoàn tất.
- Đồng bộ tên lifeline, chữ ký request và các guard theo implementation; không tạo các lớp JWT/Redis/TimeSlot giả chỉ để giống tên hình.
- Thay hai ảnh Architecture/Package cloud bằng bản JSP hiện có; sửa UC-04/05, vai trò UC-15 và nhánh opt/revenue/empty của SQ20.
- Bổ sung callNext/createDraftForQueue và closeRecord vào sequence liên quan để nối CD09 với CD10 và mô tả rõ vòng đời lượt khám.

Kiểm tra mới trong lần đọc lại: **37 HTTP checks chỉ đăng nhập/xem trang**, gồm search, queue/EMR/schedule, reception, Lab, billing, reports, hồ sơ/feedback bệnh nhân, 403 sai vai trò, báo cáo rỗng 200 và filter sai 422. Không chạy lại toàn bộ test ghi dữ liệu hay VNPay/SMTP/LIS thật trong lần này. Các kết quả kiểm thử ngày 31/10 ở cuối tài liệu là bằng chứng lịch sử, không phải số test vừa chạy lại.

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
| Tráng | 3.5 / CD05 Catalog | SQ09 Manage Service Catalog and Pricing | `services.jsp` → `ServiceCatalogController` → `ServiceCatalogService.save` → `DepartmentDAO` + `MedicalServiceDAO` + `AuditDAO` | Luồng lưu/đổi giá có. Đã thống nhất tên ServiceCatalogService; hình còn dùng AuditLogger thay vì AuditDAO. |
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
| CD05 / SQ09 | ServiceCatalogService, AuditLogger | ServiceCatalogService và AuditDAO. Quyền code: ADMIN/MANAGER; SRS còn khác nhau giữa Manager và Admin/Accountant, cần chốt. |
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

## Bàn giao workflow và project tổng hợp — 02/11/2026

File duy nhất để mở/sửa Visual Paradigm: `E:/SWD392/DESIGN/Hospital_All_Diagrams.vpp` (16 diagram). Đã bổ sung WF01–WF04 sửa các lỗi thứ tự và trách nhiệm mô tả ở mục 0.1–0.2; các class/sequence và architecture/package trong 3 project cũ được bảo toàn. Các file VPP rời đã được thay bằng project tổng hợp, không giữ bản backup. Bốn ảnh WF01–WF04 thay bộ ảnh workflow local cũ.

WF02/WF03 là luồng yêu cầu SRS; bước Manager duyệt nghỉ và gói khám vẫn chưa có trong code. Cần sửa implementation hoặc thống nhất lại phạm vi SRS trước khi khẳng định khớp hoàn toàn. Class/sequence cũ vẫn còn các khác biệt đã liệt kê trong báo cáo đối chiếu này.

## Dọn code và kiểm tra lại — 02/11/2026

Sau mốc review commit 24d101c ở trên, source local đã được refactor: CatalogService thành ServiceCatalogService; bỏ Role/RoleDAO không được gọi và các API/PrescriptionItem tương thích test cũ; test nay dùng cùng request model với Controller. Các tên test/script cá nhân đã thay bằng HospitalWorkflowTest, ClinicalWorkflowTest, ClinicalSafetyTest và smoke_result_recalls.py. Controller dùng request/response/param, các biến kết nối và comment quan trọng rõ nghĩa bằng tiếng Việt.

Schema tạo mới đã gộp đủ 25 bảng trong 01-schema.sql; tài khoản nền/trưởng khoa ở 02-seed.sql; dữ liệu demo ở 04-demo-data.sql. Không còn áp dụng chuỗi migration MVP/extension cũ. Bảng roles vẫn được dùng trong UserDAO đăng nhập; chỉ bỏ Java Bean/DAO Role không có caller.

Đã chạy đủ 41 kiểm thử Java (không skip), 86 kiểm tra HTTP hành trình chính và 36 kiểm tra HTTP thu hồi/thông báo; cài mới và nạp lại bộ demo không nhân đôi dữ liệu. Đây là bằng chứng luồng đang có chạy được, không phải xác nhận đã hoàn thiện toàn bộ SRS. Các khoảng thiếu lịch lặp, duyệt nghỉ, gói khám và tích hợp ngoài trong bảng đối chiếu vẫn còn.
