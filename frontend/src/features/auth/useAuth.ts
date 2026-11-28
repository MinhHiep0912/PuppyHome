import { useCallback, useEffect, useState } from "react";
import type {
  Admin,
  AuthenticatedUser,
  Customer,
  Employee,
  LoginCredentials,
} from "./auth";
import { getCurrentUser, loginUser, logoutUser } from "./authApi";

function getErrorMessage(error: unknown, fallbackMessage: string): string {
  return error instanceof Error ? error.message : fallbackMessage;
}

export function useAuth() {
  const [admin, setAdmin] = useState<Admin | null>(null);
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [employee, setEmployee] = useState<Employee | null>(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const setAuthenticatedUser = useCallback(
    (user: AuthenticatedUser | null): void => {
      if (user?.authorities.includes("ROLE_ADMIN")) {
        setAdmin(user);
        setCustomer(null);
        setEmployee(null);
      } else if (user?.authorities.includes("ROLE_EMPLOYEE")) {
        setAdmin(null);
        setCustomer(null);
        setEmployee(user);
      } else if (user?.authorities.includes("ROLE_CUSTOMER")) {
        setAdmin(null);
        setCustomer(user);
        setEmployee(null);
      } else {
        setAdmin(null);
        setCustomer(null);
        setEmployee(null);
      }
    },
    [],
  );

  const refreshSession = useCallback(async (): Promise<void> => {
    setIsCheckingSession(true);
    setError(null);

    try {
      const currentUser = await getCurrentUser();
      setAuthenticatedUser(currentUser);
    } catch (caughtError: unknown) {
      setAdmin(null);
      setCustomer(null);
      setEmployee(null);
      setError(
        getErrorMessage(caughtError, "Không thể kiểm tra phiên đăng nhập."),
      );
    } finally {
      setIsCheckingSession(false);
    }
  }, [setAuthenticatedUser]);

  const login = useCallback(
    async (credentials: LoginCredentials): Promise<void> => {
      setError(null);

      try {
        const authenticatedUser = await loginUser(credentials);
        setAuthenticatedUser(authenticatedUser);
      } catch (caughtError: unknown) {
        setError(getErrorMessage(caughtError, "Đăng nhập không thành công."));
        throw caughtError;
      }
    },
    [setAuthenticatedUser],
  );

  const logout = useCallback(async (): Promise<void> => {
    setError(null);

    try {
      await logoutUser();
      setAdmin(null);
      setCustomer(null);
      setEmployee(null);
    } catch (caughtError: unknown) {
      setError(getErrorMessage(caughtError, "Đăng xuất không thành công."));
      throw caughtError;
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function checkInitialSession(): Promise<void> {
      try {
        const currentUser = await getCurrentUser();

        if (!cancelled) {
          setAuthenticatedUser(currentUser);
        }
      } catch (caughtError: unknown) {
        if (!cancelled) {
          setAdmin(null);
          setCustomer(null);
          setEmployee(null);
          setError(
            getErrorMessage(caughtError, "Không thể kiểm tra phiên đăng nhập."),
          );
        }
      } finally {
        if (!cancelled) {
          setIsCheckingSession(false);
        }
      }
    }

    void checkInitialSession();

    return () => {
      cancelled = true;
    };
  }, [setAuthenticatedUser]);

  return {
    admin,
    customer,
    employee,
    isCheckingSession,
    error,
    login,
    logout,
    refreshSession,
  };
}
