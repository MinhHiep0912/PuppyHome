import type { LoginCredentials } from "./auth";
import { LoginForm } from "./LoginForm";

interface AdminLoginFormProps {
  onLogin: (credentials: LoginCredentials) => Promise<void>;
  error: string | null;
}

export function AdminLoginForm({ onLogin, error }: AdminLoginFormProps) {
  return (
    <LoginForm
      title="Đăng nhập quản trị"
      intro="Đăng nhập để truy cập các chức năng quản lý Puppy&apos;s Home."
      identifierLabel="Tên đăng nhập"
      onLogin={onLogin}
      error={error}
    />
  );
}
