import { useCallback, useEffect, useState } from "react";
import type { Admin, LoginCredentials } from "./auth";
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
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refreshSession = useCallback(async (): Promise<void> => {
    setIsCheckingSession(true);
    setError(null);

    try {
      const currentAdmin = await getCurrentAdmin();
      setAdmin(currentAdmin);
    } catch (caughtError: unknown) {
      setAdmin(null);
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
        setAdmin(authenticatedAdmin);
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
          setAdmin(currentAdmin);
        }
      } catch (caughtError: unknown) {
        if (!cancelled) {
          setAdmin(null);
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
    isCheckingSession,
    error,
    login,
    logout,
    refreshSession,
  };
}
