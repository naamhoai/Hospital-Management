<%@ include file="_header.jspf" %>
<h1>Ca làm việc & sức chứa</h1>
<div class="card">
<h2>Tạo ca khám</h2>
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="create">
<div class="row">
<div>
<label for="schedules-1-doctorId">Bác sĩ</label>
<select id="schedules-1-doctorId" name="doctorId" required>
<c:forEach items="${doctors}" var="d">
<option value="${d.doctor_id}">
<c:out value="${d.full_name}"/>
</option>
</c:forEach>
</select>
</div>
<div>
<label for="schedules-2-date">Ngày *</label>
<input id="schedules-2-date" type="date" name="date" required>
</div>
<div>
<label for="schedules-3-shift">Tên ca *</label>
<input id="schedules-3-shift" name="shift" maxlength="20" placeholder="MORNING" required>
</div>
</div>
<div class="row">
<div>
<label for="schedules-4-start">Bắt đầu *</label>
<input id="schedules-4-start" type="time" name="start" required>
</div>
<div>
<label for="schedules-5-end">Kết thúc *</label>
<input id="schedules-5-end" type="time" name="end" required>
</div>
<div>
<label for="schedules-6-capacity">Sức chứa *</label>
<input id="schedules-6-capacity" type="number" name="capacity" min="1" max="240" value="10" required>
</div>
<button>Tạo ca</button>
</div>
</form>
</div>
<div class="card">
<h2>Danh sách ca</h2>
<ui:dataTable rows="${schedules}"/>
</div>
<div class="grid">
<div class="card">
<h2>Điều chỉnh sức chứa</h2>
<p class="muted">Bản hiện tại cho phép điều chỉnh khi ca chưa có lịch đặt.</p>
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="capacity">
<label for="schedules-7-scheduleId">Mã ca *</label>
<input id="schedules-7-scheduleId" type="number" name="scheduleId" min="1" required>
<label for="schedules-8-capacity">Sức chứa mới *</label>
<input id="schedules-8-capacity" type="number" name="capacity" min="1" max="240" required>
<p>
</p>
<button>Cập nhật sức chứa</button>
</form>
</div>
<div class="card">
<h2>Xin nghỉ / hủy ca</h2>
<form method="post" data-confirm="Hủy ca sẽ hủy các lịch chưa tiếp nhận và tạo thông báo cho bệnh nhân. Tiếp tục?">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="leave">
<label for="schedules-9-scheduleId">Mã ca *</label>
<input id="schedules-9-scheduleId" type="number" name="scheduleId" min="1" required>
<p>
</p>
<button class="danger">Xác nhận nghỉ ca</button>
</form>
</div>
</div>
<%@ include file="_footer.jspf" %>
