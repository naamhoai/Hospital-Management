# Hướng dẫn đọc code phần Nam

Bản cập nhật 29/10/2026. Không dùng Spring; vẫn là Servlet/JSP → Service → DAO → MySQL. Comment tiếng Việt tập trung ở quy tắc nghiệp vụ và transaction; getter/setter giữ ngắn.

## Nên đọc theo thứ tự

| Luồng | Controller nhận form | Service xử lý chính | DAO thao tác dữ liệu |
|---|---|---|---|
| SQ16: chẩn đoán | EMRController | EMRService.recordClinicalDiagnosis | MedicalRecordDAO |
| SQ17: kê đơn | PrescriptionController | PrescriptionService.validatePrescription / issuePrescription | MedicationDAO, PrescriptionDAO |
| SQ18: chỉ định/hủy | ClinicalOrderController | ClinicalService.createClinicalOrders / cancelOrder, BillingService.removeUnpaidClinicalCharge | ClinicalOrderDAO, InvoiceDAO, PaymentAttemptDAO |
| SQ19: duyệt kết quả | LabResultController | ClinicalService.recordTestResult | ClinicalOrderDAO, TestResultDAO |
| Bổ sung: thu hồi | ResultRecallController | ResultRecallService.requestRecall / reviewRequest | ResultRecallDAO, TestResultDAO |
| SQ20: báo cáo | ReportController | AnalyticsService.generateReport / exportReport | ReportDAO |
| Tác vụ nền | BackgroundJobs | MaintenanceService → ReportBatchService | ReportSummaryDAO |
| Thông báo bác sĩ | ClinicalUpdateController | ClinicalUpdateService | ClinicalUpdateDAO |

## 1. Hủy chỉ định đã lên hóa đơn nhưng chưa trả

Đọc `ClinicalService.cancelOrder` rồi `BillingService.removeUnpaidClinicalCharge`.

1. Kiểm tra bác sĩ phụ trách và bệnh án còn mở.
2. Khóa bản ghi theo thứ tự bệnh án → hóa đơn → chỉ định. Thứ tự này tránh chồng chéo với thu ngân đang thanh toán.
3. Chỉ hủy chỉ định chưa thực hiện và chưa thanh toán.
4. Có giao dịch VNPay còn trong 15 phút thì báo chờ xử lý, giữ nguyên chỉ định và hóa đơn.
5. Giao dịch VNPay cũ hết hạn chuyển EXPIRED; callback của nó không được tự thanh toán hóa đơn đã đổi số tiền.
6. Xóa đúng dòng phí ORDER của hóa đơn chưa trả, tính tổng lại. Hóa đơn không còn dòng phí chuyển CANCELLED.
7. Chuyển chỉ định CANCELLED và ghi audit.

Tất cả nằm trong `Database.tx`: lỗi ở bất kỳ bước nào sẽ rollback. Không xóa dòng của hóa đơn PAID.

## 2. Tiến trình khám và trạng thái khóa

`ClinicalProgress.from(record, orders)` dùng câu lệnh if và vòng for để suy ra tiến trình:

- Chưa có chẩn đoán: NOT_DIAGNOSED.
- Có chẩn đoán, không còn xét nghiệm cần làm: EXAMINED.
- Có chỉ định chưa hoàn tất hoặc kết quả bị thu hồi: WAITING_TESTS.
- Có xét nghiệm và tất cả chỉ định còn hiệu lực đã hoàn tất: RESULTS_READY.
- Bệnh án CLOSED, không có kết quả đang thu hồi: FINISHED.

`MedicalRecord.status` vẫn OPEN/CLOSED để kiểm soát quyền sửa. Tiến trình là dữ liệu hiển thị suy ra, không thêm một cột trùng trạng thái vào ERD. Khi kết quả của lượt đã đóng bị thu hồi, chẩn đoán cũ không tự mở khóa; bác sĩ nhận cảnh báo và xem xét xử lý chuyên môn/lượt tái khám.

## 3. Thu hồi kết quả có phê duyệt

Kỹ thuật viên hoặc bác sĩ phụ trách gửi lý do. Trưởng khoa chỉ thấy và duyệt dịch vụ thuộc chuyên khoa của mình; Manager/Admin không mặc nhiên có quyền trưởng khoa.

