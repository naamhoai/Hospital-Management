package vn.hospital.model;

/** Phiếu đề nghị và phê duyệt thu hồi kết quả; dữ liệu cũ luôn được giữ lại. */
public class ResultRecallRequest {
  private Integer requestId;
  private Integer resultId;
  private Integer orderId;
  private Integer recordId;
  private Integer doctorId;
  private Integer departmentId;
  private Integer requestedBy;
  private Integer reviewedBy;
  private String reason;
  private String status;
  private String reviewNote;
  private String approvalFileUrl;
  private String patientName;
  private String serviceName;
  private String departmentName;
  private String requesterName;
  private String reviewerName;
  private String resultData;
  private String conclusion;
  private java.time.LocalDateTime requestedAt;
  private java.time.LocalDateTime reviewedAt;

  public Integer getRequestId() {
    return requestId;
  }

  public void setRequestId(Integer value) {
    requestId = value;
  }

  public Integer getResultId() {
    return resultId;
  }

  public void setResultId(Integer value) {
    resultId = value;
  }

  public Integer getOrderId() {
    return orderId;
  }

  public void setOrderId(Integer value) {
    orderId = value;
  }

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer value) {
    recordId = value;
  }

  public Integer getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Integer value) {
    doctorId = value;
  }

  public Integer getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Integer value) {
    departmentId = value;
  }

  public Integer getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(Integer value) {
    requestedBy = value;
  }

  public Integer getReviewedBy() {
    return reviewedBy;
  }

  public void setReviewedBy(Integer value) {
    reviewedBy = value;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String value) {
    reason = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  public String getReviewNote() {
    return reviewNote;
  }

  public void setReviewNote(String value) {
    reviewNote = value;
  }

  public String getApprovalFileUrl() {
    return approvalFileUrl;
  }

  public void setApprovalFileUrl(String value) {
    approvalFileUrl = value;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String value) {
    patientName = value;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String value) {
    serviceName = value;
  }

  public String getDepartmentName() {
    return departmentName;
  }

  public void setDepartmentName(String value) {
    departmentName = value;
  }

  public String getRequesterName() {
    return requesterName;
  }

  public void setRequesterName(String value) {
    requesterName = value;
  }

  public String getReviewerName() {
    return reviewerName;
  }

  public void setReviewerName(String value) {
    reviewerName = value;
  }

  public String getResultData() {
    return resultData;
  }

  public void setResultData(String value) {
    resultData = value;
  }

  public String getConclusion() {
    return conclusion;
  }

  public void setConclusion(String value) {
    conclusion = value;
  }

  public java.time.LocalDateTime getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(java.time.LocalDateTime value) {
    requestedAt = value;
  }

  public java.time.LocalDateTime getReviewedAt() {
    return reviewedAt;
  }

  public void setReviewedAt(java.time.LocalDateTime value) {
    reviewedAt = value;
  }

  private String originalFileUrl;

  public String getOriginalFileUrl() {
    return originalFileUrl;
  }

  public void setOriginalFileUrl(String value) {
    originalFileUrl = value;
  }
}
