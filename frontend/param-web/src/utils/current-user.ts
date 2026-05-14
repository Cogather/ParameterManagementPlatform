/**
 * 宿主（如 iframe 外框架）在 `window.username` 注入的当前用户标识，
 * 供 creatorId / updaterId 等与后端「操作人」对齐的字段使用。
 */
export function getCurrentUsername(): string {
  if (typeof window === 'undefined') {
    return ''
  }
  const u = window.username
  if (typeof u !== 'string') {
    return ''
  }
  return u
}
