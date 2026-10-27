<%@ include file="_header.jspf" %>
<div class="page-heading">
<div>
<span class="eyebrow">QUẢN LÝ HOẠT ĐỘNG</span>
<h1>Báo cáo & phân tích</h1>
<p class="muted">Tổng hợp dữ liệu khám bệnh và doanh thu đã thanh toán.</p>
</div>
<span class="badge">Chỉ đọc dữ liệu</span>
</div>
<section class="card">
<form method="get">
<div class="row">
<div>
<label for="report-type">Loại báo cáo</label>
<select id="report-type" name="type">
<option value="flow" ${type=='flow'?'selected':''}>Lưu lượng bệnh nhân</option>
<option value="revenue" ${type=='revenue'?'selected':''}>Doanh thu dịch vụ</option>
</select>
</div>
<div>
<label for="from">Từ ngày</label>
<input id="from" type="date" name="from" value="${filter.fromDate}" required>
</div>
<div>
<label for="to">Đến ngày</label>
<input id="to" type="date" name="to" value="${filter.toDate}" required>
</div>
<div>
<label for="group">Nhóm theo</label>
<select id="group" name="groupBy">
<option value="DAY" ${filter.groupBy=='DAY'?'selected':''}>Ngày</option>
<option value="MONTH" ${filter.groupBy=='MONTH'?'selected':''}>Tháng</option>
<option value="QUARTER" ${filter.groupBy=='QUARTER'?'selected':''}>Quý</option>
</select>
</div>
</div>
<div class="row">
<div>
<label for="department">Chuyên khoa</label>
<select id="department" name="departmentId">
<option value="">Tất cả chuyên khoa</option>
<c:forEach items="${departments}" var="d">
<option value="${d.department_id}" ${filter.departmentId==d.department_id?'selected':''}>
<c:out value="${d.name}"/>
</option>
</c:forEach>
</select>
</div>
<div>
<label for="service">Dịch vụ chỉ định</label>
<select id="service" name="serviceId">
<option value="">Tất cả dịch vụ</option>
<c:forEach items="${services}" var="v">
<option value="${v.service_id}" ${filter.serviceId==v.service_id?'selected':''}>
<c:out value="${v.name}"/>
</option>
</c:forEach>
</select>
</div>
<c:if test="${type=='revenue'}"><div><label for="service-type">Loại chi phí</label><select id="service-type" name="serviceType"><option value="">Tất cả</option><option value="CONSULTATION" ${filter.serviceType=='CONSULTATION'?'selected':''}>Khám bệnh</option><option value="ORDER" ${filter.serviceType=='ORDER'?'selected':''}>Cận lâm sàng</option><option value="MEDICATION" ${filter.serviceType=='MEDICATION'?'selected':''}>Thuốc</option></select></div></c:if>
<div>
<label for="method">Phương thức thanh toán</label>
<select id="method" name="method">
<option value="">Tất cả</option>
<option value="CASH" ${filter.paymentMethod=='CASH'?'selected':''}>Tiền mặt</option>
<option value="VNPAY" ${filter.paymentMethod=='VNPAY'?'selected':''}>VNPay</option>
</select>
</div>
</div>
<div class="form-actions">
<button>Xem báo cáo</button>
<button class="secondary" name="format" value="xlsx">Xuất Excel ↓</button>
<button class="secondary" name="format" value="pdf">Xuất PDF ↓</button>
</div>
</form>
</section>
<c:choose>
<c:when test="${type=='flow'}">
<div class="metric-grid">
<div class="metric">
<span>Tổng lượt hẹn</span>
<strong>${report.patientCount}</strong>
<small>Tính theo ngày khám</small>
</div>
<div class="metric">
<span>Đã hoàn tất</span>
<strong>${report.completedCount}</strong>
<small>${report.completionRate}% tổng lượt</small>
</div>
<div class="metric">
<span>Đã hủy</span>
<strong>${report.cancelledCount}</strong>
<small>${report.cancellationRate}% tổng lượt</small>
</div>
<div class="metric">
<span>Vắng mặt</span>
<strong>${report.noShowCount}</strong>
<small>${report.noShowRate}% tổng lượt</small>
</div>
</div>
</c:when>
<c:otherwise>
<div class="metric-grid">
<div class="metric">
<span>Tổng doanh thu</span>
<strong>
<fmt:formatNumber value="${report.totalRevenue}"/> <small>đ</small>
</strong>
<small>Chỉ tính khoản đã thanh toán</small>
</div>
<div class="metric">
<span>Hóa đơn đã thanh toán</span>
<strong>${report.paidInvoiceCount}</strong>
<small>Đếm hóa đơn riêng biệt trong bộ lọc</small>
</div>
</div>
</c:otherwise>
</c:choose>
<section class="card">
<div class="section-heading">
<h2>${type=='flow'?'Lượt khám':'Doanh thu'} theo thời gian</h2>
<span class="muted">${filter.fromDate} → ${filter.toDate}</span>
</div>
<c:choose>
<c:when test="${empty report.series}">
<div class="empty-state">
<h3>Chưa có dữ liệu</h3>
<p>Thử thay đổi khoảng ngày hoặc bộ lọc.</p>
</div>
</c:when>
<c:otherwise>
<div class="chart">
<c:forEach items="${report.series}" var="point">
<div class="chart-row">
<span>
<c:out value="${point.label}"/>
</span>
<div class="bar-track">
<div class="bar" data-chart-value="${point.value}">
</div>
</div>
<strong>
<fmt:formatNumber value="${point.value}"/>${type=='revenue'?' đ':''}</strong>
</div>
</c:forEach>
</div>
</c:otherwise>
</c:choose>
</section>
<section class="card">
<h2>Chi tiết tổng hợp</h2>
<p class="muted">Doanh thu tính theo ngày thanh toán; bộ lọc dịch vụ áp dụng cho chỉ định cận lâm sàng. Số hóa đơn từng dòng có thể trùng giữa các dịch vụ.</p>
<ui:dataTable rows="${rows}"/>
</section>
<%@ include file="_footer.jspf" %>