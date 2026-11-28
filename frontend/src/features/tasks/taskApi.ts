import {
  fetchCsrfToken,
  getResponseErrorMessage,
} from "../auth/authApi";
import type { Task, TaskHistoryEntry, TaskReassignInput, TaskStatus } from "./task";

const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

export class TaskApiError extends Error {
  public readonly status: number;

  constructor(
    message: string,
    status: number,
  ) {
    super(message);
    this.name = "TaskApiError";
    this.status = status;
  }
}

async function throwResponseError(
  response: Response,
  fallbackMessage: string,
): Promise<never> {
  throw new TaskApiError(
    await getResponseErrorMessage(response, fallbackMessage),
    response.status,
  );
}

async function sendMutation(
  url: string,
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
    method: "POST",
    credentials: "same-origin",
    headers,
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });

  if (!response.ok) {
    await throwResponseError(response, "Thao tác với công việc không thành công.");
  }

  return response;
}

export async function getTasks(status?: TaskStatus | ""): Promise<Task[]> {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  const response = await fetch(`/api/tasks${query}`, {
    method: "GET",
    credentials: "same-origin",
  });

  if (!response.ok) {
    await throwResponseError(response, "Không thể tải danh sách công việc.");
  }

  return (await response.json()) as Task[];
}

export async function getTaskHistory(taskId: number): Promise<TaskHistoryEntry[]> {
  const response = await fetch(`/api/tasks/${taskId}/history`, {
    method: "GET",
    credentials: "same-origin",
  });

  if (!response.ok) {
    await throwResponseError(response, "Không thể tải lịch sử công việc.");
  }

  return (await response.json()) as TaskHistoryEntry[];
}

export async function claimTask(taskId: number): Promise<Task> {
  const response = await sendMutation(`/api/tasks/${taskId}/claim`);
  return (await response.json()) as Task;
}

export async function releaseTask(taskId: number): Promise<Task> {
  const response = await sendMutation(`/api/tasks/${taskId}/release`);
  return (await response.json()) as Task;
}

export async function reassignTask(
  taskId: number,
  input: TaskReassignInput,
): Promise<Task> {
  const response = await sendMutation(`/api/tasks/${taskId}/reassign`, input);
  return (await response.json()) as Task;
}
