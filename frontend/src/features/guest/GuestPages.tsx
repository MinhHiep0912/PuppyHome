import { useEffect, useState } from "react";
import type { Service } from "../service-catalog/service";
import { getPublishedServices } from "../service-catalog/serviceApi";
import "./GuestPages.css";

export function GuestHomePage() {
  return (
    <div className="guest-home">
      <section className="guest-hero">
        <p className="guest-eyebrow">PUPPY&apos;S HOME</p>
        <h1>Chăm sóc thú cưng, dễ dàng và tận tâm</h1>
        <p>
          Khám phá dịch vụ, theo dõi thông tin và chuẩn bị trải nghiệm phù hợp
          với bạn bè bốn chân.
        </p>
        <div className="guest-hero__actions">
          <a className="guest-primary-action" href="/services">Xem dịch vụ</a>
          <a className="guest-secondary-action" href="/news">Xem tin tức</a>
        </div>
      </section>

      <section className="guest-feature-grid" aria-label="Điểm nổi bật">
        <article><span>01</span><h2>Dịch vụ minh bạch</h2><p>Thông tin công khai được lấy từ hệ thống.</p></article>
        <article><span>02</span><h2>Luồng rõ ràng</h2><p>Giữ nội dung guest tách khỏi khu vực quản trị.</p></article>
        <article><span>03</span><h2>Trải nghiệm nhất quán</h2><p>Giao diện glassmorphism dùng chung visual tokens.</p></article>
      </section>
    </div>
  );
}

export function GuestServicesPage() {
  const [services, setServices] = useState<Service[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    void getPublishedServices()
      .then((data) => {
        if (!cancelled) setServices(data);
      })
      .catch((caughtError: unknown) => {
        if (!cancelled) {
          setError(caughtError instanceof Error ? caughtError.message : "Không thể tải dịch vụ.");
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  return (
    <section className="guest-services" aria-labelledby="guest-services-heading">
      <header className="guest-page-heading">
        <p className="guest-eyebrow">DỊCH VỤ &amp; GIÁ</p>
        <h1 id="guest-services-heading">Khám phá dịch vụ</h1>
        <p>Danh sách dịch vụ công khai đang được cung cấp.</p>
      </header>

      {isLoading && <div className="state-panel" role="status">Đang tải dịch vụ...</div>}
      {!isLoading && error && <div className="state-panel state-panel--error" role="alert">{error}</div>}
      {!isLoading && !error && services.length === 0 && (
        <div className="state-panel"><h2>Chưa có dịch vụ công khai</h2><p>Dữ liệu sẽ xuất hiện khi dịch vụ được PUBLISHED.</p></div>
      )}
      {!isLoading && !error && services.length > 0 && (
        <div className="guest-services__grid">
          {services.map((service) => (
            <article className="guest-service-card" key={service.id}>
              {service.imageUrl ? (
                <img src={service.imageUrl} alt={service.name} loading="lazy" />
              ) : (
                <div className="guest-service-card__placeholder" aria-hidden="true">PH</div>
              )}
              <div>
                <span>DỊCH VỤ</span>
                <h2>{service.name}</h2>
                <p>{service.shortDescription || service.description || "Thông tin đang được cập nhật."}</p>
                <a href="/login">Đăng nhập để tiếp tục</a>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export function GuestNewsPage() {
  return (
    <section className="guest-page-state" aria-labelledby="guest-news-heading">
      <p className="guest-eyebrow">TIN TỨC</p>
      <h1 id="guest-news-heading">Tin tức đang chờ tích hợp</h1>
      <p>Chưa có nguồn/API tin tức được xác minh. Nội dung mẫu sẽ không được trình bày như tin thật.</p>
    </section>
  );
}
