import axios from 'axios'
import type { ResponseObject } from '../types/api-response'

function formatUserMessage(raw: unknown, fallback: string) {
  const s = typeof raw === 'string' ? raw.trim() : ''
  const base = s || fallback
  // 后端 message 可能携带错误码前缀：CODE: 中文提示。前端仅展示中文提示即可。
  // 例：CMD_DUPLICATE_NAME: 同一产品下命令名称已存在 → 同一产品下命令名称已存在
  return base.replace(/^[A-Z0-9_]+[：:]\s*/u, '')
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30_000,
})

http.interceptors.response.use(
  (resp) => {
    const data = resp.data as unknown as Partial<ResponseObject<unknown>>
    if (data && typeof data === 'object' && data.success === false) {
      const message = formatUserMessage(data.message, '业务处理失败')
      return Promise.reject(Object.assign(new Error(message), { response: resp }))
    }
    return resp
  },
  (error) => {
    const message = formatUserMessage(error?.response?.data?.message || error?.message, '网络请求失败')
    return Promise.reject(Object.assign(new Error(message), { cause: error }))
  },
)

export async function request<T>(config: Parameters<typeof http.request>[0]) {
  const resp = await http.request<ResponseObject<T>>(config)
  return resp.data
}

