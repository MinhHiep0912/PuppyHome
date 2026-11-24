import { useCallback, useEffect, useState, type FormEvent } from "react";
import type { GameAccount, GameAccountInput } from "./gameAccount";
import {
  archiveGameAccount,
  createGameAccount,
  getGameAccounts,
  setGameAccountStatus,
  updateGameAccount,
} from "./gameAccountApi";
import "./GameAccountsPage.css";
import type { InvitationCode, InvitationCodeInput } from "./invitationCode";
import {
  createInvitationCode,
  deleteInvitationCode,
  getInvitationCodes,
  updateInvitationCode,
} from "./invitationCodeApi";

const EMPTY_FORM: GameAccountInput = {
  name: "",
  nocape: false,
};

export function GameAccountsPage() {
  const [accounts, setAccounts] = useState<GameAccount[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<GameAccountInput>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [codesByAccount, setCodesByAccount] = useState<
    Record<number, InvitationCode[]>
  >({});
  const [codeLoadingIds, setCodeLoadingIds] = useState<number[]>([]);
  const [codeEditingId, setCodeEditingId] = useState<number | null>(null);
  const [codeAccountId, setCodeAccountId] = useState<number | null>(null);
  const [codeForm, setCodeForm] = useState<InvitationCodeInput>({
    code: "",
    note: "",
  });
  const [codeFormError, setCodeFormError] = useState<string | null>(null);
  const [codeNotice, setCodeNotice] = useState<string | null>(null);

  const loadAccounts = useCallback(async () => {
    try {
      const data = await getGameAccounts();
      setAccounts(data);
      setError(null);
    } catch (caughtError: unknown) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : "Không thể tải danh sách tài khoản game.",
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    void getGameAccounts()
      .then((data) => {
        if (!cancelled) {
          setAccounts(data);
          setError(null);
        }
      })
      .catch((caughtError: unknown) => {
        if (!cancelled) {
          setError(
            caughtError instanceof Error
              ? caughtError.message
              : "Không thể tải danh sách tài khoản game.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const loadInvitationCodes = useCallback(async (accountId: number) => {
    setCodeLoadingIds((current) =>
      current.includes(accountId) ? current : [...current, accountId],
    );
    try {
      const codes = await getInvitationCodes(accountId);
      setCodesByAccount((current) => ({ ...current, [accountId]: codes }));
      setError(null);
    } catch (caughtError: unknown) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : "Không thể tải danh sách mã mời.",
      );
    } finally {
      setCodeLoadingIds((current) =>
        current.filter((id) => id !== accountId),
      );
    }
  }, []);

  useEffect(() => {
    accounts.forEach((account) => {
      if (!codesByAccount[account.id] && !codeLoadingIds.includes(account.id)) {
        void loadInvitationCodes(account.id);
      }
    });
  }, [accounts, codeLoadingIds, codesByAccount, loadInvitationCodes]);

  function startCreate(): void {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError(null);
    setNotice(null);
  }

  function startEdit(account: GameAccount): void {
    setEditingId(account.id);
    setForm({
      name: account.name,
      nocape: account.nocape,
    });
    setFormError(null);
    setNotice(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setFormError(null);
    setIsSaving(true);

    const trimmedName = form.name.trim();

    if (!trimmedName) {
      setFormError("Tên tài khoản không được để trống.");
      setIsSaving(false);
      return;
    }

    const input: GameAccountInput = {
      name: trimmedName,
      nocape: form.nocape,
    };

    try {
      if (editingId === null) {
        await createGameAccount(input);
        setNotice("Đã tạo tài khoản game.");
      } else {
        await updateGameAccount(editingId, input);
        setNotice("Đã cập nhật tài khoản game.");
      }
      setEditingId(null);
      setForm(EMPTY_FORM);
      setIsLoading(true);
      await loadAccounts();
    } catch (caughtError: unknown) {
      setFormError(
        caughtError instanceof Error
          ? caughtError.message
          : "Không thể lưu tài khoản game.",
      );
    } finally {
      setIsSaving(false);
    }
  }

  async function handleArchive(account: GameAccount): Promise<void> {
    setPendingId(account.id);
    setError(null);
    setNotice(null);
    try {
      await archiveGameAccount(account.id);
      setNotice(`Đã lưu trữ "${account.name}".`);
      setIsLoading(true);
      await loadAccounts();
    } catch (caughtError: unknown) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : "Không thể lưu trữ tài khoản game.",
      );
    } finally {
      setPendingId(null);
    }
  }

  async function handleRestore(account: GameAccount): Promise<void> {
    setPendingId(account.id);
    setError(null);
    setNotice(null);
    try {
      await setGameAccountStatus(account.id, "ACTIVE");
      setNotice(`Đã khôi phục "${account.name}".`);
      setIsLoading(true);
      await loadAccounts();
    } catch (caughtError: unknown) {
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : "Không thể khôi phục tài khoản game.",
      );
    } finally {
      setPendingId(null);
    }
  }

  function startCodeCreate(account: GameAccount): void {
    setCodeAccountId(account.id);
    setCodeEditingId(null);
    setCodeForm({ code: "", note: "" });
    setCodeFormError(null);
    setCodeNotice(null);
  }

  function startCodeEdit(code: InvitationCode): void {
    setCodeAccountId(code.gameAccountId);
    setCodeEditingId(code.id);
    setCodeForm({ code: code.code, note: code.note ?? "" });
    setCodeFormError(null);
    setCodeNotice(null);
  }

  async function handleCodeSubmit(
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> {
    event.preventDefault();
    if (codeAccountId === null) {
      return;
    }
    const code = codeForm.code.trim();
    if (!code) {
      setCodeFormError("Mã mời không được để trống.");
      return;
    }
    if (code.length > 128) {
      setCodeFormError("Mã mời không được vượt quá 128 ký tự.");
      return;
    }
    if ((codeForm.note ?? "").length > 255) {
      setCodeFormError("Ghi chú không được vượt quá 255 ký tự.");
      return;
    }

    setCodeFormError(null);
    setIsSaving(true);
    try {
      const input = {
        code,
        note: (codeForm.note ?? "").trim() || undefined,
      };
      if (codeEditingId === null) {
        await createInvitationCode(codeAccountId, input);
        setCodeNotice("Đã tạo mã mời.");
      } else {
        await updateInvitationCode(codeEditingId, input);
        setCodeNotice("Đã cập nhật mã mời.");
      }
      setCodeEditingId(null);
      setCodeAccountId(null);
      setCodeForm({ code: "", note: "" });
      await loadInvitationCodes(codeAccountId);
    } catch (caughtError: unknown) {
      setCodeFormError(
        caughtError instanceof Error
          ? caughtError.message
          : "Không thể lưu mã mời.",
      );
    } finally {
      setIsSaving(false);
    }
  }

  async function handleCodeDelete(code: InvitationCode): Promise<void> {
    setPendingId(code.id);
    setCodeNotice(null);
    try {
      await deleteInvitationCode(code.id);
      setCodeNotice("Đã xóa mã mời.");
      await loadInvitationCodes(code.gameAccountId);
    } catch (caughtError: unknown) {
      setCodeFormError(
        caughtError instanceof Error
          ? caughtError.message
          : "Không thể xóa mã mời.",
      );
    } finally {
      setPendingId(null);
    }
  }

  return (
    <section
      className="game-accounts"
      aria-labelledby="game-accounts-heading"
    >
      <header className="game-accounts__heading">
        <p className="game-accounts__eyebrow">TÀI KHOẢN GAME</p>
        <h1 id="game-accounts-heading">Quản lý tài khoản game</h1>
        <p>
          Tạo và quản lý các tài khoản game thuộc về tài khoản của bạn.
        </p>
      </header>

      {notice && (
        <div className="game-accounts__notice" role="status">
          {notice}
        </div>
      )}

      {error && (
        <div className="game-accounts__state game-accounts__state--error" role="alert">
          {error}
        </div>
      )}

      <form className="game-account-form" onSubmit={handleSubmit}>
        <h2>{editingId === null ? "Thêm tài khoản" : "Chỉnh sửa tài khoản"}</h2>

        <label>
          Tên tài khoản
          <input
            type="text"
            value={form.name}
            maxLength={128}
            required
            onChange={(event) =>
              setForm((current) => ({ ...current, name: event.target.value }))
            }
          />
        </label>

        <label className="game-account-form__checkbox">
          <input
            type="checkbox"
            checked={form.nocape}
            onChange={(event) =>
              setForm((current) => ({ ...current, nocape: event.target.checked }))
            }
          />
          No cape
        </label>

        {formError && (
          <p className="game-account-form__error" role="alert">
            {formError}
          </p>
        )}

        <div className="game-account-form__actions">
          <button type="submit" disabled={isSaving}>
            {isSaving
              ? "Đang lưu..."
              : editingId === null
                ? "Tạo tài khoản"
                : "Lưu thay đổi"}
          </button>
          {editingId !== null && (
            <button type="button" onClick={startCreate} disabled={isSaving}>
              Hủy chỉnh sửa
            </button>
          )}
        </div>
      </form>

      {isLoading && (
        <div className="game-accounts__state" role="status">
          Đang tải tài khoản game...
        </div>
      )}

      {!isLoading && !error && accounts.length === 0 && (
        <div className="game-accounts__state">
          <h2>Chưa có tài khoản game</h2>
          <p>Hãy tạo tài khoản game đầu tiên bằng biểu mẫu phía trên.</p>
        </div>
      )}

      {!isLoading && !error && accounts.length > 0 && (
        <ul className="game-accounts__list">
          {accounts.map((account) => (
            <li className="game-account-card" key={account.id}>
              <div className="game-account-card__content">
                <div className="game-account-card__title">
                  <h2>{account.name}</h2>
                  <span
                    className={`game-account-card__status game-account-card__status--${account.status.toLowerCase()}`}
                  >
                    {account.status === "ACTIVE" ? "Đang hoạt động" : "Đã lưu trữ"}
                  </span>
                </div>
                <dl className="game-account-card__meta">
                  <div>
                    <dt>No cape</dt>
                    <dd>{account.nocape ? "Có" : "Không"}</dd>
                  </div>
                </dl>
              </div>
              <div className="game-account-card__actions">
                <button type="button" onClick={() => startEdit(account)}>
                  Chỉnh sửa
                </button>
                {account.status === "ACTIVE" ? (
                  <button
                    type="button"
                    disabled={pendingId === account.id}
                    onClick={() => void handleArchive(account)}
                  >
                    {pendingId === account.id ? "Đang xử lý..." : "Lưu trữ"}
                  </button>
                ) : (
                  <button
                    type="button"
                    disabled={pendingId === account.id}
                    onClick={() => void handleRestore(account)}
                  >
                    {pendingId === account.id ? "Đang xử lý..." : "Khôi phục"}
                  </button>
                )}
              </div>
              <section
                className="invitation-codes"
                aria-label={`Mã mời của ${account.name}`}
              >
                {codeNotice && (
                  <p className="game-accounts__notice" role="status">
                    {codeNotice}
                  </p>
                )}
                <div className="invitation-codes__heading">
                  <h3>Mã mời</h3>
                  {account.status === "ACTIVE" && (
                    <button type="button" onClick={() => startCodeCreate(account)}>
                      Thêm mã mời
                    </button>
                  )}
                </div>

                {codeAccountId === account.id && (
                  <form className="invitation-code-form" onSubmit={handleCodeSubmit}>
                    <label>
                      Mã mời
                      <input
                        type="text"
                        value={codeForm.code}
                        maxLength={128}
                        required
                        onChange={(event) =>
                          setCodeForm((current) => ({
                            ...current,
                            code: event.target.value,
                          }))
                        }
                      />
                    </label>
                    <label>
                      Ghi chú
                      <input
                        type="text"
                        value={codeForm.note ?? ""}
                        maxLength={255}
                        onChange={(event) =>
                          setCodeForm((current) => ({
                            ...current,
                            note: event.target.value,
                          }))
                        }
                      />
                    </label>
                    {codeFormError && (
                      <p className="invitation-code-form__error" role="alert">
                        {codeFormError}
                      </p>
                    )}
                    <div className="invitation-code-form__actions">
                      <button type="submit" disabled={isSaving}>
                        {isSaving
                          ? "Đang lưu..."
                          : codeEditingId === null
                            ? "Tạo mã"
                            : "Lưu mã"}
                      </button>
                      <button
                        type="button"
                        disabled={isSaving}
                        onClick={() => {
                          setCodeAccountId(null);
                          setCodeEditingId(null);
                          setCodeFormError(null);
                        }}
                      >
                        Hủy
                      </button>
                    </div>
                  </form>
                )}

                {codeLoadingIds.includes(account.id) && (
                  <p role="status">Đang tải mã mời...</p>
                )}
                {!codeLoadingIds.includes(account.id) &&
                  (codesByAccount[account.id]?.length ?? 0) === 0 && (
                    <p>Chưa có mã mời.</p>
                  )}
                {!codeLoadingIds.includes(account.id) &&
                  (codesByAccount[account.id]?.length ?? 0) > 0 && (
                    <table className="invitation-codes__table">
                      <thead>
                        <tr>
                          <th>Mã</th>
                          <th>Ghi chú</th>
                          <th>Employee đã dùng</th>
                          <th>Thao tác</th>
                        </tr>
                      </thead>
                      <tbody>
                        {codesByAccount[account.id].map((code) => (
                          <tr key={code.id}>
                            <td>{code.code}</td>
                            <td>{code.note ?? "—"}</td>
                            <td>
                              {code.usedByEmployeeName
                                ? `${code.usedByEmployeeName} (${code.usedByEmployeeId})`
                                : "—"}
                            </td>
                            <td>
                              <button type="button" onClick={() => startCodeEdit(code)}>
                                Sửa
                              </button>
                              <button
                                type="button"
                                disabled={pendingId === code.id}
                                onClick={() => void handleCodeDelete(code)}
                              >
                                {pendingId === code.id ? "Đang xóa..." : "Xóa"}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
              </section>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
