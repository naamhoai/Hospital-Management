<%@ include file="_header.jspf" %>
<div class="page-heading">
<div>
<span class="eyebrow">CẬN LÂM SÀNG</span>
<h1>Xét nghiệm & kết quả</h1>
<p class="muted">Ưu tiên chỉ định đã thanh toán, đang chờ thực hiện.</p>
</div>
<span class="badge">${orders.size()} chỉ định hiển thị</span>
</div>
<section class="card">
<form method="get" class="row">
<div>
<label for="lab-q">Tìm bệnh nhân / mã chỉ định</label>
<input id="lab-q" name="q" value="<c:out value='${param.q}'/>" placeholder="Nhập tên hoặc mã">
</div>
<div>
<label for="lab-status">Trạng thái thực hiện</label>
<select id="lab-status" name="status">
<option value="">Tất cả</option>
<option value="RESULT_RECALLED" ${param.status=='RESULT_RECALLED'?'selected':''}>Cần cập nhật sau thu hồi</option>
<option value="PENDING" ${param.status=='PENDING'?'selected':''}>Đang chờ</option>
<option value="COMPLETED" ${param.status=='COMPLETED'?'selected':''}>Hoàn tất</option>
<option value="CANCELLED" ${param.status=='CANCELLED'?'selected':''}>Đã hủy</option>
</select>
</div>
<button class="secondary">Lọc danh sách</button>
</form>
</section>
<div class="split-layout">
<section class="card">
<h2>Danh sách chỉ định</h2>
<c:if test="${empty orders}">
<p class="empty-state">Không tìm thấy chỉ định phù hợp.</p>
</c:if>
<c:forEach items="${orders}" var="o">
<a class="worklist-item ${param.orderId==o.order_id?'selected':''}" href="${ctx}/lab?orderId=${o.order_id}">
<div class="section-heading">
<strong>
<c:out value="${o.patient_name}"/>
</strong>
<span>#${o.order_id}</span>
</div>
<p>
<c:out value="${o.service_name}"/>
</p>
<ui:status value="${o.payment_status}"/> <ui:status value="${o.status}"/>
</a>
</c:forEach>
</section>
<section class="card">
<c:choose>
<c:when test="${empty selectedOrder}">
<div class="empty-state">
<div class="empty-icon">＋</div>
<h2>Chọn một chỉ định</h2>
<p>Thông tin bệnh nhân và biểu mẫu kết quả sẽ hiển thị ở đây.</p>
</div>
</c:when>
<c:otherwise>
<span class="eyebrow">CHỈ ĐỊNH #${selectedOrder.orderId}</span>
<h2>
<c:out value="${patient.full_name}"/>
</h2>
<p>
<c:out value="${selectedOrder.service.name}"/> · Ngày sinh <c:out value="${patient.date_of_birth}"/>
</p>
<p class="muted">Chỉ dẫn: <c:out value="${selectedOrder.instructions}"/>
</p>
<c:choose>
<c:when test="${not empty selectedOrder.result}">
<div class="notice">Kết quả đã duyệt và khóa chỉnh sửa.</div><details class="recall-form"><summary>Đề nghị thu hồi kết quả</summary><form method="post" action="${ctx}/result-recalls"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="action" value="request"><input type="hidden" name="resultId" value="${selectedOrder.result.resultId}"><input type="hidden" name="orderId" value="${selectedOrder.orderId}"><label>Lý do cần thu hồi *</label><textarea name="reason" required maxlength="255" aria-label="Lý do thu hồi kết quả"></textarea><button class="secondary">Gửi đề nghị cho trưởng khoa</button></form></details>
<h3>Dữ liệu kết quả</h3>
<p class="pre-line">
<c:out value="${selectedOrder.result.resultData}"/>
</p>
<h3>Kết luận</h3>
<p>
<c:out value="${selectedOrder.result.conclusion}"/>
</p>
<c:if test="${not empty selectedOrder.result.fileUrl}">
<c:choose>
<c:when test="${selectedOrder.result.fileUrl.startsWith('attachment:')}">
<a class="button secondary" href="${ctx}/attachments?resultId=${selectedOrder.result.resultId}">Tải tệp kết quả ↓</a>
</c:when>
<c:otherwise>
<a href="<c:out value='${selectedOrder.result.fileUrl}'/>" rel="noopener noreferrer" target="_blank">Mở tệp kết quả HTTPS ↗</a>
</c:otherwise>
</c:choose>
</c:if>
</c:when>
<c:when test="${selectedOrder.paymentStatus=='PAID' && (selectedOrder.status=='PENDING' || selectedOrder.status=='RESULT_RECALLED')}">
<c:if test="${selectedOrder.status=='RESULT_RECALLED'}"><div class="notice warning">Kết quả cũ đã được thu hồi. Nhập bản thay thế sau khi kiểm tra lại; lịch sử cũ vẫn được lưu.</div></c:if>
<form method="post" enctype="multipart/form-data" data-confirm="Duyệt kết quả này? Sau khi duyệt sẽ không thể chỉnh sửa.">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="orderId" value="${selectedOrder.orderId}">
<label for="result-data">Dữ liệu kết quả *</label>
<textarea id="result-data" name="resultData" required maxlength="10000" placeholder="Nhập số liệu và mô tả kết quả">
</textarea>
<label for="result-conclusion">Kết luận</label>
<textarea id="result-conclusion" name="conclusion" maxlength="255">
</textarea>
<label for="result-file">Tệp kết quả (PDF, PNG, JPEG · tối đa 5 MB)</label>
<input id="result-file" type="file" name="attachment" accept=".pdf,.png,.jpg,.jpeg">
<details>
<summary>Hoặc sử dụng liên kết HTTPS</summary>
<label for="result-url">URL tệp kết quả</label>
<input id="result-url" type="url" name="fileUrl" maxlength="255" placeholder="https://...">
</details>
<p class="muted">Kiểm tra đúng bệnh nhân và nội dung trước khi duyệt.</p>
<button>Lưu & duyệt kết quả</button>
</form>
</c:when>
<c:otherwise>
<div class="notice warning">${selectedOrder.status=='CANCELLED'?'Chỉ định đã hủy.':'Chỉ định chưa thanh toán. Vui lòng chờ xác nhận từ thu ngân.'}</div>
</c:otherwise>
</c:choose>
<c:if test="${not empty recallHistory}"><details><summary>Lịch sử đề nghị thu hồi (${recallHistory.size()})</summary><c:forEach items="${recallHistory}" var="recall"><article class="result-item"><strong>Phiếu #${recall.requestId}</strong> <ui:status value="${recall.status}"/><p><c:out value="${recall.reason}"/></p><p>Ý kiến: <c:out value="${recall.reviewNote}"/></p><p>Người xử lý: <c:out value="${recall.reviewerName}"/></p><details><summary>Bản kết quả gốc</summary><p class="pre-line"><c:out value="${recall.resultData}"/></p><p><c:out value="${recall.conclusion}"/></p></details></article></c:forEach></details></c:if>
</c:otherwise>
</c:choose>
</section>
</div>
<%@ include file="_footer.jspf" %>