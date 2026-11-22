export interface Admin {
  username: string;
  authorities: string[];
}

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface Customer {
  username: string;
  authorities: string[];
}

export interface RegisterCredentials {
  email: string;
  password: string;
}
