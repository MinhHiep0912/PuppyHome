import {
  fetchCsrfToken,
  getResponseErrorMessage,
} from "../auth/authApi";
import type {
  ServiceRequest,
  ServiceRequestCreateInput,
  ServiceRequestStatus,
} from "./serviceRequest";

const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

async function requestJson<T>(
  path: string,
  init: RequestInit,
  fallbackMessage: string,
): Promise<T> {
  const response = await fetch(path, {
    credentials: "same-origin",
    ...init,
  });

  if (!response.ok) {
    throw new Error(await getResponseErrorMessage(response, fallbackMessage));
  }

  return (await response.json()) as T;
}

async function mutationHeaders(): Promise<HeadersInit> {
  return {
    "Content-Type": "application/json",
    [CSRF_HEADER_NAME]: await fetchCsrfToken(),
  };
}

export async function getServiceRequests(): Promise<ServiceRequest[]> {
  return requestJson(
    "/api/service-requests",
    { method: "GET" },
    "Không thể tải danh sách yêu cầu dịch vụ.",
  );
}

export async function createServiceRequest(
  input: ServiceRequestCreateInput,
): Promise<ServiceRequest> {
  return requestJson(
    "/api/service-requests",
    {
      method: "POST",
      headers: await mutationHeaders(),
      body: JSON.stringify(input),
    },
    "Không thể gửi yêu cầu dịch vụ.",
  );
}

export async function cancelServiceRequest(
  id: number,
): Promise<ServiceRequest> {
  return requestJson(
    `/api/service-requests/${id}/cancel`,
    {
      method: "POST",
      headers: await mutationHeaders(),
    },
    "Không thể hủy yêu cầu dịch vụ.",
  );
}

export async function updateServiceRequestStatus(
  id: number,
  status: ServiceRequestStatus,
): Promise<ServiceRequest> {
  return requestJson(
    `/api/service-requests/${id}/status`,
    {
      method: "PATCH",
      headers: await mutationHeaders(),
      body: JSON.stringify({ status }),
    },
    "Không thể cập nhật trạng thái yêu cầu.",
  );
}
