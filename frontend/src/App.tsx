import { useEffect, useState } from "react";
import { AdminLoginForm } from "./features/auth/AdminLoginForm";
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
    isCheckingSession,
    error: authError,
    login,
    logout,
  } = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const canManageServices =
    admin?.authorities.includes("ROLE_ADMIN") ?? false;

  useEffect(() => {
    if (!isCheckingSession && admin && canManageServices) {
      if (path !== "/admin/services") {
        window.history.replaceState({}, "", "/admin/services");
      }
    }

    if (!isCheckingSession && !admin && path.startsWith("/admin")) {
      window.history.replaceState({}, "", "/login");
    }
  }, [admin, canManageServices, isCheckingSession, path]);

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
        <section className="guest-page-state" role="status" aria-labelledby="register-heading">
          <p className="guest-eyebrow">ĐĂNG KÝ</p>
          <h1 id="register-heading">Chức năng đăng ký chưa khả dụng</h1>
          <p>Luồng Customer/Employee và API đăng ký chưa được xác minh. Chưa tạo tài khoản giả.</p>
          <a className="guest-secondary-action" href="/login">Đến trang đăng nhập</a>
        </section>
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
