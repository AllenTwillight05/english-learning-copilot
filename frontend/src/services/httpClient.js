import { getStoredAuth } from "./authStorage";

export class HttpError extends Error {
  constructor(message, { status, statusText, path, fieldErrors } = {}) {
    super(message);
    this.name = "HttpError";
    this.status = status;
    this.statusText = statusText;
    this.path = path;
    this.fieldErrors = fieldErrors;
  }
}

function buildHeaders(init) {
  const { token } = getStoredAuth();
  const isFormData = typeof FormData !== "undefined" && init?.body instanceof FormData;
  const headers = {
    Accept: "application/json",
    ...(init?.body && !isFormData ? { "Content-Type": "application/json" } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(init?.headers ?? {})
  };

  Object.keys(headers).forEach((key) => {
    if (headers[key] === undefined || headers[key] === null) {
      delete headers[key];
    }
  });

  return headers;
}

async function parseResponseBody(response) {
  const contentType = response.headers.get("content-type") ?? "";

  if (!contentType.includes("application/json")) {
    return null;
  }

  return response.json();
}

export async function requestJson(url, init = {}) {
  const response = await fetch(url, {
    ...init,
    headers: buildHeaders(init)
  });

  const body = await parseResponseBody(response);

  if (!response.ok) {
    throw new HttpError(
      body?.message || `Request failed: ${response.status} ${response.statusText}`,
      {
        status: response.status,
        statusText: response.statusText,
        path: body?.path,
        fieldErrors: body?.fieldErrors
      }
    );
  }

  return body;
}

export function getJson(url, init) {
  return requestJson(url, init);
}

export function postJson(url, body, init) {
  return requestJson(url, {
    ...init,
    method: "POST",
    body: JSON.stringify(body ?? {})
  });
}

export function patchJson(url, body, init) {
  return requestJson(url, {
    ...init,
    method: "PATCH",
    body: JSON.stringify(body ?? {})
  });
}
