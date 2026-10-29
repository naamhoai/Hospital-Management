import { useCallback, useEffect, useRef, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Container,
  Nav,
  Navbar,
  Row,
  Spinner,
} from "react-bootstrap";
import { contextPath, localPath, request } from "./api";
import { AppContext } from "./context";
import { ActionForm, Heading, Link, Panel, SaveButton } from "./components";
import { FeedbackPage, LoginPage, SearchPage } from "./pages/KhanhPages";
import { AppointmentsPage, BillingPage, CatalogPage } from "./pages/TrangPages";
import { QueuePage, ReceptionPage, SchedulesPage } from "./pages/KienPages";
import { EmrPage } from "./pages/NamEmrPage";
import { LabPage, RecallPage } from "./pages/NamLabPages";
import { MyRecordsPage, PrescriptionPage } from "./pages/NamRecordPages";
import { ReportsPage } from "./pages/NamReportPage";

const navigation = [
  ["/dashboard", "Tổng quan", null],
  ["/search", "Tìm bác sĩ & dịch vụ", null],
  ["/appointments", "Lịch khám của tôi", ["PATIENT"]],
  ["/my-records", "Hồ sơ & hóa đơn", ["PATIENT"]],
  ["/feedback", "Đánh giá", ["PATIENT"]],
  ["/schedules", "Ca làm việc", ["DOCTOR", "ADMIN"]],
  ["/reception", "Tiếp nhận", ["RECEPTIONIST", "ADMIN"]],
  ["/queue", "Hàng đợi", ["RECEPTIONIST", "DOCTOR", "ADMIN"]],
  ["/emr", "Bệnh án & kê đơn", ["DOCTOR"]],
  ["/lab", "Kết quả xét nghiệm", ["LAB_TECHNICIAN"]],
  ["/result-recalls", "Duyệt thu hồi kết quả", ["DEPARTMENT_HEAD"]],
  ["/billing", "Thu ngân", ["CASHIER", "ADMIN"]],
  ["/services", "Danh mục dịch vụ", ["MANAGER", "ADMIN"]],
  ["/reports", "Báo cáo", ["MANAGER", "ADMIN"]],
];
const pages = {
  login: LoginPage,
  search: SearchPage,
  appointments: AppointmentsPage,
  services: CatalogPage,
  schedules: SchedulesPage,
  reception: ReceptionPage,
  queue: QueuePage,
  emr: EmrPage,
  lab: LabPage,
  "result-recalls": RecallPage,
  prescription: PrescriptionPage,
  "my-records": MyRecordsPage,
  reports: ReportsPage,
  feedback: FeedbackPage,
  billing: BillingPage,
};
function currentPath() {
  return localPath(window.location.href) || "/dashboard";
}

function ClinicalNotices({ user }) {
  const [notices, setNotices] = useState([]);
  const activeAt = useRef(Date.now());
  useEffect(() => {
    if (user?.role !== "DOCTOR") {
      setNotices([]);
      return;
    }
    let cursor = null;
    let stopped = false;
    let pending = false;
    const controller = new AbortController();
    const activity = () => {
      activeAt.current = Date.now();
    };
    // Chỉ thêm thông báo; không tải lại bệnh án làm mất dữ liệu bác sĩ đang nhập.
    async function check() {
      if (
        stopped ||
        pending ||
        document.hidden ||
        Date.now() - activeAt.current > 600000
      )
        return;
      pending = true;
      try {
        const result = await request(
          "/clinical-updates" + (cursor == null ? "" : "?afterId=" + cursor),
          { signal: controller.signal },
        );
        cursor = result.cursor;
        if (!stopped && result.updates.length)
          setNotices((old) => [...old, ...result.updates].slice(-5));
      } catch {
        /* Lỗi thông báo tạm thời không chặn thao tác khám. */
      } finally {
        pending = false;
      }
    }
    document.addEventListener("pointerdown", activity);
    document.addEventListener("keydown", activity);
    check();
    const timer = window.setInterval(check, 15000);
    return () => {
      stopped = true;
      controller.abort();
      window.clearInterval(timer);
      document.removeEventListener("pointerdown", activity);
      document.removeEventListener("keydown", activity);
    };
  }, [user?.userId, user?.role]);
  return notices.length ? (
    <Alert variant="info" dismissible onClose={() => setNotices([])}>
      {notices.map((item) => (
        <div key={item.id}>
          <Link to={"/emr?queueId=" + item.queueId}>
            {item.message} Mở bệnh án →
          </Link>
        </div>
      ))}
    </Alert>
  ) : null;
}

