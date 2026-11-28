export interface AuthenticatedUser {
  username: string;
  authorities: string[];
}

export type Admin = AuthenticatedUser;
export type Customer = AuthenticatedUser;
export type Employee = AuthenticatedUser;

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface RegisterCredentials {
  email: string;
  password: string;
}
