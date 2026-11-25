import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getGameAccounts } from "../game-accounts/gameAccountApi";
import { getPublishedServices } from "../service-catalog/serviceApi";
import {
  cancelServiceRequest,
  createServiceRequest,
  getServiceRequests,
  updateServiceRequestStatus,
} from "./serviceRequestApi";
import type { ServiceRequest } from "./serviceRequest";
import { ServiceRequestsPage } from "./ServiceRequestsPage";

vi.mock("../game-accounts/gameAccountApi", () => ({
  getGameAccounts: vi.fn(),
}));

vi.mock("../service-catalog/serviceApi", () => ({
  getPublishedServices: vi.fn(),
}));

vi.mock("./serviceRequestApi", () => ({
  cancelServiceRequest: vi.fn(),
  createServiceRequest: vi.fn(),
  getServiceRequests: vi.fn(),
  updateServiceRequestStatus: vi.fn(),
}));

const request: ServiceRequest = {
  id: 1,
  customerId: 7,
  gameAccountId: 12,
  serviceId: 4,
  snapshotServiceName: "Chăm sóc",
  snapshotPriceAmount: 125000,
  snapshotCurrency: "VND",
  snapshotPriceUnit: "ngày",
  status: "NEW",
  requestedStartAt: null,
  startAt: "2026-11-25T10:00:00",
  endAt: "2026-12-25T10:00:00",
  durationDays: 30,
  notes: null,
  createdAt: "2026-11-25T10:00:00",
  updatedAt: "2026-11-25T10:00:00",
};

describe("ServiceRequestsPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(getServiceRequests).mockResolvedValue([request]);
    vi.mocked(getPublishedServices).mockResolvedValue([
      {
        id: 4,
        name: "Chăm sóc",
        slug: "cham-soc",
        shortDescription: null,
        description: null,
        imageUrl: null,
        sortOrder: 1,
        priceAmount: 125000,
        priceCurrency: "VND",
        priceUnit: "ngày",
      },
    ]);
    vi.mocked(getGameAccounts).mockResolvedValue([
      {
        id: 12,
        customerId: 7,
        name: "Tài khoản A",
        nocape: false,
        status: "ACTIVE",
        createdAt: "2026-11-25T10:00:00",
        updatedAt: "2026-11-25T10:00:00",
      },
    ]);
    vi.mocked(createServiceRequest).mockResolvedValue({
      ...request,
      id: 2,
    });
    vi.mocked(cancelServiceRequest).mockResolvedValue({
      ...request,
      status: "CANCELLED",
    });
    vi.mocked(updateServiceRequestStatus).mockResolvedValue({
      ...request,
      status: "IN_PROGRESS",
    });
  });

  it("creates a customer request from an owned active account and priced service", async () => {
    render(<ServiceRequestsPage isAdmin={false} />);

    expect(await screen.findByText("Chăm sóc")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Dịch vụ"), {
      target: { value: "4" },
    });
    fireEvent.change(screen.getByLabelText("Tài khoản game"), {
      target: { value: "12" },
    });
    fireEvent.change(screen.getByLabelText("Ghi chú (tùy chọn)"), {
      target: { value: "Xin liên hệ trước" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Gửi yêu cầu" }));

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Đã gửi yêu cầu dịch vụ.",
    );
    expect(createServiceRequest).toHaveBeenCalledWith({
      serviceId: 4,
      gameAccountId: 12,
      requestedStartAt: null,
      notes: "Xin liên hệ trước",
    });
  });

  it("allows customer cancellation only while NEW", async () => {
    render(<ServiceRequestsPage isAdmin={false} />);
    const row = await screen.findByRole("row", { name: /Chăm sóc/ });
    fireEvent.click(within(row).getByRole("button", { name: "Hủy" }));

    expect(await screen.findByText("Đã hủy")).toBeInTheDocument();
    expect(cancelServiceRequest).toHaveBeenCalledWith(1);
    expect(screen.queryByRole("button", { name: "Hủy" })).not.toBeInTheDocument();
  });

  it("shows only valid Admin transitions and applies the response", async () => {
    render(<ServiceRequestsPage isAdmin />);
    const row = await screen.findByRole("row", { name: /Chăm sóc/ });

    expect(within(row).getByRole("button", { name: "Đang thực hiện" }))
      .toBeInTheDocument();
    expect(within(row).getByRole("button", { name: "Đã hủy" }))
      .toBeInTheDocument();
    expect(within(row).queryByRole("button", { name: "Hoàn tất" }))
      .not.toBeInTheDocument();

    fireEvent.click(within(row).getByRole("button", { name: "Đang thực hiện" }));
    expect(await screen.findByText("Đã cập nhật trạng thái yêu cầu #1."))
      .toBeInTheDocument();
    expect(await within(row).findByRole("button", { name: "Hoàn tất" }))
      .toBeInTheDocument();
    expect(updateServiceRequestStatus).toHaveBeenCalledWith(1, "IN_PROGRESS");
    expect(getPublishedServices).not.toHaveBeenCalled();
    expect(getGameAccounts).not.toHaveBeenCalled();
  });

  it("shows API failures and offers retry", async () => {
    vi.mocked(getServiceRequests)
      .mockRejectedValueOnce(new Error("Không kết nối được"))
      .mockResolvedValueOnce([]);

    render(<ServiceRequestsPage isAdmin />);
    expect(await screen.findByText("Không kết nối được")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Thử tải lại" }));
    expect(await screen.findByText("Chưa có yêu cầu dịch vụ."))
      .toBeInTheDocument();
  });
});
