<%@ include file="_header.jspf" %>
<div class="login-layout">
<section class="login-intro">
<span class="eyebrow">HOSPITAL CARE</span>
<h1>Một nơi để quản lý<br>hành trình khám bệnh.</h1>
<p>Lịch hẹn, hồ sơ khám và kết quả được kết nối trong một không gian làm việc rõ ràng.</p>
<div class="intro-step">
<span>01</span>
<div>
<strong>Đặt lịch & tiếp nhận</strong>
<p>Chọn bác sĩ, ca khám và theo dõi số chờ.</p>
</div>
</div>
<div class="intro-step">
<span>02</span>
<div>
<strong>Khám & điều trị</strong>
<p>Chẩn đoán, chỉ định và đơn thuốc theo từng lượt khám.</p>
</div>
</div>
<div class="intro-step">
<span>03</span>
<div>
<strong>Theo dõi & tổng hợp</strong>
<p>Kết quả xét nghiệm, thanh toán và báo cáo.</p>
</div>
</div>
</section>
<section class="card login">
<span class="eyebrow">CHÀO MỪNG TRỞ LẠI</span>
<h2>Đăng nhập</h2>
<p class="muted">Sử dụng tài khoản được cấp để tiếp tục.</p>
<form method="post" action="${ctx}/login">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<label for="email">Email</label>
<input id="email" type="email" name="email" required maxlength="100" autocomplete="username" placeholder="ten@hospital.test">
<label for="password">Mật khẩu</label>
<input id="password" type="password" name="password" required maxlength="200" autocomplete="current-password">
<button class="full" type="submit">Đăng nhập <span aria-hidden="true">→</span>
</button>
</form>
<p class="login-help">Chức năng hiển thị theo vai trò của tài khoản.</p>
</section>
</div>
<%@ include file="_footer.jspf" %>