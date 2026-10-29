// Cùng origin với Tomcat: trình duyệt tự gửi cookie HttpSession, không lưu token đăng nhập.
const meta = document.querySelector('meta[name="hospital-context"]')?.content;
export const contextPath =
  meta !== undefined && meta !== "__APP_CONTEXT__" ? meta : "/hospital";

export function localPath(path) {
  const url = new URL(path, window.location.origin);
  if (url.origin !== window.location.origin) return null;
  if (
    url.pathname !== contextPath &&
    !url.pathname.startsWith(contextPath + "/")
  )
    return null;
  const relative = url.pathname.slice(contextPath.length);
  return (!relative || relative === "/" ? "/dashboard" : relative) + url.search;
}

export function href(path) {
  return contextPath + path;
}

export async function request(path, { body, signal } = {}) {
  const response = await fetch(contextPath + "/api" + path, {
    method: body ? "POST" : "GET",
    credentials: "same-origin",
    headers: { Accept: "application/json" },
    body,
    signal,
  });
  if (!response.headers.get("Content-Type")?.includes("application/json")) {
    throw new Error("Máy chủ trả về dữ liệu không hợp lệ. Vui lòng thử lại.");
  }
  const result = await response.json();
  if (!response.ok) {
    const error = new Error(result.error || "Không xử lý được yêu cầu.");
    error.status = response.status;
    error.result = result;
    throw error;
  }
  return result;
}

// FormData giữ nguyên các trường lặp medicationId/quantity và cả file đính kèm.
export function prepareForm(form, csrf, submitter) {
  const data = new FormData(form);
  data.set("csrf", csrf || "");
  if (submitter?.name) data.set(submitter.name, submitter.value);
  return data;
}
