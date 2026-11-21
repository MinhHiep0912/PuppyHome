import type { ReactNode } from "react";
import "./GuestShell.css";

interface GuestShellProps {
  activePage: "home" | "services" | "news";
  children: ReactNode;
}

export function GuestShell({ activePage, children }: GuestShellProps) {
  return (
    <div className="guest-shell">
      <header className="guest-header">
        <a className="guest-brand" href="/" aria-label="Puppy's Home - Trang chủ">
          <span className="guest-brand__mark">PH</span>
          <span className="guest-brand__name">
            Puppy&apos;s Home
            <small>Không gian dịch vụ của bạn</small>
          </span>
        </a>

        <nav className="guest-nav" aria-label="Điều hướng công khai">
          <a href="/" aria-current={activePage === "home" ? "page" : undefined}>
            Trang chủ
          </a>
          <a href="/services" aria-current={activePage === "services" ? "page" : undefined}>
            Dịch vụ &amp; giá
          </a>
          <a href="/news" aria-current={activePage === "news" ? "page" : undefined}>
            Tin tức
          </a>
        </nav>

        <div className="guest-actions">
          <a className="guest-actions__login" href="/login">
            Đăng nhập
          </a>
          <a className="guest-actions__register" href="/register">
            Đăng ký
          </a>
        </div>
      </header>

      <main className="guest-main">{children}</main>

      <footer className="guest-footer">
        © {new Date().getFullYear()} Puppy&apos;s Home
      </footer>
    </div>
  );
}
