import { afterEach, describe, expect, it, vi } from "vitest";
import { createEmployee, updateEmployee } from "./employeeApi";

const employeePayload = {
  id: 3,
  loginIdentifier: "NV-01",
  employeeCode: "NV-01",
  displayName: "Nhân viên A",
  workStatus: "ACTIVE",
  createdAt: "2026-01-01T00:00:00",
  updatedAt: "2026-01-01T00:00:00",
};

describe("employeeApi", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = "XSRF-TOKEN=; Max-Age=0";
  });

  it("creates an employee with the CSRF header", async () => {
    document.cookie = "XSRF-TOKEN=csrf-token";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(Response.json(employeePayload, { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);

    await createEmployee({
      employeeCode: "NV-01",
      displayName: "Nhân viên A",
      workStatus: "ACTIVE",
      password: "MatKhau123",
    });

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      "/api/auth/csrf",
      expect.objectContaining({ method: "GET" }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/admin/employees",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "X-XSRF-TOKEN": "csrf-token",
        }),
      }),
    );
  });

  it("updates an employee via PUT", async () => {
    document.cookie = "XSRF-TOKEN=csrf-token";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(Response.json(employeePayload));
    vi.stubGlobal("fetch", fetchMock);

    await updateEmployee(3, { workStatus: "INACTIVE" });

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/admin/employees/3",
      expect.objectContaining({ method: "PUT" }),
    );
  });

  it("surfaces API errors with status", async () => {
    document.cookie = "XSRF-TOKEN=csrf-token";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(
        Response.json({ title: "EMPLOYEE_CODE_EXISTS" }, { status: 409 }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      createEmployee({
        employeeCode: "NV-01",
        displayName: "Nhân viên A",
        password: "MatKhau123",
      }),
    ).rejects.toMatchObject({
      name: "EmployeeApiError",
      status: 409,
      message: "EMPLOYEE_CODE_EXISTS",
    });
  });
});
