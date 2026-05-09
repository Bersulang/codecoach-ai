import request from './request'
import type { PageResult } from '../types/api'
import type { ProjectListParams, ProjectVO } from '../types/project'

export const getProjects = (params: ProjectListParams) =>
  request.get<PageResult<ProjectVO>>('/api/projects', { params })

export const deleteProject = (id: number) =>
  request.delete<boolean>(`/api/projects/${id}`)
