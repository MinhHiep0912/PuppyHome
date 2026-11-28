import { useEffect, useRef, useState, type ReactNode } from "react";
import type { Employee } from "../auth/auth";
import "../shell/ApplicationShell.css";

interface EmployeeShellProps {
  employee: Employee;
  activePage?: "overview" | "tasks";
  isLoggingOut: boolean;
  logoutError: string | null;
  onLogout: () => void;
  children: ReactNode;
}

export function EmployeeShell({
  employee,
  activePage = "overview",
  isLoggingOut,
  logoutError,
  onLogout,
  children,
}: EmployeeShellProps) {
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
        aria-controls="employee-sidebar"
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
        id="employee-sidebar"
        className={`application-shell__sidebar${isSidebarOpen ? " application-shell__sidebar--open" : ""}`}
      >
        <a className="application-shell__brand" href="/employee/">
          <span className="application-shell__brand-mark">PH</span>
          <span>
            Puppy&apos;s Home
            <small>Khu vực nhân viên</small>
          </span>
        </a>

        <nav className="application-shell__nav" aria-label="Điều hướng nhân viên">
          <a
            href="/employee/"
            aria-current={activePage === "overview" ? "page" : undefined}
            className={`application-shell__nav-item${activePage === "overview" ? " application-shell__nav-item--active" : ""}`}
          >
            <span aria-hidden="true">⌂</span>
            Tổng quan
          </a>
          <a
            href="/employee/tasks"
            aria-current={activePage === "tasks" ? "page" : undefined}
            className={`application-shell__nav-item${activePage === "tasks" ? " application-shell__nav-item--active" : ""}`}
          >
            <span aria-hidden="true">◷</span>
            Công việc của tôi
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
            <span className="application-shell__context">Khu vực nhân viên</span>
            <strong>
              {activePage === "tasks" ? "Công việc của tôi" : "Tổng quan"}
            </strong>
          </div>

          <div className="application-shell__profile" ref={profileRef}>
            <button
              ref={profileButtonRef}
              className="application-shell__profile-button"
              type="button"
              aria-label={`Tài khoản ${employee.username}`}
              aria-haspopup="menu"
              aria-expanded={isProfileOpen}
              onClick={() => setIsProfileOpen((current) => !current)}
            >
              <span className="application-shell__avatar" aria-hidden="true">
                {employee.username.slice(0, 2).toUpperCase()}
              </span>
              <span>{employee.username}</span>
            </button>

            {isProfileOpen && (
              <div className="application-shell__profile-menu" role="menu">
                <p role="menuitem">Tài khoản: {employee.username}</p>
                <p role="menuitem">
                  Quyền: {employee.authorities.join(", ") || "Không có quyền"}
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
