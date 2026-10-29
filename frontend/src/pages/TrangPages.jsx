import { useState } from "react";
import { Button, Col, Row } from "react-bootstrap";
import {
  ActionForm,
  DataTable,
  Field,
  Heading,
  Link,
  Panel,
  SaveButton,
  Status,
  money,
  options,
} from "../components";

const scheduleOptions = (rows) =>
  options(
    rows,
    "schedule_id",
    (s) =>
      `#${s.schedule_id} · ${s.doctor_name} · ${s.work_date} ${s.start_time}–${s.end_time} · còn ${s.remaining} chỗ`,
  );

// Tráng: lịch hẹn, danh mục dịch vụ và thanh toán.
export function AppointmentsPage({ data }) {
  const booked = (data.appointments || []).filter((a) => a.status === "BOOKED");
  const appointments = options(
    booked,
    "appointment_id",
    (a) =>
      `${a.appointment_code} · ${a.appointment_date} ${a.appointment_time}`,
  );
  return (
    <>
      <Heading
        title="Lịch khám của tôi"
        description="Đặt lịch và quản lý các lượt khám sắp tới."
      />
      <Panel title="Đặt lịch mới">
        <ActionForm endpoint="/appointments" hidden={{ action: "book" }}>
          <Field
            label="Ca khám"
            name="scheduleId"
            required
            options={[
              { value: "", label: "Chọn bác sĩ và ca khám" },
              ...scheduleOptions(data.schedules),
            ]}
          />
          <Field
            label="Triệu chứng ban đầu"
            name="symptoms"
            as="textarea"
            required
            maxLength={255}
          />
          <SaveButton>Đặt lịch</SaveButton>
        </ActionForm>
      </Panel>
      <Panel title="Danh sách lịch khám">
        <DataTable
          rows={data.appointments}
          fields={[
            "appointment_code",
            "doctor_name",
            "appointment_date",
            "appointment_time",
            "initial_symptoms",
            "status",
            "consultation_fee",
          ]}
        />
      </Panel>
      {booked.length > 0 && (
        <Row>
          <Col lg={7}>
            <Panel title="Đổi ca khám">
              <p className="text-secondary">
                Tối đa 2 lần; trước khi ca cũ bắt đầu ít nhất 12 giờ.
              </p>
              <ActionForm
                endpoint="/appointments"
                hidden={{ action: "reschedule" }}
              >
                <Field
                  label="Lịch cần đổi"
                  name="appointmentId"
                  required
                  options={appointments}
                />
                <Field
                  label="Ca mới"
                  name="scheduleId"
                  required
                  options={scheduleOptions(data.schedules)}
                />
                <SaveButton>Đổi lịch</SaveButton>
              </ActionForm>
            </Panel>
          </Col>
          <Col lg={5}>
            <Panel title="Hủy lịch">
              <ActionForm
                endpoint="/appointments"
                hidden={{ action: "cancel" }}
                confirm="Bạn chắc chắn muốn hủy lịch khám này?"
              >
                <Field
                  label="Lịch cần hủy"
                  name="appointmentId"
                  required
                  options={appointments}
                />
                <SaveButton variant="outline-danger">Hủy lịch</SaveButton>
              </ActionForm>
            </Panel>
          </Col>
        </Row>
      )}
    </>
  );
}

