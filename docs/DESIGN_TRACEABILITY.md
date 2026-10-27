# Đối chiếu thiết kế và source — nhánh Nam, 27/10/2026

Nguồn: [Report 1 / SRS](https://docs.google.com/document/d/1wGyYYJdDnR7CLNjlmsUIBLMdyoLoOzIs/edit), [Report 2 / SDS](https://docs.google.com/document/d/1zj8LAZlByDeXaiw5AxKfLHPY45NiM_uS/edit). Đã đọc nội dung và hình CD10/CD11/CD12, SQ16–SQ20. Quy tắc trong tài liệu được dùng làm đặc tả thiết kế; việc sửa source không thay đổi báo cáo gốc.

## Architecture và package

| Thành phần trong báo cáo | Triển khai | Trách nhiệm |
|---|---|---|
| Browser → Filter | SecurityFilter | UTF-8, HttpSession, role, CSRF, security headers |
| Controller Servlet | controller/*Controller | Parse request → gọi Service → forward JSP / redirect / stream file |
| Service | service | Quyền sở hữu, validation, nghiệp vụ, ranh giới transaction, điều phối DAO và adapter |
| DAO | dao | SQL có tham số, khóa bản ghi, ánh xạ BeanMapper |
| bean (model) | model | Entity/request/report Java Beans; `model` là tên package đã có, không tạo hai bản domain song song |
| view JSP | WEB-INF/views, WEB-INF/tags | Hiển thị dữ liệu Controller truyền, escape dữ liệu; không scriptlet SQL |
| Utility | util | Database, Config, Checks, Passwords, AttachmentStorage |
| External integration | integration | VNPayClient, EmailClient, PharmacyClient; QueueContextPort làm biên kết nối khối vận hành |
| Background lifecycle | listener | Khởi động scheduler → MaintenanceService; không chứa SQL |

Phụ thuộc được kiểm tra bằng DesignContractTest: Controller không import DAO; Controller/Service/integration/listener không gọi Jdbc hoặc chứa SQL nghiệp vụ. Service gọi util.Database để giữ một transaction qua nhiều DAO.

## Phần của Nam: 3 class và 5 sequence

| Thiết kế | HTTP/View | Chuỗi xử lý và lớp |
|---|---|---|
| CD10 / SQ16 — Clinical Exam and Diagnosis | GET/POST /emr, emr.jsp | EMRController → EMRService.getRecord/recordClinicalDiagnosis/getPatientHistory → QueueContextPort.getContext + MedicalRecordDAO.findByQueueId/findByPatientId/save. DiagnosisRequest chứa symptoms/icdCode/diagnosis/notes |
| CD10 / SQ17 — Issue E-Prescription | POST /prescriptions; GET ?id=; GET &format=pdf | PrescriptionController → PrescriptionService.searchMedication/validatePrescription/issuePrescription/printPrescription → MedicalRecordDAO + MedicationDAO.searchApproved/findById/checkStock → PrescriptionDAO.saveWithDetails. PrescriptionRequest → Prescription/PrescriptionDetail; PharmacyClient là tùy chọn |
| CD11 / SQ18 — Order Clinical Tests | POST /clinical-orders | ClinicalOrderController → ClinicalService.createClinicalOrders(ClinicalOrderRequest)/cancelOrder → MedicalRecordDAO + MedicalServiceDAO.findById/findAllActive + ClinicalOrderDAO.findByRecordId/saveAll/updateExecutionStatus |
| CD11 / SQ19 — Update Lab Results | GET/POST /lab; GET /attachments?resultId= | LabResultController → ClinicalService.recordTestResult(orderId,TestResult[,UploadData]) → ClinicalOrderDAO + TestResultDAO.findByOrderId/save. Lưu kết quả + cập nhật trạng thái + audit + notification trong transaction; tệp rollback khi DB lỗi |
| CD12 / SQ20 — Patient Flow and Revenue Reports | GET /reports; ?format=pdf/xlsx | ReportController → AnalyticsService.generateReport(type,ReportFilter), getPatientFlowReport/getRevenueReport, exportReport → ReportDAO.queryPatientFlowMetrics/queryRevenueMetrics → ReportExportService.exportPDF/exportExcel. ReportData ← PatientFlowReport/RevenueReport, series là ChartDataPoint |

Chữ ký API CD10–12 giữ kiểu domain, Integer ID và File kết quả xuất. AuthUser được đưa vào constructor Service làm security context; các overload nhận AuthUser kiểu cũ chỉ là cầu nối có kiểm tra quyền. Controller thể hiện HTTP handler do Servlet framework gọi, không thêm một public method trùng tên cho từng thao tác chỉ để giống hình.

### Luồng SQ16

1. Kiên gọi QueueService.callNext: WAITING → IN_CONSULTATION, tạo một draft MedicalRecord cho appointment.
2. Doctor mở queueId; Service kiểm tra queue và doctor được phân công, đọc bệnh án và lịch sử.
3. Doctor nhập DiagnosisRequest. Validation yêu cầu triệu chứng/chẩn đoán/mã ICD nằm trong danh mục cấu hình.
4. DAO cập nhật bệnh án trong transaction; trả MedicalRecord → JSP. Dữ liệu sai trả 422, giữ dữ liệu form chẩn đoán.
5. Khi kết thúc: không còn chỉ định chưa hoàn tất, lập hóa đơn chi phí mới, đóng bệnh án, hoàn tất queue/appointment, tạo email outbox.

### Luồng SQ17

1. Chọn thuốc từ danh mục đang hoạt động, nhập số lượng/liều dùng/lời dặn.
2. Service kiểm tra quyền, chẩn đoán, đã có đủ kết quả cận lâm sàng (nếu có), dòng thuốc trùng, số lượng, danh mục và tồn kho. Nếu có provider thì lấy cảnh báo tương tác, cần xác nhận override khi có cảnh báo.
3. Khóa thuốc theo thứ tự ID, chụp unitPrice, lưu Prescription + Details và trừ kho cùng transaction; lỗi bất kỳ dòng nào rollback toàn bộ.
4. Trả đơn để xem/in/PDF. Đồng bộ dược sau commit; lỗi đồng bộ ghi log và không trừ kho lần hai. Khi chưa có provider, UI nêu rõ chưa kiểm tra tự động.

### Luồng SQ18–19

Bác sĩ chỉ định sau chẩn đoán → PENDING/PENDING_PAYMENT. Thu ngân thanh toán → PAID. Kỹ thuật viên chọn chỉ định PAID/PENDING → nhập resultData/conclusion và file/URL tùy chọn → Service lưu TestResult và chuyển COMPLETED. Unique(order_id) và khóa order ngăn duyệt hai lần. Hủy chỉ định chỉ cho bác sĩ phụ trách khi chưa thực hiện, chưa trả và chưa xuất hóa đơn. Hóa đơn loại bỏ chỉ định CANCELLED.

Tệp tối đa 5 MB, nhận PDF/PNG/JPEG theo magic header, tên UUID, lưu ngoài webroot. `file_url` lưu `attachment:<uuid.ext>` hoặc HTTPS URL. Endpoint tải tệp kiểm tra người bệnh/doctor sở hữu hoặc kỹ thuật viên; không cho truy cập đường dẫn trực tiếp. Bệnh nhân xem qua /my-records. Kết quả đã duyệt không có chức năng chỉnh sửa.

### Luồng SQ20

Validate khoảng ngày ≤ 2 năm, whitelist groupBy/paymentMethod → DAO SELECT với connection readOnly → report typed → tính tỷ lệ (mẫu số 0 trả 0), series → JSP. Báo cáo doanh thu chỉ tính dòng thuộc hóa đơn PAID theo paid_at; bộ lọc serviceType phân biệt khám bệnh/cận lâm sàng/thuốc; serviceId giới hạn dòng chỉ định đó, không nhân tiền do JOIN nhiều dịch vụ. Tổng hóa đơn đếm DISTINCT; không cộng số hóa đơn từng dòng dịch vụ để làm tổng. Báo cáo lượt khám đếm appointment theo appointment_date, không phải số bệnh nhân duy nhất.

Excel giữ ô chuỗi dạng inlineStr để nội dung bắt đầu '=' không thành công thức; có sheet chi tiết và sheet thông tin bộ lọc/tổng. PDF nhúng font Noto Sans (SIL OFL trong resources/fonts), giữ tiếng Việt. Không có dữ liệu hiển thị trạng thái trống và xuất file hợp lệ. Controller xóa file xuất tạm sau khi stream.

## ERD và bảng vật lý

`01-schema.sql`: roles, users, departments, rooms, doctors, patients, work_schedule, appointment, queue, medical_record, service, clinical_order, test_result, medication, prescription, prescription_detail, invoice, feedback — **18 bảng nghiệp vụ**.

Có đủ 17 Java Bean của mục 3.13 và Room. ID Java Integer tương ứng INT trong schema mới; tiền BigDecimal, ngày LocalDate, giờ LocalTime, thời điểm LocalDateTime. `users.password` lưu hash PBKDF2, tuyệt đối không lưu mật khẩu rõ. `[0..1]` trong UML map sang thuộc tính có thể null; không phải nội dung nối vào String.

Quan hệ chính:
- User 1–0..1 Patient/Doctor; Doctor thuộc Department, Room thuộc Department.
- Patient/Doctor 1–* Appointment; WorkSchedule 1–* Appointment.
- Appointment 1–0..1 Queue và 1–0..1 MedicalRecord; queueId truy ngược appointment_id. Không có queue_id trong MedicalRecord.
- MedicalRecord 1–* Prescription, Prescription 1–* PrescriptionDetail, Detail *–1 Medication.
- MedicalRecord 1–* ClinicalOrder, ClinicalOrder *–1 MedicalService, ClinicalOrder 1–0..1 TestResult.
- MedicalRecord 1–* Invoice, 1–0..1 Feedback.

### Phần mở rộng kỹ thuật được tách riêng

`04-operational-extensions.sql` tạo queue_counter (cấp số nguyên tử), invoice_line (dòng chi phí, tránh thu lặp), payment_attempt (idempotency và số tiền VNPay), notification_outbox (gửi sau commit), audit_log (truy vết). Đây là 5 bảng bổ sung, **schema chạy có 23 bảng**, không tuyên bố ERD gốc đã liệt kê chúng.

Cột bổ sung: users.failed_attempts/locked_until; patients.booking_blocked_until; appointment.created_at/cancelled_at/consultation_fee/active_slot; queue.work_date/created_at; medical_record.created_at/closed_at; clinical_order.unit_price; test_result.approved/created_at; medication.status; invoice.created_at/paid_at; feedback.created_at. Unique active_slot, queue theo ngày/phòng và unique dòng chi phí bảo vệ khi chạy đồng thời.

### Điểm tài liệu mâu thuẫn và lựa chọn code

1. User đã chọn MySQL; SDS có NVARCHAR(MAX)/SQL Server. Dùng utf8mb4 VARCHAR/TEXT, giữ trường và quan hệ nghiệp vụ. Service/medication/detail đơn giá DECIMAL(10,2), invoice DECIMAL(12,2).
2. CD10 đặt queue context tại Service; SQ16 cũ tại Controller. Chọn CD10, Controller chỉ parse HTTP; xem bước SQ16 phía trên.
3. SDS yêu cầu diagnosis NN nhưng hệ thống cần draft trước khi khám. Draft lưu chuỗi rỗng; không cho thực hiện nghiệp vụ tiếp theo trước chẩn đoán hợp lệ.
4. SRS mục tác vụ nền đề cập báo cáo tổng hợp ban đêm, UC17–18 đòi số liệu truy vấn theo bộ lọc và thời gian thực. CD12 triển khai query read-only trực tiếp; chưa có bảng kho tổng hợp.
5. Mô tả architecture cho Servlet/JSP, các hình cũ nhắc Repository/Redis/JWT. Nguồn hiện thống nhất DAO + HttpSession; không coi đổi tên này là bằng chứng đã triển khai Redis/JWT. Những yêu cầu ngoài phần Nam còn thiếu được công khai trong SCOPE.md.

## Kiểm thử

DesignContractTest kiểm tra kiến trúc, đủ bảng nghiệp vụ, chữ ký kiểu dữ liệu, mapping Java Bean, Unicode PDF, validation/filter và tệp bất hợp lệ. NamWorkflowTest chạy MySQL cho sở hữu bệnh án/tệp, prescription atomic, hủy và tính hóa đơn, duyệt/khóa kết quả, PAID/filter/group/zero-data/export. WorkflowTest giữ kịch bản toàn viện và đặt lịch đồng thời. scripts/smoke_http.py chạy HTTP/JSP và multipart, riêng dữ liệu giả lập localhost.
