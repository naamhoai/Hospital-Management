package vn.hospital.model;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
public class QueueContext {
  private Integer queueId;
  private Integer appointmentId;
  private Integer patientId;
  private Integer doctorId;
  private String status;
  private String roomAssigned;

  public QueueContext() {}

  public Integer getQueueId() {
    return queueId;
  }

  public void setQueueId(Integer value) {
    this.queueId = value;
  }

  public Integer getAppointmentId() {
    return appointmentId;
  }

  public void setAppointmentId(Integer value) {
    this.appointmentId = value;
  }

  public Integer getPatientId() {
    return patientId;
  }

  public void setPatientId(Integer value) {
    this.patientId = value;
  }

  public Integer getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(Integer value) {
    this.doctorId = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }

  public String getRoomAssigned() {
    return roomAssigned;
  }

  public void setRoomAssigned(String value) {
    this.roomAssigned = value;
  }
}
