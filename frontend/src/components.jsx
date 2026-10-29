import { useId, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Card,
  Form,
  Spinner,
  Table,
} from "react-bootstrap";
import { href, prepareForm } from "./api";
import { useApp } from "./context";

export const money = (value) =>
  new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + " đ";
export const options = (rows = [], value, label) =>
  rows.map((row) => ({ value: row[value], label: label(row) }));

export function Link({ to, children, className = "", ...props }) {
  const { navigate } = useApp();
  return (
    <a
      href={href(to)}
      className={className}
      {...props}
      onClick={(e) => {
        if (
          e.button === 0 &&
          !e.ctrlKey &&
          !e.metaKey &&
          !e.shiftKey &&
          !props.target
        ) {
          e.preventDefault();
          navigate(to);
        }
      }}
    >
      {children}
    </a>
  );
}

export function Download({ to, children, ...props }) {
  return (
    <a href={href(to)} {...props}>
      {children}
    </a>
  );
}

export function Heading({
  title,
  description,
  tag = "KHÔNG GIAN LÀM VIỆC",
  children,
}) {
  return (
    <div className="page-heading">
      <div>
        <span className="eyebrow">{tag}</span>
        <h1>{title}</h1>
        {description && <p className="text-secondary mb-0">{description}</p>}
      </div>
      {children}
    </div>
  );
}

export function Panel({ title, children, className = "" }) {
  return (
    <Card className={"mb-4 " + className}>
      <Card.Body>
        {title && <h2 className="h5 mb-3">{title}</h2>}
        {children}
      </Card.Body>
    </Card>
  );
}

export function Status({ value }) {
  const { labels = {} } = useApp();
  const color = [
    "PAID",
    "COMPLETED",
    "APPROVED",
    "ACTIVE",
    "FINISHED",
  ].includes(value)
    ? "success"
    : ["CANCELLED", "RECALLED", "REJECTED", "INACTIVE"].includes(value)
      ? "danger"
      : [
            "WAITING",
            "PENDING",
            "UNPAID",
            "PENDING_PAYMENT",
            "RESULT_RECALLED",
            "WAITING_TESTS",
          ].includes(value)
        ? "warning"
        : "primary";
  const progress = {
    NOT_DIAGNOSED: "Chưa chẩn đoán",
    EXAMINED: "Đã chẩn đoán",
    WAITING_TESTS: "Chờ kết quả",
    RESULTS_READY: "Đã đủ kết quả",
    FINISHED: "Đã kết thúc",
  };
  return value ? (
    <Badge bg={color} className="status-badge">
      {labels[value] || progress[value] || value}
    </Badge>
  ) : null;
}

export function DataTable({
  rows = [],
  columns,
  fields: visibleFields,
  empty = "Chưa có dữ liệu.",
}) {
  const { labels = {} } = useApp();
  if (!rows.length) return <p className="empty-state">{empty}</p>;
  const fields =
    columns ||
    (visibleFields || Object.keys(rows[0])).map((key) => ({
      key,
      label: labels[key] || key,
    }));
  function format(value, key) {
    if (value == null || value === "") return "—";
    if (["status", "payment_status", "paymentStatus"].includes(key))
      return <Status value={value} />;
    if (
      [
        "unit_price",
        "total_amount",
        "consultation_fee",
        "amount",
        "total_revenue",
      ].includes(key)
    )
      return money(value);
    if (typeof value === "boolean") return value ? "Có" : "Không";
    if (typeof value === "object") return "—";
    return labels[value] || String(value);
  }
  return (
    <Table responsive hover className="align-middle mb-0">
      <thead>
        <tr>
          {fields.map((c, i) => (
            <th key={c.key || i}>{c.label}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <tr key={row.id || i}>
            {fields.map((c, j) => (
              <td key={c.key || j}>
                {c.render ? c.render(row) : format(row[c.key], c.key)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </Table>
  );
}

export function Field({
  label,
  name,
  options: choices,
  type = "text",
  as,
  defaultValue = "",
  ...props
}) {
  const id = useId();
  return (
    <Form.Group className="mb-3" controlId={id}>
      <Form.Label>{label}</Form.Label>
      {choices ? (
        <Form.Select
          name={name}
          defaultValue={
            props.value === undefined ? (defaultValue ?? "") : undefined
          }
          {...props}
        >
          {choices.map((o, i) => (
            <option key={i} value={o.value}>
              {o.label}
            </option>
          ))}
        </Form.Select>
      ) : (
        <Form.Control
          name={name}
          type={type}
          as={as}
          defaultValue={
            type === "file" || props.value !== undefined
              ? undefined
              : (defaultValue ?? "")
          }
          rows={as === "textarea" ? 3 : undefined}
          {...props}
        />
      )}
    </Form.Group>
  );
}

export function ActionForm({
  endpoint,
  hidden = {},
  confirm,
  children,
  className = "",
  onSuccess,
}) {
  const { csrf, submit } = useApp();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  async function handleSubmit(event) {
    event.preventDefault();
    if (pending || (confirm && !window.confirm(confirm))) return;
    const body = prepareForm(
      event.currentTarget,
      csrf,
      event.nativeEvent.submitter,
    );
    setError("");
    setPending(true);
    try {
      await submit(endpoint, body);
      onSuccess?.();
    } catch (failure) {
      setError(failure.message);
    } finally {
      setPending(false);
    }
  }
  return (
    <Form onSubmit={handleSubmit} className={className}>
      {Object.entries(hidden).map(([key, value]) => (
        <input key={key} type="hidden" name={key} value={value ?? ""} />
      ))}
      {error && (
        <Alert variant="danger" role="alert">
          {error}
        </Alert>
      )}
      <fieldset disabled={pending}>{children}</fieldset>
      {pending && (
        <div className="mt-2 text-secondary" role="status">
          <Spinner size="sm" /> Đang xử lý…
        </div>
      )}
    </Form>
  );
}

export function QueryForm({ path, children, className = "" }) {
  const { navigate } = useApp();
  return (
    <Form
      className={className}
      onSubmit={(e) => {
        e.preventDefault();
        const data = new FormData(e.currentTarget);
        const button = e.nativeEvent.submitter;
        if (button?.name) data.set(button.name, button.value);
        const target = path + "?" + new URLSearchParams(data);
        if (data.get("format")) window.location.assign(href(target));
        else navigate(target);
      }}
    >
      {children}
    </Form>
  );
}

export function SaveButton({ children = "Lưu thay đổi", ...props }) {
  return (
    <Button type="submit" {...props}>
      {children}
    </Button>
  );
}

export function FileLink({ result }) {
  if (!result?.fileUrl) return null;
  return result.fileUrl.startsWith("attachment:") ? (
    <Download to={"/attachments?resultId=" + result.resultId}>
      Tải tệp kết quả ↓
    </Download>
  ) : (
    <a href={result.fileUrl} target="_blank" rel="noopener noreferrer">
      Mở báo cáo HTTPS ↗
    </a>
  );
}
