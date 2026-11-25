import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getPublishedServices } from "../service-catalog/serviceApi";
import { CustomerServicesPage } from "./CustomerServicesPage";

vi.mock("../service-catalog/serviceApi", () => ({
  getPublishedServices: vi.fn(),
}));

describe("CustomerServicesPage", () => {
  beforeEach(() => {
    vi.mocked(getPublishedServices).mockResolvedValue([
      {
        id: 1,
        name: "Dịch vụ Logacc",
        slug: "dich-vu-logacc",
        shortDescription: "Dịch vụ hỗ trợ tài khoản.",
        description: null,
        imageUrl: null,
        sortOrder: 1,
        priceAmount: 120000,
        priceCurrency: "VND",
        priceUnit: "ngày",
      },
      {
        id: 2,
        name: "Chăm sóc thú cưng",
        slug: "cham-soc-thu-cung",
        shortDescription: "Dịch vụ chăm sóc.",
        description: null,
        imageUrl: null,
        sortOrder: 2,
        priceAmount: null,
        priceCurrency: "VND",
        priceUnit: null,
      },
    ]);
  });

  it("renders a spaced Customer-only catalog without a login CTA", async () => {
    const { container } = render(<CustomerServicesPage />);

    expect(await screen.findByText("Dịch vụ Logacc")).toBeInTheDocument();
    expect(screen.getByText("Chăm sóc thú cưng")).toBeInTheDocument();
    expect(container.querySelector(".customer-services__grid")).not.toBeNull();
    expect(container.querySelectorAll(".customer-service-card")).toHaveLength(2);
    expect(screen.getByRole("link", { name: "Gửi yêu cầu dịch vụ" }))
      .toHaveAttribute("href", "/customer/service-requests");
    expect(
      screen.queryByRole("link", { name: /đăng nhập để tiếp tục/i }),
    ).not.toBeInTheDocument();
  });
});
