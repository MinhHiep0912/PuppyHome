import type { PriceDisplayUnit } from "./price";
import "./PriceDisplayUnitSelector.css";

interface PriceDisplayUnitSelectorProps {
  value: PriceDisplayUnit;
  onChange: (value: PriceDisplayUnit) => void;
}

export function PriceDisplayUnitSelector({
  value,
  onChange,
}: PriceDisplayUnitSelectorProps) {
  return (
    <div className="price-unit-selector" role="group" aria-label="Đơn vị hiển thị giá">
      <span>Hiển thị giá:</span>
      {(["VND", "K"] as const).map((unit) => (
        <button
          key={unit}
          type="button"
          aria-pressed={value === unit}
          onClick={() => onChange(unit)}
        >
          {unit === "K" ? "Nghìn (k)" : "VND"}
        </button>
      ))}
    </div>
  );
}
