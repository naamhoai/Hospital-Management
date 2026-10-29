import { Alert, Button, Col, Row } from "react-bootstrap";
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

// Khánh: đăng nhập, tìm kiếm và đánh giá sau khám.
export function LoginPage() {
  return (
    <div className="login-layout">
      <div className="login-intro">
        <span className="eyebrow">HOSPITAL CARE</span>
        <h1>
          Mỗi lượt khám,
          <br />
          được kết nối.
        </h1>
        <p>
          Đặt lịch thuận tiện. Theo dõi hồ sơ và phối hợp chăm sóc trên cùng một
          hệ thống.
        </p>
        <div className="login-step">
          01 · Đặt lịch <span>02 · Khám bệnh</span> 03 · Theo dõi
        </div>
      </div>
      <Panel title="Đăng nhập" className="login-card">
        <p className="text-secondary">
          Sử dụng tài khoản được cấp để tiếp tục.
        </p>
        <ActionForm endpoint="/login">
          <Field
            label="Email"
            name="email"
            type="email"
            autoComplete="username"
            required
            maxLength={100}
          />
          <Field
            label="Mật khẩu"
            name="password"
            type="password"
            autoComplete="current-password"
            required
          />
          <SaveButton className="w-100">Đăng nhập</SaveButton>
        </ActionForm>
        <p className="small text-secondary mt-4 mb-0">
          Chức năng được hiển thị theo vai trò của bạn.
        </p>
      </Panel>
    </div>
  );
}

export function SearchPage({ data, query, user }) {
  return (
    <>
      <Heading
        title="Tìm bác sĩ & dịch vụ"
        description="Chọn chuyên khoa, bác sĩ và ca khám phù hợp."
        tag="ĐẶT LỊCH KHÁM"
      />
      <Panel>
        <QueryForm path="/search">
          <Row className="align-items-end">
            <Col md={7}>
              <Field
                label="Tên bác sĩ, chuyên khoa hoặc dịch vụ"
                name="keyword"
                maxLength={100}
                defaultValue={query.get("keyword")}
              />
            </Col>
            <Col md={3}>
              <Field
                label="Ngày khám"
                name="date"
                type="date"
                defaultValue={query.get("date")}
              />
            </Col>
            <Col md={2}>
              <SaveButton className="mb-3 w-100">Tìm kiếm</SaveButton>
            </Col>
          </Row>
        </QueryForm>
      </Panel>
      <Panel title="Bác sĩ và ca khám còn chỗ">
        <DataTable
          rows={data.doctors}
          fields={[
            "full_name",
            "department",
            "work_date",
            "start_time",
            "end_time",
            "remaining",
            "rating_average",
          ]}
        />
        <Link
          to={user?.role === "PATIENT" ? "/appointments" : "/login"}
          className="btn btn-primary mt-3"
        >
          {user?.role === "PATIENT" ? "Đặt lịch khám" : "Đăng nhập để đặt lịch"}
        </Link>
      </Panel>
      <Panel title="Dịch vụ đang hoạt động">
        <DataTable
          rows={data.services}
          fields={["name", "department", "category", "unit_price"]}
        />
      </Panel>
    </>
  );
}

export function FeedbackPage({ data }) {
  const records = (data.records || []).filter((row) => row.rating == null);
  return (
    <>
      <Heading
        title="Đánh giá sau khám"
        description="Chia sẻ trải nghiệm trong vòng 7 ngày sau khi hoàn tất lượt khám."
      />
      <Panel title="Các lượt khám của bạn">
        <DataTable
          rows={data.records}
          fields={[
            "record_id",
            "doctor_name",
            "diagnosis",
            "closed_at",
            "rating",
          ]}
        />
      </Panel>
      <Panel title="Gửi đánh giá">
        {!records.length ? (
          <Alert variant="info">
            Chưa có lượt khám đủ điều kiện gửi đánh giá mới.
          </Alert>
        ) : (
          <ActionForm endpoint="/feedback">
            <Field
              label="Bệnh án đã hoàn tất"
              name="recordId"
              required
              options={options(
                records,
                "record_id",
                (r) => `#${r.record_id} · ${r.doctor_name} · ${r.diagnosis}`,
              )}
            />
            <Field
              label="Số sao"
              name="rating"
              options={[5, 4, 3, 2, 1].map((n) => ({
                value: n,
                label: `${n} sao`,
              }))}
            />
            <Field
              label="Nhận xét"
              name="comment"
              as="textarea"
              maxLength={2000}
            />
            <SaveButton>Gửi đánh giá</SaveButton>
          </ActionForm>
        )}
      </Panel>
    </>
  );
}
