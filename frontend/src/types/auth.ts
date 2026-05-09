export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthUser {
  id?: number;
  username?: string;
  nickname?: string;
  avatarUrl?: string;
  role?: string;
  [key: string]: unknown;
}

export interface CurrentUser extends AuthUser {}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

export interface RegisterRequest {
  username: string;
  password: string;
  confirmPassword: string;
}

export interface RegisterResponse {
  userId: number;
}
