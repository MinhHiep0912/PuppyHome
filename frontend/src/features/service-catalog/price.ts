export type PriceDisplayUnit = "VND" | "K";

export function convertPriceInput(
  amount: string,
  from: PriceDisplayUnit,
  to: PriceDisplayUnit,
): string {
  if (!amount.trim() || from === to) {
    return amount;
  }

  const numericAmount = Number(amount);
  if (!Number.isFinite(numericAmount)) {
    return amount;
  }

  const converted = from === "VND"
    ? numericAmount / 1000
    : numericAmount * 1000;
  return String(Number(converted.toFixed(3)));
}

export function normalizePriceToVnd(
  amount: string,
  inputUnit: PriceDisplayUnit,
): number | null {
  if (!amount.trim()) {
    return null;
  }

  const numericAmount = Number(amount);
  if (!Number.isFinite(numericAmount) || numericAmount < 0) {
    throw new Error("Giá phải là số không âm.");
  }

  const converted = inputUnit === "K" ? numericAmount * 1000 : numericAmount;
  const rounded = Math.round(converted);
  if (Math.abs(converted - rounded) > 1e-8) {
    throw new Error("Giá quy đổi phải tương ứng với số nguyên VND.");
  }
  if (!Number.isSafeInteger(rounded) || rounded > 999999999999) {
    throw new Error("Giá quy đổi vượt quá giới hạn 12 chữ số VND.");
  }
  return rounded;
}

export function formatServicePrice(
  amount: number | null,
  currency: string | null,
  unit: string | null,
  displayUnit: PriceDisplayUnit,
): string {
  if (amount === null) {
    return "Chưa cập nhật giá";
  }

  const normalizedCurrency = currency ?? "VND";
  const displayAmount = displayUnit === "K" && normalizedCurrency === "VND"
    ? `${new Intl.NumberFormat("vi-VN", {
        maximumFractionDigits: 3,
      }).format(amount / 1000)}k`
    : `${new Intl.NumberFormat("vi-VN").format(amount)} ${normalizedCurrency}`;

  return unit ? `${displayAmount} / ${unit}` : displayAmount;
}
