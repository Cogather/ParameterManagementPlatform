/**
 * 多后端 API 根：由「注册表 + 环境变量」驱动，避免每加一个服务就写 getXxxBase。
 *
 * 扩展步骤：
 * 1. 在 `API_SERVICE_REGISTRY` 增加一项：`envKeys`（按优先级尝试的 VITE_* 名）、`defaultBase`（均未配置时的兜底）。
 * 2. 在各环境 `.env.*` 中增加对应变量（通常整段根 URL，如 https://host/api/v1）。
 * 3. （可选）在 `vite-env.d.ts` 为新变量补类型声明。
 */

function trimTrailingSlashes(s: string): string {
  return s.replace(/\/+$/, '')
}

function firstNonEmptyEnv(keys: readonly string[]): string | undefined {
  const e = import.meta.env as Record<string, string | undefined>
  for (const k of keys) {
    const v = e[k]
    if (typeof v === 'string' && v.trim() !== '') return v.trim()
  }
  return undefined
}

/** 服务键 → 环境变量候选名（前者优先）+ 无 env 时的默认根（多为本地 Vite 代理路径） */
export const API_SERVICE_REGISTRY = {
  param: {
    envKeys: ['VITE_PARAM_API_BASE_URL', 'VITE_API_BASE_URL'],
    defaultBase: '/api/v1',
  },
  config: {
    envKeys: ['VITE_CONFIG_API_BASE_URL'],
    defaultBase: '/config',
  },
} as const

export type ApiServiceKey = keyof typeof API_SERVICE_REGISTRY

/**
 * 指定服务的 API 根（相对或绝对均可，末尾 / 会去掉）。
 */
export function getApiBaseUrl(service: ApiServiceKey): string {
  const { envKeys, defaultBase } = API_SERVICE_REGISTRY[service]
  const raw = firstNonEmptyEnv(envKeys as readonly string[]) ?? defaultBase
  return trimTrailingSlashes(raw)
}

/**
 * 拼接某服务下的完整请求 URL（用于 a 标签 href、window.open、导出等）。
 *
 * @param resourcePath 以 / 开头的路径（如 /products/x/commands）
 */
export function resolveApiUrl(service: ApiServiceKey, resourcePath: string): string {
  const base = getApiBaseUrl(service)
  const path = resourcePath.startsWith('/') ? resourcePath : `/${resourcePath}`
  return `${base}${path}`
}

export function resolveParamApiUrl(resourcePath: string): string {
  return resolveApiUrl('param', resourcePath)
}

export function resolveConfigApiUrl(resourcePath: string): string {
  return resolveApiUrl('config', resourcePath)
}
