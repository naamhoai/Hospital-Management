package vn.hospital.service;

import java.util.List;
import java.util.Map;
import vn.hospital.dao.ReceptionDAO;
import vn.hospital.model.AuthUser;

public class ReceptionService {
  public List<Map<String, Object>> findPatients(Object... values) {
    return new ReceptionDAO().findPatients(values);
  }

  public List<Map<String, Object>> findAppointments(Object... values) {
    return new ReceptionDAO().findAppointments(values);
  }

  /**
   * SQ14 - Tiếp nhận lịch đã đặt. Đầu vào: nhân viên từ phiên, appointmentId và mức ưu tiên. Chuyển
   * sang QueueService để kiểm tra lịch hôm nay và cấp số chờ. Đầu ra: queueId; Controller dùng mã
   * này hiển thị kết quả tiếp nhận.
   */
  public long checkIn(AuthUser actor, long appointment, String priority) {
    return new QueueService().checkIn(actor, appointment, priority);
  }

  /**
   * SQ14 - Đăng ký khách đến trực tiếp đã có hồ sơ bệnh nhân. Đầu vào: patientId, scheduleId, triệu
   * chứng và ưu tiên do lễ tân chọn. Tạo lịch và phiếu chờ trong một transaction, trả queueId.
   * Không tạo bệnh án tại quầy; bác sĩ gọi bệnh nhân thì QueueService mới mở bệnh án nháp.
   */
  public long registerWalkIn(
      AuthUser actor, long patient, long schedule, String symptoms, String priority) {
    return new QueueService().registerWalkIn(actor, patient, schedule, symptoms, priority);
  }
}
