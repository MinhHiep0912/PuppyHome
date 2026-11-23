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
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
