# Phạm vi bản triển khai

Bản này là ứng dụng học phần chạy được theo các luồng chính của 12 mục Detailed Design, có giao diện JSP và dữ liệu MySQL. Bảng dưới phân biệt phần đã có với yêu cầu trong báo cáo còn cần nhóm phát triển tiếp; không coi đây là bản triển khai bệnh viện thực tế đã nghiệm thu.

| Nhóm | Đã triển khai | Còn cần bổ sung theo báo cáo |
|---|---|---|
| Khánh · CD01 | Login/logout, HttpSession 30 phút, role filter, CSRF, băm mật khẩu, khóa đăng nhập tạm thời | RedisSessionService/JWT chưa dùng cho đăng nhập; hiện chọn HttpSession của Servlet. Chưa có đăng ký, quên mật khẩu, màn quản trị tài khoản |
| Khánh · CD02 | Tìm tên bác sĩ/chuyên khoa, ngày; xem dịch vụ và ca còn giờ đặt được | Chưa có bộ lọc specialtyId riêng và Redis hold chỗ. Transaction đặt lịch ngăn vượt sức chứa khi ghi |
| Khánh · CD03 | Đánh giá bệnh án của chính mình sau khi kết thúc, một lần/7 ngày, cập nhật điểm bác sĩ | Chưa có gửi link đánh giá có token hết hạn; vào trực tiếp bằng session |
| Tráng · CD04 | Đặt/đổi/hủy lịch, chia giờ theo sức chứa, kiểm tra giới hạn và giao dịch | Chưa giữ chỗ tạm, QR booking, hoặc giao diện chọn một giờ chính xác; hệ thống cấp giờ trống đầu tiên |
| Tráng · CD05 | Thêm/sửa/ẩn dịch vụ, giá mới không đổi chi phí đã phát sinh | Chưa quản lý gói khám nhiều dịch vụ |
| Tráng · CD06 | Hóa đơn nhiều đợt theo dòng chi phí chưa thu, tiền mặt, client VNPay sandbox có HMAC, callback và chống thu trùng | Chưa kết nối merchant thật, hoàn tiền, BHYT, mã giảm giá, thẻ ngân hàng; IPN hiện trả mã xác nhận lỗi chung, cần đối chiếu mã lỗi chi tiết khi tích hợp merchant |
| Kiên · CD07 | Tạo ca, ngăn trùng giờ bác sĩ, hủy ca/chủ động thông báo, thay sức chứa trước khi có lịch | Chưa lập lịch tuần tự động, quy tắc chốt thứ Sáu, hợp đồng số ca tối thiểu; ca đã có lịch chưa cho đổi sức chứa để giữ giờ cũ |
| Kiên · CD08 | Check-in mã lịch/điện thoại, walk-in với bệnh nhân đã có tài khoản, chỉ tiếp nhận hôm nay | Chưa tạo hồ sơ bệnh nhân mới ở quầy, máy quét QR, hoặc chọn phòng ít bận tự động |
| Kiên · CD09 | Cấp số duy nhất/ngày/phòng, thứ tự ưu tiên, gọi bệnh nhân, chuyển phòng cùng khoa | Chưa có WebSocket/màn hình gọi số công cộng; chuyển phòng vẫn giữ bác sĩ đã phân công; chưa có thao tác skip/requeue |
| Nam · CD10 | Domain/request Java Beans đúng CD10; queue context, chẩn đoán/ICD/lịch sử; kê nhiều dòng, giá chụp, tồn kho transaction, PDF đơn có Unicode; bệnh nhân xem đơn của mình | Danh mục ICD mặc định chỉ có mã demo. Adapter PharmacyClient cần nhà cung cấp theo hợp đồng; chưa xác nhận tích hợp tương tác thuốc thực tế. Chưa có hủy/hoàn kho đơn đã phát hành |
| Nam · CD11 | ClinicalOrderRequest, typed DAO; chỉ định/hủy trước thanh toán và trước lập hóa đơn; kết quả sau PAID, duyệt khóa; tải PDF/PNG/JPEG 5 MB; tải xuống kiểm tra quyền; email outbox báo có kết quả | Chưa có quy trình trưởng khoa thu hồi kết quả đã duyệt. URL ngoài cần HTTPS; hệ thống không sao chép nội dung URL ngoài về máy chủ |
| Nam · CD12 | ReportFilter; PatientFlowReport/RevenueReport/ChartDataPoint; lọc ngày/khoa/dịch vụ chỉ định/phương thức; DAY/MONTH/QUARTER; số liệu PAID; biểu đồ, bảng, Excel hai sheet, PDF Unicode; kết nối đọc dữ liệu | Tổng hợp trực tiếp khi truy vấn, chưa có kho tổng hợp theo đêm. Bộ lọc serviceId hiện áp dụng chỉ định cận lâm sàng; không gán giả serviceId cho thuốc hoặc phí khám |

