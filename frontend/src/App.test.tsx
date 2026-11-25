import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import App from "./App";
import { useAuth } from "./features/auth/useAuth";

vi.mock("./features/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("./features/auth/AdminLoginForm", () => ({
  AdminLoginForm: () => <div>Form đăng nhập Admin</div>,
}));

vi.mock("./features/admin-services/AdminServicesPage", () => ({
  AdminServicesPage: () => <div>Trang quản lý dịch vụ</div>,
}));

vi.mock("./features/service-requests/ServiceRequestsPage", () => ({
  ServiceRequestsPage: () => <div>Trang yêu cầu dịch vụ</div>,
}));

vi.mock("./features/auth/authApi", () => ({
  registerCustomer: vi.fn(),
}));

vi.mock("./features/service-catalog/serviceApi", () => ({
  getPublishedServices: vi.fn().mockResolvedValue([]),
}));

const baseAuthState = {
  admin: null,
  customer: null,
  isCheckingSession: false,
  error: null,
  login: vi.fn(),
  logout: vi.fn(),
  refreshSession: vi.fn(),
};

describe("admin service route access", () => {
  beforeEach(() => {
    window.history.replaceState({}, "", "/admin/services");
    vi.mocked(useAuth).mockReturnValue(baseAuthState);
  });

  it("requires a signed-in session", () => {
    render(<App />);
    expect(screen.getByText("Form đăng nhập Admin")).toBeInTheDocument();
    expect(screen.queryByText("Trang quản lý dịch vụ")).not.toBeInTheDocument();
  });

  it("denies authenticated users without ROLE_ADMIN", () => {
    vi.mocked(useAuth).mockReturnValue({
      ...baseAuthState,
      admin: { username: "staff", authorities: ["ROLE_USER"] },
    });

    render(<App />);
    expect(screen.getByText("Tài khoản hiện tại không có quyền Admin.")).toBeInTheDocument();
    expect(screen.queryByText("Trang quản lý dịch vụ")).not.toBeInTheDocument();
  });

  it("renders the service page for ROLE_ADMIN", () => {
    vi.mocked(useAuth).mockReturnValue({
      ...baseAuthState,
      admin: { username: "admin", authorities: ["ROLE_ADMIN"] },
    });

    render(<App />);
    expect(screen.getByText("Trang quản lý dịch vụ")).toBeInTheDocument();
  });

  it("renders the Service Request page for ROLE_ADMIN", () => {
    window.history.replaceState({}, "", "/admin/service-requests");
    vi.mocked(useAuth).mockReturnValue({
      ...baseAuthState,
      admin: { username: "admin", authorities: ["ROLE_ADMIN"] },
    });

    render(<App />);
    expect(screen.getByText("Trang yêu cầu dịch vụ")).toBeInTheDocument();
  });
});

describe("guest and login shell separation", () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue(baseAuthState);
  });

  it("renders the guest home without login form or role shell", () => {
    window.history.replaceState({}, "", "/");
    render(<App />);
    expect(screen.getByRole("heading", { name: /Chăm sóc thú cưng/ })).toBeInTheDocument();
    expect(screen.queryByText("Form đăng nhập Admin")).not.toBeInTheDocument();
  });

  it("renders a separate login route", () => {
    window.history.replaceState({}, "", "/login");
    render(<App />);
    expect(screen.getByText("Form đăng nhập Admin")).toBeInTheDocument();
  });

  it("keeps the public services route in the Guest shell", () => {
    window.history.replaceState({}, "", "/services");
    render(<App />);

    expect(
      screen.getByRole("heading", { name: "Khám phá dịch vụ" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Dịch vụ & giá" }),
    ).toHaveAttribute("aria-current", "page");
    expect(
      document.querySelector(".customer-shell"),
    ).not.toBeInTheDocument();
  });

  it("renders the Customer registration form", () => {
    window.history.replaceState({}, "", "/register");
    render(<App />);
    expect(
      screen.getByRole("heading", { name: "Đăng ký tài khoản Customer" }),
    ).toBeInTheDocument();
  });

  it("moves an Admin session from guest route to the Admin role shell", async () => {
    window.history.replaceState({}, "", "/");
    vi.mocked(useAuth).mockReturnValue({
      ...baseAuthState,
      admin: { username: "admin", authorities: ["ROLE_ADMIN"] },
    });

    render(<App />);
    await waitFor(() => {
      expect(window.location.pathname).toBe("/admin/services");
    });
    expect(screen.getByText("Trang quản lý dịch vụ")).toBeInTheDocument();
  });

  it("moves a Customer session into the customer area shell", async () => {
    window.history.replaceState({}, "", "/");
    vi.mocked(useAuth).mockReturnValue({
      ...baseAuthState,
      customer: {
        username: "customer@example.com",
        authorities: ["ROLE_CUSTOMER"],
      },
    });

    render(<App />);

    expect(
      screen.getByRole("heading", { name: "Chào mừng, customer@example.com" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Tài khoản" }),
    ).toHaveAttribute("href", "/customer/account");
    await waitFor(() => {
      expect(window.location.pathname).toBe("/customer/");
    });
  });

  it("renders customer section routes inside the customer shell", () => {
    window.history.replaceState({}, "", "/customer/account");
    vi.mocked(useAuth).mockReturnValue({
      ...baseAuthState,
      customer: {
        username: "customer@example.com",
        authorities: ["ROLE_CUSTOMER"],
      },
    });

    render(<App />);

    expect(
      screen.getByRole("heading", { name: "Thông tin khách hàng" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Tài khoản" }),
    ).toHaveAttribute("aria-current", "page");
  });

  it("keeps logout failure in the role shell and exposes its error", () => {
    window.history.replaceState({}, "", "/admin/services");
    vi.mocked(useAuth).mockReturnValue({
      ...baseAuthState,
      admin: { username: "admin", authorities: ["ROLE_ADMIN"] },
      error: "Đăng xuất không thành công.",
      logout: vi.fn().mockRejectedValue(new Error("logout failed")),
    });

    render(<App />);
    fireEvent.click(screen.getByRole("button", { name: "Tài khoản admin" }));
    expect(screen.getByText("Đăng xuất không thành công.")).toBeInTheDocument();
  });
});
