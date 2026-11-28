export type EmployeeWorkStatus = "ACTIVE" | "INACTIVE";

export interface Employee {
  id: number;
  loginIdentifier: string;
  employeeCode: string;
  displayName: string;
  workStatus: EmployeeWorkStatus;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeCreateInput {
  employeeCode: string;
  displayName: string;
  workStatus?: EmployeeWorkStatus;
  password: string;
}

export interface EmployeeUpdateInput {
  displayName?: string;
  workStatus?: EmployeeWorkStatus;
  password?: string;
}
