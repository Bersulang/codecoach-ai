import request from "./request";

export interface UserDocument {
  id: number;
  projectId?: number | null;
  title: string;
  originalFilename: string;
  fileType: "TXT" | "MARKDOWN" | "PDF";
  fileSize: number;
  parseStatus: "PENDING" | "PARSED" | "FAILED";
  indexStatus: "PENDING" | "INDEXED" | "FAILED";
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UploadUserDocumentOptions {
  projectId?: number | null;
  title?: string;
}

export interface UserDocumentListParams {
  projectId?: number;
  fileType?: UserDocument["fileType"];
}

export function uploadUserDocument(
  file: File,
  options: UploadUserDocumentOptions = {},
) {
  const formData = new FormData();
  formData.append("file", file);
  if (options.projectId) {
    formData.append("projectId", String(options.projectId));
  }
  if (options.title?.trim()) {
    formData.append("title", options.title.trim());
  }

  return request.post<UserDocument, FormData>("/api/user-documents", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
    silentError: true,
  });
}

export function getUserDocuments(params?: UserDocumentListParams) {
  return request.get<UserDocument[]>("/api/user-documents", {
    params,
    silentError: true,
  });
}

export function getUserDocumentDetail(id: number) {
  return request.get<UserDocument>(`/api/user-documents/${id}`, {
    silentError: true,
  });
}

export function deleteUserDocument(id: number) {
  return request.delete<void>(`/api/user-documents/${id}`, {
    silentError: true,
  });
}

export function reindexUserDocument(id: number) {
  return request.post<UserDocument>(`/api/user-documents/${id}/reindex`, undefined, {
    silentError: true,
  });
}
