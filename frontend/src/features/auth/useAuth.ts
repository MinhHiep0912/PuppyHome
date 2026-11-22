import { useCallback, useEffect, useState } from "react";
import type { Admin, Customer, LoginCredentials } from "./auth";
import {
  getCurrentAdmin,
  loginAdmin as requestLoginAdmin,
  logoutAdmin as requestLogoutAdmin,
} from "./authApi";

function getErrorMessage(error: unknown, fallbackMessage: string): string {
  return error instanceof Error ? error.message : fallbackMessage;
}

export function useAuth() {
  const [admin, setAdmin] = useState<Admin | null>(null);
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refreshSession = useCallback(async (): Promise<void> => {
    setIsCheckingSession(true);
    setError(null);

    try {
      const currentAdmin = await getCurrentAdmin();
      if (currentAdmin?.authorities.includes("ROLE_ADMIN")) {
        setAdmin(currentAdmin);
        setCustomer(null);
      } else if (currentAdmin?.authorities.includes("ROLE_CUSTOMER")) {
        setAdmin(null);
        setCustomer(currentAdmin as unknown as Customer);
      } else {
        setAdmin(null);
        setCustomer(null);
      }
    } catch (caughtError: unknown) {
      setAdmin(null);
      setCustomer(null);
      setError(
        getErrorMessage(caughtError, "Không thể kiểm tra phiên quản trị."),
      );
    } finally {
      setIsCheckingSession(false);
    }
  }, []);

  const login = useCallback(
    async (credentials: LoginCredentials): Promise<void> => {
      setError(null);

      try {
        const authenticatedAdmin = await requestLoginAdmin(credentials);
        if (authenticatedAdmin.authorities.includes("ROLE_ADMIN")) {
          setAdmin(authenticatedAdmin);
          setCustomer(null);
        } else if (authenticatedAdmin.authorities.includes("ROLE_CUSTOMER")) {
          setAdmin(null);
          setCustomer(authenticatedAdmin as unknown as Customer);
        } else {
          setAdmin(null);
          setCustomer(null);
        }
      } catch (caughtError: unknown) {
        setError(getErrorMessage(caughtError, "Đăng nhập không thành công."));
        throw caughtError;
      }
    },
    [],
  );

  const logout = useCallback(async (): Promise<void> => {
    setError(null);

    try {
      await requestLogoutAdmin();
      setAdmin(null);
      setCustomer(null);
    } catch (caughtError: unknown) {
      setError(getErrorMessage(caughtError, "Đăng xuất không thành công."));
      throw caughtError;
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function checkInitialSession(): Promise<void> {
      try {
        const currentAdmin = await getCurrentAdmin();

        if (!cancelled) {
          if (currentAdmin?.authorities.includes("ROLE_ADMIN")) {
            setAdmin(currentAdmin);
            setCustomer(null);
          } else if (currentAdmin?.authorities.includes("ROLE_CUSTOMER")) {
            setAdmin(null);
            setCustomer(currentAdmin as unknown as Customer);
          } else {
            setAdmin(null);
            setCustomer(null);
          }
        }
      } catch (caughtError: unknown) {
        if (!cancelled) {
          setAdmin(null);
          setCustomer(null);
          setError(
            getErrorMessage(caughtError, "Không thể kiểm tra phiên quản trị."),
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
  }, []);

  return {
    admin,
    customer,
    isCheckingSession,
    error,
    login,
    logout,
    refreshSession,
  };
}
