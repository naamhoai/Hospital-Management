package vn.hospital.model;

/** Dữ liệu nghiệp vụ hoặc dữ liệu trao đổi giữa các tầng của ứng dụng. */
public class Room {
  private Integer roomId;
  private Integer departmentId;
  private String roomNumber;
  private String roomName;
  private String roomType;
  private String status;

  public Room() {}

  public Integer getRoomId() {
    return roomId;
  }

  public void setRoomId(Integer value) {
    this.roomId = value;
  }

  public Integer getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Integer value) {
    this.departmentId = value;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public void setRoomNumber(String value) {
    this.roomNumber = value;
  }

  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String value) {
    this.roomName = value;
  }

  public String getRoomType() {
    return roomType;
  }

  public void setRoomType(String value) {
    this.roomType = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    this.status = value;
  }
}
