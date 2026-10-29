import { Alert, Col, Row } from "react-bootstrap";
import {
  ActionForm,
  Download,
  Field,
  Heading,
  Link,
  Panel,
  QueryForm,
  SaveButton,
  Status,
} from "../components";
import { RecallForm, ResultDetails } from "./NamEmrPage";

export function LabPage({ data, query }) {
  const order = data.selectedOrder;
  return (
    <>
      <Heading
        title="Xét nghiệm & kết quả"
        description="Kiểm tra chỉ định đã thanh toán trước khi nhập và duyệt kết quả."
        tag="CẬN LÂM SÀNG"
      />
      <Panel>
        <QueryForm path="/lab">
          <Row className="align-items-end">
            <Col md={6}>
              <Field
                label="Tìm bệnh nhân / mã chỉ định"
                name="q"
                defaultValue={query.get("q")}
              />
            </Col>
            <Col md={4}>
              <Field
                label="Trạng thái thực hiện"
                name="status"
                defaultValue={query.get("status")}
                options={[
                  { value: "", label: "Tất cả" },
                  { value: "PENDING", label: "Đang chờ" },
                  { value: "COMPLETED", label: "Hoàn tất" },
                  {
                    value: "RESULT_RECALLED",
                    label: "Cần cập nhật sau thu hồi",
                  },
                  { value: "CANCELLED", label: "Đã hủy" },
                ]}
              />
            </Col>
            <Col md={2}>
              <SaveButton variant="outline-primary" className="mb-3">
                Lọc danh sách
              </SaveButton>
            </Col>
          </Row>
        </QueryForm>
      </Panel>
      <Row>
        <Col lg={4}>
          <Panel title="Danh sách chỉ định">
            {!data.orders?.length && (
              <p className="empty-state">Không tìm thấy chỉ định phù hợp.</p>
            )}
            {(data.orders || []).map((o) => (
              <Link
                className={
                  "worklist-item " +
                  (order?.orderId === o.order_id ? "selected" : "")
                }
                to={"/lab?orderId=" + o.order_id}
                key={o.order_id}
              >
                <strong>
                  {o.patient_name}{" "}
                  <span className="float-end">#{o.order_id}</span>
                </strong>
                <p className="mb-2 mt-2">{o.service_name}</p>
                <Status value={o.payment_status} /> <Status value={o.status} />
              </Link>
            ))}
          </Panel>
        </Col>
        <Col lg={8}>
          <Panel
            title={order ? "Chỉ định #" + order.orderId : "Chọn một chỉ định"}
          >
            {!order ? (
              <p className="empty-state">
                Thông tin bệnh nhân và biểu mẫu kết quả sẽ hiển thị ở đây.
              </p>
            ) : (
              <>
                <h3 className="h5">{data.patient.full_name}</h3>
                <p>
                  {order.service.name} · Ngày sinh {data.patient.date_of_birth}
                </p>
                <p className="text-secondary">
                  Chỉ dẫn: {order.instructions || "—"}
                </p>
                {order.result ? (
                  <>
                    <Alert variant="success">
                      Kết quả đã duyệt và khóa chỉnh sửa.
                    </Alert>
                    <ResultDetails result={order.result} />
                    <RecallForm result={order.result} orderId={order.orderId} />
                  </>
                ) : order.paymentStatus === "PAID" &&
                  ["PENDING", "RESULT_RECALLED"].includes(order.status) ? (
                  <>
                    {order.status === "RESULT_RECALLED" && (
                      <Alert variant="warning">
                        Kết quả cũ đã được thu hồi. Nhập bản thay thế sau khi
                        kiểm tra lại; lịch sử cũ được giữ.
                      </Alert>
                    )}
                    <ActionForm
                      endpoint="/lab"
                      hidden={{ orderId: order.orderId }}
                      confirm="Duyệt kết quả này? Sau khi duyệt sẽ không thể chỉnh sửa."
                    >
                      <Field
                        label="Dữ liệu kết quả"
                        name="resultData"
                        as="textarea"
                        required
                        maxLength={10000}
                      />
                      <Field
                        label="Kết luận"
                        name="conclusion"
                        as="textarea"
                        maxLength={255}
                      />
                      <Field
                        label="Tệp kết quả (PDF, PNG, JPEG · tối đa 5 MB)"
                        name="attachment"
                        type="file"
                        accept=".pdf,.png,.jpg,.jpeg"
                      />
                      <Field
                        label="Hoặc liên kết HTTPS đến kết quả"
                        name="fileUrl"
                        type="url"
                        maxLength={255}
                        placeholder="https://..."
                      />
                      <SaveButton>Lưu & duyệt kết quả</SaveButton>
                    </ActionForm>
                  </>
                ) : (
                  <Alert variant="warning">
                    {order.status === "CANCELLED"
                      ? "Chỉ định đã hủy."
                      : "Chỉ định chưa thanh toán. Vui lòng chờ xác nhận từ thu ngân."}
                  </Alert>
                )}
                {!!data.recallHistory?.length && (
                  <details className="mt-4">
                    <summary>
                      Lịch sử đề nghị thu hồi ({data.recallHistory.length})
                    </summary>
                    {data.recallHistory.map((r) => (
                      <article className="result-item" key={r.requestId}>
                        <strong>Phiếu #{r.requestId}</strong>{" "}
                        <Status value={r.status} />
                        <p>{r.reason}</p>
                        <p>Ý kiến: {r.reviewNote || "—"}</p>
                        <p>Người xử lý: {r.reviewerName || "Chưa xử lý"}</p>
                        <details>
                          <summary>Bản kết quả gốc</summary>
                          <p className="pre-line">{r.resultData}</p>
                          <p>{r.conclusion}</p>
                        </details>
                      </article>
                    ))}
                  </details>
                )}
              </>
            )}
          </Panel>
        </Col>
      </Row>
    </>
  );
}

