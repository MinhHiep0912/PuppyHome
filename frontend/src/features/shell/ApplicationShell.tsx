import { useEffect, useRef, useState, type ReactNode } from "react";
import type { Admin } from "../auth/auth";
import "./ApplicationShell.css";

interface ApplicationShellProps {
  admin: Admin;
  activePage?: "services" | "service-requests";
  isLoggingOut: boolean;
  logoutError: string | null;
  onLogout: () => void;
  children: ReactNode;
}

export function ApplicationShell({
  admin,
  activePage = "services",
  isLoggingOut,
  logoutError,
  onLogout,
  children,
}: ApplicationShellProps) {
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
    <div className="application-shell">
      <button
        className="application-shell__menu-button"
        type="button"
        aria-label="Mở điều hướng"
        aria-expanded={isSidebarOpen}
        aria-controls="application-sidebar"
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
        id="application-sidebar"
        className={`application-shell__sidebar${isSidebarOpen ? " application-shell__sidebar--open" : ""}`}
      >
        <a className="application-shell__brand" href="/admin/services">
          <span className="application-shell__brand-mark">PH</span>
          <span>
            Puppy&apos;s Home
            <small>Không gian quản trị</small>
          </span>
        </a>

        <nav className="application-shell__nav" aria-label="Điều hướng quản trị">
          <a
            href="/admin/services"
            aria-current={activePage === "services" ? "page" : undefined}
            className={`application-shell__nav-item${activePage === "services" ? " application-shell__nav-item--active" : ""}`}
          >
            <span aria-hidden="true">▦</span>
            Dịch vụ &amp; giá
          </a>
          <a
            href="/admin/service-requests"
            aria-current={activePage === "service-requests" ? "page" : undefined}
            className={`application-shell__nav-item${activePage === "service-requests" ? " application-shell__nav-item--active" : ""}`}
          >
            <span aria-hidden="true">◷</span>
            Yêu cầu dịch vụ
          </a>
          <span className="application-shell__nav-item application-shell__nav-item--blocked">
            <span aria-hidden="true">◷</span>
            Chức năng khác — chờ tích hợp
          </span>
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
            <span className="application-shell__context">Khu vực quản trị</span>
            <strong>
              {activePage === "service-requests"
                ? "Quản lý yêu cầu dịch vụ"
                : "Quản lý dịch vụ"}
            </strong>
          </div>

          <div className="application-shell__profile" ref={profileRef}>
            <button
              ref={profileButtonRef}
              className="application-shell__profile-button"
              type="button"
              aria-label={`Tài khoản ${admin.username}`}
              aria-haspopup="menu"
              aria-expanded={isProfileOpen}
              onClick={() => setIsProfileOpen((current) => !current)}
            >
              <span className="application-shell__avatar" aria-hidden="true">
                {admin.username.slice(0, 2).toUpperCase()}
              </span>
              <span>{admin.username}</span>
            </button>

            {isProfileOpen && (
              <div className="application-shell__profile-menu" role="menu">
                <p role="menuitem">Tài khoản: {admin.username}</p>
                <p role="menuitem">
                  Quyền: {admin.authorities.join(", ") || "Không có quyền"}
                </p>
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
