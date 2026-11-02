<%@ include file="_header.jspf" %>
<h1>Lịch bác sĩ và duyệt nghỉ</h1>
<c:if test="${role=='DOCTOR' || role=='ADMIN'}">
<section class="card">
<h2>Tạo ca khám</h2>
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="create">
<div class="row">
<div><label>Bác sĩ</label><select name="doctorId" required><c:forEach items="${doctors}" var="doctor"><option value="${doctor.doctor_id}"><c:out value="${doctor.full_name}"/></option></c:forEach></select></div>
<div><label>Ngày bắt đầu</label><input type="date" name="date" required></div>
<div><label>Tên ca</label><input name="shift" maxlength="20" placeholder="MORNING" required></div>
</div>
<div class="row">
<div><label>Bắt đầu</label><input type="time" name="start" required></div>
<div><label>Kết thúc</label><input type="time" name="end" required></div>
<div><label>Số chỗ</label><input type="number" name="capacity" min="1" max="240" value="12" required></div>
</div>
<div class="row">
<div><label>Lặp lịch</label><select name="recurrence"><option value="ONCE">Một ngày</option><option value="WEEKLY">Mỗi tuần</option><option value="MONTHLY">Mỗi tháng</option></select></div>
<div><label>Ngày kết thúc (chọn khi lặp lịch)</label><input type="date" name="until"></div>
<button>Tạo lịch</button>
</div>
</form>
</section>
</c:if>
<section class="card">
<h2>Danh sách ca khám</h2>
<div class="table-wrap"><table>
<thead><tr><th>Mã ca</th><th>Bác sĩ</th><th>Ngày / giờ</th><th>Sức chứa</th><th>Trạng thái</th><th>Yêu cầu nghỉ</th><th>Lý do / kết quả duyệt</th></tr></thead>
<tbody><c:forEach items="${schedules}" var="schedule"><tr>
<td>${schedule.schedule_id}</td><td><c:out value="${schedule.doctor_name}"/></td>
<td>${schedule.work_date}<br>${schedule.start_time} - ${schedule.end_time}<br><c:out value="${schedule.shift}"/></td>
<td>${schedule.booked_count} / ${schedule.max_slots}</td>
<td><c:out value="${applicationScope.labels[schedule.status]}"/></td>
<td><c:choose><c:when test="${schedule.leave_status=='PENDING'}">Chờ quản lý duyệt</c:when><c:otherwise><c:out value="${applicationScope.labels[schedule.leave_status]}"/></c:otherwise></c:choose></td>
<td><c:out value="${schedule.leave_reason}"/><br><c:out value="${schedule.leave_review_note}"/>
<c:if test="${(role=='MANAGER' || role=='ADMIN') && schedule.leave_status=='PENDING'}">
<form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="scheduleId" value="${schedule.schedule_id}">
<input name="reviewNote" maxlength="255" placeholder="Ghi chú duyệt">
<button name="action" value="approveLeave">Duyệt nghỉ</button><button class="secondary" name="action" value="rejectLeave">Từ chối</button></form>
</c:if></td></tr></c:forEach></tbody></table></div>
</section>
<c:if test="${role=='DOCTOR' || role=='ADMIN'}">
<div class="grid">
<section class="card"><h2>Điều chỉnh số chỗ</h2><p>Chỉ đổi ca chưa có người đặt, để giữ nguyên giờ hẹn đã phát hành.</p>
<form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="capacity">
<label>Ca khám</label><select name="scheduleId" required><c:forEach items="${schedules}" var="schedule"><c:if test="${schedule.status=='AVAILABLE' && schedule.booked_count==0}"><option value="${schedule.schedule_id}">#${schedule.schedule_id} · ${schedule.work_date} · <c:out value="${schedule.shift}"/></option></c:if></c:forEach></select>
<label>Số chỗ mới</label><input type="number" name="capacity" min="1" max="240" required><p><button>Cập nhật</button></p></form></section>
<section class="card"><h2>Gửi yêu cầu nghỉ</h2><p>Chỉ gửi yêu cầu. Lịch hẹn được giữ cho đến khi quản lý đồng ý.</p>
<form method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="leave">
<label>Ca khám</label><select name="scheduleId" required><c:forEach items="${schedules}" var="schedule"><c:if test="${schedule.status=='AVAILABLE' && schedule.leave_status!='PENDING'}"><option value="${schedule.schedule_id}">#${schedule.schedule_id} · ${schedule.work_date} · <c:out value="${schedule.shift}"/></option></c:if></c:forEach></select>
<label>Lý do nghỉ</label><textarea name="reason" maxlength="255" required></textarea><p><button>Gửi quản lý</button></p></form></section>
</div>
</c:if>
<%@ include file="_footer.jspf" %>
