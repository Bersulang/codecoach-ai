import request from "./request";
import type { PageResult } from "../types/api";
import type {
  ProjectCreateRequest,
  ProjectCreateResponse,
  ProjectListParams,
  ProjectUpdateRequest,
  ProjectVO,
} from "../types/project";

export const getProjects = (
  params: ProjectListParams,
  options?: { silentError?: boolean },
) =>
  request.get<PageResult<ProjectVO>>("/api/projects", {
    params,
    silentError: options?.silentError,
  });

export const deleteProject = (id: number) =>
  request.delete<boolean>(`/api/projects/${id}`);

export const createProject = (payload: ProjectCreateRequest) =>
  request.post<ProjectCreateResponse>("/api/projects", payload);

export const getProjectDetail = (id: number) =>
  request.get<ProjectVO>(`/api/projects/${id}`);

export const updateProject = (id: number, payload: ProjectUpdateRequest) =>
  request.put<boolean>(`/api/projects/${id}`, payload);
