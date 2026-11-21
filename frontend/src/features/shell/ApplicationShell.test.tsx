import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ApplicationShell } from "./ApplicationShell";

describe("ApplicationShell", () => {
  it("shows session data, verified Admin navigation, and logout state", () => {
    const onLogout = vi.fn();
    render(
      <ApplicationShell
        admin={{
          username: "admin",
          authorities: ["ROLE_ADMIN"],
        }}
        isLoggingOut={false}
        logoutError={null}
        onLogout={onLogout}
      >
        <div>Shell content</div>
      </ApplicationShell>,
    );

    expect(screen.getByText("Shell content")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /Dịch vụ & giá/i }),
    ).toHaveAttribute("aria-current", "page");
    expect(
      screen.getByText("Chức năng khác — chờ tích hợp"),
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: "Tài khoản admin" }),
    );
    expect(screen.getByText("Tài khoản: admin")).toBeInTheDocument();
    expect(screen.getByText("Quyền: ROLE_ADMIN")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("menuitem", { name: "Đăng xuất" }));
    expect(onLogout).toHaveBeenCalledTimes(1);
  });

  it("keeps logout errors visible and supports Escape closing the profile", () => {
    render(
      <ApplicationShell
        admin={{
          username: "admin",
          authorities: ["ROLE_ADMIN"],
        }}
        isLoggingOut={false}
        logoutError="Phiên vẫn còn do máy chủ từ chối đăng xuất."
        onLogout={vi.fn()}
      >
        <div>Shell content</div>
      </ApplicationShell>,
    );

    fireEvent.click(
      screen.getByRole("button", { name: "Tài khoản admin" }),
    );
    expect(
      screen.getByRole("alert"),
    ).toHaveTextContent("Phiên vẫn còn");

    fireEvent.keyDown(document, { key: "Escape" });
    expect(
      screen.queryByText("Tài khoản: admin"),
    ).not.toBeInTheDocument();
  });
});
