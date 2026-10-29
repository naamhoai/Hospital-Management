<%@ include file="_header.jspf" %>
<div class="page-heading">
<div>
<span class="eyebrow">TRƯỞNG KHOA</span>
<h1>Duyệt thu hồi kết quả</h1>
<p class="muted">Chỉ hiển thị kết quả thuộc chuyên khoa phụ trách. Bản kết quả cũ luôn được giữ lại.</p>
</div>
</div>
<c:if test="${empty requests}">
<section class="card empty-state">Chưa có đề nghị thu hồi.</section>
</c:if>
<c:forEach items="${requests}" var="item">
<section class="card">
<div class="section-heading">
<h2>Đề nghị #${item.requestId} · <c:out value="${item.patientName}"/>
</h2>
<ui:status value="${item.status}"/>
</div>
<p>
<strong>Dịch vụ:</strong> <c:out value="${item.serviceName}"/> · <c:out value="${item.departmentName}"/>
</p>
<p>
<strong>Người đề nghị:</strong> <c:out value="${item.requesterName}"/> · ${item.requestedAt}</p>
<p>
<strong>Lý do:</strong> <c:out value="${item.reason}"/>
</p>
<details>
<summary>Xem nội dung kết quả gốc #${item.resultId}</summary>
<p class="pre-line">
<c:out value="${item.resultData}"/>
</p>
<p>
<c:out value="${item.conclusion}"/>
</p>
<c:if test="${not empty item.originalFileUrl}">
<c:choose>
<c:when test="${item.originalFileUrl.startsWith('attachment:')}">
<a href="${ctx}/result-recalls?originalRequestId=${item.requestId}">Tải tệp kết quả gốc ↓</a>
</c:when>
<c:otherwise>
<a href="<c:out value='${item.originalFileUrl}'/>" target="_blank" rel="noopener noreferrer">Mở tệp gốc HTTPS ↗</a>
</c:otherwise>
</c:choose>
</c:if>
</details>
<c:choose>
<c:when test="${item.status=='PENDING'}">
<form method="post" enctype="multipart/form-data" action="${ctx}/result-recalls">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="requestId" value="${item.requestId}">
<label for="review-note-${item.requestId}">Ý kiến phê duyệt / lý do từ chối *</label>
<textarea id="review-note-${item.requestId}" name="reviewNote" required maxlength="1000">
</textarea>
<label for="signed-report-${item.requestId}">Biên bản có chữ ký (PDF, tối đa 5 MB; bắt buộc khi phê duyệt)</label>
<input id="signed-report-${item.requestId}" type="file" name="signedReport" accept=".pdf">
<div class="form-actions">
<button name="action" value="approve">Phê duyệt thu hồi</button>
<button class="secondary" name="action" value="reject">Từ chối đề nghị</button>
</div>
</form>
</c:when>
<c:otherwise>
<p>
<strong>Người xử lý:</strong> <c:out value="${item.reviewerName}"/> · ${item.reviewedAt}</p>
<p>
<strong>Ý kiến:</strong> <c:out value="${item.reviewNote}"/>
</p>
<c:if test="${not empty item.approvalFileUrl}">
<a class="button secondary" href="${ctx}/result-recalls?fileRequestId=${item.requestId}">Tải biên bản PDF ↓</a>
</c:if>
</c:otherwise>
</c:choose>
</section>
</c:forEach>
<%@ include file="_footer.jspf" %>
