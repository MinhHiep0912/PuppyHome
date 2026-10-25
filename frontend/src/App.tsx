import { useEffect, useState } from "react";
import type { Service } from "./features/service-catalog/service";
import { getPublishedServices } from "./features/service-catalog/serviceApi";
import "./App.css";

function App() {
  const [services, setServices] = useState<Service[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadServices() {
      setIsLoading(true);
      setError(null);

      try {
        const data = await getPublishedServices();

        if (!cancelled) {
          setServices(data);
        }
      } catch (caughtError: unknown) {
        if (!cancelled) {
          const message =
            caughtError instanceof Error
              ? caughtError.message.includes("fetch")
                ? "Không thể kết nối đến máy chủ. Hãy kiểm tra backend và Vite proxy."
                : caughtError.message
              : "Đã xảy ra lỗi không xác định.";

          setError(message);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadServices();

    return () => {
      cancelled = true;
    };
  }, [retryKey]);

  return (
    <div className="app">
      <header className="site-header">
        <a className="brand" href="/" aria-label="Puppy's Home - Trang chủ">
          <span className="brand__mark">PH</span>

          <span className="brand__name">
            Puppy&apos;s Home
            <small>Không gian dịch vụ của bạn</small>
          </span>
        </a>

        <span className="site-header__tagline">Danh mục dịch vụ</span>
      </header>

      <main className="main-content">
        <section className="intro">
          <p className="eyebrow">PUPPY&apos;S HOME</p>

          <h1>Khám phá các dịch vụ</h1>

          <p className="intro__description">
            Tìm hiểu các dịch vụ đang được cung cấp và lựa chọn thông tin phù
            hợp với nhu cầu của bạn.
          </p>
        </section>

        <section
          className="services-section"
          aria-labelledby="services-heading"
        >
          <div className="section-heading">
            <div>
              <h2 id="services-heading">Danh sách dịch vụ</h2>
              <p>Thông tin được tải trực tiếp từ hệ thống.</p>
            </div>

            {!isLoading && !error && (
              <span className="service-count">{services.length} dịch vụ</span>
            )}
          </div>

          {isLoading && (
            <div className="state-panel" role="status">
              <span className="spinner" aria-hidden="true" />
              <p>Đang tải danh sách dịch vụ...</p>
            </div>
          )}

          {!isLoading && error && (
            <div className="state-panel state-panel--error" role="alert">
              <h3>Chưa thể tải danh sách dịch vụ</h3>
              <p>{error}</p>

              <button
                className="retry-button"
                type="button"
                onClick={() => setRetryKey((current) => current + 1)}
              >
                Thử tải lại
              </button>
            </div>
          )}

          {!isLoading && !error && services.length === 0 && (
            <div className="state-panel">
              <h3>Chưa có dịch vụ được xuất bản</h3>
              <p>Danh sách sẽ hiển thị khi có dịch vụ được xuất bản.</p>
            </div>
          )}

          {!isLoading && !error && services.length > 0 && (
            <div className="services-grid">
              {services.map((service) => (
                <article className="service-card" key={service.id}>
                  {service.imageUrl ? (
                    <img
                      className="service-card__image"
                      src={service.imageUrl}
                      alt={service.name}
                      loading="lazy"
                    />
                  ) : (
                    <div
                      className="service-card__placeholder"
                      aria-hidden="true"
                    >
                      <span>PH</span>
                    </div>
                  )}

                  <div className="service-card__content">
                    <span className="service-card__label">DỊCH VỤ</span>

                    <h3>{service.name}</h3>

                    <p>
                      {service.shortDescription ||
                        service.description ||
                        "Thông tin dịch vụ đang được cập nhật."}
                    </p>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>

      <footer className="site-footer">
        <p>© {new Date().getFullYear()} Puppy&apos;s Home</p>
      </footer>
    </div>
  );
}

export default App;
