# Kiểm tra trước khi đẩy Git — 29/10/2026

## Những lỗi đã sửa trong lượt rà này

1. **Callback VNPay bị xử lý hai lần:** trình duyệt mở trang return rồi React gọi API, khiến callback thất bại bị xử lý lại và báo sai. Trang HTML hiện chỉ tải React; API xử lý xác nhận. IPN vẫn là endpoint JSON kiểm tra chữ ký. Có 3 test hồi quy; chưa đối soát merchant VNPay thật.
2. **Mở /hospital/ bị lỗi điều hướng:** frontend đã chuẩn hóa URL gốc có/không có dấu / sang dashboard và nhận đúng context rỗng nếu triển khai ROOT. Có kiểm thử React và đã mở /hospital/ trực tiếp trên trình duyệt.
3. **File local bị đóng vào WAR:** Maven loại WEB-INF/classes/application-local.properties khỏi WAR. File vẫn dùng được cho phát triển; triển khai WAR cấu hình database bằng biến môi trường của Tomcat.

## Kết quả thực chạy

| Kiểm tra | Kết quả |
|---|---|
| Build từ source sạch, không copy cấu hình riêng/node_modules | Thành công |
| Java/MySQL | 49 qua, 0 lỗi, 0 bỏ qua |
| React | 11 qua |
| HTTP luồng chính | 109 qua |
| HTTP thu hồi + kết quả thay thế | 34 qua |
| npm audit (cả dependency phát triển) | 0 lỗ hổng được báo tại thời điểm chạy |
| Git diff --check | Qua |
| WAR cuối | Có React shell + 1 JS bundle; không có JSP cũ hoặc cấu hình local |
| Docker Compose config | Hợp lệ |

HTTP chạy trên database demo riêng, cổng 3001; kiểm thử Java chỉ dùng hospital_test. Cổng 3000 chạy database chính hospital. Đã quan sát báo cáo doanh thu, biểu đồ, điều hướng và console trên trình duyệt; không thấy lỗi JavaScript trong các màn hình đã kiểm.

Docker Engine chưa chạy trên máy nên chưa kiểm thử build container/MySQL 8.4. Các test database ở lượt này dùng MySQL 9.3 cài sẵn. VNPay merchant/SMTP/hệ thống dược thật và các yêu cầu SRS còn thiếu vẫn theo SCOPE.md; đây không phải chứng nhận hoàn tất toàn bộ SRS.

## Khi commit

- Commit cả frontend/, package-lock.json, .npmrc, source Java, SQL/hướng dẫn và các file JSP đã xóa. Chỉ dùng git commit -a sẽ bỏ sót các file mới.
- target/, node_modules/, .local/, .env và application-local.properties được gitignore; không đưa chúng lên repository.
- Thành viên build source cần Node.js 22.12+, npm, JDK 17+ và Maven. Database cũ cần migration theo DATABASE_TROUBLESHOOTING.md.
- Báo cáo cloud/.vpp vẫn cần đổi Presentation từ JSP sang React + HTTP/JSON như REACT_FRONTEND.md hướng dẫn.

Lệnh gợi ý cho nhánh nam (chưa thực hiện commit/push thay người dùng):

```powershell
git add -A
git commit -m "Migrate frontend to React Bootstrap and fix integration issues"
git push origin nam
```

Kết quả chi tiết tạm được giữ ngoài Git trong target/build-pre-git.log, target/http-smoke-results.txt, target/http-nam-workflows-results.txt và target/pre-git-results.json.
