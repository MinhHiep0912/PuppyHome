import {
  fetchCsrfToken,
  getResponseErrorMessage,
} from "../auth/authApi";
import type {
  Employee,
  EmployeeCreateInput,
  EmployeeUpdateInput,
} from "./employee";

const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

export class EmployeeApiError extends Error {
  public readonly status: number;

  constructor(
    message: string,
    status: number,
  ) {
    super(message);
    this.name = "EmployeeApiError";
    this.status = status;
  }
}

async function throwResponseError(
  response: Response,
  fallbackMessage: string,
): Promise<never> {
  throw new EmployeeApiError(
    await getResponseErrorMessage(response, fallbackMessage),
    response.status,
  );
}

async function sendMutation(
  url: string,
  method: "POST" | "PUT",
  body: unknown,
): Promise<Response> {
  const csrfToken = await fetchCsrfToken();
  const response = await fetch(url, {
    method,
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      [CSRF_HEADER_NAME]: csrfToken,
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    await throwResponseError(response, "Thao tác với nhân viên không thành công.");
  }

  return response;
}

export async function getEmployees(): Promise<Employee[]> {
  const response = await fetch("/api/admin/employees", {
    method: "GET",
    credentials: "same-origin",
  });

  if (!response.ok) {
    await throwResponseError(response, "Không thể tải danh sách nhân viên.");
  }

  return (await response.json()) as Employee[];
}

export async function createEmployee(
  employee: EmployeeCreateInput,
): Promise<Employee> {
  const response = await sendMutation(
    "/api/admin/employees",
    "POST",
    employee,
  );
  return (await response.json()) as Employee;
}

export async function updateEmployee(
  userId: number,
  employee: EmployeeUpdateInput,
): Promise<Employee> {
  const response = await sendMutation(
    `/api/admin/employees/${userId}`,
    "PUT",
    employee,
  );
  return (await response.json()) as Employee;
}
