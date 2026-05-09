import axios, { AxiosError } from 'axios'
import type { AxiosRequestConfig } from 'axios'
import { message } from 'antd'
import type { Result } from '../types/api'

const DEFAULT_BASE_URL = 'http://localhost:8080'

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || DEFAULT_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('userInfo')
}

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    window.location.replace('/login')
  }
}

function handleUnauthorized(messageText?: string) {
  clearAuth()
  message.error(messageText || '登录已过期，请重新登录')
  redirectToLogin()
}

function isResultPayload(data: unknown): data is Result<unknown> {
  return (
    typeof data === 'object' &&
    data !== null &&
    'code' in data &&
    typeof (data as Result<unknown>).code === 'number'
  )
}

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    const payload = response.data

    if (!isResultPayload(payload)) {
      return payload
    }

    if (payload.code === 200) {
      return payload.data
    }

    if (payload.code === 401) {
      handleUnauthorized(payload.message)
      return Promise.reject(new Error(payload.message || 'Unauthorized'))
    }

    message.error(payload.message || '请求失败')
    return Promise.reject(new Error(payload.message || 'Request failed'))
  },
  (error: AxiosError) => {
    const status = error.response?.status
    const data = error.response?.data as Result<unknown> | undefined

    if (status === 401 || data?.code === 401) {
      handleUnauthorized(data?.message)
      return Promise.reject(error)
    }

    if (data?.message) {
      message.error(data.message)
      return Promise.reject(error)
    }

    message.error('网络异常，请稍后重试')
    return Promise.reject(error)
  },
)

const request = {
  get: <T = unknown, D = unknown>(
    url: string,
    config?: AxiosRequestConfig<D>,
  ) => instance.get<T, T, D>(url, config),
  delete: <T = unknown, D = unknown>(
    url: string,
    config?: AxiosRequestConfig<D>,
  ) => instance.delete<T, T, D>(url, config),
  post: <T = unknown, D = unknown>(
    url: string,
    data?: D,
    config?: AxiosRequestConfig<D>,
  ) => instance.post<T, T, D>(url, data, config),
  put: <T = unknown, D = unknown>(
    url: string,
    data?: D,
    config?: AxiosRequestConfig<D>,
  ) => instance.put<T, T, D>(url, data, config),
}

export default request
