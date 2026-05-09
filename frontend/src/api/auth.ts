import request from "./request";
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from "../types/auth";

export const login = (payload: LoginRequest) =>
  request.post<LoginResponse>("/api/auth/login", payload);

export const register = (payload: RegisterRequest) =>
  request.post<RegisterResponse>("/api/auth/register", payload);
