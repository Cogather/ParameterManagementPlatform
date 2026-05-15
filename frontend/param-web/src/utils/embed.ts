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
