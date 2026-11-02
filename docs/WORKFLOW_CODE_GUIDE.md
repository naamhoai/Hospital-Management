# Các hàm theo class và sequence

Mở file trong `src/main/java/vn/hospital`. Comment của hàm chính ghi rõ đầu vào, xử lý và đầu ra. Controller đọc request/session; Service giữ nghiệp vụ; DAO giữ SQL; JSP chỉ hiển thị dữ liệu.

| Class / sequence | Hàm cần mở | Dữ liệu vào → xử lý → dữ liệu ra |
|---|---|---|
| CD01 / SQ01–03 Authentication | AuthController, AuthService.login, SecurityFilter | Email/mật khẩu → băm/khóa tạm/kiểm tra vai trò → AuthUser lưu HttpSession; logout hủy session |
| CD02 / SQ04 Search | SearchService.searchDoctorsAndServices | Từ khóa/ngày → SearchDAO lọc ca còn giờ → danh sách bác sĩ/dịch vụ |
| CD03 / SQ05 Feedback | FeedbackService.submitFeedback | recordId/điểm/nhận xét → kiểm tra chủ hồ sơ, CLOSED và 7 ngày → feedback và điểm trung bình |
| CD04 / SQ06 Book | AppointmentService.createAppointment, book | patientId/scheduleId → khóa ca, tìm giờ, chụp phí khám → appointmentId và số chỗ mới |
| CD04 / SQ07 Reschedule | AppointmentService.rescheduleAppointment | Lịch cũ/ca mới → giới hạn 2 lần, trước ca 12 giờ → đổi ngày giờ, trả/nhận số chỗ |
| CD04 / SQ08 Cancel | AppointmentService.cancelAppointment | Lịch của bệnh nhân → kiểm tra BOOKED → CANCELLED và trả chỗ |
| CD05 / SQ09 Catalog | ServiceCatalogService.save | Thông tin dịch vụ/gói/thành phần → kiểm tra quyền/giá/thành phần → service và service_package_item |
| CD06 / SQ10 Billing | BillingService.createInvoice, processCashPayment, completePayment | recordId → gom khoản chưa tính → invoiceId; xác nhận thu tiền → PAID đúng các dòng nguồn |
| CD07 / SQ11 Schedule | ScheduleService.createSchedule, createRecurringSchedule | Bác sĩ/ngày/giờ/số chỗ → kiểm tra trùng trong transaction → một/nhiều ca |
| CD07 / SQ12 Leave | ScheduleService.requestLeave, reviewLeave | Mã ca/lý do → PENDING → quản lý duyệt/từ chối → cập nhật ca và booking bị ảnh hưởng |
| CD07 / SQ13 Capacity | ScheduleService.adjustCapacity | Mã ca/số chỗ mới → chỉ ca chưa có booking → max_slots mới |
| CD08 / SQ14 Reception | ReceptionService.checkIn, registerWalkIn → QueueService | Lịch hoặc bệnh nhân/ca → kiểm tra hôm nay, cấp số → queueId |
| CD09 / SQ15 Routing | QueueService.routePatient, callNext | Lượt chờ/phòng hoặc bác sĩ → chuyển phòng/gọi người tiếp theo → IN_CONSULTATION và bệnh án nháp |
| CD10 / SQ16 Diagnosis | EMRService.getRecord, recordClinicalDiagnosis, getPatientHistory | queueId/DiagnosisRequest → QueueContextPort kiểm tra phân công, ICD/OPEN → MedicalRecord đã lưu và lịch sử |
| CD10 / SQ17 Prescription | PrescriptionService.validatePrescription, issuePrescription, printPrescription | PrescriptionRequest → đủ kết quả/tồn kho, lưu đơn/chi tiết/trừ kho cùng transaction → Prescription/PDF |
| CD11 / SQ18 Orders | ClinicalService.createClinicalOrders | recordId/serviceIds → có chẩn đoán, tách gói, chụp giá → List<ClinicalOrder> chờ thu phí/thực hiện |
| CD11 / SQ19 Results | ClinicalService.recordTestResult | orderId/kết quả/tệp tùy chọn → chỉ PAID, duyệt một lần → TestResult và chỉ định COMPLETED |
| CD12 / SQ20 Reports | AnalyticsService.generateReport, getPatientFlowReport, getRevenueReport, exportReport | ReportFilter → SELECT/tính tỷ lệ/PAID → bảng, biểu đồ hoặc PDF/XLSX |

## Ranh giới bốn khối

- Vận hành bàn giao **queueId**. EMR không tự cấp số hoặc tự đặt lịch.
- Bệnh án nối lượt khám qua **appointment_id**; QueueContextPort tra qua liên kết này.
- Khám/chỉ định/đơn chụp đơn giá tại lúc tạo. Billing đọc các khoản chưa lập hóa đơn.
- Lab chỉ trả kết quả khi chỉ định đã PAID. Kê đơn chặn chỉ định còn chờ; kết thúc tạo hóa đơn cho phần chưa tính.
- Báo cáo màn hình chỉ đọc. Tác vụ tổng hợp nền dùng một bảng hỗ trợ, không sửa dữ liệu khám.

## Phần triển khai cần giải thích khi thuyết trình

- Code dùng **HttpSession + Servlet Filter**, phù hợp stack Servlet/JSP đã chọn; không giả lập JWT/Redis.
- Gói và dịch vụ đều là MedicalService. service_package_item là bảng liên kết thành phần; clinical_order.package_service_id cho biết chỉ định từ gói nào.
- Giá gói được phân bổ theo tỷ lệ giá thành phần, làm tròn xuống các dòng đầu và để dòng cuối nhận phần dư; tổng không lệch giá gói.
- Ca có các cột leave_status/lý do/người duyệt. requestLeave chưa hủy gì; reviewLeave mới quyết định.
- Hóa đơn CLOSED/PAID khác trạng thái bệnh án OPEN/CLOSED. Hết khám không tự có nghĩa đã thu đủ tiền.
- Có 18 bảng ERD và 8 bảng hỗ trợ triển khai; tất cả CREATE/INSERT nằm trong database/hospital.sql.

Các sơ đồ trong báo cáo mô tả nghiệp vụ. Phần mapping này chỉ rõ tên hàm chạy thực tế; các quyết định Servlet/Session và các trường hỗ trợ được ghi công khai để trình bày đúng code, không khẳng định mọi chi tiết tích hợp ngoài đã có.

TestResult và TestResultDAO là model/DAO **kết quả xét nghiệm** trong CD11, không phải bộ bài kiểm thử.
