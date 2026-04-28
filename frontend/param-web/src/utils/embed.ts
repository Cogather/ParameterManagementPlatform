/**
 * spec-04 §3.1：嵌入宿主时的紧凑模式（宿主无需改代码）
 */
export function isEmbeddedFromQuery(query: Record<string, unknown>): boolean {
  const e = query.embed
  const s = typeof e === 'string' ? e : Array.isArray(e) && typeof e[0] === 'string' ? e[0] : ''
  return s === '1' || s === 'true' || s === 'yes'
}

export function isEmbeddedFromEnv(): boolean {
  return String(import.meta.env.VITE_EMBEDDED || '') === 'true'
}