- Đề nghị PENDING → APPROVED hoặc REJECTED.
- Phê duyệt cần ý kiến và biên bản PDF tối đa 5 MB.
- Biên bản do trưởng khoa upload. Phần mềm lưu người phê duyệt, thời gian và tệp; **không xác minh chữ ký số/PKI hoặc tự nhận diện chữ ký trong PDF**.
- Phê duyệt đặt kết quả cũ RECALLED và `approved=false`, chỉ định RESULT_RECALLED.
- Kỹ thuật viên nhập bản thay thế: tạo TestResult có ID mới, giữ nguyên toàn bộ nội dung và tệp bản gốc. Mỗi chỉ định chỉ có tối đa một TestResult APPROVED.
- Bác sĩ/bệnh nhân chỉ tải kết quả đang có hiệu lực. Trưởng khoa có thể tra cứu tệp cũ và biên bản qua phiếu thuộc khoa của mình.
- Tải biên bản/tệp gốc không xóa file. `BaseController.download` chỉ dành cho file xuất tạm (đơn thuốc, báo cáo); controller thu hồi stream tệp lưu lâu dài bằng hàm riêng.

Tài khoản demo: `head@hospital.test`, mật khẩu `Demo@1234`, phụ trách khoa Nội. Tài khoản được thêm bằng script demo riêng, không đưa thông tin này vào database thực.

## 4. Thông báo và báo cáo nền

`ClinicalUpdateDAO` chỉ đọc sự kiện APPROVE_RESULT/RECALL_RESULT thuộc bệnh nhân của bác sĩ đăng nhập. JavaScript kiểm tra mỗi 15 giây, hiện liên kết mở bệnh án; không reload form đang nhập. Tab ẩn hoặc không thao tác hơn 10 phút sẽ tạm dừng kiểm tra. Đây là polling, không phải WebSocket.

`ReportBatchService` tổng hợp ngày trước vào `report_daily_summary` sau nửa đêm (múi giờ Việt Nam), hoặc khi ứng dụng khởi động lại. Chạy lại cập nhật cùng một dòng, không cộng lặp. Màn hình vẫn dùng ReportDAO đọc số liệu thời gian thực; tác vụ nền không làm thay đổi bệnh án/hóa đơn.

## 5. Cài database

Database đã có schema SDS-aligned: sao lưu, chạy **05-nam-workflows.sql một lần** bằng tài khoản có quyền ALTER/CREATE. Script giữ các TestResult cũ và thêm UNIQUE cho kết quả đang có hiệu lực. Không chạy lại schema/seed trên dữ liệu đã có.

Database mới: **01-schema → 04-operational-extensions → 02-seed → 05-nam-workflows**. `06-demo-department-head.sql` là tùy chọn cho demo. Compose đã mount theo đúng thứ tự.

Schema thực thi hiện có **25 bảng**: 18 nghiệp vụ + 5 kỹ thuật cũ + result_recall_request và report_daily_summary. Quan hệ ClinicalOrder → TestResult vẫn là 0..1 bản đang có hiệu lực; phần lịch sử bổ sung giữ nhiều phiên bản cũ. Báo cáo/class diagram cần thể hiện rõ phần mở rộng này.

## 6. Năm sequence đồng bộ với code

### SQ16: Clinical Exam and Diagnosis

```mermaid
sequenceDiagram
    actor Doctor
    participant View as Consultation JSP
    participant C as EMRController
    participant S as EMRService
    participant Q as QueueContextPort
    participant D as MedicalRecordDAO
    Doctor->>View: Enter symptoms, ICD, diagnosis and notes
    View->>C: POST queueId and DiagnosisRequest
    C->>S: recordClinicalDiagnosis(queueId, request)
    S->>S: Validate required fields and ICD
    S->>Q: getContext(queueId)
    Q-->>S: QueueContext
    S->>D: findByQueueId(queueId)
    D-->>S: MedicalRecord
    S->>S: Check assigned doctor and OPEN record
    alt Invalid data or forbidden record
        S-->>C: BusinessException
        C-->>View: Validation message, keep entered diagnosis
    else Valid
        S->>D: save(record)
        D-->>S: Saved MedicalRecord
        S-->>C: MedicalRecord
        C-->>View: Redirect and display saved diagnosis
    end
```

### SQ17: Issue E-Prescription

