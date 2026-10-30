<%@ include file="_header.jspf" %>
<h1>Hóa đơn & thanh toán</h1>
<div class="card no-print">
<h2>Lập hóa đơn cho chi phí chưa được tính</h2>
<form class="row" method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="create">
<div>
<label for="billing-1-recordId">Bệnh án</label>
<select id="billing-1-recordId" name="recordId" required>
<c:forEach items="${records}" var="m">
<option value="${m.record_id}">#${m.record_id} · <c:out value="${m.patient_name}"/> · <c:out value="${m.diagnosis}"/>
</option>
</c:forEach>
</select>
</div>
<button>Lập / mở hóa đơn chưa trả</button>
</form>
</div>
<c:if test="${not empty invoice}">
<div class="card">
<h2>Hóa đơn <c:out value="${invoice.invoice_code}"/>
</h2>
<p>Bệnh nhân: <c:out value="${invoice.patient_name}"/> · Trạng thái: <strong>
<c:out value="${invoice.status}"/>
</strong>
</p>
<ui:dataTable rows="${lines}"/>
<h3>Tổng cộng: <fmt:formatNumber value="${invoice.total_amount}"/> VND</h3>
<c:if test="${invoice.status=='UNPAID'}">
<div class="row no-print">
<form method="post" data-confirm="Xác nhận đã nhận đủ tiền mặt?">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="cash">
<input type="hidden" name="invoiceId" value="${invoice.invoice_id}">
<button>Đã nhận tiền mặt</button>
</form>
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<input type="hidden" name="action" value="vnpay">
<input type="hidden" name="invoiceId" value="${invoice.invoice_id}">
<button>Thanh toán VNPay sandbox</button>
</form>
</div>
</c:if>
<p class="no-print">
<button class="secondary" data-print>In hóa đơn</button>
</p>
</div>
</c:if>
<div class="card no-print">
<h2>Danh sách hóa đơn</h2>
<div class="table-wrap">
<table>
<thead>
<tr>
<th>Mã hóa đơn</th>
<th>Bệnh nhân</th>
<th>Tổng tiền</th>
<th>Trạng thái</th>
<th>
</th>
</tr>
</thead>
<tbody>
<c:forEach items="${invoices}" var="i">
<tr>
<td>
<c:out value="${i.invoice_code}"/>
</td>
<td>
<c:out value="${i.patient_name}"/>
</td>
<td>
<fmt:formatNumber value="${i.total_amount}"/>
</td>
<td>
<c:out value="${i.status}"/>
</td>
<td>
<a href="${ctx}/billing?id=${i.invoice_id}">Chi tiết</a>
</td>
</tr>
</c:forEach>
</tbody>
</table>
</div>
</div>
<%@ include file="_footer.jspf" %>
