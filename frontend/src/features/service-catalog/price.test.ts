import { describe, expect, it } from "vitest";
import {
  convertPriceInput,
  formatServicePrice,
  normalizePriceToVnd,
} from "./price";

describe("service price units", () => {
  it("formats canonical VND as VND or thousands without changing the amount", () => {
    expect(formatServicePrice(150000, "VND", null, "K")).toBe("150k");
    expect(formatServicePrice(1500000, "VND", null, "K")).toBe("1.500k");
    expect(formatServicePrice(150000, "VND", "ngày", "VND"))
      .toBe("150.000 VND / ngày");
    expect(formatServicePrice(null, "VND", null, "K"))
      .toBe("Chưa cập nhật giá");
  });

  it("converts the Admin input unit while preserving whole-VND precision", () => {
    expect(convertPriceInput("150000", "VND", "K")).toBe("150");
    expect(convertPriceInput("150", "K", "VND")).toBe("150000");
    expect(normalizePriceToVnd("150", "K")).toBe(150000);
    expect(() => normalizePriceToVnd("0.0001", "K"))
      .toThrow("Giá quy đổi phải tương ứng với số nguyên VND.");
  });
});
