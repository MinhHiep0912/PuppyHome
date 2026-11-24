import {
  fetchCsrfToken,
  getResponseErrorMessage,
} from "../auth/authApi";
import type { InvitationCode, InvitationCodeInput } from "./invitationCode";

const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

async function mutationHeaders(): Promise<HeadersInit> {
  const csrfToken = await fetchCsrfToken();
  return {
    "Content-Type": "application/json",
    [CSRF_HEADER_NAME]: csrfToken,
  };
}

async function handleMutation<T>(
  path: string,
  init: RequestInit,
  fallbackMessage: string,
): Promise<T> {
  const response = await fetch(path, {
    credentials: "same-origin",
    ...init,
  });
  if (!response.ok) {
    throw new Error(await getResponseErrorMessage(response, fallbackMessage));
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export async function getInvitationCodes(
  accountId: number,
): Promise<InvitationCode[]> {
  const response = await fetch(
    `/api/game-accounts/${accountId}/invitation-codes`,
    { credentials: "same-origin" },
  );
  if (!response.ok) {
    throw new Error(
      await getResponseErrorMessage(
        response,
        "Không thể tải danh sách mã mời.",
      ),
    );
  }
  return (await response.json()) as InvitationCode[];
}

export async function createInvitationCode(
  accountId: number,
  input: InvitationCodeInput,
): Promise<InvitationCode> {
  return handleMutation(
    `/api/game-accounts/${accountId}/invitation-codes`,
    {
      method: "POST",
      headers: await mutationHeaders(),
      body: JSON.stringify(input),
    },
    "Không thể tạo mã mời.",
  );
}

export async function updateInvitationCode(
  codeId: number,
  input: InvitationCodeInput,
): Promise<InvitationCode> {
  return handleMutation(
    `/api/invitation-codes/${codeId}`,
    {
      method: "PUT",
      headers: await mutationHeaders(),
      body: JSON.stringify(input),
    },
    "Không thể cập nhật mã mời.",
  );
}

export async function deleteInvitationCode(codeId: number): Promise<void> {
  await handleMutation<void>(
    `/api/invitation-codes/${codeId}`,
    {
      method: "DELETE",
      headers: await mutationHeaders(),
    },
    "Không thể xóa mã mời.",
  );
}
