/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 参数服务 API 根（优先于 VITE_API_BASE_URL） */
  readonly VITE_PARAM_API_BASE_URL?: string
  /** 参数服务 API 根（兼容旧名） */
  readonly VITE_API_BASE_URL?: string
  /** 配置网关 API 根（须含路径前缀，如 https://host/config） */
  readonly VITE_CONFIG_API_BASE_URL?: string
  /** 本地：将 /config 代理到该目标（见 vite.config） */
  readonly VITE_CONFIG_PROXY_TARGET?: string

  readonly VITE_DEV_PORT?: string
  readonly VITE_PROXY_TARGET?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
