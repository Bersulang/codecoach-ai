import request from "./request";
import type {
  CreateResumeRequest,
  ResumeListItem,
  ResumeProfile,
  ResumeProjectDraft,
  ResumeProjectSaveRequest,
  ResumeProjectSaveResponse,
} from "../types/resume";

export function createResume(payload: CreateResumeRequest) {
  return request.post<ResumeProfile>("/api/resumes", payload, {
    silentError: true,
  });
}

export function getResumes() {
  return request.get<ResumeListItem[]>("/api/resumes", {
    silentError: true,
  });
}

export function getResumeDetail(id: number | string) {
  return request.get<ResumeProfile>(`/api/resumes/${id}`, {
    silentError: true,
  });
}

export function analyzeResume(id: number | string) {
  return request.post<ResumeProfile>(`/api/resumes/${id}/analyze`, undefined, {
    silentError: true,
    timeout: 300000,
  });
}

export function deleteResume(id: number | string) {
  return request.delete<void>(`/api/resumes/${id}`, {
    silentError: true,
  });
}

export function linkResumeProject(
  resumeId: number | string,
  resumeProjectId: number | string,
  projectId: number,
) {
  return request.put<ResumeProfile>(
    `/api/resumes/${resumeId}/projects/${resumeProjectId}/link`,
    { projectId },
    { silentError: true },
  );
}

export function generateResumeProjectDraft(
  resumeId: number | string,
  resumeProjectId: number | string,
) {
  return request.post<ResumeProjectDraft>(
    `/api/resumes/${resumeId}/projects/${resumeProjectId}/draft`,
    undefined,
    { silentError: true },
  );
}

export function saveResumeProjectFromDraft(
  resumeId: number | string,
  resumeProjectId: number | string,
  payload: ResumeProjectSaveRequest,
) {
  return request.post<ResumeProjectSaveResponse>(
    `/api/resumes/${resumeId}/projects/${resumeProjectId}/save-project`,
    payload,
    { silentError: true },
  );
}
