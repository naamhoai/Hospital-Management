# Kịch bản demo bốn workflow

Nạp duy nhất `database/hospital.sql`, cấu hình MySQL theo README rồi chạy `scripts/run-dev.ps1`. Mật khẩu web dùng chung: `Demo@1234`.

## WF01 — Đặt lịch, khám và thanh toán liền mạch

1. **Bệnh nhân `patient@hospital.test`:** tìm bác sĩ An, chọn ca **hôm nay còn giờ**, đặt lịch. Ghi lại mã AP vừa tạo; có thể thấy trong “Lịch khám của tôi”.
2. **Lễ tân `reception@hospital.test`:** vào Tiếp nhận, tìm đúng mã AP, check-in. Hệ thống cấp số chờ/phòng; không tự tạo bệnh án tại quầy.
3. **Bác sĩ `doctor@hospital.test`:** vào Hàng đợi, gọi người tiếp theo. Hệ thống mở bệnh án nháp và chuyển đến EMR bằng queueId.
4. Lưu triệu chứng, ICD **J00**, chẩn đoán và ghi chú. Sau đó chọn một xét nghiệm hoặc **gói kiểm tra cơ bản**. Gói tạo các chỉ định thành phần với tổng giá đúng giá gói.
5. **Thu ngân `cashier@hospital.test`:** chọn đúng bệnh án, lập hóa đơn và thu tiền mặt lần 1. Có phí khám và xét nghiệm; chỉ những chỉ định thuộc hóa đơn đã PAID mới được làm.
6. **Kỹ thuật viên `lab@hospital.test`:** tìm đúng chỉ định/bệnh nhân vừa tạo, nhập nội dung và duyệt từng kết quả. Không chọn nhầm chỉ định mẫu của bác sĩ Bình.
7. **Bác sĩ:** mở lại EMR, xem đủ kết quả, kê thuốc theo danh mục minh họa rồi kết thúc. Kho chỉ trừ khi phát hành đơn. Đóng bệnh án tự lập hóa đơn cho phí mới chưa tính.
8. **Thu ngân:** thu khoản còn lại. Phí khám/xét nghiệm đã thu không được cộng lần nữa.
9. **Bệnh nhân:** xem hồ sơ, kết quả, đơn và hóa đơn; gửi đánh giá. **Quản lý** xem báo cáo phát sinh.

Nếu không cần xét nghiệm, bỏ chỉ định và đi tiếp kê đơn/kết thúc. Không kê đơn khi vẫn còn chỉ định chưa hoàn tất. Hết giờ hôm nay thì bác sĩ tạo thêm ca còn giờ hoặc trình bày bằng lượt đã được tiếp nhận sẵn.

## WF02 — Lịch bác sĩ và xin nghỉ

- Bác sĩ mở **Lịch bác sĩ**, tạo ca chưa trùng, chọn một ngày hoặc lặp tuần/tháng tới ngày kết thúc.
- Danh sách hiện mã ca, ngày/giờ, số người đã đặt và trạng thái.
- Gửi yêu cầu nghỉ cho một ca chưa tiếp nhận; nhập lý do. Trạng thái chuyển “Chờ quản lý duyệt”, lịch hẹn vẫn được giữ.
- Đăng nhập **manager**, mở cùng màn hình, từ chối hoặc đồng ý kèm ghi chú.
- Từ chối giữ ca/lịch hẹn. Đồng ý khóa ca, hủy lịch BOOKED liên quan và tạo thông báo đặt lại. Ca đã tiếp nhận không cho hủy.

## WF03 — Danh mục dịch vụ, gói và giá

- Quản lý mở **Danh mục dịch vụ**, thêm/sửa mã dịch vụ, khoa, mô tả, giá, loại và trạng thái.
- Với loại **Gói khám**, chọn ít nhất hai thành phần LAB/IMAGING; không lồng một gói trong gói khác.
- Gói có giá riêng. Khi bác sĩ chọn gói, hệ thống phân bổ giá vào các chỉ định thành phần; tổng vẫn đúng giá gói.
- Đổi giá sau khi đã tạo chỉ định không đổi chi phí của chỉ định cũ. Chuyển INACTIVE để ngừng chọn cho lượt khám mới.

## WF04 — Báo cáo

- Quản lý mở **Báo cáo**, chọn lượt khám hoặc doanh thu.
- Chọn ngày/khoa/dịch vụ/phương thức và cách nhóm DAY/MONTH/QUARTER.
- Xem chỉ số, bảng và biểu đồ cột CSS. Dữ liệu rỗng hiển thị hợp lệ; ngày bắt đầu sau ngày kết thúc báo lỗi.
- Có thể xuất PDF/XLSX. Hóa đơn UNPAID không cộng vào doanh thu.

## Dữ liệu có sẵn trong một file SQL

| Tài khoản | Dùng để trình bày |
|---|---|
| patient / doctor | Cặp chính để bắt đầu lượt khám mới |
| patient2 | Lịch BOOKED vào ngày nạp SQL, tiếp nhận được ngay trong ngày đó |
| patient3 | Bệnh nhân dự phòng; có thể đặt lượt mới nếu lượt trước đã hoàn tất |
| patient4 / doctor2 | Lượt đang khám, xét nghiệm đã trả tiền, chờ Lab nhập kết quả |
| patient5 | Hồ sơ lịch sử đã trả tiền và có kết quả/đơn |
| patient6 | Lượt đã hoàn tất nhưng hóa đơn còn UNPAID |
| patient7 / patient8 | Lịch sử hủy/vắng để báo cáo có số liệu |
| patient9 | Lịch ngày sau ngày nạp SQL, dùng trình bày đổi lịch |

Lịch mẫu tính theo ngày nạp SQL. Không cần nạp lại file mỗi ngày: bác sĩ có thể tạo ca mới và bệnh nhân đặt lượt mới ngay trên web.

Mỗi vai trò dùng email `tên@hospital.test` theo bảng README. Nếu đổi người trình bày, giao tiếp bằng **mã lịch → queueId → recordId → invoiceId**; không lấy tùy ý mã từ bệnh nhân khác.
