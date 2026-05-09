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