```mermaid
sequenceDiagram
    actor Doctor
    participant C as PrescriptionController
    participant S as PrescriptionService
    participant M as MedicalRecordDAO
    participant D as MedicationDAO
    participant P as PrescriptionDAO
    Doctor->>C: Submit PrescriptionRequest
    C->>S: issuePrescription(request)
    S->>M: findById(recordId)
    S->>S: Check doctor, diagnosis and completed tests
    S->>D: checkStock(items)
    D-->>S: Stock available
    opt Pharmacy provider configured
        S->>S: Check interactions and required override
    end
    alt Invalid request or insufficient stock
        S-->>C: BusinessException
        C-->>Doctor: Error message
    else Valid
        S->>M: Lock record again and recheck editable state
        S->>D: Lock medication rows and read prices
        S->>P: saveWithDetails(prescription, items)
        P-->>S: Prescription
        loop Each prescription item
            S->>D: deductStock(item)
        end
        Note over S,P: Save details and deduct stock in one transaction
        S-->>C: Prescription
        C-->>Doctor: Display prescription
    end
    opt Print or PDF requested
        Doctor->>C: GET prescription ID and format
        C->>S: printPrescription(id)
        S-->>C: PDF file
        C-->>Doctor: Download PDF
    end
```

### SQ18: Order Clinical Tests

```mermaid
sequenceDiagram
    actor Doctor
    participant C as ClinicalOrderController
    participant S as ClinicalService
    participant M as MedicalRecordDAO
    participant V as MedicalServiceDAO
    participant D as ClinicalOrderDAO
    Doctor->>C: Submit ClinicalOrderRequest
    C->>S: createClinicalOrders(request)
    S->>M: findById(recordId)
    M-->>S: MedicalRecord
    S->>S: Check ownership, OPEN and preliminary diagnosis
    loop Selected service IDs
        S->>V: findById(serviceId)
        V-->>S: Active MedicalService
        S->>D: findExisting(recordId, serviceId)
        S->>S: Check duplicates and snapshot price
    end
    alt Invalid request
        S-->>C: BusinessException
        C-->>Doctor: Error message
    else Valid
        S->>D: saveAll(orders)
        D-->>S: ClinicalOrder list
        S-->>C: ClinicalOrder list
        C-->>Doctor: Display orders and PDF links
    end
```

### SQ19: Update Lab Results

```mermaid
sequenceDiagram
    actor Technician
    participant C as LabResultController
    participant S as ClinicalService
    participant O as ClinicalOrderDAO
    participant R as TestResultDAO
    Technician->>C: Submit result, conclusion and optional file
    C->>S: recordTestResult(orderId, result, upload)
    S->>S: Validate input and attachment
    S->>O: findById(orderId), lock order
    O-->>S: ClinicalOrder
    alt Not paid or already approved
        S-->>C: BusinessException
        C-->>Technician: Error message
    else Paid and pending or recalled
        S->>R: findByOrderId(orderId)
        R-->>S: No active result
        S->>R: save(newResult)
        R-->>S: Approved TestResult
        S->>O: updateExecutionStatus(orderId, COMPLETED)
        Note over S,R: Commit result, order status, audit and outbox together
        S-->>C: TestResult
        C-->>Technician: Approved and locked result
    end
```

### SQ20: Patient Flow and Revenue Reports

```mermaid
sequenceDiagram
    actor Manager
    participant C as ReportController
    participant S as AnalyticsService
    participant D as ReportDAO
    participant E as ReportExportService
    Manager->>C: Select type and ReportFilter
    C->>S: generateReport(type, filter)
    S->>S: Check role and validate filters
    alt Invalid date or filter
        S-->>C: BusinessException
        C-->>Manager: Validation message
    else Valid
        S->>D: queryPatientFlowMetrics or queryRevenueMetrics
        D-->>S: Typed ReportData
        S->>S: Build rates and chart series
        S-->>C: ReportData
        C-->>Manager: Table and chart, or empty state
    end
    opt Export requested
        Manager->>C: Select PDF or XLSX
        C->>S: exportReport(type, filter, format)
        S->>D: Read same filtered data
        D-->>S: ReportData
        S->>E: exportPDF(report) or exportExcel(report)
        E-->>S: File
        S-->>C: File
        C-->>Manager: Download file
    end
```

## Giới hạn cần cấu hình thật

Danh mục ICD qua ICD_CATALOG_FILE, hệ thống dược qua PHARMACY_BASE_URL và email qua SMTP cần dữ liệu/tài khoản hợp lệ do nhóm cung cấp. Không tự chế dữ liệu y khoa hoặc báo thành công giả khi chưa kết nối nhà cung cấp. Kết nối máy xét nghiệm LIS vẫn là tích hợp ngoài tùy chọn.
