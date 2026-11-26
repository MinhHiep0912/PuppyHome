import {
  act,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AdminServicesPage } from "./AdminServicesPage";
import {
  AdminServiceApiError,
  createAdminService,
  getAdminServices,
  softDeleteAdminService,
  updateAdminService,
  updateAdminServiceStatus,
} from "./adminServiceApi";
import type { AdminService } from "./adminService";

vi.mock("./adminServiceApi", () => ({
  AdminServiceApiError: class AdminServiceApiError extends Error {
    public readonly status: number;

    constructor(message: string, status: number) {
      super(message);
      this.name = "AdminServiceApiError";
      this.status = status;
    }
  },
  createAdminService: vi.fn(),
  getAdminServices: vi.fn(),
  softDeleteAdminService: vi.fn(),
  updateAdminService: vi.fn(),
  updateAdminServiceStatus: vi.fn(),
}));

const draftService: AdminService = {
  id: 1,
  name: "Chăm sóc thú cưng",
  slug: "pet-care",
  shortDescription: "Mô tả ngắn",
  description: "Thông tin chi tiết",
  imageUrl: null,
  status: "DRAFT",
  sortOrder: 1,
  priceAmount: null,
  priceCurrency: "VND",
  priceUnit: null,
};

const publishedService: AdminService = {
  ...draftService,
  status: "PUBLISHED",
};

