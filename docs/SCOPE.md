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
| Nam · CD10 | Nhận queueId, kiểm tra bác sĩ, lưu triệu chứng/ICD/chẩn đoán, lịch sử, kê nhiều thuốc, in đơn, tồn kho transaction | Danh mục ICD chỉ có vài mã demo; chưa có tương tác thuốc hoặc PharmacyClient thật; chưa có hủy/hoàn kho cho đơn chưa trả |
| Nam · CD11 | Chỉ định dịch vụ đang hoạt động, giữ giá, nhập kết quả sau PAID, duyệt khóa kết quả | Chưa tải file nhị phân; nhận URL HTTPS. Chưa có luồng trưởng khoa hủy kết quả đã duyệt |
| Nam · CD12 | Lưu lượng, tỷ lệ hoàn tất/hủy/vắng, doanh thu PAID theo ngày/khoa/loại/phương thức, biểu đồ tỷ lệ, Excel/PDF | Chưa lọc từng serviceId/departmentId hoặc group theo tháng/quý; PDF dùng chữ không dấu, Excel giữ Unicode |

## Các quyết định cần đồng bộ UML

- Service nhận `AuthUser` để kiểm tra quyền ngoài các request nghiệp vụ. Danh sách JOIN dùng Map projection, các request/item DTO dùng Java record; chưa tạo đủ 17 Java Bean trong mục 3.13.
- Thống nhất DAO thay vì Repository trong code mới. Các luồng trong tài liệu cũ còn dùng tên Repository cần cập nhật khi nhóm chốt source.
- EMRService gọi `QueueContextPort.getContext(queueId)` như CD10, không để EMRController tự xác thực dữ liệu hàng đợi.
- QueueService tạo bệnh án OPEN khi gọi bệnh nhân; xử lý được trường hợp ban đầu chưa có bệnh án, vốn sequence SQ16 cũ chưa thể hiện.
- Database thêm `medical_record.queue_id`, bảng ICD, chụp giá, invoice_line, payment_attempt, outbox và audit để các luồng vận hành được. Schema có FK, UNIQUE và CHECK; không sửa ngầm file báo cáo của nhóm.
- Thuốc trừ kho khi phát hành đơn. Bệnh án đóng khi các chỉ định đã hoàn tất; chi phí thuốc có thể được thu sau đó.
- Lịch hẹn nhiều đợt trong cùng một ca có giờ khác nhau; khóa dòng ca bảo đảm hai giao dịch không cấp cùng giờ. Nhiều ca cùng bác sĩ được kiểm tra trùng khi tạo qua Service.

## Triển khai và tích hợp

Tomcat demo bind localhost; triển khai qua reverse proxy cần HTTPS, cookie Secure và cấu hình proxy hợp lý. Chưa benchmark mục tiêu 100 người dùng. JDBC hiện mở connection theo transaction; nhóm có thể thay Database.open bằng JNDI DataSource/connection pool khi triển khai lâu dài. Tác vụ outbox hiện phù hợp một instance ứng dụng; cần cơ chế nhận việc/lock nếu chạy nhiều instance. Email sau lỗi có thể được gửi lại (at-least-once).

Tài khoản demo và mật khẩu local được ghi công khai để phục vụ học phần; thay khi dùng môi trường khác. Không có dữ liệu bệnh nhân thực trong seed hoặc kiểm thử.
