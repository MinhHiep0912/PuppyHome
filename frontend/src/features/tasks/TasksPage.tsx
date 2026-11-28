import { useEffect, useState } from "react";
import {
  claimTask,
  getTaskHistory,
  getTasks,
  reassignTask,
  releaseTask,
} from "./taskApi";
import type { Task, TaskHistoryEntry, TaskStatus } from "./task";
import { getEmployees } from "../employee/employeeApi";
import type { Employee } from "../employee/employee";
import { ErrorBadge } from "../shared/ErrorBadge";
import "./TasksPage.css";

interface TasksPageProps {
  isAdmin: boolean;
}

const STATUS_OPTIONS: { value: TaskStatus | ""; label: string }[] = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "OPEN", label: "Mở" },
  { value: "IN_PROGRESS", label: "Đang làm" },
  { value: "DONE", label: "Hoàn thành" },
  { value: "CANCELLED", label: "Đã hủy" },
];

const ACTION_LABELS: Record<string, string> = {
  CLAIM: "Nhận việc",
  RELEASE: "Bàn giao",
  REASSIGN: "Phân công lại",
  COMPLETE: "Hoàn thành",
  CANCELLED: "Hủy công việc",
};

function getErrorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Đã xảy ra lỗi không xác định.";
}

function statusLabel(status: TaskStatus): string {
  return STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status;
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString("vi-VN");
}

