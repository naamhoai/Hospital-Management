<%@ include file="_header.jspf" %>
<h1>Tìm bác sĩ & dịch vụ</h1>
<div class="card">
<form class="row" method="get">
<div>
<label for="search-1-keyword">Tên bác sĩ, chuyên khoa hoặc dịch vụ</label>
<input id="search-1-keyword" name="keyword" maxlength="100" value="<c:out value='${param.keyword}'/>">
</div>
<div>
<label for="search-2-date">Ngày khám</label>
<input id="search-2-date" type="date" name="date" value="<c:out value='${param.date}'/>">
</div>
<button>Tìm kiếm</button>
</form>
</div>
<div class="card">
<h2>Bác sĩ và ca khám còn chỗ</h2>
<ui:dataTable rows="${doctors}"/>
<c:if test="${role=='PATIENT'}">
<p>
</p>
<a class="button" href="${ctx}/appointments">Đặt lịch khám</a>
</c:if>
</div>
<div class="card">
<h2>Dịch vụ đang hoạt động</h2>
<ui:dataTable rows="${services}"/>
</div>
<%@ include file="_footer.jspf" %>
