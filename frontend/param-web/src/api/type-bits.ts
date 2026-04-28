import { request } from './http'

export type TypeBitItem = {
  typeBitId: string
  typeEnum: string
  bitCount: number
}

/** 类型枚举与 BIT 位数（仅数据库维护） */
export async function fetchTypeBits(): Promise<TypeBitItem[]> {
  const ro = await request<TypeBitItem[]>({
    url: '/type-bits',
    method: 'GET',
  })
  return ro.data || []
}

