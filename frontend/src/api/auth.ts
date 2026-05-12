import request from "./request";
import type {
  CurrentUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  AvatarUploadResponse,
} from "../types/auth";

export const login = (payload: LoginRequest) =>
  request.post<LoginResponse>("/api/auth/login", payload);

export const register = (payload: RegisterRequest) =>
  request.post<RegisterResponse>("/api/auth/register", payload);

export const getCurrentUser = () => request.get<CurrentUser>("/api/users/me");

export const deleteCurrentUser = () => request.delete<boolean>("/api/users/me");

export const uploadAvatar = (file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return request.post<AvatarUploadResponse, FormData>(
    "/api/users/avatar",
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    },
  );
};
