import { useState, type FormEvent } from "react";
import type { RegisterCredentials } from "./auth";

interface CustomerRegistrationFormProps {
  onRegister: (credentials: RegisterCredentials) => Promise<void>;
  error: string | null;
}

export function CustomerRegistrationForm({
  onRegister,
  error,
}: CustomerRegistrationFormProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> {
    event.preventDefault();
    setLocalError(null);

    if (password !== confirmPassword) {
      setLocalError("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsSubmitting(true);
    try {
      await onRegister({
        email: email.trim().toLowerCase(),
        password,
      });
    } catch {
      // useAuth hoặc API loader giữ thông báo lỗi tại prop error.
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="admin-auth-section" aria-labelledby="register-heading">
      <h2 id="register-heading">Đăng ký tài khoản Customer</h2>
      <p>Tạo tài khoản bằng email và mật khẩu để sử dụng dịch vụ.</p>

      <form className="admin-login-form" onSubmit={handleSubmit}>
        <label htmlFor="customer-email">
          Email
          <input
            id="customer-email"
            name="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            maxLength={254}
            disabled={isSubmitting}
          />
        </label>

        <label htmlFor="customer-password">
          Mật khẩu
          <input
            id="customer-password"
            name="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            minLength={8}
            maxLength={128}
            disabled={isSubmitting}
          />
        </label>

        <label htmlFor="customer-confirm-password">
          Xác nhận mật khẩu
          <input
            id="customer-confirm-password"
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            required
            minLength={8}
            maxLength={128}
            disabled={isSubmitting}
          />
        </label>

        {(localError || error) && (
          <p className="admin-auth-error" role="alert">
            {localError ?? error}
          </p>
        )}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Đang đăng ký..." : "Đăng ký"}
        </button>
      </form>
    </section>
  );
}
