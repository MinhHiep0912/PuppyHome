import { useEffect, useState } from "react";
import { AdminLoginForm } from "./features/auth/AdminLoginForm";
import { EmployeeLoginForm } from "./features/auth/EmployeeLoginForm";
import { CustomerRegistrationForm } from "./features/auth/CustomerRegistrationForm";
import { registerCustomer } from "./features/auth/authApi";
import { useAuth } from "./features/auth/useAuth";
import { AdminServicesPage } from "./features/admin-services/AdminServicesPage";
import { CustomerServicesPage } from "./features/customer-services/CustomerServicesPage";
import { GameAccountsPage } from "./features/game-accounts/GameAccountsPage";
import { ServiceRequestsPage } from "./features/service-requests/ServiceRequestsPage";
import { EmployeesPage } from "./features/employee/EmployeesPage";
import { EmployeeShell } from "./features/employee/EmployeeShell";
import { TasksPage } from "./features/tasks/TasksPage";
import { ApplicationShell } from "./features/shell/ApplicationShell";
import { CustomerShell } from "./features/shell/CustomerShell";
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
  const isCustomerRoute = path === "/customer" || path.startsWith("/customer/");
  const customerPath = isCustomerRoute ? path : "/customer";
  const isEmployeeRoute = path === "/employee" || path.startsWith("/employee/");
  const employeePath = isEmployeeRoute ? path : "/employee";
  const {
    admin,
    customer,
    employee,
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
      if (!isCustomerRoute) {
        window.history.replaceState({}, "", "/customer/");
      }
    }

    if (!isCheckingSession && admin && canManageServices) {
      if (
        path !== "/admin/services" &&
        path !== "/admin/service-requests" &&
        path !== "/admin/employees" &&
        path !== "/admin/tasks"
      ) {
        window.history.replaceState({}, "", "/admin/services");
      }
    }

    if (!isCheckingSession && admin && isEmployeeRoute) {
      window.history.replaceState({}, "", "/admin/services");
    }

    if (!isCheckingSession && employee && !isEmployeeRoute) {
      window.history.replaceState({}, "", "/employee/");
    }

    if (
      !isCheckingSession &&
      employee &&
      employeePath === "/employee/login"
    ) {
      window.history.replaceState({}, "", "/employee/");
    }

    if (!isCheckingSession && !admin && !employee && path.startsWith("/admin")) {
      window.history.replaceState({}, "", "/login");
    }
  }, [
    admin,
    customer,
    employee,
    employeePath,
    canManageServices,
    isCheckingSession,
    isCustomerRoute,
    isEmployeeRoute,
    path,
  ]);

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
      <CustomerShell
        customer={customer}
        activePage={
          customerPath === "/customer/services"
            ? "services"
            : customerPath === "/customer/news"
              ? "news"
              : customerPath === "/customer/game-accounts"
                ? "game-accounts"
                : customerPath === "/customer/service-requests"
                  ? "service-requests"
                  : customerPath === "/customer/account"
                    ? "account"
                    : "overview"
        }
        isLoggingOut={isLoggingOut}
        logoutError={authError}
        onLogout={() => void handleLogout()}
      >
        {customerPath === "/customer" && (
          <section className="guest-page-state" aria-labelledby="customer-heading">
            <p className="guest-eyebrow">KHU VỰC KHÁCH HÀNG</p>
            <h1 id="customer-heading">Chào mừng, {customer.username}</h1>
            <p>Tài khoản đã được kích hoạt và phiên đăng nhập đang hoạt động.</p>
            <div className="guest-hero__actions">
              <a className="guest-primary-action" href="/customer/services">Khám phá dịch vụ</a>
              <a className="guest-secondary-action" href="/customer/account">Thông tin tài khoản</a>
            </div>
          </section>
        )}
        {customerPath === "/customer/services" && <CustomerServicesPage />}
        {customerPath === "/customer/game-accounts" && <GameAccountsPage />}
        {customerPath === "/customer/service-requests" && (
          <ServiceRequestsPage isAdmin={false} />
        )}
        {customerPath === "/customer/news" && <GuestNewsPage />}
        {customerPath === "/customer/account" && (
          <section className="guest-page-state" aria-labelledby="customer-account-heading">
            <p className="guest-eyebrow">TÀI KHOẢN</p>
            <h1 id="customer-account-heading">Thông tin khách hàng</h1>
            <p>Email đăng nhập: {customer.username}</p>
            <p>Trạng thái: Đang hoạt động</p>
          </section>
        )}
        {isCustomerRoute &&
          ![
            "/customer",
            "/customer/services",
            "/customer/news",
            "/customer/game-accounts",
            "/customer/service-requests",
            "/customer/account",
          ].includes(customerPath) && (
          <section className="guest-page-state" role="alert">
            <h1>Không tìm thấy trang</h1>
            <p><a href="/customer/">Về tổng quan khách hàng</a></p>
          </section>
        )}
      </CustomerShell>
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
        activePage={
          path === "/admin/service-requests"
            ? "service-requests"
            : path === "/admin/employees"
              ? "employees"
              : path === "/admin/tasks"
                ? "tasks"
                : "services"
        }
        isLoggingOut={isLoggingOut}
        logoutError={authError}
        onLogout={() => void handleLogout()}
      >
        {path === "/admin/services" || path === "/" || path === "/services" || path === "/news" || path === "/login" ? (
          <AdminServicesPage />
        ) : path === "/admin/service-requests" ? (
          <ServiceRequestsPage isAdmin />
        ) : path === "/admin/employees" ? (
          <EmployeesPage />
        ) : path === "/admin/tasks" ? (
          <TasksPage isAdmin />
        ) : (
          <section className="state-panel" role="alert">
            <h1>Trang role chưa được triển khai</h1>
            <p>Chỉ route Dịch vụ &amp; giá được xác minh trong phạm vi hiện tại.</p>
          </section>
        )}
      </ApplicationShell>
    );
  }

  if (employee) {
    return (
      <EmployeeShell
        employee={employee}
        activePage={employeePath === "/employee/tasks" ? "tasks" : "overview"}
        isLoggingOut={isLoggingOut}
        logoutError={authError}
        onLogout={() => void handleLogout()}
      >
        {employeePath === "/employee" && (
          <section className="guest-page-state" aria-labelledby="employee-heading">
            <p className="guest-eyebrow">KHU VỰC NHÂN VIÊN</p>
            <h1 id="employee-heading">Chào mừng, {employee.username}</h1>
            <p>Tài khoản nhân viên đã được kích hoạt và phiên đăng nhập đang hoạt động.</p>
            <div className="guest-hero__actions">
              <a className="guest-primary-action" href="/employee/tasks">Công việc của tôi</a>
            </div>
          </section>
        )}
        {employeePath === "/employee/tasks" && <TasksPage isAdmin={false} />}
        {isEmployeeRoute &&
          ![
            "/employee",
            "/employee/tasks",
          ].includes(employeePath) && (
          <section className="guest-page-state" role="alert">
            <h1>Không tìm thấy trang</h1>
            <p><a href="/employee/">Về tổng quan nhân viên</a></p>
          </section>
        )}
      </EmployeeShell>
    );
  }

  if (path === "/login" || path.startsWith("/admin") || isEmployeeRoute) {
    return (
      <GuestShell activePage="home">
        <section className="login-page" aria-labelledby="login-page-heading">
          {isEmployeeRoute ? (
            <EmployeeLoginForm onLogin={handleLogin} error={authError} />
          ) : (
            <AdminLoginForm onLogin={handleLogin} error={authError} />
          )}
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
