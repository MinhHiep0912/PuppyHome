import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getPublishedServices } from "../service-catalog/serviceApi";
import { GuestServicesPage } from "./GuestPages";

vi.mock("../service-catalog/serviceApi", () => ({
  getPublishedServices: vi.fn(),
}));

describe("GuestServicesPage", () => {
  beforeEach(() => {
    vi.mocked(getPublishedServices).mockResolvedValue([
      {
        id: 1,
        name: "Chăm sóc",
        slug: "cham-soc",
        shortDescription: "Dịch vụ chăm sóc.",
        description: null,
        imageUrl: null,
        sortOrder: 1,
        priceAmount: 150000,
        priceCurrency: "VND",
        priceUnit: "ngày",
      },
    ]);
  });

  it("shows service price to guests and converts VND to k on selection", async () => {
    render(<GuestServicesPage />);

    expect(await screen.findByText("150k / ngày")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "VND" }));
    expect(screen.getByText("150.000 VND / ngày")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Nghìn (k)" }));
    expect(screen.getByText("150k / ngày")).toBeInTheDocument();
  });

  it("renders API failures as a retryable error badge", async () => {
    vi.mocked(getPublishedServices)
      .mockRejectedValueOnce(new Error("Không tải được dịch vụ"))
      .mockResolvedValueOnce([
        {
          id: 1,
          name: "Chăm sóc",
          slug: "cham-soc",
          shortDescription: null,
          description: null,
          imageUrl: null,
          sortOrder: 1,
          priceAmount: 150000,
          priceCurrency: "VND",
          priceUnit: null,
        },
      ]);
    render(<GuestServicesPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Không tải được dịch vụ",
    );
    fireEvent.click(screen.getByRole("button", { name: "Thử tải lại" }));
    expect(await screen.findByText("150k")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
