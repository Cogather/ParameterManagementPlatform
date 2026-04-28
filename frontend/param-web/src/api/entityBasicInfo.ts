import { request } from './http'
import type { PageResponse } from '../types/api-response'

export interface EntityBasicInfoRow {
  entityName?: string
  productFormId?: string
  productSoftParamType?: string
  productForm?: string
  productId?: string
  ownerList?: string
  creatorId?: string
  updaterId?: string
  entityStatus?: number | null
  creationTimestamp?: string
  updateTimestamp?: string
}

export async function fetchEntityBasicInfoPage(params: {
  page: number
  size: number
  keyword?: string
  /** 与顶栏产品选择器联动：仅查当前产品下的形态行 */
  productId?: string
}): Promise<PageResponse<EntityBasicInfoRow>> {
  const ro = await request<PageResponse<EntityBasicInfoRow>>({
    url: '/entity-basic-infos',
    method: 'GET',
    params,
  })
  return ro.data
}

export async function fetchProductChoices(): Promise<EntityBasicInfoRow[]> {
  const ro = await request<EntityBasicInfoRow[]>({
    url: '/entity-basic-infos/product-choices',
    method: 'GET',
  })
  return ro.data
}

export async function createEntityBasicInfo(body: Partial<EntityBasicInfoRow>): Promise<EntityBasicInfoRow> {
  const ro = await request<EntityBasicInfoRow>({
    url: '/entity-basic-infos',
    method: 'POST',
    data: body,
  })
  return ro.data
}

export async function updateEntityBasicInfo(
  productFormId: string,
  body: Partial<EntityBasicInfoRow>,
): Promise<EntityBasicInfoRow> {
  const ro = await request<EntityBasicInfoRow>({
    url: `/entity-basic-infos/${encodeURIComponent(productFormId)}`,
    method: 'PUT',
    data: body,
  })
  return ro.data
}

export async function deleteEntityBasicInfo(productFormId: string): Promise<void> {
  await request<unknown>({
    url: `/entity-basic-infos/${encodeURIComponent(productFormId)}`,
    method: 'DELETE',
  })
}
