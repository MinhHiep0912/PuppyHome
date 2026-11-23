import {
  fetchCsrfToken,
  getResponseErrorMessage,
} from "../auth/authApi";
import type { GameAccount, GameAccountInput } from "./gameAccount";

const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

async function requestJson<T>(
  path: string,
  init: RequestInit,
  fallbackMessage: string,
): Promise<T> {
  const response = await fetch(path, {
    credentials: "same-origin",
    ...init,
  });

  if (!response.ok) {
    throw new Error(
      await getResponseErrorMessage(response, fallbackMessage),
    );
  }

  return (await response.json()) as T;
}

async function mutationHeaders(): Promise<HeadersInit> {
  const csrfToken = await fetchCsrfToken();

  return {
    "Content-Type": "application/json",
    [CSRF_HEADER_NAME]: csrfToken,
  };
}

export async function getGameAccounts(): Promise<GameAccount[]> {
  const response = await fetch("/api/game-accounts", {
    credentials: "same-origin",
  });

  if (!response.ok) {
    throw new Error(
      await getResponseErrorMessage(
        response,
        "Không thể tải danh sách tài khoản game.",
      ),
    );
  }

  return (await response.json()) as GameAccount[];
}

export async function createGameAccount(
  input: GameAccountInput,
): Promise<GameAccount> {
  return requestJson<GameAccount>(
    "/api/game-accounts",
    {
      method: "POST",
      headers: await mutationHeaders(),
      body: JSON.stringify(input),
    },
    "Không thể tạo tài khoản game.",
  );
}

export async function updateGameAccount(
  id: number,
  input: GameAccountInput,
): Promise<GameAccount> {
  return requestJson<GameAccount>(
    `/api/game-accounts/${id}`,
    {
      method: "PUT",
      headers: await mutationHeaders(),
      body: JSON.stringify(input),
    },
    "Không thể cập nhật tài khoản game.",
  );
}

export async function archiveGameAccount(id: number): Promise<void> {
  const response = await fetch(`/api/game-accounts/${id}`, {
    method: "DELETE",
    credentials: "same-origin",
    headers: await mutationHeaders(),
  });

  if (!response.ok) {
    throw new Error(
      await getResponseErrorMessage(
        response,
        "Không thể lưu trữ tài khoản game.",
      ),
    );
  }
}

export async function setGameAccountStatus(
  id: number,
  status: "ACTIVE" | "ARCHIVED",
): Promise<GameAccount> {
  return requestJson<GameAccount>(
    `/api/game-accounts/${id}/status`,
    {
      method: "PATCH",
      headers: await mutationHeaders(),
      body: JSON.stringify({ status }),
    },
    "Không thể cập nhật trạng thái tài khoản game.",
  );
}
