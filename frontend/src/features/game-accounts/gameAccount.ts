export type GameAccountStatus = "ACTIVE" | "ARCHIVED";

export interface GameAccount {
  id: number;
  customerId: number;
  name: string;
  nocape: boolean;
  status: GameAccountStatus;
  createdAt: string;
  updatedAt: string;
}

export interface GameAccountInput {
  name: string;
  nocape: boolean;
}
