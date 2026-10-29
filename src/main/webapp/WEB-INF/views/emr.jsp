<%@ include file="_header.jspf" %>
<div class="page-heading">
<div>
<span class="eyebrow">PHÒNG KHÁM</span>
<h1>Bệnh án điện tử</h1>
<p class="muted">Chẩn đoán, chỉ định và kê đơn theo từng lượt khám.</p>
</div>
<a class="button secondary" href="${ctx}/queue">← Hàng đợi</a>
</div>
<div class="card">
<div class="section-heading">
<h2>Bệnh nhân được phân công</h2>
<form method="get" class="search-form">
<input aria-label="Tìm bệnh nhân" name="q" value="<c:out value='${param.q}'/>" placeholder="Tên hoặc số điện thoại">
<button class="secondary">Tìm</button>
</form>
</div>
<div class="table-wrap">
<table>
<thead>
<tr>
<th>Bệnh án / Số chờ</th>
<th>Bệnh nhân</th>
<th>Chẩn đoán</th>
<th>Trạng thái</th>
<th>
</th>
</tr>
</thead>
<tbody>
<c:if test="${empty records}">
<tr>
<td colspan="5" class="empty-state">Chưa có bệnh án. Mở hàng đợi và gọi bệnh nhân vào khám.</td>
</tr>
</c:if>
<c:forEach items="${records}" var="m">
<tr>
<td>#${m.record_id} / ${m.queue_number}</td>
<td>
<c:out value="${m.patient_name}"/>
</td>
<td>
<c:out value="${m.diagnosis}"/>
</td>
<td>
<ui:status value="${m.status}"/>
</td>
<td>
<a href="${ctx}/emr?queueId=${m.queue_id}">Mở bệnh án</a>
</td>
</tr>
</c:forEach>
</tbody>
</table>
</div>
</div>
<c:if test="${not empty record}">
<section class="card patient-banner">
<div class="patient-avatar">BN</div>
<div>
<span class="eyebrow">BỆNH NHÂN ĐANG CHỌN</span>
<h2>
<c:out value="${patient.full_name}"/>
</h2>
<p class="muted">Ngày sinh: <c:out value="${patient.date_of_birth}"/> · Điện thoại: <c:out value="${patient.phone_number}"/> · Phòng <c:out value="${queueContext.roomAssigned}"/>
</p>
</div>
<div><ui:status value="${record.status}"/><p class="muted">Tiến trình: <strong><c:out value="${clinicalProgress.label}"/></strong></p></div>
</section>
<div class="workspace-tabs no-print">
<a href="#diagnosis">Chẩn đoán</a>
<a href="#clinical">Chỉ định & kết quả</a>
<a href="#medication">Đơn thuốc</a>
<a href="#history">Lịch sử khám</a>
</div>
<div class="card" id="diagnosis">
<h2>Bệnh án #${record.recordId} · lượt khám ${queueContext.queueId}</h2>
<c:choose>
<c:when test="${record.status=='OPEN'}">
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="diagnosis">
<input type="hidden" name="queueId" value="${queueContext.queueId}">
<label for="emr-1-symptoms">Triệu chứng *</label>
<textarea id="emr-1-symptoms" name="symptoms" required maxlength="5000">
<c:out value="${submitted ? param.symptoms : record.symptoms}"/>
</textarea>
<label for="emr-2-icdCode">Mã bệnh ICD *</label>
<select id="emr-2-icdCode" name="icdCode" required>
<option value="">Chọn mã bệnh</option>
<c:forEach items="${icds}" var="icd">
<option value="${icd.code}" ${(submitted ? param.icdCode : record.icdCode)==icd.code?'selected':''}>
<c:out value="${icd.code}"/> · <c:out value="${icd.description}"/>
</option>
</c:forEach>
</select>
<label for="emr-3-diagnosis">Chẩn đoán *</label>
<input id="emr-3-diagnosis" name="diagnosis" required maxlength="255" value="<c:out value='${submitted ? param.diagnosis : record.diagnosis}'/>">
<label for="emr-4-notes">Ghi chú</label>
<textarea id="emr-4-notes" name="notes" maxlength="5000">
<c:out value="${submitted ? param.notes : record.notes}"/>
</textarea>
<p>
</p>
<button>Lưu chẩn đoán</button>
</form>
</c:when>
<c:otherwise>
<p>Bệnh án đã kết thúc: <c:out value="${submitted ? param.diagnosis : record.diagnosis}"/> · <c:out value="${record.icdCode}"/>
</p>
</c:otherwise>
</c:choose>
</div>
<div class="card" id="history">
<h2>Lịch sử khám đã hoàn tất của bệnh nhân</h2>
<ui:dataTable rows="${history}"/>
</div>
<c:if test="${record.status=='OPEN'}">
<div class="card" id="clinical">
<h2>Chỉ định xét nghiệm / dịch vụ</h2>
<form method="post" action="${ctx}/clinical-orders">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="recordId" value="${record.recordId}">
<input type="hidden" name="queueId" value="${queueContext.queueId}">
<c:forEach items="${services}" var="service">
<label>
<input type="checkbox" name="serviceId" value="${service.serviceId}"> <c:out value="${service.name}"/> · <fmt:formatNumber value="${service.unitPrice}"/> VND</label>
</c:forEach>
<label for="emr-5-instructions">Chỉ dẫn</label>
<input id="emr-5-instructions" name="instructions" maxlength="255">
<p>
</p>
<button>Tạo chỉ định</button>
</form>
</div>
<div class="card" id="medication">
<h2>Kê đơn thuốc điện tử</h2>
<p class="muted">Sử dụng thuốc demo để kiểm tra phần mềm. Bác sĩ nhập liều, số lần trong ngày và thời gian dùng vào ô liều dùng.</p>
<c:if test="${!pharmacyConfigured}">
<p class="notice warning">Chưa kết nối hệ thống dược để kiểm tra tương tác tự động. Bác sĩ cần kiểm tra thuốc trước khi phát hành.</p>
</c:if>
<form method="get" action="${ctx}/emr#medication" class="search-form"><input type="hidden" name="queueId" value="${queueContext.queueId}"><input aria-label="Tìm thuốc theo tên hoặc hoạt chất" name="medicationKeyword" value="<c:out value='${param.medicationKeyword}'/>" placeholder="Tên thuốc hoặc hoạt chất"><button class="secondary">Tìm thuốc</button></form>
<c:if test="${hasPendingTests}"><div class="notice warning">Đang chờ kết quả cận lâm sàng. Chỉ kê đơn khi có đủ kết quả hoặc đã hủy chỉ định không thực hiện.</div></c:if>
<form method="post" action="${ctx}/prescriptions">
<fieldset ${hasPendingTests?'disabled':''}>
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="recordId" value="${record.recordId}">
<div id="med-items">
<div class="med-row">
<select aria-label="Tên thuốc" name="medicationId" required>
<option value="">Chọn thuốc</option>
<c:forEach items="${medications}" var="med">
<option value="${med.medicationId}">
<c:out value="${med.name}"/> · tồn ${med.stockQuantity}</option>
</c:forEach>
</select>
<input aria-label="Số lượng thuốc" type="number" name="quantity" min="1" max="1000" placeholder="Số lượng" required>
<input aria-label="Liều dùng" name="dosage" maxlength="100" placeholder="Liều dùng" required>
<button type="button" class="secondary" data-remove-med>Xóa dòng</button>
</div>
</div>
<button type="button" class="secondary" data-add-med>+ Thêm thuốc</button>
<c:if test="${pharmacyConfigured}">
<label>
<input type="checkbox" name="overrideInteractions" value="true"> Tôi đã xem xét và chịu trách nhiệm về cảnh báo tương tác thuốc.</label>
</c:if>
<label for="emr-6-advice">Lời dặn</label>
<input id="emr-6-advice" name="advice" maxlength="255">
<p>
</p>
<button>Phát hành đơn thuốc</button>
</fieldset>
</form>
<template id="med-template">
<div class="med-row">
<select aria-label="Tên thuốc" name="medicationId" required>
<option value="">Chọn thuốc</option>
<c:forEach items="${medications}" var="med">
<option value="${med.medicationId}">
<c:out value="${med.name}"/> · tồn ${med.stockQuantity}</option>
</c:forEach>
</select>
<input aria-label="Số lượng thuốc" type="number" name="quantity" min="1" max="1000" placeholder="Số lượng" required>
<input aria-label="Liều dùng" name="dosage" maxlength="100" placeholder="Liều dùng" required>
<button type="button" class="secondary" data-remove-med>Xóa dòng</button>
</div>
</template>
</div>
</c:if>
<div class="card">
<h2>Chỉ định và kết quả xét nghiệm</h2>
<c:if test="${empty orders}">
<p class="empty-state">Chưa có chỉ định cho lượt khám này.</p>
</c:if>
<c:forEach items="${orders}" var="o">
<article class="result-item">
<div class="section-heading">
<div>
<strong>
<c:out value="${o.service.name}"/>
</strong>
<p class="muted"><a href="${ctx}/clinical-orders?id=${o.orderId}">Phiếu chỉ định #${o.orderId} ↓</a> · <fmt:formatNumber value="${o.unitPrice}"/> đ</p>
</div>
<div>
<ui:status value="${o.paymentStatus}"/> <ui:status value="${o.status}"/>
</div>
</div>
<c:if test="${o.status=='RESULT_RECALLED'}"><div class="notice warning">Kết quả cũ đã thu hồi theo phê duyệt. Cần đối chiếu lại thông tin khám khi có bản thay thế.</div></c:if>
<c:if test="${not empty o.result}"><details class="recall-form"><summary>Đề nghị thu hồi kết quả</summary><form method="post" action="${ctx}/result-recalls"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="request"><input type="hidden" name="resultId" value="${o.result.resultId}"><input type="hidden" name="orderId" value="${o.orderId}"><label>Lý do cần thu hồi *</label><textarea name="reason" required maxlength="255" aria-label="Lý do thu hồi kết quả"></textarea><button class="secondary">Gửi đề nghị cho trưởng khoa</button></form></details>
<p>
<strong>Kết quả:</strong> <c:out value="${o.result.resultData}"/>
</p>
<p>
<strong>Kết luận:</strong> <c:out value="${o.result.conclusion}"/>
</p>
<c:if test="${not empty o.result.fileUrl}">
<c:choose>
<c:when test="${o.result.fileUrl.startsWith('attachment:')}">
<a href="${ctx}/attachments?resultId=${o.result.resultId}">Tải báo cáo đính kèm ↓</a>
</c:when>
<c:otherwise>
<a href="<c:out value='${o.result.fileUrl}'/>" target="_blank" rel="noopener noreferrer">Mở báo cáo HTTPS ↗</a>
</c:otherwise>
</c:choose>
</c:if>
</c:if>
<c:if test="${record.status=='OPEN' && o.status=='PENDING' && o.paymentStatus!='PAID'}">
<form method="post" action="${ctx}/clinical-orders" data-confirm="Hủy chỉ định chưa thanh toán này?">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="cancel">
<input type="hidden" name="orderId" value="${o.orderId}">
<input type="hidden" name="queueId" value="${queueContext.queueId}">
<button class="text-button">Hủy chỉ định</button>
</form>
</c:if>
</article>
</c:forEach>
</div>
<div class="card">
<h2>Đơn thuốc</h2>
<c:forEach items="${prescriptions}" var="rx">
<p>
<a href="${ctx}/prescriptions?id=${rx.prescriptionId}">Đơn #${rx.prescriptionId} · ${rx.prescribedDate}</a> · <ui:status value="${rx.paymentStatus}"/>
</p>
</c:forEach>
</div>
<c:if test="${record.status=='OPEN'}">
<div class="card">
<form method="post" data-confirm="Kết thúc bệnh án sẽ khóa chỉnh sửa chẩn đoán và kê đơn. Tiếp tục?">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="close">
<input type="hidden" name="queueId" value="${queueContext.queueId}">
<button>Kết thúc khám</button>
</form>
</div>
</c:if>
</c:if>
<%@ include file="_footer.jspf" %>
