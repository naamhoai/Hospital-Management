# Dữ liệu và kịch bản test luồng chính

Bản hiện tại: Java Servlet/JSP/JSTL, CSS và JavaScript thuần; Tomcat/MySQL. Không dùng React Bootstrap.

## Nạp dữ liệu

File `database/07-demo-main-flow.sql` bổ sung dữ liệu giả lập vào database `hospital` đã có schema/seed/migration 05. Mở file này bằng MySQL Workbench và chạy toàn bộ bằng tài khoản quản trị database (cần CREATE/DROP ROUTINE). Không chạy lại 01/02/04/05 trên database cũ.

Script dùng ngày hiện tại theo múi giờ Việt Nam, tạo lịch cho hôm nay và 8 ngày tiếp theo. Có thể chạy lại: cùng ngày, các mã `DEMO-YYYYMMDD-Cxx` đã tồn tại sẽ được bỏ qua, không đổi trạng thái bạn đã test, không tạo hóa đơn trùng hoặc trừ lại tồn kho. Ngày khác tạo bộ ca mới, giữ lịch sử cũ. Chạy tuần tự, không chạy đồng thời nhiều bản script. Script không reset mật khẩu/tồn kho hay khôi phục ca đã xử lý; nếu cần test lại một luồng từ đầu, dùng một bệnh nhân còn trống hoặc lịch hẹn mới.

Docker chỉ tự nạp SQL khi volume database còn trống. Volume đã có dữ liệu: chạy riêng script 07; giữ volume hiện có.

Ngày 02/11/2026 đã nạp bộ dữ liệu vào `hospital` local, MySQL cổng **33307**. Các bản sao lưu tạm trước khi nạp đã được xóa theo yêu cầu dọn thư mục; database đang dùng và dữ liệu demo được giữ nguyên.

## Tài khoản

Mật khẩu của các tài khoản mới: **`Demo@1234`**.

| Tài khoản | Dùng để test |
|---|---|
| `demo.doctor@hospital.test` | Bác sĩ Demo Nội, phòng P101; luồng khám chính |
| `demo.doctor2@hospital.test` | Bác sĩ Demo TMH, phòng P201; tiếp tục ca xét nghiệm có sẵn |
| `demo01@hospital.test` | Bệnh nhân chưa có lịch: bắt đầu từ tìm kiếm/đặt lịch |
| `demo02@hospital.test` | Lịch hôm nay BOOKED: tìm mã lịch và check-in |
| `demo03@hospital.test` | Đã check-in, queue WAITING: bác sĩ gọi vào khám |
| `demo04@hospital.test` | Đang khám với bác sĩ Demo TMH, đã có chẩn đoán, chỉ định PENDING và hóa đơn UNPAID: thu ngân trả tiền → Lab nhập kết quả → bác sĩ kê đơn/kết thúc |
| `demo05@hospital.test` | Đã kết thúc khám; phí khám/xét nghiệm đã trả, hóa đơn thuốc còn UNPAID: test thu phần còn lại |
| `demo06@hospital.test` | Đã khám xong và thanh toán; chưa đánh giá: test xem hồ sơ, đơn thuốc/PDF, đánh giá trong 7 ngày |
| `demo07@hospital.test` | Lịch sau hôm nay 2 ngày: test đổi lịch (trước 12 giờ), hoặc hủy lịch |
| `demo08@hospital.test` | Lịch quá khứ NO_SHOW: số liệu thống kê |
| `demo09@hospital.test` | Lịch quá khứ CANCELLED: số liệu thống kê |
| `demo10@hospital.test` – `demo12@hospital.test` | Hồ sơ đã hoàn tất, kết quả xét nghiệm, thuốc, 2 hóa đơn đã trả và đánh giá; lịch sử từ 7–35 ngày trước |

Các tài khoản chức năng cũ tiếp tục dùng: `reception@hospital.test`, `cashier@hospital.test`, `lab@hospital.test`, `manager@hospital.test`, `admin@hospital.test` (mật khẩu ban đầu `Demo@1234`, script 07 không đổi mật khẩu cũ).

## Bộ test riêng cho 4 thành viên

