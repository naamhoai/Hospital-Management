package vn.hospital.listener;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import java.util.*;
@WebListener
public class ViewLabels implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {
        Map<String,String> labels=new HashMap<>();
        String[][] pairs={{"appointment_id","Mã lịch"},{"appointment_code","Mã đặt lịch"},{"patient_id","Mã bệnh nhân"},{"doctor_id","Mã bác sĩ"},{"schedule_id","Mã ca"},{"appointment_date","Ngày khám"},{"appointment_time","Giờ khám"},{"initial_symptoms","Triệu chứng ban đầu"},{"reschedule_count","Số lần đổi"},{"consultation_fee","Phí khám"},{"status","Trạng thái"},{"created_at","Ngày tạo"},{"cancelled_at","Ngày hủy"},{"doctor_name","Bác sĩ"},{"patient_name","Bệnh nhân"},{"phone_number","Điện thoại"},{"queue_id","Mã lượt khám"},{"queue_number","Số chờ"},{"work_date","Ngày"},{"room_assigned","Phòng khám"},{"priority_type","Ưu tiên"},{"full_name","Họ tên"},{"academic_title","Học vị"},{"department","Chuyên khoa"},{"room_number","Phòng"},{"rating_average","Điểm đánh giá"},{"shift","Ca"},{"start_time","Bắt đầu"},{"end_time","Kết thúc"},{"remaining","Chỗ còn lại"},{"max_slots","Sức chứa"},{"booked_count","Đã đặt"},{"service_id","Mã dịch vụ"},{"department_id","Mã khoa"},{"name","Tên"},{"category","Phân loại"},{"unit_price","Đơn giá"},{"record_id","Mã bệnh án"},{"diagnosis","Chẩn đoán"},{"icd_code","Mã ICD"},{"closed_at","Ngày kết thúc"},{"rating","Số sao"},{"order_id","Mã chỉ định"},{"order_date","Ngày chỉ định"},{"instructions","Chỉ dẫn"},{"payment_status","Thanh toán"},{"result_data","Kết quả"},{"conclusion","Kết luận"},{"line_id","Mã dòng"},{"invoice_id","Mã hóa đơn"},{"source_type","Loại chi phí"},{"source_id","Mã phát sinh"},{"description","Nội dung"},{"quantity","Số lượng"},{"amount","Thành tiền"},{"day","Ngày"},{"patient_count","Số lượt khám"},{"completed_count","Hoàn tất"},{"cancelled_count","Đã hủy"},{"no_show_count","Vắng mặt"},{"completion_rate","Tỷ lệ hoàn tất %"},{"cancellation_rate","Tỷ lệ hủy %"},{"no_show_rate","Tỷ lệ vắng %"},{"payment_method","Phương thức"},{"invoice_count","Số hóa đơn"},{"total_revenue","Doanh thu"}};
        for(var p:pairs) labels.put(p[0],p[1]); labels.put("active_slot","Giờ đã giữ"); event.getServletContext().setAttribute("labels",labels);
    }
}