function Dashboard({ user, labels }) {
  const shortcuts = navigation.filter(([path, , roles]) =>
    roles?.includes(user.role),
  );
  return (
    <>
      <Heading
        title={"Xin chào, " + user.fullName}
        description="Mọi công việc trong ngày, bắt đầu từ đây."
      />
      <div className="welcome-banner">
        <span className="eyebrow">CHĂM SÓC ĐƯỢC KẾT NỐI</span>
        <h2>
          Một hành trình khám bệnh,
          <br />
          đầy đủ thông tin.
        </h2>
        <p className="mb-0">
          {labels[user.role]} · Chọn công việc bên dưới để bắt đầu.
        </p>
      </div>
      <Row>
        {shortcuts.map(([path, label], i) => (
          <Col md={6} xl={4} key={path}>
            <Card className="shortcut-card mb-4">
              <Card.Body>
                <span className="shortcut-number">
                  {String(i + 1).padStart(2, "0")}
                </span>
                <h2 className="h5 mt-3">{label}</h2>
                <Link to={path} className="stretched-link">
                  Mở chức năng →
                </Link>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>
      <Panel title="Quy trình khám bệnh">
        <p className="workflow mb-0">
          Đặt lịch <span>→</span> Tiếp nhận <span>→</span> Khám & chỉ định{" "}
          <span>→</span> Thanh toán & xét nghiệm <span>→</span> Hoàn tất
        </p>
      </Panel>
    </>
  );
}

export default function App() {
  const [route, setRoute] = useState(currentPath);
  const [revision, setRevision] = useState(0);
  const [screen, setScreen] = useState(null);
  const [session, setSession] = useState({ user: null, csrf: "", labels: {} });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);

  const navigate = useCallback((path) => {
    if (!path.startsWith("/") || path.startsWith("//")) return;
    window.history.pushState({}, "", contextPath + path);
    setRoute(path);
    setRevision((value) => value + 1);
    setMenuOpen(false);
    window.scrollTo(0, 0);
  }, []);

  function acceptSession(result) {
    if (Object.hasOwn(result, "user"))
      setSession({
        user: result.user,
        csrf: result.csrf,
        labels: result.labels || {},
      });
  }

  useEffect(() => {
    const back = () => {
      setRoute(currentPath());
      setRevision((value) => value + 1);
    };
    window.addEventListener("popstate", back);
    return () => window.removeEventListener("popstate", back);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    request(route, { signal: controller.signal })
      .then((result) => {
        acceptSession(result);
        if (result.redirect) {
          const path = localPath(result.redirect);
          if (path) navigate(path);
          else window.location.assign(result.redirect);
        } else setScreen({ ...result, key: route + ":" + revision });
      })
      .catch((failure) => {
        if (failure.name === "AbortError") return;
        if (failure.result) acceptSession(failure.result);
        if (failure.status === 401 && !route.startsWith("/login"))
          navigate("/login?reason=session-expired");
        else {
          setError(failure.message);
          if (!screen) setScreen({ page: "error", data: {}, key: route });
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [route, revision, navigate]);

  async function submit(endpoint, body) {
    try {
      const result = await request(endpoint, { body });
      acceptSession(result);
      if (result.redirect) {
        const path = localPath(result.redirect);
        if (path) navigate(path);
        else window.location.assign(result.redirect);
      } else setRevision((value) => value + 1);
      return result;
    } catch (failure) {
      if (failure.result) acceptSession(failure.result);
      if (failure.status === 401) navigate("/login?reason=session-expired");
      throw failure;
    }
  }

  const Page = pages[screen?.page];
  const user = session.user;
  const items = navigation.filter(([path, , roles]) =>
    user ? !roles || roles.includes(user.role) : path === "/search",
  );
  if (!user) items.push(["/login", "Đăng nhập"]);
  const activePath = route.split("?")[0];
  const menu = (
    <Nav className="flex-column gap-1">
      {items.map(([path, label]) => (
        <Nav.Item key={path}>
          <Link
            className={"nav-link " + (activePath === path ? "active" : "")}
            to={path}
            aria-current={activePath === path ? "page" : undefined}
          >
            {label}
          </Link>
        </Nav.Item>
      ))}
    </Nav>
  );

  return (
    <AppContext.Provider value={{ ...session, navigate, submit }}>
      <Navbar
        expanded={menuOpen}
        onToggle={setMenuOpen}
        expand="lg"
        className="app-header bg-white border-bottom no-print"
      >
        <Container fluid>
          <Link to={user ? "/dashboard" : "/login"} className="navbar-brand">
            <span className="brand-mark">H</span> Hospital{" "}
            <small>CARE WORKSPACE</small>
          </Link>
          <div className="d-flex align-items-center gap-3">
            <div className="identity d-none d-sm-block">
              {user && (
                <>
                  {user.fullName}
                  <small>{session.labels[user.role] || user.role}</small>
                </>
              )}
            </div>
            {user && (
              <ActionForm endpoint="/logout">
                <SaveButton variant="outline-secondary" size="sm">
                  Đăng xuất
                </SaveButton>
              </ActionForm>
            )}
            <Navbar.Toggle aria-controls="mobile-menu" />
          </div>
          <Navbar.Collapse id="mobile-menu" className="d-lg-none">
            <div className="w-100 pt-3 d-lg-none">{menu}</div>
          </Navbar.Collapse>
        </Container>
      </Navbar>
      <div
        className={
          "app-layout " + (screen?.page === "login" ? "login-shell" : "")
        }
      >
        {screen?.page !== "login" && (
          <aside className="app-sidebar d-none d-lg-block no-print">
            <span className="nav-caption">KHÔNG GIAN LÀM VIỆC</span>
            {menu}
          </aside>
        )}
        <main className="app-main" aria-busy={loading}>
          {loading && (
            <div className="loading-line" role="status">
              <Spinner size="sm" /> Đang tải…
            </div>
          )}
          {error && (
            <Alert variant="danger" role="alert">
              {error}
            </Alert>
          )}
          {screen?.error && <Alert variant="warning">{screen.error}</Alert>}
          {screen?.flash && (
            <Alert
              variant="success"
              dismissible
              onClose={() => setScreen({ ...screen, flash: null })}
            >
              {screen.flash}
            </Alert>
          )}
          <ClinicalNotices user={user} />
          {Page && (
            <Page
              key={screen.key}
              data={screen.data}
              query={new URLSearchParams(route.split("?")[1] || "")}
              user={user}
            />
          )}
          {screen?.page === "dashboard" && user && (
            <Dashboard user={user} labels={session.labels} />
          )}
          {screen?.page === "payment-result" && (
            <>
              <Heading title="Kết quả thanh toán" />
              <Panel>
                <p>{screen.data.message}</p>
                <Link
                  to={user?.role === "PATIENT" ? "/my-records" : "/billing"}
                >
                  Trở về hóa đơn →
                </Link>
              </Panel>
            </>
          )}
          {screen?.page === "error" && (
            <Panel title="Không thể mở chức năng">
              <p>
                Kiểm tra thông báo phía trên hoặc chọn chức năng khác từ menu.
              </p>
              <Button
                variant="outline-primary"
                onClick={() => navigate("/search")}
              >
                Tìm bác sĩ & dịch vụ
              </Button>
            </Panel>
          )}
        </main>
      </div>
      <footer className="app-footer no-print">
        Hospital Appointment & Medical Service Management System · SE1976 · Nhóm
        6
      </footer>
    </AppContext.Provider>
  );
}