Đã nạp thêm vào `hospital` local ngày 02/11/2026. Phần bổ sung nằm ngay trong **cùng script 07**, không phải schema/database mới. Mỗi người có bác sĩ và phòng riêng để tránh lấy nhầm bệnh nhân khi Gọi tiếp theo. Mật khẩu các tài khoản mới: **`Demo@1234`**.

| Người test | Bác sĩ | Các bệnh nhân | Phòng |
|---|---|---|---|
| Khánh | `khanh.doctor@hospital.test` | `khanh01@hospital.test` đến `khanh04@hospital.test` | `DEMO-KHANH` |
| Tráng | `trang.doctor@hospital.test` | `trang01@hospital.test` đến `trang04@hospital.test` | `DEMO-TRANG` |
| Kiên | `kien.doctor@hospital.test` | `kien01@hospital.test` đến `kien04@hospital.test` | `DEMO-KIEN` |
| Nam | `nam.doctor@hospital.test` | `nam01@hospital.test` đến `nam04@hospital.test` | `DEMO-NAM` |

- Bệnh nhân **01** chưa có lịch: test toàn bộ từ tìm bác sĩ của mình → đặt lịch → tiếp nhận → khám → xét nghiệm/thanh toán → kê đơn → kết thúc → đánh giá.
- Bệnh nhân **02** đã có lịch **hôm nay**, BOOKED: lễ tân tìm mã `DEMO-TEAM-YYYYMMDD-TÊN-2` và check-in, bác sĩ tương ứng gọi vào khám. Ví dụ Nam: `DEMO-TEAM-20261102-NAM-2`.
- Bệnh nhân **03** đã có lịch **sau 2 ngày**: test đổi hoặc hủy lịch. Không check-in lịch tương lai.
- Bệnh nhân **04** chưa có lịch: dự phòng test đặt lịch hoặc đăng ký khám trực tiếp bằng tài khoản lễ tân; chọn đúng bác sĩ/phòng của mình.

Các tài khoản lễ tân, thu ngân, kỹ thuật viên và quản lý vẫn dùng chung như bảng trên. Khi nhiều người test, kiểm tra đúng **tên bệnh nhân, mã lịch, recordId và orderId**, dùng các trình duyệt/profile riêng cho từng vai trò vì các tab cùng trình duyệt dùng chung HttpSession.

Riêng phần nhóm trong script 07 bổ sung **20 tài khoản, 4 phòng khám, 108 ca trong 9 ngày và 8 lịch hẹn**, dành cho test từ bước đặt lịch. Muốn có hàng đợi/bệnh án hiển thị ngay, dùng thêm script 08 bên dưới; bộ này đã được nạp trên máy local.

### Đã bổ sung dữ liệu hiển thị trên các trang web (script 08)

Script **`database/08-demo-web-workflows.sql`** chạy sau 07, bổ sung các ca đã đi qua tiếp nhận/khám/thanh toán để mở các màn hình có dữ liệu ngay. Ngày 02/11/2026 đã nạp trực tiếp vào `hospital` local, áp dụng cho **cả 8 bác sĩ**. Mỗi tài khoản `khanh.doctor`, `trang.doctor`, `kien.doctor`, `nam.doctor` có các bệnh nhân cùng tiền tố, ví dụ Nam:

| Bệnh nhân | Trạng thái / trang để test |
|---|---|
| `nam05@hospital.test` | Đã check-in, WAITING; hiển thị Hàng đợi hôm nay, chưa có bệnh án cho tới khi bác sĩ gọi |
| `nam06@hospital.test` | Đang khám; có chẩn đoán và chỉ định đã trả tiền, chờ kỹ thuật viên nhập kết quả |
| `nam07@hospital.test` | Hoàn tất hôm nay; có kết quả, đơn thuốc, hóa đơn đã trả; có thể xem hồ sơ và gửi đánh giá |
| `nam08@hospital.test` | Hồ sơ 14 ngày trước; có kết quả, đơn thuốc, hóa đơn và đánh giá; có đề nghị thu hồi kết quả chờ trưởng khoa xử lý |
| `nam09@hospital.test` | Hoàn tất hôm qua; còn hóa đơn thuốc UNPAID để thu ngân test thanh toán |

