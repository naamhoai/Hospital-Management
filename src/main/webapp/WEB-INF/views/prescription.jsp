<%@ include file="_header.jspf" %>
<div class="page-heading no-print">
<div>
<span class="eyebrow">HỒ SƠ ĐIỀU TRỊ</span>
<h1>Đơn thuốc #${prescription.prescriptionId}</h1>
</div>
<div>
<a class="button secondary" href="${ctx}/prescriptions?id=${prescription.prescriptionId}&amp;format=pdf">Tải PDF ↓</a> <button type="button" data-print>In đơn</button>
</div>
</div>
<article class="card prescription-paper">
<div class="section-heading">
<h2>ĐƠN THUỐC</h2>
<ui:status value="${prescription.paymentStatus}"/>
</div>
<p>Bác sĩ: <strong>
<c:out value="${doctorName}"/>
</strong>
</p>
<p>Bệnh nhân: <strong>
<c:out value="${patient.full_name}"/>
</strong> · Ngày sinh: <c:out value="${patient.date_of_birth}"/>
</p>
<p>Chẩn đoán: <c:out value="${record.diagnosis}"/> · ICD <c:out value="${record.icdCode}"/>
</p>
<p class="muted">Ngày kê: ${prescription.prescribedDate}</p>
<div class="table-wrap">
<table>
<thead>
<tr>
<th>Thuốc</th>
<th>Số lượng</th>
<th>Đơn vị</th>
<th>Liều dùng</th>
<th>Đơn giá (đ)</th>
</tr>
</thead>
<tbody>
<c:forEach items="${prescription.items}" var="i">
<tr>
<td>
<strong>
<c:out value="${i.medication.name}"/>
</strong>
</td>
<td>${i.quantity}</td>
<td>
<c:out value="${i.medication.unit}"/>
</td>
<td>
<c:out value="${i.dosage}"/>
</td>
<td>
<fmt:formatNumber value="${i.unitPrice}"/>
</td>
</tr>
</c:forEach>
</tbody>
</table>
</div>
<div class="subtle advice">
<strong>Lời dặn của bác sĩ</strong>
<p>
<c:out value="${prescription.advice}" default="Không có lời dặn bổ sung."/>
</p>
</div>
<p class="no-print">
<a href="${ctx}/${role=='PATIENT'?'my-records':'emr'}">← Trở về bệnh án</a>
</p>
</article>
<%@ include file="_footer.jspf" %>