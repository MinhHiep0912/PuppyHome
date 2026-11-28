import { useState, type FormEvent } from "react";
import type { LoginCredentials } from "./auth";

interface LoginFormProps {
  title: string;
  intro: string;
  identifierLabel: string;
  onLogin: (credentials: LoginCredentials) => Promise<void>;
  error: string | null;
}

export function LoginForm({
  title,
  intro,
  identifierLabel,
  onLogin,
  error,
}: LoginFormProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> {
    event.preventDefault();
    setIsSubmitting(true);

    try {
      await onLogin({
        username: username.trim(),
        password,
      });

      setPassword("");
    } catch {
      // useAuth đã lưu thông báo lỗi để giao diện hiển thị.
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section
      className="admin-auth-section"
      aria-labelledby="login-form-heading"
    >
      <h2 id="login-form-heading">{title}</h2>

      <p>{intro}</p>

      <form className="admin-login-form" onSubmit={handleSubmit}>
        <label htmlFor="login-username">
          {identifierLabel}
          <input
            id="login-username"
            name="username"
            type="text"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
            maxLength={50}
            disabled={isSubmitting}
          />
        </label>

        <label htmlFor="login-password">
          Mật khẩu
          <input
            id="login-password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            disabled={isSubmitting}
          />
        </label>

        {error && (
          <p className="admin-auth-error" role="alert">
            {error}
          </p>
        )}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Đang đăng nhập..." : "Đăng nhập"}
        </button>
      </form>
    </section>
  );
}
