export type TaskStatus = "OPEN" | "IN_PROGRESS" | "DONE" | "CANCELLED";

export type TaskHistoryAction =
  | "CLAIM"
  | "RELEASE"
  | "REASSIGN"
  | "COMPLETE"
  | "CANCELLED";

export interface Task {
  id: number;
  serviceRequestId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  currentAssigneeId: number | null;
  assigneeDisplayName: string | null;
  deadlineAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaskHistoryEntry {
  id: number;
  taskId: number;
  actorId: number | null;
  actorDisplayName: string | null;
  action: TaskHistoryAction;
  fromAssigneeId: number | null;
  fromAssigneeDisplayName: string | null;
  toAssigneeId: number | null;
  toAssigneeDisplayName: string | null;
  reason: string | null;
  createdAt: string;
}

export interface TaskReassignInput {
  assigneeUserId: number;
  reason?: string;
}
