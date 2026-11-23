import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { GameAccount } from "./gameAccount";
import {
  archiveGameAccount,
  createGameAccount,
  getGameAccounts,
  setGameAccountStatus,
  updateGameAccount,
} from "./gameAccountApi";
import { GameAccountsPage } from "./GameAccountsPage";

vi.mock("./gameAccountApi", () => ({
  getGameAccounts: vi.fn(),
  createGameAccount: vi.fn(),
  updateGameAccount: vi.fn(),
  archiveGameAccount: vi.fn(),
  setGameAccountStatus: vi.fn(),
}));

const activeAccount: GameAccount = {
  id: 1,
  customerId: 10,
  name: "Tài khoản chính",
  nocape: false,
  status: "ACTIVE",
  createdAt: "2026-11-22T00:00:00",
  updatedAt: "2026-11-22T00:00:00",
};

const archivedAccount: GameAccount = {
  id: 2,
  customerId: 10,
  name: "Tài khoản cũ",
  nocape: true,
  status: "ARCHIVED",
  createdAt: "2026-11-21T00:00:00",
  updatedAt: "2026-11-21T00:00:00",
};

describe("GameAccountsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getGameAccounts).mockResolvedValue([activeAccount, archivedAccount]);
    vi.mocked(createGameAccount).mockResolvedValue(activeAccount);
    vi.mocked(updateGameAccount).mockResolvedValue(activeAccount);
    vi.mocked(archiveGameAccount).mockResolvedValue(undefined);
    vi.mocked(setGameAccountStatus).mockResolvedValue({
      ...archivedAccount,
      status: "ACTIVE",
    });
  });

  it("shows loading then lists active and archived accounts", async () => {
    render(<GameAccountsPage />);

    expect(screen.getByRole("status")).toHaveTextContent(
      "Đang tải tài khoản game...",
    );

    expect(await screen.findByText("Tài khoản chính")).toBeInTheDocument();
    expect(screen.getByText("Tài khoản cũ")).toBeInTheDocument();
    expect(screen.getByText("Đang hoạt động")).toBeInTheDocument();
    expect(screen.getByText("Đã lưu trữ")).toBeInTheDocument();
  });

  it("shows empty state when customer has no accounts", async () => {
    vi.mocked(getGameAccounts).mockResolvedValue([]);

    render(<GameAccountsPage />);

    expect(
      await screen.findByText("Chưa có tài khoản game"),
    ).toBeInTheDocument();
  });

  it("shows error state when loading fails", async () => {
    vi.mocked(getGameAccounts).mockRejectedValue(
      new Error("Không có quyền truy cập."),
    );

    render(<GameAccountsPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Không có quyền truy cập.",
    );
  });

  it("creates an account through the form", async () => {
    render(<GameAccountsPage />);
    await screen.findByText("Tài khoản chính");

    fireEvent.change(screen.getByLabelText(/Tên tài khoản/), {
      target: { value: "Account mới" },
    });
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(screen.getByRole("button", { name: "Tạo tài khoản" }));

    await waitFor(() => {
      expect(createGameAccount).toHaveBeenCalledWith({
        name: "Account mới",
        nocape: true,
      });
    });

    expect(await screen.findByRole("status")).toHaveTextContent(
      "Đã tạo tài khoản game.",
    );
    expect(getGameAccounts).toHaveBeenCalledTimes(2);
  });

  it("shows validation error for blank name without calling API", async () => {
    render(<GameAccountsPage />);
    await screen.findByText("Tài khoản chính");

    fireEvent.change(screen.getByLabelText(/Tên tài khoản/), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Tạo tài khoản" }));

    expect(
      await screen.findByText("Tên tài khoản không được để trống."),
    ).toBeInTheDocument();
    expect(createGameAccount).not.toHaveBeenCalled();
  });

  it("edits an existing account", async () => {
    render(<GameAccountsPage />);
    await screen.findByText("Tài khoản chính");

    const editButtons = screen.getAllByRole("button", { name: "Chỉnh sửa" });
    fireEvent.click(editButtons[0]);

    const nameInput = screen.getByLabelText(/Tên tài khoản/);
    expect(nameInput).toHaveValue("Tài khoản chính");

    fireEvent.change(nameInput, { target: { value: "Tên đã sửa" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));

    await waitFor(() => {
      expect(updateGameAccount).toHaveBeenCalledWith(1, {
        name: "Tên đã sửa",
        nocape: false,
      });
    });
  });

  it("archives an active account", async () => {
    render(<GameAccountsPage />);
    await screen.findByText("Tài khoản chính");

    fireEvent.click(screen.getByRole("button", { name: "Lưu trữ" }));

    await waitFor(() => {
      expect(archiveGameAccount).toHaveBeenCalledWith(1);
    });
    expect(await screen.findByRole("status")).toHaveTextContent(
      'Đã lưu trữ "Tài khoản chính".',
    );
  });

  it("restores an archived account", async () => {
    render(<GameAccountsPage />);
    await screen.findByText("Tài khoản cũ");

    fireEvent.click(screen.getByRole("button", { name: "Khôi phục" }));

    await waitFor(() => {
      expect(setGameAccountStatus).toHaveBeenCalledWith(2, "ACTIVE");
    });
    expect(await screen.findByRole("status")).toHaveTextContent(
      'Đã khôi phục "Tài khoản cũ".',
    );
  });

  it("surfaces API errors during mutations", async () => {
    vi.mocked(createGameAccount).mockRejectedValue(
      new Error("Tên tài khoản không được để trống."),
    );

    render(<GameAccountsPage />);
    await screen.findByText("Tài khoản chính");

    fireEvent.change(screen.getByLabelText(/Tên tài khoản/), {
      target: { value: "Account lỗi" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Tạo tài khoản" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Tên tài khoản không được để trống.",
    );
  });
});
