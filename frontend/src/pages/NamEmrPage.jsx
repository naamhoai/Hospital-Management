import { useState } from "react";
import { Alert, Button, Col, Form, Row } from "react-bootstrap";
import {
  ActionForm,
  DataTable,
  Download,
  Field,
  FileLink,
  Heading,
  Link,
  Panel,
  QueryForm,
  SaveButton,
  Status,
  money,
  options,
} from "../components";

// Nam: bệnh án nhận queueId từ khối vận hành; mọi kiểm tra nghiệp vụ vẫn ở Service.
export function RecallForm({ result, orderId, queueId }) {
  return (
    <details className="my-3">
      <summary>Đề nghị thu hồi kết quả</summary>
      <ActionForm
        endpoint="/result-recalls"
        hidden={{
          action: "request",
          resultId: result.resultId,
          orderId,
          queueId,
        }}
        className="mt-3"
      >
        <Field
          label="Lý do cần thu hồi"
          name="reason"
          as="textarea"
          required
          maxLength={255}
        />
        <SaveButton variant="outline-primary">
          Gửi đề nghị cho trưởng khoa
        </SaveButton>
      </ActionForm>
    </details>
  );
}

export function ResultDetails({ result }) {
  return result ? (
    <div className="result-content">
      <p className="pre-line">
        <strong>Kết quả:</strong> {result.resultData}
      </p>
      <p>
        <strong>Kết luận:</strong> {result.conclusion || "—"}
      </p>
      <FileLink result={result} />
    </div>
  ) : null;
}

function PrescriptionForm({ data }) {
  const [lines, setLines] = useState([1]);
  const [nextId, setNextId] = useState(2);
  function addLine() {
    setLines([...lines, nextId]);
    setNextId(nextId + 1);
  }
  return (
    <Panel title="Kê đơn thuốc điện tử">
      {!data.pharmacyConfigured && (
        <Alert variant="warning">
          Chưa kết nối hệ thống dược để kiểm tra tương tác tự động. Bác sĩ cần
          kiểm tra thuốc trước khi phát hành.
        </Alert>
      )}
      {data.hasPendingTests && (
        <Alert variant="info">
          Đang chờ kết quả cận lâm sàng. Chỉ kê đơn khi đủ kết quả hoặc đã hủy
          chỉ định chưa thực hiện.
        </Alert>
      )}
      <QueryForm path="/emr" className="mb-3">
        <input type="hidden" name="queueId" value={data.queueContext.queueId} />
        <Row className="align-items-end">
          <Col md={9}>
            <Field
              label="Tìm thuốc theo tên hoặc hoạt chất"
              name="medicationKeyword"
              maxLength={100}
            />
          </Col>
          <Col md={3}>
            <SaveButton variant="outline-primary" className="mb-3">
              Tìm thuốc
            </SaveButton>
          </Col>
        </Row>
      </QueryForm>
      <ActionForm
        endpoint="/prescriptions"
        hidden={{ recordId: data.record.recordId }}
      >
        <fieldset disabled={data.hasPendingTests}>
          {lines.map((id) => (
            <Row key={id} className="medication-row align-items-end">
              <Col lg={4}>
                <Field
                  label="Tên thuốc"
                  name="medicationId"
                  required
                  options={[
                    { value: "", label: "Chọn thuốc" },
                    ...options(
                      data.medications,
                      "medicationId",
                      (m) => `${m.name} · tồn ${m.stockQuantity}`,
                    ),
                  ]}
                />
              </Col>
              <Col lg={2}>
                <Field
                  label="Số lượng thuốc"
                  name="quantity"
                  type="number"
                  min="1"
                  max="1000"
                  required
                />
              </Col>
              <Col lg={4}>
                <Field
                  label="Liều dùng"
                  name="dosage"
                  required
                  maxLength={100}
                  placeholder="Liều, số lần/ngày, thời gian dùng"
                />
              </Col>
              <Col lg={2}>
                <Button
                  variant="outline-danger"
                  className="mb-3"
                  disabled={lines.length === 1}
                  onClick={() => setLines(lines.filter((item) => item !== id))}
                >
                  Xóa dòng
                </Button>
              </Col>
            </Row>
          ))}
          <Button
            variant="outline-primary"
            className="mb-3"
            onClick={addLine}
            disabled={lines.length >= 30}
          >
            + Thêm thuốc
          </Button>
          {data.pharmacyConfigured && (
            <Form.Check
              className="mb-3"
              name="overrideInteractions"
              value="true"
              label="Tôi đã xem xét và chịu trách nhiệm về cảnh báo tương tác thuốc."
            />
          )}
          <Field label="Lời dặn" name="advice" maxLength={255} />
          <SaveButton>Phát hành đơn thuốc</SaveButton>
        </fieldset>
      </ActionForm>
    </Panel>
  );
}

