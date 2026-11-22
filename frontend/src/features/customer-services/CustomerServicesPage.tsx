import { useEffect, useState } from "react";
import type { Service } from "../service-catalog/service";
import { getPublishedServices } from "../service-catalog/serviceApi";
import "./CustomerServicesPage.css";

export function CustomerServicesPage() {
  const [services, setServices] = useState<Service[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
  }, []);

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
        <div className="customer-services__state customer-services__state--error" role="alert">
          {error}
        </div>
      )}

      {!isLoading && !error && services.length === 0 && (
        <div className="customer-services__state">
          <h2>Chưa có dịch vụ công khai</h2>
          <p>Dịch vụ sẽ xuất hiện tại đây khi được công bố.</p>
        </div>
      )}

      {!isLoading && !error && services.length > 0 && (
        <>
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
                  <h2>{service.name}</h2>
                  <p>
                    {service.shortDescription ||
                      service.description ||
                      "Thông tin đang được cập nhật."}
                  </p>
                </div>
              </article>
            ))}
          </div>
          <p className="customer-services__notice">
            Tính năng gửi yêu cầu dịch vụ sẽ có sau khi hoàn tất luồng đặt dịch vụ.
          </p>
        </>
      )}
    </section>
  );
}
