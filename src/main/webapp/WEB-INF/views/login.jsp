<%@ include file="_header.jspf" %>
<div class="card login"><h1>Đăng nhập</h1><p class="muted">Truy cập chức năng theo vai trò của bạn.</p><c:if test="${not empty error}"><div class="notice error"><c:out value="${error}"/></div></c:if>
<form method="post" action="${ctx}/login"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><label>Email *</label><input type="email" name="email" required maxlength="100" autocomplete="username"><label>Mật khẩu *</label><input type="password" name="password" required maxlength="200" autocomplete="current-password"><p></p><button>Đăng nhập</button></form></div>
<%@ include file="_footer.jspf" %>