export function CatalogPage({ data }) {
  const [selected, setSelected] = useState(null);
  const columns = [
    { key: "service_id", label: "Mã" },
    { key: "name", label: "Dịch vụ" },
    { key: "category", label: "Phân loại" },
    { key: "unit_price", label: "Đơn giá" },
    { key: "status", label: "Trạng thái" },
    {
      label: "",
      render: (row) => (
        <Button
          variant="outline-primary"
          size="sm"
          onClick={() => setSelected(row)}
        >
          Chỉnh sửa
        </Button>
      ),
    },
  ];
  return (
    <>
      <Heading
        title="Danh mục dịch vụ & giá"
        description="Quản lý dịch vụ; giá mới áp dụng cho các chi phí phát sinh tiếp theo."
      />
      <Panel title="Danh sách dịch vụ">
        <DataTable rows={data.services} columns={columns} />
      </Panel>
      <Panel
        title={
          selected ? "Cập nhật dịch vụ #" + selected.service_id : "Thêm dịch vụ"
        }
      >
        {selected && (
          <Button
            variant="link"
            className="px-0 mb-2"
            onClick={() => setSelected(null)}
          >
            Chuyển sang thêm mới
          </Button>
        )}
        <ActionForm
          key={selected?.service_id || "new"}
          endpoint="/services"
          hidden={{ serviceId: selected?.service_id || "" }}
          onSuccess={() => setSelected(null)}
        >
          <Row>
            <Col md={7}>
              <Field
                label="Tên dịch vụ"
                name="name"
                required
                maxLength={100}
                defaultValue={selected?.name}
              />
            </Col>
            <Col md={5}>
              <Field
                label="Chuyên khoa"
                name="departmentId"
                required
                defaultValue={selected?.department_id}
                options={options(
                  data.departments,
                  "department_id",
                  (d) => d.name,
                )}
              />
            </Col>
          </Row>
          <Row>
            <Col md={4}>
              <Field
                label="Phân loại"
                name="category"
                defaultValue={selected?.category || "LAB"}
                options={[
                  { value: "CONSULTATION", label: "Khám bệnh" },
                  { value: "LAB", label: "Xét nghiệm" },
                  { value: "IMAGING", label: "Chẩn đoán hình ảnh" },
                ]}
              />
            </Col>
            <Col md={4}>
              <Field
                label="Đơn giá VND"
                name="price"
                type="number"
                min="0"
                step="1"
                required
                defaultValue={selected?.unit_price}
              />
            </Col>
            <Col md={4}>
              <Field
                label="Trạng thái"
                name="status"
                defaultValue={selected?.status || "ACTIVE"}
                options={[
                  { value: "ACTIVE", label: "Hoạt động" },
                  { value: "INACTIVE", label: "Ngừng hoạt động" },
                ]}
              />
            </Col>
          </Row>
          <SaveButton>Lưu dịch vụ</SaveButton>
        </ActionForm>
      </Panel>
    </>
  );
}

export function BillingPage({ data }) {
  const invoice = data.invoice;
  return (
    <>
      <Heading
        title="Hóa đơn & thanh toán"
        description="Theo dõi chi phí và xác nhận các khoản đã thu."
      />
      <Panel title="Lập hóa đơn" className="no-print">
        <ActionForm endpoint="/billing" hidden={{ action: "create" }}>
          <Field
            label="Bệnh án"
            name="recordId"
            required
            options={options(
              data.records,
              "record_id",
              (r) =>
                `#${r.record_id} · ${r.patient_name} · ${r.diagnosis || "Chưa chẩn đoán"}`,
            )}
          />
          <SaveButton>Lập / mở hóa đơn chưa trả</SaveButton>
        </ActionForm>
      </Panel>
      {invoice && (
        <Panel title={"Hóa đơn " + invoice.invoice_code}>
          <p>
            Bệnh nhân: <strong>{invoice.patient_name}</strong>{" "}
            <Status value={invoice.status} />
          </p>
          <DataTable
            rows={data.lines}
            fields={[
              "description",
              "source_type",
              "quantity",
              "unit_price",
              "amount",
            ]}
          />
          <h3 className="h4 mt-4">Tổng cộng: {money(invoice.total_amount)}</h3>
          {invoice.status === "UNPAID" && (
            <div className="d-flex flex-wrap gap-3 no-print mt-3">
              <ActionForm
                endpoint="/billing"
                hidden={{ action: "cash", invoiceId: invoice.invoice_id }}
                confirm="Xác nhận đã nhận đủ tiền mặt?"
              >
                <SaveButton>Đã nhận tiền mặt</SaveButton>
              </ActionForm>
              <ActionForm
                endpoint="/billing"
                hidden={{ action: "vnpay", invoiceId: invoice.invoice_id }}
              >
                <SaveButton variant="outline-primary">
                  Thanh toán VNPay sandbox
                </SaveButton>
              </ActionForm>
            </div>
          )}
          <Button
            variant="outline-secondary"
            className="mt-3 no-print"
            onClick={() => window.print()}
          >
            In hóa đơn
          </Button>
        </Panel>
      )}
      <Panel title="Danh sách hóa đơn" className="no-print">
        <DataTable
          rows={data.invoices}
          columns={[
            { key: "invoice_code", label: "Mã hóa đơn" },
            { key: "patient_name", label: "Bệnh nhân" },
            { key: "total_amount", label: "Tổng tiền" },
            { key: "status", label: "Trạng thái" },
            {
              label: "",
              render: (i) => (
                <Link to={"/billing?id=" + i.invoice_id}>Chi tiết →</Link>
              ),
            },
          ]}
        />
      </Panel>
    </>
  );
}

export { scheduleOptions };
