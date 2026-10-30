<%@ include file="_header.jspf" %>
<h1>Đánh giá sau khám</h1>
<p>Đánh giá một lần trong vòng 7 ngày sau khi kết thúc bệnh án.</p>
<div class="card">
<ui:dataTable rows="${records}"/>
</div>
<div class="card">
<form method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<label for="feedback-1-recordId">Bệnh án đã hoàn tất</label>
<select id="feedback-1-recordId" name="recordId" required>
<c:forEach items="${records}" var="m">
<c:if test="${empty m.rating}">
<option value="${m.record_id}">#${m.record_id} · <c:out value="${m.doctor_name}"/> · <c:out value="${m.diagnosis}"/>
</option>
</c:if>
</c:forEach>
</select>
<label for="feedback-2-rating">Số sao *</label>
<select id="feedback-2-rating" name="rating">
<option value="5">5 · Rất hài lòng</option>
<option value="4">4 · Hài lòng</option>
<option value="3">3 · Bình thường</option>
<option value="2">2 · Chưa hài lòng</option>
<option value="1">1 · Không hài lòng</option>
</select>
<label for="feedback-3-comment">Nhận xét</label>
<textarea id="feedback-3-comment" name="comment" maxlength="2000">
</textarea>
<p>
</p>
<button>Gửi đánh giá</button>
</form>
</div>
<%@ include file="_footer.jspf" %>