Tương tự với `khanh05`–`khanh09`, `trang05`–`trang09`, `kien05`–`kien09` (tất cả thêm `@hospital.test`). Các bác sĩ cũ cũng có dữ liệu: `doctor@hospital.test` → `an05`–`an09`; `doctor2@hospital.test` → `binh05`–`binh09`; `demo.doctor@hospital.test` → `demo.main05`–`demo.main09`; `demo.doctor2@hospital.test` → `demo.tmh05`–`demo.tmh09`.

Mật khẩu các bệnh nhân mới vẫn là **`Demo@1234`**. Giữ các bệnh nhân 01/04 để tự bắt đầu luồng mới; 05–09 dùng để tiếp tục từ các bước có dữ liệu sẵn.

**Cách thử ngay:** bác sĩ vào EMR mở bệnh nhân 06 → kỹ thuật viên `lab@hospital.test` mở chỉ định của bệnh nhân đó và Lưu & duyệt kết quả → bác sĩ kê đơn/kết thúc. Sau khi hoàn tất ca đang khám mới bấm Gọi bệnh nhân tiếp theo để gọi bệnh nhân 05; mỗi bác sĩ chỉ có một ca IN_CONSULTATION. Nếu bác sĩ đã có ca đang khám trước lúc seed thì ca cũ được giữ nguyên, ca 06 mới được tạo dạng lịch sử hoàn tất, không tạo thêm một ca đang khám song song.

Hàng đợi chỉ lọc **hôm nay và đúng bác sĩ đang đăng nhập**. Nếu qua ngày khác cần chạy lại 07 rồi 08; script thêm bộ ngày mới, không dời ngày hoặc reset ca cũ. Hóa đơn/kết quả/đơn thuốc được nối đúng recordId, orderId và patientId để hiện ở các màn hình thật. Đề nghị thu hồi chỉ ở trạng thái PENDING; không giả biên bản hoặc chữ ký đã được duyệt.

Máy thành viên khác có database riêng: chạy **07 rồi 08** trên máy đó. Docker tự nạp cả hai khi tạo database volume mới; không xóa volume đang dùng để lấy seed.

Đã kiểm tra chạy lại script không tạo trùng, và đối chiếu các dòng lịch hẹn/bệnh án/hóa đơn/thuốc có trước khi nạp: giữ nguyên. Máy thành viên khác cần chạy script 07 trên **database local của họ**; dữ liệu trên máy này không tự đồng bộ qua Git.

## Chạy một luồng từ đầu đến cuối

1. Bệnh nhân `demo01`: đăng nhập → Tìm kiếm → chọn **Bác sĩ Demo Nội**, ca hôm nay còn giờ tương lai → đặt lịch. Ghi mã lịch, ngày và giờ. Tương ứng **CD01/SQ01, CD02/SQ04, CD04/SQ06**.
2. Lễ tân: Tiếp nhận → tìm theo mã lịch → check-in, ghi số chờ và `queueId`. **CD08/SQ14**.
3. Bác sĩ `demo.doctor`: Hàng đợi → Gọi tiếp theo. `demo03` đã có sẵn trong hàng đợi nên có thể được gọi trước; xem đúng tên bệnh nhân, hoàn tất ca đang khám rồi mới gọi tiếp. Code không cho cùng bác sĩ khám hai người đồng thời. **CD09/SQ15**.
4. Bác sĩ vào EMR từ `queueId`, nhập triệu chứng, chọn ICD `J00`, nhập chẩn đoán và lưu. **CD10/SQ16**.
5. Chọn dịch vụ `DEMO - Xét nghiệm tổng quát` → Tạo chỉ định. Ghi `recordId`/`orderId`. **CD11/SQ18**.
6. Thu ngân: Thanh toán → chọn đúng bệnh án → Lập hóa đơn → xác nhận tiền mặt. Hóa đơn gồm phí khám + xét nghiệm. **CD06/SQ10**.
7. Kỹ thuật viên: Xét nghiệm → chọn đúng chỉ định đã trả tiền → nhập nội dung/kết luận → Lưu & duyệt. Không cần tải tệp để test luồng cơ bản. **CD11/SQ19**.
8. Bác sĩ mở lại bệnh án, xem kết quả → kê `DEMO - Thuốc minh họa 1`, số lượng 2, liều minh họa → phát hành/xem/PDF đơn. **CD10/SQ17**.
9. Thu ngân lập hóa đơn lần 2 cho cùng bệnh án, thu phần thuốc mới. Phí khám/xét nghiệm đã trả không xuất hiện lại. **CD06/SQ10**.
10. Bác sĩ kết thúc khám: bệnh án CLOSED, queue và lịch hẹn COMPLETED. Đây là bước kết thúc triển khai trong `EMRService.closeRecord`, chưa có một sequence riêng trong bộ 20 SQ gốc.
11. Bệnh nhân xem Hồ sơ của tôi → đánh giá đúng bệnh án một lần. **CD03/SQ05**.
12. Quản lý: Báo cáo → Lưu lượng/Doanh thu → chọn khoảng ngày từ **40 ngày trước đến hôm nay**, nhóm Ngày/Tháng/Quý → xuất Excel hoặc PDF. **CD12/SQ20**. Mặc định báo cáo chỉ lấy từ đầu tháng nên có thể chưa hiện đủ dữ liệu lịch sử.

