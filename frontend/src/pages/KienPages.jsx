import { Col, Row } from "react-bootstrap";
import {
  ActionForm,
  DataTable,
  Field,
  Heading,
  Link,
  Panel,
  QueryForm,
  SaveButton,
  options,
} from "../components";
import { scheduleOptions } from "./TrangPages";

const priorities = [
  { value: "STANDARD", label: "Thông thường" },
  { value: "ELDERLY", label: "Người cao tuổi" },
  { value: "EMERGENCY", label: "Khẩn cấp" },
];

// Kiên: ca làm việc, tiếp nhận và điều phối lượt khám.
export function SchedulesPage({ data }) {
  return (
    <>
      <Heading
        title="Ca làm việc & sức chứa"
        description="Mở ca khám và theo dõi số chỗ đã đặt."
      />
      <Panel title="Tạo ca khám">
        <ActionForm endpoint="/schedules" hidden={{ action: "create" }}>
          <Row>
            <Col md={5}>
              <Field
                label="Bác sĩ"
                name="doctorId"
                required
                options={options(data.doctors, "doctor_id", (d) => d.full_name)}
              />
            </Col>
            <Col md={4}>
              <Field label="Ngày" name="date" type="date" required />
            </Col>
            <Col md={3}>
              <Field
                label="Tên ca"
                name="shift"
                required
                maxLength={20}
                placeholder="MORNING"
              />
            </Col>
          </Row>
          <Row>
            <Col md={4}>
              <Field label="Bắt đầu" name="start" type="time" required />
            </Col>
            <Col md={4}>
              <Field label="Kết thúc" name="end" type="time" required />
            </Col>
            <Col md={4}>
              <Field
                label="Sức chứa"
                name="capacity"
                type="number"
                min="1"
                max="240"
                defaultValue="10"
                required
              />
            </Col>
          </Row>
          <SaveButton>Tạo ca</SaveButton>
        </ActionForm>
      </Panel>
      <Panel title="Danh sách ca">
        <DataTable
          rows={data.schedules}
          fields={[
            "schedule_id",
            "doctor_name",
            "work_date",
            "shift",
            "start_time",
            "end_time",
            "max_slots",
            "booked_count",
            "status",
          ]}
        />
      </Panel>
      <Row>
        <Col lg={6}>
          <Panel title="Điều chỉnh sức chứa">
            <p className="text-secondary">Áp dụng khi ca chưa có lịch đặt.</p>
            <ActionForm endpoint="/schedules" hidden={{ action: "capacity" }}>
              <Field
                label="Ca khám"
                name="scheduleId"
                required
                options={options(
                  data.schedules,
                  "schedule_id",
                  (s) => `#${s.schedule_id} · ${s.work_date} · ${s.shift}`,
                )}
              />
              <Field
                label="Sức chứa mới"
                name="capacity"
                type="number"
                min="1"
                max="240"
                required
              />
              <SaveButton>Cập nhật sức chứa</SaveButton>
            </ActionForm>
          </Panel>
        </Col>
        <Col lg={6}>
          <Panel title="Xin nghỉ / hủy ca">
            <ActionForm
              endpoint="/schedules"
              hidden={{ action: "leave" }}
              confirm="Hủy ca sẽ hủy các lịch chưa tiếp nhận và tạo thông báo. Tiếp tục?"
            >
              <Field
                label="Ca cần hủy"
                name="scheduleId"
                required
                options={options(
                  data.schedules,
                  "schedule_id",
                  (s) => `#${s.schedule_id} · ${s.work_date} · ${s.shift}`,
                )}
              />
              <SaveButton variant="outline-danger">Xác nhận nghỉ ca</SaveButton>
            </ActionForm>
          </Panel>
        </Col>
      </Row>
    </>
  );
}

