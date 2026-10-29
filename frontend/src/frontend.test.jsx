import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi, describe, it, expect } from "vitest";
import { AppContext } from "./context";
import { ActionForm } from "./components";
import { prepareForm, request, localPath } from "./api";
import { LoginPage } from "./pages/KhanhPages";
import { EmrPage } from "./pages/NamEmrPage";
import { RecallPage } from "./pages/NamLabPages";
import App from "./App";

function wrapper(component, overrides = {}) {
  const context = {
    csrf: "csrf-test",
    labels: {},
    navigate: vi.fn(),
    submit: vi.fn().mockResolvedValue({}),
    ...overrides,
  };
  return {
    ...render(
      <AppContext.Provider value={context}>{component}</AppContext.Provider>,
    ),
    context,
  };
}

describe("React forms and server contract", () => {
  it("opens the dashboard when the application URL ends in a slash", () => {
    expect(localPath("/hospital/")).toBe("/dashboard");
    expect(localPath("/hospital")).toBe("/dashboard");
  });

  it("uses an empty context path when deployed as the root web application", async () => {
    const meta = document.createElement("meta");
    meta.name = "hospital-context";
    meta.content = "";
    document.head.append(meta);
    try {
      vi.resetModules();
      const rootApi = await import("./api");
      expect(rootApi.contextPath).toBe("");
      expect(rootApi.href("/search")).toBe("/search");
      expect(rootApi.localPath("/")).toBe("/dashboard");
    } finally {
      meta.remove();
    }
  });
  it("opens the prescription screen using the singular view name returned by Servlet", async () => {
    window.history.replaceState({}, "", "/hospital/prescriptions?id=7");
    const payload = {
      page: "prescription",
      user: { userId: 8, role: "PATIENT", fullName: "Demo" },
      csrf: "token",
      labels: {},
      data: {
        prescription: { prescriptionId: 7, items: [], paymentStatus: "UNPAID" },
        patient: { full_name: "Demo" },
        record: { diagnosis: "Demo" },
        doctorName: "Doctor demo",
      },
    };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: async () => payload,
      }),
    );
    render(<App />);
    expect(
      await screen.findByRole("heading", { name: "Đơn thuốc #7", exact: true }),
    ).toBeTruthy();
    expect(
      screen.getByRole("link", { name: "Tải PDF ↓" }).getAttribute("href"),
    ).toBe("/hospital/prescriptions?id=7&format=pdf");
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/");
  });
  it("login sends actual form fields and CSRF through the shared API form", async () => {
    const { context } = wrapper(<LoginPage />);
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Email"), "patient@hospital.test");
    await user.type(screen.getByLabelText("Mật khẩu"), "Demo@1234");
    await user.click(screen.getByRole("button", { name: "Đăng nhập" }));
    await waitFor(() => expect(context.submit).toHaveBeenCalledOnce());
    const [endpoint, body] = context.submit.mock.calls[0];
    expect(endpoint).toBe("/login");
    expect(body.get("csrf")).toBe("csrf-test");
    expect(body.get("email")).toBe("patient@hospital.test");
  });

  it("validation errors keep entered values on the form", async () => {
    wrapper(<LoginPage />, {
      submit: vi.fn().mockRejectedValue(new Error("Sai email/mật khẩu.")),
    });
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Email"), "patient@hospital.test");
    await user.type(screen.getByLabelText("Mật khẩu"), "incorrect");
    await user.click(screen.getByRole("button", { name: "Đăng nhập" }));
    expect(await screen.findByRole("alert")).toHaveProperty(
      "textContent",
      "Sai email/mật khẩu.",
    );
    expect(screen.getByLabelText("Email").value).toBe("patient@hospital.test");
  });

  it("keeps repeated prescription fields and selected submit action", () => {
    const form = document.createElement("form");
    form.innerHTML =
      '<input name="medicationId" value="1"><input name="medicationId" value="2"><button name="action" value="reject"></button>';
    const body = prepareForm(form, "token", form.querySelector("button"));
    expect(body.getAll("medicationId")).toEqual(["1", "2"]);
    expect(body.get("action")).toBe("reject");
    expect(body.get("csrf")).toBe("token");
  });

  it("blocks a duplicate form submission while the first request is pending", async () => {
    let resolve;
    const submit = vi.fn().mockImplementation(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    );
    const { container } = wrapper(
      <ActionForm endpoint="/queue">
        <button type="submit">Next</button>
      </ActionForm>,
      { submit },
    );
    fireEvent.submit(container.querySelector("form"));
    fireEvent.submit(container.querySelector("form"));
    expect(submit).toHaveBeenCalledOnce();
    await act(async () => resolve({}));
  });

  it("renders user content as text rather than executing HTML", () => {
    const { container } = wrapper(
      <RecallPage
        data={{
          requests: [
            {
              requestId: 1,
              status: "REJECTED",
              patientName: "<script>bad()</script>",
              resultId: 2,
            },
          ],
        }}
      />,
    );
    expect(container.querySelector("script")).toBeNull();
    expect(screen.getByText(/<script>bad/)).toBeTruthy();
  });

  it("keeps prescription disabled while clinical results are pending", () => {
    wrapper(
      <EmrPage
        query={new URLSearchParams()}
        data={{
          records: [],
          services: [],
          medications: [],
          icds: [],
          patient: { full_name: "Demo" },
          record: { recordId: 1, status: "OPEN" },
          queueContext: { queueId: 1 },
          hasPendingTests: true,
        }}
      />,
    );
    expect(
      screen
        .getByRole("button", { name: "Phát hành đơn thuốc" })
        .closest("fieldset").disabled,
    ).toBe(true);
    expect(screen.getByRole("button", { name: "Lưu chẩn đoán" }).disabled).toBe(
      false,
    );
  });

  it("uses JSON API and same-origin session cookies", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: async () => ({ csrf: "token" }),
      }),
    );
    await expect(request("/login")).resolves.toEqual({ csrf: "token" });
    expect(fetch).toHaveBeenCalledWith(
      "/hospital/api/login",
      expect.objectContaining({ credentials: "same-origin", method: "GET" }),
    );
    vi.unstubAllGlobals();
  });

  it("reports authorization errors returned by Servlet without treating them as success", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: async () => ({ error: "Bạn không có quyền." }),
      }),
    );
    await expect(request("/reports")).rejects.toMatchObject({
      status: 403,
      message: "Bạn không có quyền.",
    });
    vi.unstubAllGlobals();
  });
});
