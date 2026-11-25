export type ServiceRequestStatus =
  | "NEW"
  | "IN_PROGRESS"
  | "DONE"
  | "CANCELLED";

export interface ServiceRequest {
  id: number;
  customerId: number;
  gameAccountId: number;
  serviceId: number;
  snapshotServiceName: string;
  snapshotPriceAmount: number;
  snapshotCurrency: string;
  snapshotPriceUnit: string;
  status: ServiceRequestStatus;
  requestedStartAt: string | null;
  startAt: string;
  endAt: string;
  durationDays: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceRequestCreateInput {
  serviceId: number;
  gameAccountId: number;
  requestedStartAt: string | null;
  notes: string | null;
}
