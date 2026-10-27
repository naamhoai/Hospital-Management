<%@ include file="_header.jspf" %>
<div class="page-heading">
<div>
<span class="eyebrow">SỨC KHỎE CỦA TÔI</span>
<h1>Hồ sơ & hóa đơn</h1>
<p class="muted">Thông tin khám bệnh, đơn thuốc và các khoản thanh toán của bạn.</p>
</div>
</div>
<section class="card">
<h2>Lịch sử khám bệnh</h2>
<c:if test="${empty records}">
<p class="empty-state">Bạn chưa có bệnh án trên hệ thống.</p>
</c:if>
<c:forEach items="${records}" var="m">
<a class="worklist-item" href="${ctx}/my-records?id=${m.record_id}">
<div class="section-heading">
<strong>Bệnh án #${m.record_id} · <c:out value="${m.doctor_name}"/>
</strong>
<ui:status value="${m.status}"/>
</div>
<p>
<c:out value="${m.diagnosis}" default="Chưa có chẩn đoán"/>
</p>
<small>${m.created_at}</small>
</a>
</c:forEach>
</section>
<c:if test="${not empty record}">
<section class="card">
<h2>Chi tiết bệnh án #${record.recordId}</h2>
<p>Bác sĩ: <c:out value="${doctorName}"/>
</p>
<p>
<strong>Triệu chứng:</strong> <c:out value="${record.symptoms}"/>
</p>
<p>
<strong>Chẩn đoán:</strong> <c:out value="${record.diagnosis}"/> · ICD <c:out value="${record.icdCode}"/>
</p>
<h3>Đơn thuốc</h3>
<c:forEach items="${prescriptions}" var="rx">
<p>
<a href="${ctx}/prescriptions?id=${rx.prescriptionId}">Xem & in đơn #${rx.prescriptionId} →</a>
</p>
</c:forEach>
<h3>Chỉ định & kết quả</h3>
<c:forEach items="${orders}" var="o">
<article class="result-item">
<strong>
<c:out value="${o.service.name}"/>
</strong> <ui:status value="${o.status}"/>
<c:if test="${not empty o.result}">
<p class="pre-line">
<c:out value="${o.result.resultData}"/>
</p>
<p>
<strong>Kết luận:</strong> <c:out value="${o.result.conclusion}"/>
</p>
<c:if test="${not empty o.result.fileUrl}">
<c:choose>
<c:when test="${o.result.fileUrl.startsWith('attachment:')}">
<a href="${ctx}/attachments?resultId=${o.result.resultId}">Tải tệp kết quả ↓</a>
</c:when>
<c:otherwise>
<a href="<c:out value='${o.result.fileUrl}'/>" rel="noopener noreferrer" target="_blank">Mở báo cáo HTTPS ↗</a>
</c:otherwise>
</c:choose>
</c:if>
</c:if>
</article>
</c:forEach>
</section>
</c:if>
<section class="card">
<h2>Hóa đơn của tôi</h2>
<c:if test="${empty invoices}">
<p class="empty-state">Chưa có hóa đơn.</p>
</c:if>
<c:forEach items="${invoices}" var="invoice">
<div class="result-item section-heading">
<div>
<strong>
<c:out value="${invoice.invoice_code}"/>
</strong>
<p>Bệnh án #${invoice.record_id} · <fmt:formatNumber value="${invoice.total_amount}"/> đ</p>
<ui:status value="${invoice.status}"/>
</div>
<c:if test="${invoice.status=='UNPAID'}">
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="invoiceId" value="${invoice.invoice_id}">
<button>Thanh toán VNPay</button>
</form>
</c:if>
</div>
</c:forEach>
</section>
<%@ include file="_footer.jspf" %>