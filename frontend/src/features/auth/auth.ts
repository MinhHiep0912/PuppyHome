export interface Admin {
  username: string;
  authorities: string[];
}

export interface LoginCredentials {
  username: string;
  password: string;
}