export function TasksPage({ isAdmin }: TasksPageProps) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [statusFilter, setStatusFilter] = useState<TaskStatus | "">("");
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyTaskId, setBusyTaskId] = useState<number | null>(null);
  const [historyTaskId, setHistoryTaskId] = useState<number | null>(null);
  const [history, setHistory] = useState<TaskHistoryEntry[] | null>(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [reassignTaskId, setReassignTaskId] = useState<number | null>(null);
  const [reassignAssigneeId, setReassignAssigneeId] = useState("");
  const [reassignReason, setReassignReason] = useState("");
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;

    async function loadTasks(): Promise<void> {
      setIsLoading(true);
      setLoadError(null);

      try {
        const data = await getTasks(statusFilter);
        if (!cancelled) {
          setTasks(data);
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

    void loadTasks();
    return () => {
      cancelled = true;
    };
  }, [statusFilter, reloadToken]);

  useEffect(() => {
    if (!isAdmin) {
      return;
    }
    let cancelled = false;

    async function loadEmployees(): Promise<void> {
      try {
        const data = await getEmployees();
        if (!cancelled) {
          setEmployees(data.filter((employee) => employee.workStatus === "ACTIVE"));
        }
      } catch {
        // Danh sách nhân viên chỉ cần cho form phân công; lỗi sẽ hiện khi submit.
      }
    }

    void loadEmployees();
    return () => {
      cancelled = true;
    };
  }, [isAdmin]);

  async function refreshTasks(): Promise<void> {
    try {
      setTasks(await getTasks(statusFilter));
    } catch (error: unknown) {
      setActionError(getErrorMessage(error));
    }
  }

  async function runAction(
    taskId: number,
    action: () => Promise<Task>,
  ): Promise<void> {
    setBusyTaskId(taskId);
    setActionError(null);
    try {
      await action();
      await refreshTasks();
      if (historyTaskId === taskId) {
        setHistory(await getTaskHistory(taskId));
      }
    } catch (error: unknown) {
      setActionError(getErrorMessage(error));
    } finally {
      setBusyTaskId(null);
    }
  }

  async function toggleHistory(taskId: number): Promise<void> {
    if (historyTaskId === taskId) {
      setHistoryTaskId(null);
      setHistory(null);
      return;
    }
    setHistoryTaskId(taskId);
    setHistory(null);
    setHistoryLoading(true);
    try {
      setHistory(await getTaskHistory(taskId));
    } catch (error: unknown) {
      setHistory([]);
      setActionError(getErrorMessage(error));
    } finally {
      setHistoryLoading(false);
    }
  }

  async function handleReassign(task: Task): Promise<void> {
    const assigneeUserId = Number(reassignAssigneeId);
    if (!Number.isFinite(assigneeUserId) || assigneeUserId <= 0) {
      setActionError("Vui lòng chọn nhân viên nhận công việc.");
      return;
    }
    await runAction(task.id, () =>
      reassignTask(task.id, {
        assigneeUserId,
        reason: reassignReason.trim() || undefined,
      }),
    );
    setReassignTaskId(null);
    setReassignAssigneeId("");
    setReassignReason("");
  }

  if (isLoading && tasks.length === 0) {
    return (
      <section className="tasks" aria-label="Đang tải công việc">
        <p>Đang tải danh sách công việc...</p>
      </section>
    );
  }

  if (loadError) {
    return (
      <section className="tasks" aria-label="Lỗi tải công việc">
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
    <section className="tasks" aria-labelledby="tasks-heading">
      <header className="tasks__heading">
        <div>
          <p className="guest-eyebrow">{isAdmin ? "QUẢN TRỊ" : "NHÂN VIÊN"}</p>
          <h1 id="tasks-heading">
            {isAdmin ? "Quản lý công việc" : "Công việc của tôi"}
          </h1>
          <p>
            {isAdmin
              ? "Theo dõi và phân công công việc phát sinh từ yêu cầu dịch vụ."
              : "Nhận và thực hiện các công việc được giao."}
          </p>
        </div>
        <label className="tasks__filter">
          Lọc theo trạng thái
          <select
            value={statusFilter}
            onChange={(event) =>
              setStatusFilter(event.target.value as TaskStatus | "")
            }
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </header>

      {actionError && <ErrorBadge message={actionError} />}

      {tasks.length === 0 ? (
        <section className="tasks__empty" aria-label="Không có công việc">
          <p>Không có công việc nào phù hợp.</p>
        </section>
      ) : (
        <ul className="tasks__list">
          {tasks.map((task) => (
            <li key={task.id} className="tasks__card">
              <div className="tasks__card-head">
                <div>
                  <h2>{task.title}</h2>
                  <p className="tasks__meta">
                    Yêu cầu #{task.serviceRequestId}
                    {task.deadlineAt &&
                      ` · Hạn ${formatDateTime(task.deadlineAt)}`}
                  </p>
                  {task.description && (
                    <p className="tasks__description">{task.description}</p>
                  )}
                </div>
                <div className="tasks__card-side">
                  <span className={`tasks__status tasks__status--${task.status.toLowerCase()}`}>
                    {statusLabel(task.status)}
                  </span>
                  <p className="tasks__assignee">
                    {task.assigneeDisplayName ?? "Chưa phân công"}
                  </p>
                </div>
              </div>

              <div className="tasks__actions">
                {!isAdmin && task.status === "OPEN" && (
                  <button
                    type="button"
                    disabled={busyTaskId === task.id}
                    onClick={() =>
                      void runAction(task.id, () => claimTask(task.id))
                    }
                  >
                    {busyTaskId === task.id ? "Đang xử lý..." : "Nhận việc"}
                  </button>
                )}
                {!isAdmin &&
                  task.status === "IN_PROGRESS" &&
                  task.currentAssigneeId !== null && (
                    <button
                      type="button"
                      disabled={busyTaskId === task.id}
                      onClick={() =>
                        void runAction(task.id, () => releaseTask(task.id))
                      }
                    >
                      Bàn giao
                    </button>
                  )}
                {isAdmin &&
                  task.status !== "DONE" &&
                  task.status !== "CANCELLED" && (
                    <button
                      type="button"
                      onClick={() => {
                        setReassignTaskId(
                          reassignTaskId === task.id ? null : task.id,
                        );
                      }}
                    >
                      Phân công lại
                    </button>
                  )}
                <button type="button" onClick={() => void toggleHistory(task.id)}>
                  {historyTaskId === task.id ? "Ẩn lịch sử" : "Lịch sử"}
                </button>
              </div>

              {isAdmin && reassignTaskId === task.id && (
                <div className="tasks__reassign">
                  <label>
                    Nhân viên nhận
                    <select
                      value={reassignAssigneeId}
                      onChange={(event) =>
                        setReassignAssigneeId(event.target.value)
                      }
                    >
                      <option value="">— Chọn nhân viên —</option>
                      {employees.map((employee) => (
                        <option key={employee.id} value={employee.id}>
                          {employee.displayName} ({employee.employeeCode})
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Lý do
                    <input
                      value={reassignReason}
                      onChange={(event) =>
                        setReassignReason(event.target.value)
                      }
                      maxLength={500}
                    />
                  </label>
                  <button
                    type="button"
                    disabled={busyTaskId === task.id}
                    onClick={() => void handleReassign(task)}
                  >
                    Xác nhận phân công
                  </button>
                </div>
              )}

              {historyTaskId === task.id && (
                <div className="tasks__history">
                  {historyLoading ? (
                    <p>Đang tải lịch sử...</p>
                  ) : !history || history.length === 0 ? (
                    <p>Chưa có lịch sử.</p>
                  ) : (
                    <ol>
                      {history.map((entry) => (
                        <li key={entry.id}>
                          <strong>{ACTION_LABELS[entry.action] ?? entry.action}</strong>
                          {" — "}
                          {entry.actorDisplayName ?? `#${entry.actorId}`}
                          {entry.fromAssigneeDisplayName &&
                            ` · Từ: ${entry.fromAssigneeDisplayName}`}
                          {entry.toAssigneeDisplayName &&
                            ` · Đến: ${entry.toAssigneeDisplayName}`}
                          {entry.reason && ` · ${entry.reason}`}
                          <small> {formatDateTime(entry.createdAt)}</small>
                        </li>
                      ))}
                    </ol>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