export function RecallPage({ data }) {
  return (
    <>
      <Heading
        title="Duyệt thu hồi kết quả"
        description="Chỉ hiển thị kết quả thuộc chuyên khoa phụ trách. Bản kết quả cũ luôn được giữ lại."
        tag="TRƯỞNG KHOA"
      />
      {!data.requests?.length && (
        <Panel>
          <p className="empty-state">Chưa có đề nghị thu hồi.</p>
        </Panel>
      )}
      {(data.requests || []).map((item) => (
        <Panel
          title={`Đề nghị #${item.requestId} · ${item.patientName}`}
          key={item.requestId}
        >
          <Status value={item.status} />
          <p className="mt-3">
            <strong>Dịch vụ:</strong> {item.serviceName} · {item.departmentName}
          </p>
          <p>
            <strong>Người đề nghị:</strong> {item.requesterName} ·{" "}
            {item.requestedAt}
          </p>
          <p>
            <strong>Lý do:</strong> {item.reason}
          </p>
          <details className="mb-3">
            <summary>Xem nội dung kết quả gốc #{item.resultId}</summary>
            <p className="pre-line mt-2">{item.resultData}</p>
            <p>{item.conclusion}</p>
            {item.originalFileUrl &&
              (item.originalFileUrl.startsWith("attachment:") ? (
                <Download
                  to={"/result-recalls?originalRequestId=" + item.requestId}
                >
                  Tải tệp kết quả gốc ↓
                </Download>
              ) : (
                <a
                  href={item.originalFileUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Mở tệp gốc HTTPS ↗
                </a>
              ))}
          </details>
          {item.status === "PENDING" ? (
            <ActionForm
              endpoint="/result-recalls"
              hidden={{ requestId: item.requestId }}
            >
              <Field
                label="Ý kiến phê duyệt / lý do từ chối"
                name="reviewNote"
                as="textarea"
                required
                maxLength={1000}
              />
              <Field
                label="Biên bản có chữ ký (PDF, tối đa 5 MB; bắt buộc khi phê duyệt)"
                name="signedReport"
                type="file"
                accept=".pdf"
              />
              <div className="d-flex gap-3 flex-wrap">
                <SaveButton name="action" value="approve">
                  Phê duyệt thu hồi
                </SaveButton>
                <SaveButton
                  name="action"
                  value="reject"
                  variant="outline-danger"
                >
                  Từ chối đề nghị
                </SaveButton>
              </div>
            </ActionForm>
          ) : (
            <>
              <p>
                <strong>Người xử lý:</strong> {item.reviewerName} ·{" "}
                {item.reviewedAt}
              </p>
              <p>
                <strong>Ý kiến:</strong> {item.reviewNote}
              </p>
              {item.approvalFileUrl && (
                <Download
                  className="btn btn-outline-primary"
                  to={"/result-recalls?fileRequestId=" + item.requestId}
                >
                  Tải biên bản PDF ↓
                </Download>
              )}
            </>
          )}
        </Panel>
      ))}
    </>
  );
}
