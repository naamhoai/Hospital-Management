# Phạm vi ứng dụng demo

Bốn luồng chính: đặt lịch/tiếp nhận/EMR/thanh toán; lịch bác sĩ và duyệt nghỉ; danh mục/giá/gói; báo cáo/xuất file. Có UI JSP và dữ liệu MySQL để đi từ đầu đến cuối bằng các vai trò thật trong ứng dụng.

- Lịch hỗ trợ tạo một ca hoặc lặp tuần/tháng, không trùng giờ; xin nghỉ phải qua quản lý duyệt.
- Gói khám có thành phần, giá được phân bổ vào chỉ định; danh mục đổi giá không hồi tố.
- Xét nghiệm cần trả tiền trước; kê đơn cần đủ kết quả và đủ thuốc; đóng bệnh án không tự đồng nghĩa PAID.
- UI dùng HTML/CSS/JavaScript thuần. Login dùng HttpSession; SQL nằm ở DAO, transaction ở Service.
- Chỉ có database/hospital.sql chứa tạo bảng và dữ liệu. Không có bộ test, SQL phụ hay backup trong source.

Ngoài phạm vi demo cơ bản: SMS/LIS thật, merchant VNPay thật, kiểm tra tương tác thuốc với nhà cung cấp, JWT/Redis, QR, BHYT, hoàn tiền và lịch theo quy tắc hợp đồng nâng cao. Adapter VNPay/email/dược chỉ dùng khi có cấu hình ngoài; tiền mặt đủ để đi hết luồng.

Chi tiết thao tác: DEMO_MAIN_FLOW.md. Tên hàm và dữ liệu vào/ra: WORKFLOW_CODE_GUIDE.md.
