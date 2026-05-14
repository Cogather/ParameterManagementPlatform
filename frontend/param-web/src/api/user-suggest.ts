/**
 * 责任人自动补全（后端未就绪时本地 mock，后续可改为 request 调用真实接口）。
 */
export interface UserSuggestItem {
  /** 写入 ownerList 的工号/账号（英文逗号分隔多值） */
  value: string
  /** 下拉展示文案 */
  label: string
}

const MOCK_USERS: UserSuggestItem[] = [
  { value: 'system', label: 'system（系统）' },
  { value: 'zhangsan', label: '张三 zhangsan' },
  { value: 'lisi', label: '李四 lisi' },
  { value: 'wangwu', label: '王五 wangwu' },
  { value: 'zhaoliu', label: '赵六 zhaoliu' },
  { value: 'dev_owner', label: '开发责任人 dev_owner' },
  { value: 'qa_owner', label: '测试责任人 qa_owner' },
  { value: 'plm_admin', label: 'PLM 管理员 plm_admin' },
]

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

/**
 * 按关键字过滤 mock 用户列表（模拟网络延迟）。用于责任人远程搜索。
 *
 * @param keyword 搜索关键字（可为空，返回一批默认候选项）
 */
export async function searchUserSuggestions(keyword: string): Promise<UserSuggestItem[]> {
  await delay(150)
  const k = keyword.trim().toLowerCase()
  if (!k) {
    return MOCK_USERS.slice(0, 10)
  }
  return MOCK_USERS.filter((u) => {
    const v = u.value.toLowerCase()
    const l = u.label.toLowerCase()
    return v.includes(k) || l.includes(k)
  }).slice(0, 20)
}