## Quyết định triển khai và điểm báo cáo cần nhóm thống nhất

- MVC Layered: Filter → Servlet → Service → DAO → MySQL, Servlet → JSP. SQL đã tách khỏi Controller/Service/listener. `Database` thuộc `util`, quản lý connection/transaction; DAO map dữ liệu thành Java Bean.
- Có đủ 17 entity Java Bean của 3.13 và Room của ERD. Projection danh sách JOIN dùng Map; `DiagnosisRequest`, `PrescriptionRequest`, `ClinicalOrderRequest`, `ReportFilter` là Java Bean. `AuthUser` là session principal, `PrescriptionItem` chỉ là adapter tương thích test cũ.
- CD10 đặt `QueueContextPort.getContext` tại Service; SQ16 cũ đặt `verifyQueueContext` tại Controller. Code chọn ranh giới CD10 để validation dùng lại được; không sửa ngầm báo cáo. SQ16 cũng chưa thể hiện tạo bệnh án lần đầu: QueueService tạo draft OPEN khi gọi bệnh nhân.
- `medical_record` không thêm queue_id. EMR nhận queueId rồi JOIN qua appointment_id. Draft có diagnosis chuỗi rỗng để thỏa NN; chỉ định/kê đơn/kết thúc yêu cầu chẩn đoán đã lưu thực sự.
- Schema nền có 18 bảng nghiệp vụ, 5 bảng kỹ thuật tách riêng. File migration cho bản cũ giữ dữ liệu và các cột BIGINT cũ; schema mới dùng INT theo SDS. Chi tiết trường/kiểu và phần mở rộng trong DESIGN_TRACEABILITY.md.
- MySQL theo yêu cầu đã chốt với người dùng; báo cáo đang ghi kiểu SQL Server (NVARCHAR(MAX)). Chuyển sang VARCHAR/TEXT với utf8mb4, giữ domain/quan hệ. SRS bảng công nghệ ghi SQL Server và một số CD/SQ nhắc Redis/JWT, trong khi mô tả architecture là Servlet/JSP + session. Bản này sử dụng HttpSession; không tuyên bố đã dùng Redis/JWT.
- Bệnh án đóng sau khi chỉ định hoàn tất hoặc hủy. Chi phí chưa thu được tự lập/cộng vào hóa đơn cuối lượt; tổng doanh thu chỉ gồm hóa đơn PAID. Không tự đánh dấu thanh toán khi đóng bệnh án.
- UI hiện hỗ trợ bệnh nhân xem hồ sơ/đơn/kết quả/hóa đơn. Chức năng của các thành viên khác vẫn cần hoàn thiện những mục SRS ghi ở các hàng phía trên; bản refactor này không đồng nghĩa toàn bộ SRS đã nghiệm thu.

## Triển khai và tích hợp

Tomcat demo bind localhost; triển khai qua reverse proxy cần HTTPS, cookie Secure và cấu hình proxy hợp lý. Chưa benchmark mục tiêu 100 người dùng. JDBC hiện mở connection theo transaction; nhóm có thể thay Database.open bằng JNDI DataSource/connection pool khi triển khai lâu dài. Tác vụ outbox hiện phù hợp một instance ứng dụng; cần cơ chế nhận việc/lock nếu chạy nhiều instance. Email sau lỗi có thể được gửi lại (at-least-once).

Tài khoản demo và mật khẩu local được ghi công khai để phục vụ học phần; thay khi dùng môi trường khác. Không có dữ liệu bệnh nhân thực trong seed hoặc kiểm thử.
