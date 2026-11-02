# Dữ liệu và luồng demo

## Tài khoản chung

Các tài khoản demo dùng mật khẩu `Demo@1234`.

| Vai trò | Email |
|---|---|
| Bệnh nhân | `patient@hospital.test`, `patient2@hospital.test`, `patient3@hospital.test` |
| Bác sĩ | `doctor@hospital.test`, `doctor2@hospital.test` |
| Lễ tân | `reception@hospital.test` |
| Kỹ thuật viên | `lab@hospital.test` |
| Thu ngân | `cashier@hospital.test` |
| Quản lý | `manager@hospital.test` |
| Trưởng khoa | `head@hospital.test` |
| Quản trị | `admin@hospital.test` |

## Bộ dữ liệu phòng khám

Nạp `database/04-demo-data.sql` sau 01/02. Bốn bộ độc lập mang mã phòng khám, không mang tên thành viên:

| Phòng | Bác sĩ | Bệnh nhân |
|---|---|---|
| DEMO-CLINIC01 | `clinic01.doctor@hospital.test` | `clinic0101` đến `clinic0109` + `@hospital.test` |
| DEMO-CLINIC02 | `clinic02.doctor@hospital.test` | `clinic0201` đến `clinic0209` + `@hospital.test` |
| DEMO-CLINIC03 | `clinic03.doctor@hospital.test` | `clinic0301` đến `clinic0309` + `@hospital.test` |
| DEMO-CLINIC04 | `clinic04.doctor@hospital.test` | `clinic0401` đến `clinic0409` + `@hospital.test` |

Trong từng bộ: 01/04 là bệnh nhân để đặt lịch mới; 02 có lịch hôm nay; 03 có lịch sau hai ngày; 05 đang chờ; 06 đang khám và có chỉ định đã trả phí; 07 hoàn tất hôm nay; 08 có lịch sử; 09 còn khoản tiền thuốc chưa trả. Script cũng giữ bộ bác sĩ/bệnh nhân nền và hai bác sĩ demo chung.

Script tạo ca từ ngày nạp đến 8 ngày sau. Nạp lại cùng ngày bỏ qua ca đã có, không đưa ca đã khám về trạng thái cũ, không thu tiền hoặc trừ kho lần hai. Ngày khác tạo bộ lượt khám mới. Không xóa volume/data để cập nhật dữ liệu.

## Đường đi cần demo và code tương ứng

| Bước | Vai trò/màn hình | Controller → Service → DAO |
|---|---|---|
| Đăng nhập, tìm kiếm | Bệnh nhân `/login`, `/search` | AuthController → AuthService → UserDAO; SearchController → SearchService → SearchDAO |
| Đặt/đổi/hủy lịch | Bệnh nhân `/appointments` | AppointmentController → AppointmentService → PatientDAO, ScheduleDAO, AppointmentDAO |
| Tạo ca, điều chỉnh ca | Bác sĩ `/schedules` | ScheduleController → ScheduleService → ScheduleDAO |
| Tiếp nhận | Lễ tân `/reception` | ReceptionController → ReceptionService → QueueService → AppointmentDAO, QueueDAO |
| Gọi vào khám | Bác sĩ `/queue` | QueueController → QueueService.callNext → QueueDAO, MedicalRecordDAO |
| Chẩn đoán | Bác sĩ `/emr?queueId=...` | EMRController → EMRService → QueueContextPort, MedicalRecordDAO |
| Chỉ định | Bác sĩ trong EMR | ClinicalOrderController → ClinicalService → ClinicalOrderDAO, MedicalServiceDAO |
| Thu phí | Thu ngân `/billing` | BillingController → BillingService → InvoiceDAO, ClinicalOrderDAO, PrescriptionDAO |
| Nhập/duyệt kết quả | Kỹ thuật viên `/lab` | LabResultController → ClinicalService → TestResultDAO, ClinicalOrderDAO |
| Kê đơn/kết thúc | Bác sĩ trong EMR | PrescriptionController → PrescriptionService → MedicationDAO, PrescriptionDAO; EMRService.closeRecord |
| Hồ sơ/đánh giá | Bệnh nhân `/my-records`, `/feedback` | PatientRecordController/Service; FeedbackController → FeedbackService → FeedbackDAO |
| Danh mục, báo cáo | Quản lý `/services`, `/reports` | ServiceCatalogController → ServiceCatalogService → MedicalServiceDAO; ReportController → AnalyticsService → ReportDAO |

Dùng cùng một bệnh nhân để đi liên tục: tìm và đặt lịch còn giờ hôm nay → lễ tân check-in → bác sĩ gọi tiếp theo. Bác sĩ chỉ được có một bệnh nhân đang khám, nên hoàn tất ca đang mở trước khi gọi người tiếp theo. Không chọn bệnh nhân ở bộ 06 đang khám sẵn nếu muốn demo từ đầu.

## Điều kiện quan trọng

- Không đặt được giờ đã qua hoặc ca hết chỗ; check-in chỉ cho ngày hẹn hôm nay.
- Lưu chẩn đoán trước khi chỉ định. Thu phí trước khi kỹ thuật viên duyệt kết quả.
- Chưa đủ kết quả thì không phát hành đơn thuốc; thiếu tồn kho phải hoàn tác cả đơn.
- Kết thúc khám chỉ cộng các phí mới chưa lập hóa đơn. Không mặc định CLOSED là đã PAID.
- Chỉ bệnh nhân của lượt khám đã đóng được đánh giá, một lần trong 7 ngày.
- Báo cáo doanh thu chỉ lấy PAID; bộ lọc sai bị từ chối, dữ liệu rỗng vẫn là báo cáo hợp lệ.

Quy trình phụ: thu hồi kết quả tại `/result-recalls`, cần trưởng khoa đúng khoa và biên bản PDF. Bộ test HTTP cho quy trình này là `scripts/smoke_result_recalls.py`.
