export interface InvitationCode {
  id: number;
  gameAccountId: number;
  code: string;
  note: string | null;
  usedByEmployeeId: number | null;
  usedByEmployeeName: string | null;
  usedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface InvitationCodeInput {
  code: string;
  note?: string;
}
