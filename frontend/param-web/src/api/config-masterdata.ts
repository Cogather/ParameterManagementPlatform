import { request } from './http'
import type { PageResponse } from '../types/api-response'

const enc = encodeURIComponent

/** 配置主数据分页拉取（spec-02），用于参数表单下拉 */
export async function fetchApplicableNes(productId: string, size = 500): Promise<Record<string, unknown>[]> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: `/products/${enc(productId)}/applicable-nes`,
    params: { page: 1, size },
  })
  return ro.data.records
}

export async function fetchBusinessCategories(productId: string, size = 500): Promise<Record<string, unknown>[]> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: `/products/${enc(productId)}/business-categories`,
    params: { page: 1, size },
  })
  return ro.data.records
}

export async function fetchEffectiveModes(productId: string, size = 500): Promise<Record<string, unknown>[]> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: `/products/${enc(productId)}/effective-modes`,
    params: { page: 1, size },
  })
  return ro.data.records
}

export async function fetchProjectTeams(productId: string, size = 500): Promise<Record<string, unknown>[]> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: `/products/${enc(productId)}/project-teams`,
    params: { page: 1, size },
  })
  return ro.data.records
}

export async function fetchVersionFeatures(
  productId: string,
  versionId: string,
  size = 500,
): Promise<Record<string, unknown>[]> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: `/products/${enc(productId)}/versions/${enc(versionId)}/features`,
    params: { page: 1, size },
  })
  return ro.data.records
}
