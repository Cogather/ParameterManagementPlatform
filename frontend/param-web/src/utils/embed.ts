export function pickQueryString(query: Record<string, unknown>, key: string): string {
  const v = query[key]
  if (typeof v === 'string') {
    return v.trim()
  }
  if (Array.isArray(v) && typeof v[0] === 'string') {
    return v[0].trim()
  }
  return ''
}

export interface HostQueryContext {
  productId: string
  productName: string
  versionId: string
}

/** 解析宿主/外链注入的 Query：productId、productName、versionId */
export function parseHostQuery(query: Record<string, unknown>): HostQueryContext {
  return {
    productId: pickQueryString(query, 'productId'),
    productName: pickQueryString(query, 'productName'),
    versionId: pickQueryString(query, 'versionId'),
  }
}

/** URL 同时带 productId 与 productName 时锁定产品上下文，隐藏顶栏产品选择器 */
export function isProductLockedFromQuery(query: Record<string, unknown>): boolean {
  const { productId, productName } = parseHostQuery(query)
  return !!productId && !!productName
}

/**
 * 嵌入宿主时的紧凑布局（由环境或 URL Query 判定）
 */
export function isEmbeddedFromQuery(query: Record<string, unknown>): boolean {
  const e = query.embed
  let s = ''
  if (typeof e === 'string') {
    s = e
  } else if (Array.isArray(e) && typeof e[0] === 'string') {
    s = e[0]
  }
  return s === '1' || s === 'true' || s === 'yes'
}

export function isEmbeddedFromEnv(): boolean {
  return String(import.meta.env.VITE_EMBEDDED || '') === 'true'
}
