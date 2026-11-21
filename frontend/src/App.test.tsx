import { render, screen } from "@testing-library/react";
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

vi.mock("./features/service-catalog/serviceApi", () => ({
  getPublishedServices: vi.fn().mockResolvedValue([]),
}));

const baseAuthState = {
  admin: null,
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
});