describe("AdminServicesPage", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.mocked(getAdminServices).mockResolvedValue([]);
    vi.mocked(createAdminService).mockResolvedValue(draftService);
    vi.mocked(updateAdminService).mockResolvedValue(draftService);
    vi.mocked(updateAdminServiceStatus).mockResolvedValue(publishedService);
    vi.mocked(softDeleteAdminService).mockResolvedValue();
  });

  it("shows loading before the service list resolves", async () => {
    let resolveList!: (services: AdminService[]) => void;
    vi.mocked(getAdminServices).mockReturnValue(
      new Promise((resolve) => {
        resolveList = resolve;
      }),
    );

    render(<AdminServicesPage />);
    expect(
      screen.getByText("Đang tải danh sách dịch vụ quản trị..."),
    ).toBeInTheDocument();

    await act(async () => resolveList([]));
    expect(await screen.findByText("Chưa có dịch vụ")).toBeInTheDocument();
  });

  it("shows the empty state after a successful empty response", async () => {
    render(<AdminServicesPage />);
    expect(await screen.findByText("Chưa có dịch vụ")).toBeInTheDocument();
  });

  it("renders the service list before the create/edit form", async () => {
    vi.mocked(getAdminServices).mockResolvedValue([draftService]);
    render(<AdminServicesPage />);

    const listHeading = await screen.findByRole("heading", {
      name: "Danh sách dịch vụ",
    });
    const formHeading = screen.getByRole("heading", {
      name: "Tạo dịch vụ",
    });

    expect(
      listHeading.compareDocumentPosition(formHeading) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).not.toBe(0);
  });

  it("shows load errors and supports retry", async () => {
    vi.mocked(getAdminServices)
      .mockRejectedValueOnce(new Error("Máy chủ tạm thời không khả dụng"))
      .mockResolvedValueOnce([]);

    render(<AdminServicesPage />);

    expect(
      await screen.findByText("Máy chủ tạm thời không khả dụng"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Thử tải lại" }));
    expect(await screen.findByText("Chưa có dịch vụ")).toBeInTheDocument();
    expect(getAdminServices).toHaveBeenCalledTimes(2);
  });

  it("shows no-permission state for an unauthorized API response", async () => {
    vi.mocked(getAdminServices).mockRejectedValue(
      new AdminServiceApiError("Không được phép", 403),
    );

    render(<AdminServicesPage />);

    expect(
      await screen.findByRole("heading", { name: "Không có quyền truy cập" }),
    ).toBeInTheDocument();
  });

  it("creates, edits, publishes, then soft-deletes without removing the row", async () => {
    vi.mocked(getAdminServices).mockResolvedValue([draftService]);
    vi.mocked(createAdminService).mockResolvedValue({
      ...draftService,
      id: 2,
      name: "Dịch vụ mới",
      slug: "dich-vu-moi",
    });
    vi.mocked(updateAdminService).mockResolvedValue({
      ...draftService,
      name: "Dịch vụ đã sửa",
      slug: "dich-vu-da-sua",
    });
    vi.mocked(updateAdminServiceStatus).mockResolvedValue({
      ...draftService,
      name: "Dịch vụ đã sửa",
      slug: "dich-vu-da-sua",
      status: "PUBLISHED",
    });
    vi.spyOn(window, "confirm").mockReturnValue(true);

    render(<AdminServicesPage />);
    const nameInput = await screen.findByLabelText("Tên dịch vụ");

    fireEvent.change(nameInput, { target: { value: "Dịch vụ mới" } });
    fireEvent.change(screen.getByLabelText("Slug"), {
      target: { value: "dich-vu-moi" },
    });
    fireEvent.change(screen.getByLabelText("Giá (để trống nếu chưa có)"), {
      target: { value: "125500" },
    });
    fireEvent.change(screen.getByLabelText("Đơn vị dịch vụ (tùy chọn)"), {
      target: { value: "ngày" },
    });
    const priceInput = screen.getByLabelText("Giá (để trống nếu chưa có)");
    fireEvent.change(screen.getByLabelText("Đơn vị nhập giá"), {
      target: { value: "K" },
    });
    expect(priceInput).toHaveValue(125.5);
    fireEvent.change(screen.getByLabelText("Đơn vị nhập giá"), {
      target: { value: "VND" },
    });
    expect(priceInput).toHaveValue(125500);
    fireEvent.change(screen.getByLabelText("Đơn vị nhập giá"), {
      target: { value: "K" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Tạo dịch vụ" }));

    expect(
      await screen.findByText(/Đã tạo dịch vụ “Dịch vụ mới”/),
    ).toBeInTheDocument();
    expect(createAdminService).toHaveBeenCalledWith(
      expect.objectContaining({
        name: "Dịch vụ mới",
        slug: "dich-vu-moi",
        sortOrder: 0,
        priceAmount: 125500,
        priceCurrency: "VND",
        priceUnit: "ngày",
      }),
    );

    const initialRow = screen.getByRole("row", { name: /Chăm sóc thú cưng/ });
    fireEvent.click(within(initialRow).getByRole("button", { name: "Sửa" }));
    fireEvent.change(screen.getByLabelText("Tên dịch vụ"), {
      target: { value: "Dịch vụ đã sửa" },
    });
    fireEvent.change(screen.getByLabelText("Slug"), {
      target: { value: "dich-vu-da-sua" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));

    expect(
      await screen.findByText(/Đã cập nhật dịch vụ “Dịch vụ đã sửa”/),
    ).toBeInTheDocument();
    const updatedRow = screen.getByRole("row", { name: /Dịch vụ đã sửa/ });

    fireEvent.click(
      within(updatedRow).getByRole("button", { name: "Xuất bản" }),
    );
    expect(
      await screen.findByText(/đã chuyển “Dịch vụ đã sửa” sang PUBLISHED/i),
    ).toBeInTheDocument();
    expect(updateAdminServiceStatus).toHaveBeenCalledWith(1, "PUBLISHED");

    const publishedRow = screen.getByRole("row", { name: /Dịch vụ đã sửa/ });
    fireEvent.click(
      within(publishedRow).getByRole("button", { name: "Đưa về DRAFT" }),
    );
    expect(await screen.findByText(/bản ghi được giữ lại/)).toBeInTheDocument();
    expect(softDeleteAdminService).toHaveBeenCalledWith(1);
    expect(screen.getByRole("row", { name: /Dịch vụ đã sửa/ })).toBeInTheDocument();
    expect(window.confirm).toHaveBeenCalledWith(
      expect.stringContaining("không bị xóa vĩnh viễn"),
    );
  });

  it("shows mutation errors without a success message", async () => {
    vi.mocked(createAdminService).mockRejectedValue(
      new Error("Slug đã tồn tại (HTTP 409)"),
    );

    render(<AdminServicesPage />);
    await screen.findByText("Chưa có dịch vụ");
    fireEvent.change(screen.getByLabelText("Tên dịch vụ"), {
      target: { value: "Trùng slug" },
    });
    fireEvent.change(screen.getByLabelText("Slug"), {
      target: { value: "existing-slug" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Tạo dịch vụ" }));

    expect(await screen.findByRole("alert"))
      .toHaveTextContent("Slug đã tồn tại (HTTP 409)");
    expect(screen.queryByText(/Đã tạo dịch vụ/)).not.toBeInTheDocument();
  });
});
