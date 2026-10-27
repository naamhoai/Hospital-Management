<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="rows" required="true" type="java.util.List" %>
<c:choose><c:when test="${empty rows}"><p class="muted">Không có dữ liệu.</p></c:when><c:otherwise><div class="table-wrap"><table><thead><tr><c:forEach items="${rows[0]}" var="cell"><th><c:out value="${empty applicationScope.labels[cell.key] ? cell.key : applicationScope.labels[cell.key]}"/></th></c:forEach></tr></thead><tbody><c:forEach items="${rows}" var="row"><tr><c:forEach items="${row}" var="cell"><td><c:out value="${cell.value}"/></td></c:forEach></tr></c:forEach></tbody></table></div></c:otherwise></c:choose>
