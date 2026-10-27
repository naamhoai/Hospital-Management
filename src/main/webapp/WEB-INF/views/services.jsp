<%@ include file="_header.jspf" %>
<h1>Danh mục dịch vụ & giá</h1>
<div class="card">
<ui:dataTable rows="${services}"/>
</div>
<div class="card">
<h2>Thêm / cập nhật dịch vụ</h2>
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<div class="row">
<div>
<label for="services-1-serviceId">Mã dịch vụ (để trống khi thêm)</label>
<input id="services-1-serviceId" type="number" name="serviceId" min="1">
</div>
<div>
<label for="services-2-name">Tên dịch vụ *</label>
<input id="services-2-name" name="name" required maxlength="100">
</div>
<div>
<label for="services-3-departmentId">Chuyên khoa</label>
<select id="services-3-departmentId" name="departmentId">
<c:forEach items="${departments}" var="d">
<option value="${d.department_id}">
<c:out value="${d.name}"/>
</option>
</c:forEach>
</select>
</div>
</div>
<div class="row">
<div>
<label for="services-4-category">Phân loại</label>
<select id="services-4-category" name="category">
<option value="CONSULTATION">Khám bệnh</option>
<option value="LAB">Xét nghiệm</option>
<option value="IMAGING">Chẩn đoán hình ảnh</option>
</select>
</div>
<div>
<label for="services-5-price">Đơn giá VND *</label>
<input id="services-5-price" type="number" name="price" required min="0" step="1">
</div>
<div>
<label for="services-6-status">Trạng thái</label>
<select id="services-6-status" name="status">
<option value="ACTIVE">Hoạt động</option>
<option value="INACTIVE">Ngừng hoạt động</option>
</select>
</div>
<button>Lưu dịch vụ</button>
</div>
</form>
</div>
<%@ include file="_footer.jspf" %>
