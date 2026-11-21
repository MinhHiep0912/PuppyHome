import { afterEach, describe, expect, it, vi } from "vitest";
import {
  createAdminService,
  softDeleteAdminService,
} from "./adminServiceApi";

describe("adminServiceApi", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = "XSRF-TOKEN=; Max-Age=0";
  });

  it("sends the CSRF cookie value in write request headers", async () => {
    document.cookie = "XSRF-TOKEN=csrf-token";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(
        Response.json({
          id: 7,
          name: "Dịch vụ",
          slug: "dich-vu",
          shortDescription: null,
          description: null,
          imageUrl: null,
          status: "DRAFT",
          sortOrder: 0,
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await createAdminService({
      name: "Dịch vụ",
      slug: "dich-vu",
      shortDescription: null,
      description: null,
      imageUrl: null,
      sortOrder: 0,
    });

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      "/api/auth/csrf",
      expect.objectContaining({ method: "GET" }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/admin/services",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "X-XSRF-TOKEN": "csrf-token",
        }),
      }),
    );
  });

  it("uses DELETE for soft delete and accepts the 204 response", async () => {
    document.cookie = "XSRF-TOKEN=csrf-token";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(softDeleteAdminService(13)).resolves.toBeUndefined();
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/admin/services/13",
      expect.objectContaining({ method: "DELETE" }),
    );
  });

  it("surfaces API errors with the HTTP status for permission handling", async () => {
    document.cookie = "XSRF-TOKEN=csrf-token";
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(
        Response.json(
          { detail: "Slug đã tồn tại" },
          { status: 409 },
        ),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      createAdminService({
        name: "Dịch vụ",
        slug: "trung",
        shortDescription: null,
        description: null,
        imageUrl: null,
        sortOrder: 0,
      }),
    ).rejects.toMatchObject({
      name: "AdminServiceApiError",
      status: 409,
      message: "Slug đã tồn tại",
    });
  });
});
