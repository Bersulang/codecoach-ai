import request from "./request";
import type {
  CurrentUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from "../types/auth";

export const login = (payload: LoginRequest) =>
  request.post<LoginResponse>("/api/auth/login", payload);

export const register = (payload: RegisterRequest) =>
  request.post<RegisterResponse>("/api/auth/register", payload);

export const getCurrentUser = () => request.get<CurrentUser>("/api/users/me");
