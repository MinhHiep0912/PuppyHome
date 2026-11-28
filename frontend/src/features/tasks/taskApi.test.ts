import { afterEach, describe, expect, it, vi } from "vitest";
import { claimTask, getTasks, reassignTask } from "./taskApi";

const taskPayload = {
  id: 5,
  serviceRequestId: 12,
  title: "Logacc",
  description: null,
  status: "OPEN",
  currentAssigneeId: null,
  assigneeDisplayName: null,
  deadlineAt: null,
  createdAt: "2026-01-01T00:00:00",
  updatedAt: "2026-01-01T00:00:00",
};

describe("taskApi", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = "XSRF-TOKEN=; Max-Age=0";
  });

  it("fetches tasks with optional status filter", async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json([taskPayload]));
    vi.stubGlobal("fetch", fetchMock);

    await getTasks("OPEN");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/tasks?status=OPEN",
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("claims a task with CSRF header", async () => {
    document.cookie = "XSRF-TOKEN=csrf-token";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(
        Response.json({ ...taskPayload, status: "IN_PROGRESS" }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await claimTask(5);

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/tasks/5/claim",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "X-XSRF-TOKEN": "csrf-token",
        }),
      }),
    );
  });

  it("reassigns with assignee and reason body", async () => {
    document.cookie = "XSRF-TOKEN=csrf-token";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(Response.json(taskPayload));
    vi.stubGlobal("fetch", fetchMock);

    await reassignTask(5, { assigneeUserId: 9, reason: "Đổi người" });

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/tasks/5/reassign",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ assigneeUserId: 9, reason: "Đổi người" }),
      }),
    );
  });

  it("surfaces conflict errors", async () => {
    document.cookie = "XSRF-TOKEN=csrf-token";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(
        Response.json({ detail: "TASK_ALREADY_CLAIMED" }, { status: 409 }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(claimTask(5)).rejects.toMatchObject({
      name: "TaskApiError",
      status: 409,
      message: "TASK_ALREADY_CLAIMED",
    });
  });
});
