import axios, { type AxiosInstance } from 'axios'
import type { ResponseObject } from '../types/api-response'
import { getApiBaseUrl, type ApiServiceKey } from './api-config'
import { getCurrentUsername } from '../utils/current-user'

function formatUserMessage(raw: unknown, fallback: string): string {
  const s = typeof raw === 'string' ? raw.trim() : ''
  const base = s || fallback
  // 后端 message 可能携带错误码前缀：CODE: 中文提示。前端仅展示中文提示即可。
  // 例：CMD_DUPLICATE_NAME: 同一产品下命令名称已存在 → 同一产品下命令名称已存在
  return base.replace(/^[A-Z0-9_]+[：:]\s*/u, '')
}

function isParameterSavePayload(data: Record<string, unknown>): boolean {
  return (
    Object.prototype.hasOwnProperty.call(data, 'main') &&
    Object.prototype.hasOwnProperty.call(data, 'changeDescriptions') &&
    data.main !== null &&
    typeof data.main === 'object' &&
    !Array.isArray(data.main)
  )
}

/**
 * 为 JSON 请求体注入当前用户（覆盖已有 creatorId/updaterId，与宿主 window.username 一致）。
 * 参数保存请求写入 `main` 子对象；FormData / 非对象不处理。
 */
function injectCurrentUserIntoRequestData(data: unknown): unknown {
  const who = getCurrentUsername()
  if (!who) {
    return data
  }
  if (data == null || typeof data !== 'object' || Array.isArray(data) || data instanceof FormData) {
    return data
  }
  const o = data as Record<string, unknown>
  if (isParameterSavePayload(o)) {
    const main = o.main as Record<string, unknown>
    return {
      ...o,
      main: { ...main, creatorId: who, updaterId: who },
    }
  }
  return { ...o, creatorId: who, updaterId: who }
}

/**
 * DELETE 常无 JSON body：将当前用户写入 query（与 POST/PUT 字段名一致；后端未读则忽略）。
 */
function mergeCurrentUserIntoQueryParams(config: { params?: unknown }): void {
  const who = getCurrentUsername()
  if (!who) {
    return
  }
  const p = config.params
  if (p instanceof URLSearchParams) {
    p.set('updaterId', who)
    p.set('creatorId', who)
    return
  }
  const base =
    p !== null && p !== undefined && typeof p === 'object' && !Array.isArray(p)
      ? { ...(p as Record<string, unknown>) }
      : {}
  config.params = { ...base, updaterId: who, creatorId: who }
}

function attachRequestInterceptor(client: AxiosInstance): void {
  client.interceptors.request.use((config) => {
    const m = (config.method || 'get').toUpperCase()
    if (m === 'POST' || m === 'PUT' || m === 'PATCH') {
      config.data = injectCurrentUserIntoRequestData(config.data)
    } else if (m === 'DELETE') {
      const d = config.data
      const hasJsonBody =
        d != null &&
        typeof d === 'object' &&
        !Array.isArray(d) &&
        !(d instanceof FormData)
      if (hasJsonBody) {
        config.data = injectCurrentUserIntoRequestData(config.data)
      } else {
        mergeCurrentUserIntoQueryParams(config)
      }
    }
    return config
  })
}

function attachResponseInterceptor(client: AxiosInstance): void {
  client.interceptors.response.use(
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
}

export const http = axios.create({
  baseURL: getApiBaseUrl('param'),
  timeout: 30_000,
})
attachRequestInterceptor(http)
attachResponseInterceptor(http)

/**
 * 为其他后端创建独立 axios 实例（与 param 相同的业务错误解析）。
 * 需先在 `api-config` 的 `API_SERVICE_REGISTRY` 登记服务键与对应 `VITE_*` 变量。
 */
export function createServiceHttp(service: ApiServiceKey): AxiosInstance {
  const client = axios.create({
    baseURL: getApiBaseUrl(service),
    timeout: 30_000,
  })
  attachRequestInterceptor(client)
  attachResponseInterceptor(client)
  return client
}

export async function request<T>(
  config: Parameters<typeof http.request>[0],
): Promise<ResponseObject<T>> {
  const resp = await http.request<ResponseObject<T>>(config)
  return resp.data
}
