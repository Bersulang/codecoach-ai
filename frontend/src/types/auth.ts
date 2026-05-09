export interface LoginRequest {
  username: string
  password: string
}

export interface AuthUser {
  id?: number
  username?: string
  [key: string]: unknown
}

export interface LoginResponse {
  token: string
  user: AuthUser
}

export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
}

export interface RegisterResponse {
  userId: number
}
