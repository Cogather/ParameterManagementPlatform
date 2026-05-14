import { createServiceHttp } from './http'

/** 配置网关（根地址见 `api-config` 注册表 + `VITE_CONFIG_API_BASE_URL`） */
const httpConfig = createServiceHttp('config')

/**
 * 拉取配置中心版本列表（GET /core_config/v2/versions，无查询参数）。
 * 响应体形状由配置网关约定；此处保持宽松类型。
 */
export async function fetchConfigVersions(): Promise<unknown> {
  const resp = await httpConfig.get('/versions')
  return resp.data
}
