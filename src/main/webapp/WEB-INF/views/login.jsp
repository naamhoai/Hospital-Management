<%@ include file="_header.jspf" %>
<section class="card login">
<h1>Đăng nhập Hospital</h1>
<p class="muted">Quản lý lịch hẹn, tiếp nhận, khám bệnh và báo cáo.</p>
<form method="post" action="${ctx}/login">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<label for="email">Email</label><input id="email" name="email" type="email" required maxlength="100" autocomplete="username">
<label for="password">Mật khẩu</label><input id="password" name="password" type="password" required maxlength="200" autocomplete="current-password">
<button class="full">Đăng nhập</button>
</form>
<p class="muted">Các chức năng sẽ hiển thị theo vai trò tài khoản.</p>
</section>
<%@ include file="_footer.jspf" %>
