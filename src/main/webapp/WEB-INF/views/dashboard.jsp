<%@ include file="_header.jspf" %>
<div class="page-heading">
<div>
<span class="eyebrow">KHÔNG GIAN LÀM VIỆC</span>
<h1>Xin chào, <c:out value="${sessionScope.user.fullName}"/>
</h1>
<p class="muted">Mọi công việc trong ngày, bắt đầu từ đây.</p>
</div>
<span class="badge">${applicationScope.labels[role]}</span>
</div>
<div class="grid action-grid">
<c:if test="${role=='DEPARTMENT_HEAD'}"><a class="card action-card" href="${ctx}/result-recalls"><span class="action-icon">01</span><h2>Duyệt thu hồi kết quả</h2><p>Xem đề nghị, kiểm tra lý do và lưu biên bản phê duyệt.</p><span class="link-text">Mở danh sách đề nghị →</span></a></c:if>
<c:if test="${role=='DOCTOR'}">
<a class="card action-card" href="${ctx}/queue">
<span class="action-icon">01</span>
<h2>Gọi bệnh nhân</h2>
<p>Xem hàng đợi, gọi người tiếp theo vào khám.</p>
<span class="link-text">Mở hàng đợi →</span>
</a>
<a class="card action-card" href="${ctx}/emr">
<span class="action-icon">02</span>
<h2>Bệnh án điện tử</h2>
<p>Lưu chẩn đoán, chỉ định xét nghiệm và kê đơn.</p>
<span class="link-text">Vào phòng khám →</span>
</a>
<a class="card action-card" href="${ctx}/schedules">
<span class="action-icon">03</span>
<h2>Ca làm việc</h2>
<p>Theo dõi và quản lý lịch khám được mở.</p>
<span class="link-text">Xem lịch làm việc →</span>
</a>
</c:if>
<c:if test="${role=='LAB_TECHNICIAN'}">
<a class="card action-card" href="${ctx}/lab">
<span class="action-icon">01</span>
<h2>Xét nghiệm & kết quả</h2>
<p>Chọn chỉ định đã thanh toán, nhập và duyệt kết quả.</p>
<span class="link-text">Mở danh sách chỉ định →</span>
</a>
</c:if>
<c:if test="${role=='MANAGER' || role=='ADMIN'}">
<a class="card action-card" href="${ctx}/reports">
<span class="action-icon">01</span>
<h2>Báo cáo & phân tích</h2>
<p>Theo dõi lượt khám và doanh thu theo thời gian, chuyên khoa và dịch vụ.</p>
<span class="link-text">Xem báo cáo →</span>
</a>
<a class="card action-card" href="${ctx}/services">
<span class="action-icon">02</span>
<h2>Danh mục dịch vụ</h2>
<p>Quản lý dịch vụ và đơn giá áp dụng.</p>
<span class="link-text">Quản lý danh mục →</span>
</a>
</c:if>
<c:if test="${role=='RECEPTIONIST' || role=='ADMIN'}">
<a class="card action-card" href="${ctx}/reception">
<span class="action-icon">03</span>
<h2>Tiếp nhận bệnh nhân</h2>
<p>Tìm lịch hẹn, tiếp nhận hoặc đăng ký khám trực tiếp.</p>
<span class="link-text">Đến quầy tiếp nhận →</span>
</a>
</c:if>
<c:if test="${role=='CASHIER' || role=='ADMIN'}">
<a class="card action-card" href="${ctx}/billing">
<span class="action-icon">04</span>
<h2>Thu ngân</h2>
<p>Lập hóa đơn và xác nhận các khoản thanh toán.</p>
<span class="link-text">Mở quầy thu ngân →</span>
</a>
</c:if>
<c:if test="${role=='PATIENT'}">
<a class="card action-card" href="${ctx}/search">
<span class="action-icon">01</span>
<h2>Tìm bác sĩ & dịch vụ</h2>
<p>Tra cứu thông tin và chọn ca khám còn chỗ.</p>
<span class="link-text">Tìm lịch khám →</span>
</a>
<a class="card action-card" href="${ctx}/appointments">
<span class="action-icon">02</span>
<h2>Lịch khám của tôi</h2>
<p>Đặt lịch, thay đổi hoặc theo dõi lịch đã hẹn.</p>
<span class="link-text">Xem lịch hẹn →</span>
</a>
<a class="card action-card" href="${ctx}/feedback">
<span class="action-icon">03</span>
<h2>Đánh giá sau khám</h2>
<p>Chia sẻ trải nghiệm sau khi hoàn tất khám bệnh.</p>
<span class="link-text">Gửi đánh giá →</span>
</a>
</c:if>
</div>
<section class="card subtle">
<h2>Quy trình khám bệnh</h2>
<div class="journey">
<span>Đặt lịch</span>
<b>→</b>
<span>Tiếp nhận</span>
<b>→</b>
<span>Khám & chỉ định</span>
<b>→</b>
<span>Thanh toán & xét nghiệm</span>
<b>→</b>
<span>Hoàn tất</span>
</div>
</section>
<%@ include file="_footer.jspf" %>