export function ReceptionPage({ data, query }) {
  const booked = (data.appointments || []).filter((a) => a.status === "BOOKED");
  const today = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
  }).format(new Date());
  const todaySchedules = (data.schedules || []).filter(
    (s) => s.work_date === today,
  );
  return (
    <>
      <Heading
        title="Tiếp nhận bệnh nhân"
        description="Xác nhận bệnh nhân đã đến và cấp phiếu chờ theo phòng khám."
      />
      <Panel>
        <QueryForm path="/reception">
          <Row className="align-items-end">
            <Col md={9}>
              <Field
                label="Mã lịch, số điện thoại hoặc tên"
                name="keyword"
                defaultValue={query.get("keyword")}
              />
            </Col>
            <Col md={3}>
              <SaveButton className="mb-3">Tìm trong hôm nay</SaveButton>
            </Col>
          </Row>
        </QueryForm>
        <DataTable
          rows={data.appointments}
          fields={[
            "appointment_code",
            "patient_name",
            "phone_number",
            "appointment_date",
            "appointment_time",
            "status",
          ]}
        />
      </Panel>
      <Row>
        <Col lg={6}>
          <Panel title="Check-in lịch đã đặt">
            <ActionForm endpoint="/reception" hidden={{ action: "checkin" }}>
              <Field
                label="Lịch khám"
                name="appointmentId"
                required
                options={[
                  { value: "", label: "Chọn lịch chưa tiếp nhận" },
                  ...options(
                    booked,
                    "appointment_id",
                    (a) => `${a.appointment_code} · ${a.patient_name}`,
                  ),
                ]}
              />
              <Field label="Ưu tiên" name="priority" options={priorities} />
              <SaveButton>Cấp số chờ</SaveButton>
            </ActionForm>
          </Panel>
        </Col>
        <Col lg={6}>
          <Panel title="Đăng ký khám trực tiếp">
            <ActionForm endpoint="/reception" hidden={{ action: "walkin" }}>
              <Field
                label="Bệnh nhân đã có hồ sơ"
                name="patientId"
                required
                options={options(
                  data.patients,
                  "patient_id",
                  (p) => `${p.full_name} · ${p.email}`,
                )}
              />
              <Field
                label="Ca khám hôm nay còn chỗ"
                name="scheduleId"
                required
                options={scheduleOptions(todaySchedules)}
              />
              <Field
                label="Triệu chứng"
                name="symptoms"
                required
                maxLength={255}
              />
              <Field label="Ưu tiên" name="priority" options={priorities} />
              <SaveButton>Đăng ký & cấp số</SaveButton>
            </ActionForm>
          </Panel>
        </Col>
      </Row>
    </>
  );
}

export function QueuePage({ data, user }) {
  return (
    <>
      <Heading
        title="Hàng đợi khám hôm nay"
        description="Theo dõi bệnh nhân đã đến và tiếp nhận lượt khám tiếp theo."
      />
      {user.role === "DOCTOR" && (
        <Panel title="Bắt đầu lượt khám">
          <p className="text-secondary">
            Hệ thống chọn bệnh nhân theo thứ tự ưu tiên, tạo bệnh án nháp và mở
            màn hình khám.
          </p>
          <ActionForm endpoint="/queue" hidden={{ action: "next" }}>
            <SaveButton>Gọi bệnh nhân tiếp theo</SaveButton>
          </ActionForm>
          <Link className="d-inline-block mt-3" to="/emr">
            Mở bệnh án đang xử lý →
          </Link>
        </Panel>
      )}
      <Panel title="Danh sách chờ">
        <DataTable
          rows={data.queue}
          fields={[
            "queue_id",
            "queue_number",
            "patient_name",
            "doctor_name",
            "room_assigned",
            "priority_type",
            "status",
          ]}
        />
      </Panel>
      {["ADMIN", "RECEPTIONIST"].includes(user.role) && (
        <Panel title="Điều phối phòng cùng chuyên khoa">
          <ActionForm endpoint="/queue" hidden={{ action: "route" }}>
            <Field
              label="Mã phiếu chờ"
              name="queueId"
              type="number"
              required
              min="1"
            />
            <Field
              label="Phòng đích"
              name="room"
              required
              options={options(
                data.rooms,
                "room_number",
                (r) => `${r.room_number} · ${r.room_name}`,
              )}
            />
            <SaveButton>Chuyển phòng</SaveButton>
          </ActionForm>
        </Panel>
      )}
    </>
  );
}
