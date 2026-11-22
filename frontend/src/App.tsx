import { useEffect, useState } from "react";
import { AdminLoginForm } from "./features/auth/AdminLoginForm";
import { CustomerRegistrationForm } from "./features/auth/CustomerRegistrationForm";
import { registerCustomer } from "./features/auth/authApi";
import { useAuth } from "./features/auth/useAuth";
import { AdminServicesPage } from "./features/admin-services/AdminServicesPage";
import { ApplicationShell } from "./features/shell/ApplicationShell";
import {
  GuestHomePage,
  GuestNewsPage,
  GuestServicesPage,
} from "./features/guest/GuestPages";
import { GuestShell } from "./features/guest/GuestShell";
import "./App.css";

function normalizedPath(): string {
  return window.location.pathname.replace(/\/+$/, "") || "/";
}

function App() {
  const path = normalizedPath();
  const {
    admin,
    customer,
    isCheckingSession,
    error: authError,
    login,
    logout,
  } = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [registrationError, setRegistrationError] = useState<string | null>(null);
  const canManageServices =
    admin?.authorities.includes("ROLE_ADMIN") ?? false;

  useEffect(() => {
    if (!isCheckingSession && customer) {
      if (path !== "/customer") {
        window.history.replaceState({}, "", "/customer/");
      }
    }

    if (!isCheckingSession && admin && canManageServices) {
      if (path !== "/admin/services") {
        window.history.replaceState({}, "", "/admin/services");
      }
    }

    if (!isCheckingSession && !admin && path.startsWith("/admin")) {
      window.history.replaceState({}, "", "/login");
    }
  }, [admin, customer, canManageServices, isCheckingSession, path]);

  async function handleLogout(): Promise<void> {
    setIsLoggingOut(true);
    try {
      await logout();
      window.history.replaceState({}, "", "/");
      window.location.reload();
    } catch {
      // useAuth keeps the session and surfaces the logout error.
    } finally {
      setIsLoggingOut(false);
    }
  }

  async function handleLogin(credentials: Parameters<typeof login>[0]) {
    await login(credentials);
  }

  async function handleRegister(
    credentials: Parameters<typeof registerCustomer>[0],
  ): Promise<void> {
    setRegistrationError(null);
    try {
      await registerCustomer(credentials);
      window.history.replaceState({}, "", "/customer/");
      window.location.reload();
    } catch (caughtError: unknown) {
      setRegistrationError(
        caughtError instanceof Error
          ? caughtError.message
          : "Đăng ký không thành công.",
      );
      throw caughtError;
    }
  }

  if (isCheckingSession) {
    return (
      <div className="app">
        <div className="state-panel" role="status">
          <span className="spinner" aria-hidden="true" />
          <p>Đang kiểm tra phiên đăng nhập...</p>
        </div>
      </div>
    );
  }

  if (customer) {
    return (
      <GuestShell activePage="home">
        <section className="guest-page-state" role="status" aria-labelledby="customer-heading">
          <p className="guest-eyebrow">KHU VỰC CUSTOMER</p>
          <h1 id="customer-heading">Chào mừng, {customer.username}</h1>
          <p>Tài khoản đã được kích hoạt và phiên đăng nhập đang hoạt động.</p>
        </section>
      </GuestShell>
    );
  }

  if (admin) {
    if (!canManageServices) {
      return (
        <div className="app">
          <section className="state-panel state-panel--error" role="alert">
            <h1>Không có quyền truy cập</h1>
            <p>Tài khoản hiện tại không có quyền Admin.</p>
          </section>
        </div>
      );
    }

    return (
      <ApplicationShell
        admin={admin}
        isLoggingOut={isLoggingOut}
        logoutError={authError}
        onLogout={() => void handleLogout()}
      >
        {path === "/admin/services" || path === "/" || path === "/services" || path === "/news" || path === "/login" ? (
          <AdminServicesPage />
        ) : (
          <section className="state-panel" role="alert">
            <h1>Trang role chưa được triển khai</h1>
            <p>Chỉ route Dịch vụ &amp; giá được xác minh trong phạm vi hiện tại.</p>
          </section>
        )}
      </ApplicationShell>
    );
  }

  if (path === "/login" || path.startsWith("/admin")) {
    return (
      <GuestShell activePage="home">
        <section className="login-page" aria-labelledby="login-page-heading">
          <AdminLoginForm onLogin={handleLogin} error={authError} />
        </section>
      </GuestShell>
    );
  }

  if (path === "/services") {
    return (
      <GuestShell activePage="services">
        <GuestServicesPage />
      </GuestShell>
    );
  }

  if (path === "/news") {
    return (
      <GuestShell activePage="news">
        <GuestNewsPage />
      </GuestShell>
    );
  }

  if (path === "/register") {
    return (
      <GuestShell activePage="home">
        <CustomerRegistrationForm
          onRegister={handleRegister}
          error={registrationError}
        />
      </GuestShell>
    );
  }

  if (path === "/") {
    return (
      <GuestShell activePage="home">
        <GuestHomePage />
      </GuestShell>
    );
  }

  return (
    <GuestShell activePage="home">
      <section className="guest-page-state" role="alert">
        <h1>Không tìm thấy trang</h1>
        <p><a href="/">Về trang chủ</a></p>
      </section>
    </GuestShell>
  );
}

export default App;
