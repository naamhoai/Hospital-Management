<%@ include file="_header.jspf" %>
<h1>Lịch khám của tôi</h1>
<div class="card">
<h2>Đặt lịch mới</h2>
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="book">
<label for="appointments-1-scheduleId">Ca khám *</label>
<select id="appointments-1-scheduleId" name="scheduleId" required>
<option value="">Chọn bác sĩ và ca khám</option>
<c:forEach items="${schedules}" var="w">
<option value="${w.schedule_id}">#${w.schedule_id} · <c:out value="${w.doctor_name}"/> · ${w.work_date} ${w.start_time}–${w.end_time} · còn ${w.remaining} chỗ</option>
</c:forEach>
</select>
<label for="appointments-2-symptoms">Triệu chứng ban đầu *</label>
<textarea id="appointments-2-symptoms" name="symptoms" maxlength="255" required>
</textarea>
<p>
</p>
<button>Đặt lịch</button>
</form>
</div>
<div class="card">
<h2>Danh sách lịch khám</h2>
<ui:dataTable rows="${appointments}"/>
</div>
<div class="grid">
<div class="card">
<h2>Đổi ca khám</h2>
<p class="muted">Tối đa 2 lần; trước khi ca cũ bắt đầu ít nhất 12 giờ.</p>
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="reschedule">
<label for="appointments-3-appointmentId">Lịch cần đổi</label>
<select id="appointments-3-appointmentId" name="appointmentId" required>
<c:forEach items="${appointments}" var="a">
<c:if test="${a.status=='BOOKED'}">
<option value="${a.appointment_id}">
<c:out value="${a.appointment_code}"/> · ${a.appointment_date} ${a.appointment_time}</option>
</c:if>
</c:forEach>
</select>
<label for="appointments-4-scheduleId">Ca mới</label>
<select id="appointments-4-scheduleId" name="scheduleId" required>
<c:forEach items="${schedules}" var="w">
<option value="${w.schedule_id}">
<c:out value="${w.doctor_name}"/> · ${w.work_date} ${w.start_time}</option>
</c:forEach>
</select>
<p>
</p>
<button>Đổi lịch</button>
</form>
</div>
<div class="card">
<h2>Hủy lịch</h2>
<form method="post" data-confirm="Bạn chắc chắn muốn hủy lịch khám này?">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="cancel">
<label for="appointments-5-appointmentId">Lịch cần hủy</label>
<select id="appointments-5-appointmentId" name="appointmentId" required>
<c:forEach items="${appointments}" var="a">
<c:if test="${a.status=='BOOKED'}">
<option value="${a.appointment_id}">
<c:out value="${a.appointment_code}"/> · ${a.appointment_date}</option>
</c:if>
</c:forEach>
</select>
<p>
</p>
<button class="danger">Hủy lịch</button>
</form>
</div>
</div>
<%@ include file="_footer.jspf" %>