Tất cả dữ liệu y tế/thuốc trong bộ này là giả lập phục vụ học tập. Phần thanh toán demo dùng tiền mặt; VNPay, SMTP và API dược cần cấu hình nhà cung cấp riêng, không được seed giả thành giao dịch gateway thật.

## Số lượng của bộ demo chung mỗi ngày (chưa tính bộ 4 thành viên)

14 tài khoản (12 bệnh nhân, 2 bác sĩ); 54 ca khám trong 9 ngày và 7 ca lịch sử; 11 lịch hẹn; 7 queue; 6 bệnh án; 6 chỉ định; 5 kết quả; 5 đơn thuốc; 11 hóa đơn; 17 dòng phí; 3 đánh giá. Danh mục thêm 1 dịch vụ xét nghiệm và 2 thuốc demo. Các lịch sử đã hoàn tất giúp biểu đồ có số liệu ngay. Số ID phụ thuộc dữ liệu có sẵn; xem SELECT cuối script thay vì ghi cứng ID.

## Đã kiểm tra ngày 02/11/2026

- Nạp script hai lần trên database kiểm thử riêng: số ca/hóa đơn và tồn kho giữ nguyên ở lần hai.
- 77 kiểm tra HTTP đạt trên database riêng: hoàn tất ca WAITING có sẵn, đặt lịch/check-in/khám/xét nghiệm/kê đơn/thanh toán hai hóa đơn/đánh giá; chặn nhập kết quả trước thanh toán; xuất đơn thuốc PDF và báo cáo PDF/XLSX.
- 24 kiểm tra HTTP đăng nhập/xem trang đạt trên `hospital` local. Không thực hiện khám/thanh toán trên các ca demo chính. Tổng các hóa đơn demo khớp tổng dòng phí.
- Sau khi bổ sung bộ 4 thành viên: thêm 32 kiểm tra HTTP đăng nhập và mở trang cho 4 bác sĩ/4 bệnh nhân mới đều đạt; giữ nguyên các ca để nhóm tự test.
- Script 08: nạp thử hai lần trên database riêng, xác nhận không nhân đôi dữ liệu/trừ lại kho; sau đó nạp vào hospital và giữ nguyên tất cả lịch hẹn/bệnh án/hóa đơn có trước. 107 kiểm tra HTTP đạt với nội dung thật: hàng đợi/EMR/lịch của cả 8 bác sĩ đều có dòng; hồ sơ bệnh nhân có kết quả và đơn thuốc; trang thu ngân có hóa đơn chưa trả; danh sách Lab, trưởng khoa, tiếp nhận và báo cáo đều có dữ liệu. Không xử lý hết các ca demo trong quá trình kiểm tra.
- Build WAR sạch thành công; có JSP và không còn ApiController/FrontendResponse của bản React. Các tích hợp nhà cung cấp bên ngoài chưa được kiểm tra bằng giao dịch thật trong lần bổ sung dữ liệu này.
