import { useState } from "react";
import { Col, ProgressBar, Row } from "react-bootstrap";
import {
  DataTable,
  Field,
  Heading,
  Panel,
  QueryForm,
  SaveButton,
  money,
  options,
} from "../components";

export function ReportsPage({ data }) {
  const { report, filter } = data;
  const [type, setType] = useState(data.type);
  const [costType, setCostType] = useState(filter.serviceType || "");
  const maximum = Math.max(
    1,
    ...(report.series || []).map((p) => Number(p.value)),
  );
  const metrics =
    data.type === "flow"
      ? [
          ["Tổng lượt hẹn", report.patientCount],
          ["Đã hoàn tất", report.completedCount],
          ["Đã hủy", report.cancelledCount],
          ["Vắng mặt", report.noShowCount],
        ]
      : [
          ["Tổng doanh thu", money(report.totalRevenue)],
          ["Hóa đơn đã thanh toán", report.paidInvoiceCount],
        ];
  return (
    <>
      <Heading
        title="Báo cáo & phân tích"
        description="Tổng hợp dữ liệu khám bệnh và doanh thu đã thanh toán."
        tag="QUẢN LÝ HOẠT ĐỘNG"
      />
      <Panel>
        <QueryForm path="/reports">
          <Row>
            <Col md={3}>
              <Field
                label="Loại báo cáo"
                name="type"
                value={type}
                onChange={(e) => setType(e.target.value)}
                options={[
                  { value: "flow", label: "Lưu lượng bệnh nhân" },
                  { value: "revenue", label: "Doanh thu dịch vụ" },
                ]}
              />
            </Col>
            <Col md={3}>
              <Field
                label="Từ ngày"
                name="from"
                type="date"
                required
                defaultValue={filter.fromDate}
              />
            </Col>
            <Col md={3}>
              <Field
                label="Đến ngày"
                name="to"
                type="date"
                required
                defaultValue={filter.toDate}
              />
            </Col>
            <Col md={3}>
              <Field
                label="Nhóm theo"
                name="groupBy"
                defaultValue={filter.groupBy}
                options={[
                  { value: "DAY", label: "Ngày" },
                  { value: "MONTH", label: "Tháng" },
                  { value: "QUARTER", label: "Quý" },
                ]}
              />
            </Col>
          </Row>
          <Row>
            <Col md={3}>
              <Field
                label="Chuyên khoa"
                name="departmentId"
                defaultValue={filter.departmentId}
                options={[
                  { value: "", label: "Tất cả chuyên khoa" },
                  ...options(data.departments, "department_id", (d) => d.name),
                ]}
              />
            </Col>
            <Col md={3}>
              <Field
                label="Dịch vụ chỉ định"
                name="serviceId"
                defaultValue={filter.serviceId}
                disabled={
                  type === "revenue" && costType !== "" && costType !== "ORDER"
                }
                options={[
                  { value: "", label: "Tất cả dịch vụ" },
                  ...options(data.services, "service_id", (s) => s.name),
                ]}
              />
            </Col>
            <Col md={3}>
              <Field
                label="Loại chi phí"
                name="serviceType"
                value={costType}
                disabled={type !== "revenue"}
                onChange={(e) => setCostType(e.target.value)}
                options={[
                  { value: "", label: "Tất cả" },
                  { value: "CONSULTATION", label: "Khám bệnh" },
                  { value: "ORDER", label: "Cận lâm sàng" },
                  { value: "MEDICATION", label: "Thuốc" },
                ]}
              />
            </Col>
            <Col md={3}>
              <Field
                label="Phương thức thanh toán"
                name="method"
                defaultValue={filter.paymentMethod}
                options={[
                  { value: "", label: "Tất cả" },
                  { value: "CASH", label: "Tiền mặt" },
                  { value: "VNPAY", label: "VNPay" },
                ]}
              />
            </Col>
          </Row>
          <div className="d-flex flex-wrap gap-2">
            <SaveButton>Xem báo cáo</SaveButton>
            <SaveButton variant="outline-primary" name="format" value="xlsx">
              Xuất Excel ↓
            </SaveButton>
            <SaveButton variant="outline-primary" name="format" value="pdf">
              Xuất PDF ↓
            </SaveButton>
          </div>
        </QueryForm>
      </Panel>
      <Row>
        {metrics.map(([label, value]) => (
          <Col md={6} xl={data.type === "flow" ? 3 : 6} key={label}>
            <Panel className="metric">
              <span className="text-secondary">{label}</span>
              <strong>{value}</strong>
            </Panel>
          </Col>
        ))}
      </Row>
      <Panel
        title={
          (data.type === "flow" ? "Lượt khám" : "Doanh thu") + " theo thời gian"
        }
      >
        <p className="text-secondary">
          {filter.fromDate} → {filter.toDate}
        </p>
        {!report.series?.length && (
          <p className="empty-state">
            Chưa có dữ liệu. Thử thay đổi khoảng ngày hoặc bộ lọc.
          </p>
        )}
        {(report.series || []).map((point, i) => (
          <div key={i} className="chart-row">
            <span>{point.label}</span>
            <ProgressBar
              now={(Number(point.value) * 100) / maximum}
              aria-label={point.label}
            />
            <strong>
              {data.type === "revenue" ? money(point.value) : point.value}
            </strong>
          </div>
        ))}
      </Panel>
      <Panel title="Chi tiết tổng hợp">
        <p className="text-secondary">
          Doanh thu tính theo ngày thanh toán. Bộ lọc dịch vụ áp dụng cho chỉ
          định cận lâm sàng.
        </p>
        <DataTable rows={data.rows} />
      </Panel>
    </>
  );
}
