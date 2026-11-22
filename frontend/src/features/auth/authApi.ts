import type {
  AuthenticatedUser,
  Customer,
  LoginCredentials,
  RegisterCredentials,
} from "./auth";

const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

export async function getResponseErrorMessage(
  response: Response,
  fallbackMessage: string,
): Promise<string> {
  try {
    const body: unknown = await response.json();

    if (typeof body === "object" && body !== null) {
      if (
        "detail" in body &&
        typeof body.detail === "string" &&
        body.detail.length > 0
      ) {
        return body.detail;
      }

      if (
        "title" in body &&
        typeof body.title === "string" &&
        body.title.length > 0
      ) {
        return body.title;
      }
    }
  } catch {
    // Phản hồi có thể không chứa JSON hợp lệ.
  }

  return `${fallbackMessage} (HTTP ${response.status})`;
}

function readCsrfCookie(): string {
  const prefix = `${CSRF_COOKIE_NAME}=`;

  const cookie = document.cookie
    .split(";")
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix));

  if (!cookie) {
    throw new Error("Không tìm thấy CSRF token. Hãy lấy token mới từ máy chủ.");
  }

  const token = decodeURIComponent(cookie.slice(prefix.length));

  if (!token) {
    throw new Error("CSRF token đang trống.");
  }

  return token;
}

export async function fetchCsrfToken(): Promise<string> {
  const response = await fetch("/api/auth/csrf", {
    method: "GET",
    credentials: "same-origin",
  });

  if (!response.ok) {
    throw new Error(
      await getResponseErrorMessage(response, "Không thể lấy CSRF token."),
    );
  }

  return readCsrfCookie();
}

export async function loginUser(
  credentials: LoginCredentials,
): Promise<AuthenticatedUser> {
  const csrfToken = await fetchCsrfToken();

  const response = await fetch("/api/auth/login", {
    method: "POST",
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      [CSRF_HEADER_NAME]: csrfToken,
    },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    throw new Error(
      await getResponseErrorMessage(response, "Đăng nhập không thành công."),
    );
  }

  return (await response.json()) as AuthenticatedUser;
}

export async function registerCustomer(
  credentials: RegisterCredentials,
): Promise<Customer> {
  const csrfToken = await fetchCsrfToken();

  const response = await fetch("/api/auth/register", {
    method: "POST",
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      [CSRF_HEADER_NAME]: csrfToken,
    },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    throw new Error(
      await getResponseErrorMessage(response, "Đăng ký không thành công."),
    );
  }

  return (await response.json()) as Customer;
}

export async function getCurrentUser(): Promise<AuthenticatedUser | null> {
  const response = await fetch("/api/auth/me", {
    method: "GET",
    credentials: "same-origin",
  });

  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new Error(
      await getResponseErrorMessage(
        response,
        "Không thể lấy thông tin quản trị viên.",
      ),
    );
  }

  return (await response.json()) as AuthenticatedUser;
}

export async function logoutUser(): Promise<void> {
  const csrfToken = await fetchCsrfToken();

  const response = await fetch("/api/auth/logout", {
    method: "POST",
    credentials: "same-origin",
    headers: {
      [CSRF_HEADER_NAME]: csrfToken,
    },
  });

  if (!response.ok) {
    throw new Error(
      await getResponseErrorMessage(response, "Đăng xuất không thành công."),
    );
  }
}
