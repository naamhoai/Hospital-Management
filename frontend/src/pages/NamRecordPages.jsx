import { Alert, Button } from "react-bootstrap";
import {
  ActionForm,
  DataTable,
  Download,
  Heading,
  Link,
  Panel,
  SaveButton,
  Status,
  money,
} from "../components";
import { ResultDetails } from "./NamEmrPage";

export function PrescriptionPage({ data, user }) {
  const rx = data.prescription;
  return (
    <>
      <Heading title={"Đơn thuốc #" + rx.prescriptionId} tag="HỒ SƠ ĐIỀU TRỊ">
        <div className="d-flex gap-2 no-print">
          <Download
            to={"/prescriptions?id=" + rx.prescriptionId + "&format=pdf"}
            className="btn btn-outline-primary"
          >
            Tải PDF ↓
          </Download>
          <Button onClick={() => window.print()}>In đơn</Button>
        </div>
      </Heading>
      <Panel title="ĐƠN THUỐC">
        <Status value={rx.paymentStatus} />
        <p className="mt-3">
          Bác sĩ: <strong>{data.doctorName}</strong>
        </p>
        <p>
          Bệnh nhân: <strong>{data.patient.full_name}</strong> · Ngày sinh{" "}
          {data.patient.date_of_birth}
        </p>
        <p>
          Chẩn đoán: {data.record.diagnosis} · ICD {data.record.icdCode}
        </p>
        <p className="text-secondary">Ngày kê: {rx.prescribedDate}</p>
        <DataTable
          rows={rx.items}
          columns={[
            { label: "Thuốc", render: (i) => i.medication.name },
            { key: "quantity", label: "Số lượng" },
            { label: "Đơn vị", render: (i) => i.medication.unit },
            { key: "dosage", label: "Liều dùng" },
            { label: "Đơn giá", render: (i) => money(i.unitPrice) },
          ]}
        />
        <div className="advice mt-4">
          <strong>Lời dặn của bác sĩ</strong>
          <p className="mb-0 mt-2">
            {rx.advice || "Không có lời dặn bổ sung."}
          </p>
        </div>
        <Link
          className="d-inline-block mt-4 no-print"
          to={user.role === "PATIENT" ? "/my-records" : "/emr"}
        >
          ← Trở về bệnh án
        </Link>
      </Panel>
    </>
  );
}

export function MyRecordsPage({ data }) {
  return (
    <>
      <Heading
        title="Hồ sơ & hóa đơn"
        description="Theo dõi kết quả khám, đơn thuốc và các khoản thanh toán của bạn."
        tag="SỨC KHỎE CỦA TÔI"
      />
      <Panel title="Lịch sử khám bệnh">
        {!data.records?.length && (
          <p className="empty-state">Bạn chưa có bệnh án trên hệ thống.</p>
        )}
        {(data.records || []).map((m) => (
          <Link
            className="worklist-item"
            to={"/my-records?id=" + m.record_id}
            key={m.record_id}
          >
            <strong>
              Bệnh án #{m.record_id} · {m.doctor_name}
            </strong>{" "}
            <Status value={m.status} />
            <p className="mt-2 mb-1">{m.diagnosis || "Chưa có chẩn đoán"}</p>
            <small>{m.created_at}</small>
          </Link>
        ))}
      </Panel>
      {data.record && (
        <Panel title={"Chi tiết bệnh án #" + data.record.recordId}>
          <p>Bác sĩ: {data.doctorName}</p>
          <p>
            <strong>Triệu chứng:</strong> {data.record.symptoms}
          </p>
          <p>
            <strong>Chẩn đoán:</strong> {data.record.diagnosis} · ICD{" "}
            {data.record.icdCode}
          </p>
          <h3 className="h6 mt-4">Đơn thuốc</h3>
          {(data.prescriptions || []).map((rx) => (
            <p key={rx.prescriptionId}>
              <Link to={"/prescriptions?id=" + rx.prescriptionId}>
                Xem & in đơn #{rx.prescriptionId} →
              </Link>
            </p>
          ))}
          <h3 className="h6 mt-4">Chỉ định & kết quả</h3>
          {(data.orders || []).map((o) => (
            <article className="result-item" key={o.orderId}>
              <strong>{o.service.name}</strong> <Status value={o.status} />
              {o.status === "RESULT_RECALLED" && (
                <Alert variant="warning" className="mt-3">
                  Kết quả đang được kiểm tra và cập nhật. Bản đã thu hồi không
                  còn hiệu lực.
                </Alert>
              )}
              <ResultDetails result={o.result} />
            </article>
          ))}
        </Panel>
      )}
      <Panel title="Hóa đơn của tôi">
        {!data.invoices?.length && (
          <p className="empty-state">Chưa có hóa đơn.</p>
        )}
        {(data.invoices || []).map((i) => (
          <article
            className="result-item d-flex align-items-center justify-content-between flex-wrap gap-3"
            key={i.invoice_id}
          >
            <div>
              <strong>{i.invoice_code}</strong>
              <p className="mt-2 mb-2">
                Bệnh án #{i.record_id} · {money(i.total_amount)}
              </p>
              <Status value={i.status} />
            </div>
            {i.status === "UNPAID" && (
              <ActionForm
                endpoint="/my-records"
                hidden={{ invoiceId: i.invoice_id }}
              >
                <SaveButton>Thanh toán VNPay</SaveButton>
              </ActionForm>
            )}
          </article>
        ))}
      </Panel>
    </>
  );
}
