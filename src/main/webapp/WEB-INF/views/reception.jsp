<%@ include file="_header.jspf" %>
<h1>Tiếp nhận bệnh nhân</h1>
<div class="card">
<form class="row" method="get">
<div>
<label for="reception-1-keyword">Tìm mã lịch, số điện thoại hoặc tên</label>
<input id="reception-1-keyword" name="keyword" value="<c:out value='${param.keyword}'/>">
</div>
<button>Tìm trong hôm nay</button>
</form>
<p>
</p>
<ui:dataTable rows="${appointments}"/>
</div>
<div class="grid">
<div class="card">
<h2>Check-in lịch đã đặt</h2>
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="checkin">
<label for="reception-2-appointmentId">Lịch khám</label>
<select id="reception-2-appointmentId" name="appointmentId" required>
<c:forEach items="${appointments}" var="a">
<c:if test="${a.status=='BOOKED'}">
<option value="${a.appointment_id}">
<c:out value="${a.appointment_code}"/> · <c:out value="${a.patient_name}"/>
</option>
</c:if>
</c:forEach>
</select>
<label for="reception-3-priority">Ưu tiên</label>
<select id="reception-3-priority" name="priority">
<option value="STANDARD">Thông thường</option>
<option value="ELDERLY">Người cao tuổi</option>
<option value="EMERGENCY">Khẩn cấp</option>
</select>
<p>
</p>
<button>Cấp số chờ</button>
</form>
</div>
<div class="card">
<h2>Đăng ký khám trực tiếp</h2>
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="walkin">
<label for="reception-4-patientId">Bệnh nhân đã có hồ sơ</label>
<select id="reception-4-patientId" name="patientId" required>
<c:forEach items="${patients}" var="p">
<option value="${p.patient_id}">
<c:out value="${p.full_name}"/> · <c:out value="${p.email}"/>
</option>
</c:forEach>
</select>
<label for="reception-5-scheduleId">Ca khám hôm nay còn chỗ</label>
<select id="reception-5-scheduleId" name="scheduleId" required>
<c:forEach items="${schedules}" var="w">
<option value="${w.schedule_id}">#${w.schedule_id} · <c:out value="${w.doctor_name}"/> · ${w.work_date} ${w.start_time}</option>
</c:forEach>
</select>
<label for="reception-6-symptoms">Triệu chứng *</label>
<input id="reception-6-symptoms" name="symptoms" maxlength="255" required>
<label for="reception-7-priority">Ưu tiên</label>
<select id="reception-7-priority" name="priority">
<option value="STANDARD">Thông thường</option>
<option value="ELDERLY">Người cao tuổi</option>
<option value="EMERGENCY">Khẩn cấp</option>
</select>
<p>
</p>
<button>Đăng ký & cấp số</button>
</form>
</div>
</div>
<%@ include file="_footer.jspf" %>
