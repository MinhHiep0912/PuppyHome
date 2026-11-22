import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CustomerShell } from "./CustomerShell";

describe("CustomerShell", () => {
  it("provides customer navigation and a working logout action", () => {
    const onLogout = vi.fn();

    render(
      <CustomerShell
        customer={{
          username: "customer@example.com",
          authorities: ["ROLE_CUSTOMER"],
        }}
        activePage="overview"
        isLoggingOut={false}
        logoutError={null}
        onLogout={onLogout}
      >
        <div>Customer content</div>
      </CustomerShell>,
    );

    expect(screen.getAllByText("Khu vực khách hàng")).toHaveLength(2);
    expect(screen.getByText("Customer content")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Dịch vụ & giá" }),
    ).toHaveAttribute("href", "/customer/services");
    expect(
      screen.getByRole("link", { name: "Tin tức" }),
    ).toHaveAttribute("href", "/customer/news");
    expect(
      screen.getByRole("link", { name: "Tổng quan" }),
    ).toHaveAttribute("aria-current", "page");

    fireEvent.click(
      screen.getByRole("button", { name: "Tài khoản customer@example.com" }),
    );
    fireEvent.click(screen.getByRole("menuitem", { name: "Đăng xuất" }));

    expect(onLogout).toHaveBeenCalledOnce();
  });

  it("shows logout errors in the customer profile menu", () => {
    render(
      <CustomerShell
        customer={{
          username: "customer@example.com",
          authorities: ["ROLE_CUSTOMER"],
        }}
        activePage="account"
        isLoggingOut={false}
        logoutError="Không thể đăng xuất."
        onLogout={vi.fn()}
      >
        <div>Account content</div>
      </CustomerShell>,
    );

    fireEvent.click(
      screen.getByRole("button", { name: "Tài khoản customer@example.com" }),
    );
    expect(screen.getByRole("alert")).toHaveTextContent("Không thể đăng xuất.");
  });
});
