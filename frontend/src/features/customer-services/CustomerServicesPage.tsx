import { useEffect, useState } from "react";
import type { Service } from "../service-catalog/service";
import { getPublishedServices } from "../service-catalog/serviceApi";
import { PriceDisplayUnitSelector } from "../service-catalog/PriceDisplayUnitSelector";
import { formatServicePrice, type PriceDisplayUnit } from "../service-catalog/price";
import { ErrorBadge } from "../shared/ErrorBadge";
import "./CustomerServicesPage.css";

export function CustomerServicesPage() {
  const [services, setServices] = useState<Service[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const [displayUnit, setDisplayUnit] = useState<PriceDisplayUnit>("K");

  useEffect(() => {
    let cancelled = false;

    void getPublishedServices()
      .then((data) => {
        if (!cancelled) {
          setServices(data);
        }
      })
      .catch((caughtError: unknown) => {
        if (!cancelled) {
          setError(
            caughtError instanceof Error
              ? caughtError.message
              : "Không thể tải danh sách dịch vụ.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [retryKey]);

  function retryLoading(): void {
    setIsLoading(true);
    setError(null);
    setRetryKey((current) => current + 1);
  }

  return (
    <section
      className="customer-services"
      aria-labelledby="customer-services-heading"
    >
      <header className="customer-services__heading">
        <p className="customer-services__eyebrow">DỊCH VỤ &amp; GIÁ</p>
        <h1 id="customer-services-heading">Khám phá dịch vụ</h1>
        <p>Danh sách dịch vụ đang được cung cấp.</p>
      </header>

      {isLoading && (
        <div className="customer-services__state" role="status">
          Đang tải dịch vụ...
        </div>
      )}

      {!isLoading && error && (
        <ErrorBadge message={error}>
          <button type="button" onClick={retryLoading}>
            Thử tải lại
          </button>
        </ErrorBadge>
      )}

      {!isLoading && !error && services.length === 0 && (
        <div className="customer-services__state">
          <h2>Chưa có dịch vụ công khai</h2>
          <p>Dịch vụ sẽ xuất hiện tại đây khi được công bố.</p>
        </div>
      )}

      {!isLoading && !error && services.length > 0 && (
        <>
          <PriceDisplayUnitSelector value={displayUnit} onChange={setDisplayUnit} />
          <div className="customer-services__grid">
            {services.map((service) => (
              <article className="customer-service-card" key={service.id}>
                {service.imageUrl ? (
                  <img
                    className="customer-service-card__image"
                    src={service.imageUrl}
                    alt={service.name}
                    loading="lazy"
                  />
                ) : (
                  <div
                    className="customer-service-card__placeholder"
                    aria-hidden="true"
                  >
                    PH
                  </div>
                )}
                <div className="customer-service-card__content">
                  <span className="customer-service-card__eyebrow">DỊCH VỤ</span>
                  <h2 title={service.name}>{service.name}</h2>
                  <p className="customer-service-card__description">
                    {service.shortDescription ||
                      service.description ||
                      "Thông tin đang được cập nhật."}
                  </p>
                  {service.priceAmount !== null && (
                    <p className="customer-service-card__price">
                      {formatServicePrice(
                        service.priceAmount,
                        service.priceCurrency,
                        service.priceUnit,
                        displayUnit,
                      )}
                    </p>
                  )}
                  {service.priceAmount === null && (
                    <p className="customer-service-card__price">
                      {formatServicePrice(
                        null,
                        service.priceCurrency,
                        service.priceUnit,
                        displayUnit,
                      )}
                    </p>
                  )}
                </div>
              </article>
            ))}
          </div>
          <a
            className="customer-services__floating-cta"
            href="/customer/service-requests"
          >
            Gửi yêu cầu dịch vụ
          </a>
        </>
      )}
    </section>
  );
}
