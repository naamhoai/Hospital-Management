package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class Queue {
  private Integer queueId;
  private Integer appointmentId;
  private Integer patientId;
  private Integer doctorId;
  private Integer queueNumber;
  private String roomAssigned;
  private String priorityType;
  private String status;

  public Queue() {}

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

  public Integer getQueueNumber() {
    return queueNumber;
  }

  public void setQueueNumber(Integer value) {
    this.queueNumber = value;
  }

  public String getRoomAssigned() {
    return roomAssigned;
  }

  public void setRoomAssigned(String value) {
    this.roomAssigned = value;
  }

  public String getPriorityType() {
    return priorityType;
  }

  public void setPriorityType(String value) {
    this.priorityType = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
