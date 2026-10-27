package vn.hospital.model;

import java.time.*;
import java.util.*;

/** Java Bean mapped to the SDS domain or request/data model. */
public class Feedback {
  private Integer feedbackId;
  private Integer recordId;
  private Integer patientId;
  private Integer doctorId;
  private Integer rating;
  private String comment;

  public Feedback() {}

  public Integer getFeedbackId() {
    return feedbackId;
  }

  public void setFeedbackId(Integer value) {
    this.feedbackId = value;
  }

  public Integer getRecordId() {
    return recordId;
  }

  public void setRecordId(Integer value) {
    this.recordId = value;
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

  public Integer getRating() {
    return rating;
  }

  public void setRating(Integer value) {
    this.rating = value;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String value) {
    this.comment = value;
  }
}