export function EmrPage({ data, query }) {
  const { record, queueContext: queue, patient } = data;
  return (
    <>
      <Heading
        title="Bệnh án điện tử"
        description="Chẩn đoán, chỉ định và kê đơn theo từng lượt khám."
        tag="PHÒNG KHÁM"
      >
        <Link className="btn btn-outline-primary" to="/queue">
          ← Hàng đợi
        </Link>
      </Heading>
      <Panel title="Bệnh nhân được phân công">
        <QueryForm path="/emr">
          <Row className="align-items-end">
            <Col md={9}>
              <Field
                label="Tìm bệnh nhân"
                name="q"
                defaultValue={query.get("q")}
                placeholder="Tên hoặc số điện thoại"
              />
            </Col>
            <Col md={3}>
              <SaveButton variant="outline-primary" className="mb-3">
                Tìm
              </SaveButton>
            </Col>
          </Row>
        </QueryForm>
        <DataTable
          rows={data.records}
          empty="Chưa có bệnh án. Mở hàng đợi và tiếp nhận lượt khám."
          columns={[
            {
              label: "Bệnh án / Số chờ",
              render: (r) => `#${r.record_id} / ${r.queue_number}`,
            },
            { key: "patient_name", label: "Bệnh nhân" },
            { key: "diagnosis", label: "Chẩn đoán" },
            { key: "status", label: "Trạng thái" },
            {
              label: "",
              render: (r) => (
                <Link to={"/emr?queueId=" + r.queue_id}>Mở bệnh án →</Link>
              ),
            },
          ]}
        />
      </Panel>
      {record && (
        <>
          <Panel className="patient-banner">
            <div className="d-flex gap-3 align-items-center flex-wrap">
              <div className="patient-avatar">BN</div>
              <div className="flex-grow-1">
                <span className="eyebrow">BỆNH NHÂN ĐANG CHỌN</span>
                <h2 className="h4 mb-1">{patient.full_name}</h2>
                <p className="text-secondary mb-0">
                  Ngày sinh: {patient.date_of_birth} · Điện thoại:{" "}
                  {patient.phone_number} · Phòng {queue.roomAssigned}
                </p>
              </div>
              <div>
                <Status value={record.status} />
                <p className="mb-0 mt-2 small">
                  Tiến trình: <Status value={data.clinicalProgress} />
                </p>
              </div>
            </div>
          </Panel>
          <Panel
            title={
              "Bệnh án #" + record.recordId + " · lượt khám " + queue.queueId
            }
          >
            {record.status === "OPEN" ? (
              <ActionForm
                endpoint="/emr"
                hidden={{ action: "diagnosis", queueId: queue.queueId }}
              >
                <Field
                  label="Triệu chứng"
                  name="symptoms"
                  as="textarea"
                  required
                  maxLength={5000}
                  defaultValue={record.symptoms}
                />
                <Row>
                  <Col md={5}>
                    <Field
                      label="Mã bệnh ICD"
                      name="icdCode"
                      required
                      defaultValue={record.icdCode}
                      options={[
                        { value: "", label: "Chọn mã bệnh" },
                        ...options(
                          data.icds,
                          "code",
                          (i) => `${i.code} · ${i.description}`,
                        ),
                      ]}
                    />
                  </Col>
                  <Col md={7}>
                    <Field
                      label="Chẩn đoán"
                      name="diagnosis"
                      required
                      maxLength={255}
                      defaultValue={record.diagnosis}
                    />
                  </Col>
                </Row>
                <Field
                  label="Ghi chú"
                  name="notes"
                  as="textarea"
                  maxLength={5000}
                  defaultValue={record.notes}
                />
                <SaveButton>Lưu chẩn đoán</SaveButton>
              </ActionForm>
            ) : (
              <p>
                Bệnh án đã kết thúc: <strong>{record.diagnosis}</strong> ·{" "}
                {record.icdCode}
              </p>
            )}
          </Panel>
          {record.status === "OPEN" && (
            <Panel title="Chỉ định xét nghiệm / dịch vụ">
              <ActionForm
                endpoint="/clinical-orders"
                hidden={{ recordId: record.recordId, queueId: queue.queueId }}
              >
                <div className="service-options">
                  {(data.services || []).map((s) => (
                    <Form.Check
                      key={s.serviceId}
                      id={"service-" + s.serviceId}
                      name="serviceId"
                      value={s.serviceId}
                      label={`${s.name} · ${money(s.unitPrice)}`}
                    />
                  ))}
                </div>
                <Field label="Chỉ dẫn" name="instructions" maxLength={255} />
                <SaveButton>Tạo chỉ định</SaveButton>
              </ActionForm>
            </Panel>
          )}
          <Panel title="Chỉ định và kết quả xét nghiệm">
            {!data.orders?.length && (
              <p className="empty-state">Chưa có chỉ định cho lượt khám này.</p>
            )}
            {(data.orders || []).map((o) => (
              <article className="result-item" key={o.orderId}>
                <div className="d-flex justify-content-between gap-3 flex-wrap">
                  <div>
                    <strong>{o.service.name}</strong>
                    <p className="text-secondary small">
                      <Download to={"/clinical-orders?id=" + o.orderId}>
                        Phiếu chỉ định #{o.orderId} ↓
                      </Download>{" "}
                      · {money(o.unitPrice)}
                    </p>
                  </div>
                  <div>
                    <Status value={o.paymentStatus} />{" "}
                    <Status value={o.status} />
                  </div>
                </div>
                {o.status === "RESULT_RECALLED" && (
                  <Alert variant="warning">
                    Kết quả cũ đã thu hồi. Cần đối chiếu lại thông tin khi có
                    bản thay thế.
                  </Alert>
                )}
                <ResultDetails result={o.result} />
                {o.result && (
                  <RecallForm
                    result={o.result}
                    orderId={o.orderId}
                    queueId={queue.queueId}
                  />
                )}
                {record.status === "OPEN" &&
                  o.status === "PENDING" &&
                  o.paymentStatus !== "PAID" && (
                    <ActionForm
                      endpoint="/clinical-orders"
                      hidden={{
                        action: "cancel",
                        orderId: o.orderId,
                        queueId: queue.queueId,
                      }}
                      confirm="Hủy chỉ định chưa thanh toán này?"
                    >
                      <SaveButton variant="outline-danger" size="sm">
                        Hủy chỉ định
                      </SaveButton>
                    </ActionForm>
                  )}
              </article>
            ))}
          </Panel>
          {record.status === "OPEN" && <PrescriptionForm data={data} />}
          <Panel title="Đơn thuốc">
            {!(data.prescriptions || []).length && (
              <p className="empty-state">Chưa có đơn thuốc.</p>
            )}
            {(data.prescriptions || []).map((rx) => (
              <p key={rx.prescriptionId}>
                <Link to={"/prescriptions?id=" + rx.prescriptionId}>
                  Đơn #{rx.prescriptionId} · {rx.prescribedDate}
                </Link>{" "}
                <Status value={rx.paymentStatus} />
              </p>
            ))}
          </Panel>
          <Panel title="Lịch sử khám đã hoàn tất">
            <DataTable rows={data.history} />
          </Panel>
          {record.status === "OPEN" && (
            <Panel>
              <ActionForm
                endpoint="/emr"
                hidden={{ action: "close", queueId: queue.queueId }}
                confirm="Kết thúc khám sẽ khóa chỉnh sửa bệnh án và kê đơn. Tiếp tục?"
              >
                <SaveButton>Kết thúc khám</SaveButton>
              </ActionForm>
            </Panel>
          )}
        </>
      )}
    </>
  );
}
