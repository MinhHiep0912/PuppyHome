import { useEffect, useRef, useState, type ReactNode } from "react";
import type { Customer } from "../auth/auth";
import "./ApplicationShell.css";
import "./CustomerShell.css";

type CustomerPage = "overview" | "services" | "news" | "account";

interface CustomerShellProps {
  customer: Customer;
  activePage: CustomerPage;
  isLoggingOut: boolean;
  logoutError: string | null;
  onLogout: () => void;
  children: ReactNode;
}

const navigation: Array<{
  page: CustomerPage;
  href: string;
  label: string;
  icon: string;
}> = [
  { page: "overview", href: "/customer/", label: "Tổng quan", icon: "⌂" },
  { page: "services", href: "/customer/services", label: "Dịch vụ & giá", icon: "▦" },
  { page: "news", href: "/customer/news", label: "Tin tức", icon: "▤" },
  { page: "account", href: "/customer/account", label: "Tài khoản", icon: "◎" },
];

export function CustomerShell({
  customer,
  activePage,
  isLoggingOut,
  logoutError,
  onLogout,
  children,
}: CustomerShellProps) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);
  const profileButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    function handlePointerDown(event: PointerEvent): void {
      if (
        profileRef.current &&
        !profileRef.current.contains(event.target as Node)
      ) {
        setIsProfileOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent): void {
      if (event.key === "Escape") {
        setIsProfileOpen(false);
        setIsSidebarOpen(false);
        profileButtonRef.current?.focus();
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, []);

  return (
    <div className="application-shell customer-shell">
      <button
        className="application-shell__menu-button"
        type="button"
        aria-label="Mở điều hướng khách hàng"
        aria-expanded={isSidebarOpen}
        aria-controls="customer-sidebar"
        onClick={() => setIsSidebarOpen(true)}
      >
        ☰
      </button>

      {isSidebarOpen && (
        <button
          className="application-shell__backdrop"
          type="button"
          aria-label="Đóng điều hướng"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      <aside
        id="customer-sidebar"
        className={`application-shell__sidebar${isSidebarOpen ? " application-shell__sidebar--open" : ""}`}
      >
        <a className="application-shell__brand" href="/customer/">
          <span className="application-shell__brand-mark">PH</span>
          <span>
            Puppy&apos;s Home
            <small>Khu vực khách hàng</small>
          </span>
        </a>

        <nav className="application-shell__nav" aria-label="Điều hướng khách hàng">
          {navigation.map((item) => (
            <a
              key={item.page}
              href={item.href}
              aria-current={activePage === item.page ? "page" : undefined}
              className={`application-shell__nav-item${activePage === item.page ? " application-shell__nav-item--active" : ""}`}
              onClick={() => setIsSidebarOpen(false)}
            >
              <span aria-hidden="true">{item.icon}</span>
              {item.label}
            </a>
          ))}
          <a className="application-shell__nav-item" href="/">
            <span aria-hidden="true">↗</span>
            Trang công khai
          </a>
        </nav>

        <button
          className="application-shell__sidebar-close"
          type="button"
          onClick={() => setIsSidebarOpen(false)}
        >
          Đóng menu
        </button>
      </aside>

      <div className="application-shell__workspace">
        <header className="application-shell__topbar">
          <div>
            <span className="application-shell__context">Khu vực khách hàng</span>
            <strong>{navigation.find((item) => item.page === activePage)?.label}</strong>
          </div>

          <div className="application-shell__profile" ref={profileRef}>
            <button
              ref={profileButtonRef}
              className="application-shell__profile-button"
              type="button"
              aria-label={`Tài khoản ${customer.username}`}
              aria-haspopup="menu"
              aria-expanded={isProfileOpen}
              onClick={() => setIsProfileOpen((current) => !current)}
            >
              <span className="application-shell__avatar" aria-hidden="true">
                {customer.username.slice(0, 2).toUpperCase()}
              </span>
              <span>{customer.username}</span>
            </button>

            {isProfileOpen && (
              <div className="application-shell__profile-menu" role="menu">
                <p role="menuitem">Tài khoản: {customer.username}</p>
                <p role="menuitem">Quyền: ROLE_CUSTOMER</p>
                <button
                  type="button"
                  role="menuitem"
                  disabled={isLoggingOut}
                  onClick={onLogout}
                >
                  {isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"}
                </button>
                {logoutError && (
                  <p className="application-shell__logout-error" role="alert">
                    {logoutError}
                  </p>
                )}
              </div>
            )}
          </div>
        </header>

        <main className="application-shell__main">{children}</main>

        <footer className="application-shell__footer">
          © {new Date().getFullYear()} Puppy&apos;s Home
        </footer>
      </div>
    </div>
  );
}
