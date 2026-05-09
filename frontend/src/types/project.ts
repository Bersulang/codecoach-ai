export interface ProjectVO {
  id: number
  name: string
  description: string
  techStack: string
  role: string
  highlights?: string
  difficulties?: string
  createdAt?: string
  updatedAt?: string
}

export interface ProjectListParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
}

export interface ProjectCreateRequest {
  name: string
  description: string
  techStack: string
  role?: string
  highlights?: string
  difficulties?: string
}

export interface ProjectUpdateRequest extends ProjectCreateRequest {}

export interface ProjectCreateResponse {
  projectId: number
}
