import { request } from './http'

/**
 * 责任人自动补全：GET /user/detail?info=…（不经 /api/v1）。
 * baseURL 使用环境变量 `VITE_PARAM_GATEWAY_ORIGIN`（当前 param 后端网关 origin，无末尾 /）。
 * 返回体为接口 `data` 原文，不做结构转换。
 */
export async function searchUserSuggestions(keyword: string): Promise<unknown> {
  const origin = import.meta.env.VITE_PARAM_GATEWAY_ORIGIN;
  const ro = await request<unknown>({
    ...(origin ? { baseURL: origin } : {}),
    url: '/user/detail',
    method: 'GET',
    params: { info: keyword.trim() },
  })
  return ro.data
}
