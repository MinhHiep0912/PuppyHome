import { useEffect, useState, type FormEvent } from "react";
import { createEmployee, getEmployees, updateEmployee } from "./employeeApi";
import type {
  Employee,
  EmployeeCreateInput,
  EmployeeUpdateInput,
  EmployeeWorkStatus,
} from "./employee";
import { ErrorBadge } from "../shared/ErrorBadge";
import "./EmployeesPage.css";

interface CreateFormValues {
  employeeCode: string;
  displayName: string;
  workStatus: EmployeeWorkStatus;
  password: string;
}

interface EditFormValues {
  displayName: string;
  workStatus: EmployeeWorkStatus;
  password: string;
}

const EMPTY_CREATE_FORM: CreateFormValues = {
  employeeCode: "",
  displayName: "",
  workStatus: "ACTIVE",
  password: "",
};

function getErrorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Đã xảy ra lỗi không xác định.";
}

function workStatusLabel(status: EmployeeWorkStatus): string {
  return status === "ACTIVE" ? "Đang làm" : "Ngừng làm";
}

export function EmployeesPage() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [createForm, setCreateForm] = useState<CreateFormValues>(EMPTY_CREATE_FORM);
  const [isCreating, setIsCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<EditFormValues>({
    displayName: "",
    workStatus: "ACTIVE",
    password: "",
  });
  const [isSaving, setIsSaving] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadEmployees(): Promise<void> {
      setIsLoading(true);
      setLoadError(null);

      try {
        const data = await getEmployees();
        if (!cancelled) {
          setEmployees(data);
        }
      } catch (error: unknown) {
        if (!cancelled) {
          setLoadError(getErrorMessage(error));
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void loadEmployees();
    return () => {
      cancelled = true;
    };
  }, [reloadToken]);

  async function refreshEmployees(): Promise<void> {
    try {
      setEmployees(await getEmployees());
    } catch (error: unknown) {
      setActionError(getErrorMessage(error));
    }
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setIsCreating(true);
    setCreateError(null);

    const input: EmployeeCreateInput = {
      employeeCode: createForm.employeeCode.trim(),
      displayName: createForm.displayName.trim(),
      workStatus: createForm.workStatus,
      password: createForm.password,
    };

    try {
      await createEmployee(input);
      setCreateForm(EMPTY_CREATE_FORM);
      await refreshEmployees();
    } catch (error: unknown) {
      setCreateError(getErrorMessage(error));
    } finally {
      setIsCreating(false);
    }
  }

  function startEdit(employee: Employee): void {
    setEditingId(employee.id);
    setEditForm({
      displayName: employee.displayName,
      workStatus: employee.workStatus,
      password: "",
    });
    setActionError(null);
  }

  async function handleSaveEdit(employee: Employee): Promise<void> {
    setIsSaving(true);
    setActionError(null);

    const input: EmployeeUpdateInput = {
      displayName: editForm.displayName.trim(),
      workStatus: editForm.workStatus,
    };
    if (editForm.password.length > 0) {
      input.password = editForm.password;
    }

    try {
      await updateEmployee(employee.id, input);
      setEditingId(null);
      await refreshEmployees();
    } catch (error: unknown) {
      setActionError(getErrorMessage(error));
    } finally {
      setIsSaving(false);
    }
  }

  if (isLoading) {
    return (
      <section className="employees" aria-label="Đang tải nhân viên">
        <p>Đang tải danh sách nhân viên...</p>
      </section>
    );
  }

  if (loadError) {
    return (
      <section className="employees" aria-label="Lỗi tải nhân viên">
        <ErrorBadge message={loadError} />
        <button
          type="button"
          onClick={() => setReloadToken((token) => token + 1)}
        >
          Thử lại
        </button>
      </section>
    );
  }

  return (
    <section className="employees" aria-labelledby="employees-heading">
      <header className="employees__heading">
        <div>
          <p className="guest-eyebrow">QUẢN TRỊ</p>
          <h1 id="employees-heading">Nhân viên</h1>
          <p>
            Tạo tài khoản đăng nhập cho nhân viên và quản lý trạng thái làm việc.
          </p>
        </div>
      </header>

      <section className="employees__form-panel" aria-labelledby="employee-create-heading">
        <h2 id="employee-create-heading">Tạo tài khoản nhân viên</h2>
        <form className="employees__form" onSubmit={handleCreate}>
          <label>
            Mã nhân viên
            <input
              value={createForm.employeeCode}
              onChange={(event) =>
                setCreateForm((current) => ({
                  ...current,
                  employeeCode: event.target.value,
                }))
              }
              required
              maxLength={64}
              disabled={isCreating}
            />
          </label>
          <label>
            Tên hiển thị
            <input
              value={createForm.displayName}
              onChange={(event) =>
                setCreateForm((current) => ({
                  ...current,
                  displayName: event.target.value,
                }))
              }
              required
              maxLength={128}
              disabled={isCreating}
            />
          </label>
          <label>
            Trạng thái
            <select
              value={createForm.workStatus}
              onChange={(event) =>
                setCreateForm((current) => ({
                  ...current,
                  workStatus: event.target.value as EmployeeWorkStatus,
                }))
              }
              disabled={isCreating}
            >
              <option value="ACTIVE">Đang làm</option>
              <option value="INACTIVE">Ngừng làm</option>
            </select>
          </label>
          <label>
            Mật khẩu ban đầu
            <input
              type="password"
              value={createForm.password}
              onChange={(event) =>
                setCreateForm((current) => ({
                  ...current,
                  password: event.target.value,
                }))
              }
              required
              minLength={8}
              maxLength={128}
              disabled={isCreating}
            />
          </label>
          <button type="submit" disabled={isCreating}>
            {isCreating ? "Đang tạo..." : "Tạo tài khoản"}
          </button>
        </form>
        {createError && <ErrorBadge message={createError} />}
      </section>

      <section className="employees__table-panel" aria-labelledby="employee-list-heading">
        <h2 id="employee-list-heading">Danh sách nhân viên</h2>
        {actionError && <ErrorBadge message={actionError} />}
        {employees.length === 0 ? (
          <p>Chưa có nhân viên nào.</p>
        ) : (
          <table className="employees__table">
            <thead>
              <tr>
                <th>Mã</th>
                <th>Tên hiển thị</th>
                <th>Trạng thái</th>
                <th>Mật khẩu mới</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {employees.map((employee) => (
                <tr key={employee.id}>
                  {editingId === employee.id ? (
                    <>
                      <td>{employee.employeeCode}</td>
                      <td>
                        <input
                          value={editForm.displayName}
                          onChange={(event) =>
                            setEditForm((current) => ({
                              ...current,
                              displayName: event.target.value,
                            }))
                          }
                          maxLength={128}
                          disabled={isSaving}
                        />
                      </td>
                      <td>
                        <select
                          value={editForm.workStatus}
                          onChange={(event) =>
                            setEditForm((current) => ({
                              ...current,
                              workStatus: event.target.value as EmployeeWorkStatus,
                            }))
                          }
                          disabled={isSaving}
                        >
                          <option value="ACTIVE">Đang làm</option>
                          <option value="INACTIVE">Ngừng làm</option>
                        </select>
                      </td>
                      <td>
                        <input
                          type="password"
                          placeholder="Mật khẩu mới (để trống nếu giữ nguyên)"
                          value={editForm.password}
                          onChange={(event) =>
                            setEditForm((current) => ({
                              ...current,
                              password: event.target.value,
                            }))
                          }
                          minLength={8}
                          maxLength={128}
                          disabled={isSaving}
                        />
                      </td>
                      <td>
                        <button
                          type="button"
                          disabled={isSaving}
                          onClick={() => void handleSaveEdit(employee)}
                        >
                          {isSaving ? "Đang lưu..." : "Lưu"}
                        </button>
                        <button
                          type="button"
                          disabled={isSaving}
                          onClick={() => setEditingId(null)}
                        >
                          Hủy
                        </button>
                      </td>
                    </>
                  ) : (
                    <>
                      <td>{employee.employeeCode}</td>
                      <td>{employee.displayName}</td>
                      <td>{workStatusLabel(employee.workStatus)}</td>
                      <td>—</td>
                      <td>
                        <button type="button" onClick={() => startEdit(employee)}>
                          Chỉnh sửa
                        </button>
                      </td>
                    </>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </section>
  );
}
