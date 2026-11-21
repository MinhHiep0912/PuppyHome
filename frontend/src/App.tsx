import { useEffect, useState } from "react";
import type { Service } from "./features/service-catalog/service";
import { getPublishedServices } from "./features/service-catalog/serviceApi";
import { useAuth } from "./features/auth/useAuth";
import { AdminLoginForm } from "./features/auth/AdminLoginForm";
import { AdminServicesPage } from "./features/admin-services/AdminServicesPage";
import "./App.css";

function App() {
  const isAdminServicesRoute =
    window.location.pathname.replace(/\/+$/, "") === "/admin/services";
  const [services, setServices] = useState<Service[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const {
    admin,
    isCheckingSession,
    error: authError,
    login,
    logout,
  } = useAuth();

  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const canManageServices =
    admin?.authorities.includes("ROLE_ADMIN") ?? false;

  useEffect(() => {
    if (isAdminServicesRoute) {
      return;
    }

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
  }, [isAdminServicesRoute, retryKey]);

  async function handleLogout(): Promise<void> {
    setIsLoggingOut(true);

    try {
      await logout();
    } catch {
      // useAuth đã lưu lỗi; giữ trạng thái phiên hiện tại nếu thất bại.
    } finally {
      setIsLoggingOut(false);
    }
  }

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

        <div className="site-header__account" aria-live="polite">
          <span className="site-header__tagline">
            {isCheckingSession
              ? "Đang kiểm tra phiên quản trị..."
              : authError
                ? "Không thể kiểm tra phiên quản trị"
                : admin
                  ? `Đã đăng nhập: ${admin.username}`
                  : "Chưa đăng nhập quản trị"}
          </span>

          {authError && (
            <span className="site-header__auth-error" role="status">
              {authError}
            </span>
          )}
          {canManageServices && (
            <a className="admin-services-link" href="/admin/services">
              Quản lý dịch vụ
            </a>
          )}
        </div>
      </header>

      {!isAdminServicesRoute && !isCheckingSession && !admin && (
        <AdminLoginForm onLogin={login} error={authError} />
      )}

      {admin && (
        <section
          className="admin-session-panel"
          aria-labelledby="admin-session-heading"
        >
          <div>
            <h2 id="admin-session-heading">Khu vực quản trị</h2>

            <p>
              Đang đăng nhập với tài khoản <strong>{admin.username}</strong>.
            </p>
          </div>

          <button
            type="button"
            className="admin-logout-button"
            disabled={isLoggingOut}
            onClick={() => void handleLogout()}
          >
            {isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"}
          </button>

          {authError && (
            <p className="admin-auth-error" role="alert">
              {authError}
            </p>
          )}
        </section>
      )}

      <main className="main-content">
        {isAdminServicesRoute ? (
          isCheckingSession ? (
            <div className="state-panel" role="status">
              <span className="spinner" aria-hidden="true" />
              <p>Đang xác minh quyền Admin...</p>
            </div>
          ) : !admin ? (
            <AdminLoginForm onLogin={login} error={authError} />
          ) : !canManageServices ? (
            <section
              className="state-panel state-panel--error"
              role="alert"
              aria-labelledby="admin-permission-heading"
            >
              <h2 id="admin-permission-heading">Không có quyền truy cập</h2>
              <p>Tài khoản hiện tại không có quyền Admin.</p>
            </section>
          ) : (
            <AdminServicesPage />
          )
        ) : (
          <>
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
          </>
        )}
      </main>

      <footer className="site-footer">
        <p>© {new Date().getFullYear()} Puppy&apos;s Home</p>
      </footer>
    </div>
  );
}

export default App;
