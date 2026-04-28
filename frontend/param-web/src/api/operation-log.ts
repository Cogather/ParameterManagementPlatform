import { request } from './http'
import type { PageResponse } from '../types/api-response'

export interface OperationLogRow {
  logId: string
  bizTable: string
  ownedProductId: string
  ownedVersionId?: string | null
  resourceId?: string | null
  resourceName?: string | null
  operationType: string
  fieldLabelCn?: string | null
  oldValue?: string | null
  newValue?: string | null
  operatorId?: string | null
  operatedAt?: string | null
  logBatchId?: string | null
}

export interface OperationLogGroupLine {
  logId: string
  fieldLabelCn?: string | null
  oldValue?: string | null
  newValue?: string | null
  operatedAt?: string | null
}

export interface OperationLogGroupItem {
  groupKey: string
  bizTable: string
  ownedProductId: string
  ownedVersionId?: string | null
  resourceId?: string | null
  resourceName?: string | null
  operationType: string
  operatorId?: string | null
  operatedAt?: string | null
  logBatchId?: string | null
  itemCount: number
  items: OperationLogGroupLine[]
}

export async function fetchOperationLogs(params: {
  productId: string
  bizTable: string
  versionId?: string
  /** 为 true 且未传 versionId 时查全部版本相关记录（与后端 ignoreVersionFilter 一致） */
  ignoreVersionFilter?: boolean
  resourceId?: string
  /** ISO-8601 本地/带时区字符串 */
  operatedFrom?: string
  operatedTo?: string
  /** 如 operatedAt,asc 升序，否则默认倒序 */
  sort?: string
  page: number
  size: number
}): Promise<PageResponse<OperationLogRow>> {
  const ro = await request<PageResponse<OperationLogRow>>({
    url: '/operation-logs',
    method: 'GET',
    params: {
      productId: params.productId,
      bizTable: params.bizTable,
      versionId: params.versionId,
      ignoreVersionFilter: params.ignoreVersionFilter,
      resourceId: params.resourceId,
      operatedFrom: params.operatedFrom,
      operatedTo: params.operatedTo,
      sort: params.sort,
      page: params.page,
      size: params.size,
    },
  })
  return ro.data
}

export async function fetchOperationLogGroups(params: {
  productId: string
  bizTable: string
  versionId?: string
  ignoreVersionFilter?: boolean
  resourceId?: string
  operatedFrom?: string
  operatedTo?: string
  page: number
  size: number
}): Promise<PageResponse<OperationLogGroupItem>> {
  const ro = await request<PageResponse<OperationLogGroupItem>>({
    url: '/operation-logs/groups',
    method: 'GET',
    params: {
      productId: params.productId,
      bizTable: params.bizTable,
      versionId: params.versionId,
      ignoreVersionFilter: params.ignoreVersionFilter,
      resourceId: params.resourceId,
      operatedFrom: params.operatedFrom,
      operatedTo: params.operatedTo,
      page: params.page,
      size: params.size,
    },
  })
  return ro.data
}
