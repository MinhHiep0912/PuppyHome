import {
  fetchCsrfToken,
  getResponseErrorMessage,
} from "../auth/authApi";
import type { AdminService, ServiceStatus, ServiceUpsertInput } from "./adminService";

const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

export class AdminServiceApiError extends Error {
  public readonly status: number;

  constructor(
    message: string,
    status: number,
  ) {
    super(message);
    this.name = "AdminServiceApiError";
    this.status = status;
  }
}

async function throwResponseError(
  response: Response,
  fallbackMessage: string,
): Promise<never> {
  throw new AdminServiceApiError(
    await getResponseErrorMessage(response, fallbackMessage),
    response.status,
  );
}

async function sendMutation(
  url: string,
  method: "POST" | "PUT" | "PATCH" | "DELETE",
  body?: unknown,
): Promise<Response> {
  const csrfToken = await fetchCsrfToken();
  const headers: Record<string, string> = {
    [CSRF_HEADER_NAME]: csrfToken,
  };

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    method,
    credentials: "same-origin",
    headers,
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });

  if (!response.ok) {
    await throwResponseError(response, "Thao tác với dịch vụ không thành công.");
  }

  return response;
}

export async function getAdminServices(): Promise<AdminService[]> {
  const response = await fetch("/api/admin/services", {
    method: "GET",
    credentials: "same-origin",
  });

  if (!response.ok) {
    await throwResponseError(response, "Không thể tải danh sách dịch vụ.");
  }

  return (await response.json()) as AdminService[];
}

export async function createAdminService(
  service: ServiceUpsertInput,
): Promise<AdminService> {
  const response = await sendMutation(
    "/api/admin/services",
    "POST",
    service,
  );
  return (await response.json()) as AdminService;
}

export async function updateAdminService(
  id: number,
  service: ServiceUpsertInput,
): Promise<AdminService> {
  const response = await sendMutation(
    `/api/admin/services/${id}`,
    "PUT",
    service,
  );
  return (await response.json()) as AdminService;
}

export async function updateAdminServiceStatus(
  id: number,
  status: ServiceStatus,
): Promise<AdminService> {
  const response = await sendMutation(
    `/api/admin/services/${id}/status`,
    "PATCH",
    { status },
  );
  return (await response.json()) as AdminService;
}

export async function softDeleteAdminService(id: number): Promise<void> {
  await sendMutation(`/api/admin/services/${id}`, "DELETE");
}
