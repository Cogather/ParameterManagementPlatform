import { request } from './http'

/**
 * 责任人自动补全：param 服务 GET /detail，Query 参数 `info` 为待搜索字段。
 * 返回体为接口 `data` 原文，不做结构转换。
 */
export async function searchUserSuggestions(keyword: string): Promise<unknown> {
  const ro = await request<unknown>({
    url: '/detail',
    method: 'GET',
    params: { info: keyword.trim() },
  })
  return ro.data
}
