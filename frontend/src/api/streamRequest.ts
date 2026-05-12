import { message } from "antd";
import { readNdjsonStream } from "../utils/stream";
import type { NdjsonStreamHandlers } from "../utils/stream";

const DEFAULT_BASE_URL = "http://localhost:8080";

function getBaseUrl() {
  return import.meta.env.VITE_API_BASE_URL || DEFAULT_BASE_URL;
}

function getToken() {
  return localStorage.getItem("token");
}

function handleUnauthorized() {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  localStorage.removeItem("userInfo");
  message.error("登录已过期，请重新登录");
  if (window.location.pathname !== "/login") {
    window.location.replace("/login");
  }
}

export async function postNdjsonStream<TPayload>(
  path: string,
  body: unknown,
  handlers: NdjsonStreamHandlers<TPayload>,
) {
  const token = getToken();
  const response = await fetch(`${getBaseUrl()}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
  });

  if (response.status === 401) {
    handleUnauthorized();
    throw new Error("Unauthorized");
  }

  if (!response.ok) {
    throw new Error(`Stream request failed: ${response.status}`);
  }

  await readNdjsonStream<TPayload>(response, handlers);
}
