import { request } from './http'
import { searchMockUsers } from './mock-users'

/**
 * 责任人自动补全：GET /user/detail?info=…（不经 /api/v1）。
 * 本地开发（`import.meta.env.DEV`）使用前端 mock 名录，无需后端 /user/detail。
 */
export async function searchUserSuggestions(keyword: string): Promise<unknown> {
  if (import.meta.env.DEV) {
    return searchMockUsers(keyword)
  }

  const origin = import.meta.env.VITE_PARAM_GATEWAY_ORIGIN
  const ro = await request<unknown>({
    ...(origin ? { baseURL: origin } : {}),
    url: '/user/detail',
    method: 'GET',
    params: { info: keyword.trim() },
  })
  return ro.data
}
