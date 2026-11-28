import type { LoginCredentials } from "./auth";
import { LoginForm } from "./LoginForm";

interface EmployeeLoginFormProps {
  onLogin: (credentials: LoginCredentials) => Promise<void>;
  error: string | null;
}

export function EmployeeLoginForm({ onLogin, error }: EmployeeLoginFormProps) {
  return (
    <LoginForm
      title="Đăng nhập nhân viên"
      intro="Đăng nhập bằng mã nhân viên để truy cập khu vực công việc."
      identifierLabel="Mã nhân viên"
      onLogin={onLogin}
      error={error}
    />
  );
}
