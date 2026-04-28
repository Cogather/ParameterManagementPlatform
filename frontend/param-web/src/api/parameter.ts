import { request } from './http'
import type { BatchImportResult, PageResponse } from '../types/api-response'

const enc = encodeURIComponent

export interface AvailableSequenceItem {
  sequence: number
  availability: 'FULL' | 'PARTIAL'
}

export interface AvailableSequencesData {
  sequences: AvailableSequenceItem[]
}

export interface AvailableBitsData {
  sequence: number
  availableBitIndexes: number[]
}

export interface ParameterSaveRequest {
  main: Record<string, unknown>
  changeDescriptions: Record<string, unknown>[]
}

export interface ConfigChangeTypeItem {
  changeTypeId?: number
  changeTypeName?: string
  changeTypeNameCn?: string
  changeTypeNameEn?: string
  changeSequence?: number
}

export interface ParameterTypeTreeNode {
  code: string
  name: string
}

export interface ParameterCommandTreeNode {
  commandId: string
  commandName: string
  types: ParameterTypeTreeNode[]
}

export interface BaselineCountPayload {
  baselineCount: number
}

/** 版本选择「全产品」哨兵，与路由中的具体 versionId 区分 */
export const VERSION_ALL = '__ALL__'

function basePath(productId: string, versionId: string) {
  return `/products/${enc(productId)}/versions/${enc(versionId)}/parameters`
}

function productParametersBase(productId: string) {
  return `/products/${enc(productId)}/parameters`
}

/** 产品下全部版本参数分页（版本下拉 ALL） */
export async function fetchParameterPageByProduct(
  productId: string,
  params: {
    page: number
    size: number
    commandId?: string
    /** spec-03 §4：与 commandTypeCode 同义（类型前缀，与 parameter_code 一致）；优先传此字段 */
    commandTypeId?: string
    /** @deprecated 兼容旧 Query，与 commandTypeId 二选一 */
    commandTypeCode?: string
  },
): Promise<PageResponse<Record<string, unknown>>> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: productParametersBase(productId),
    method: 'GET',
    params,
  })
  return ro.data
}

export async function fetchBaselineCountByProduct(productId: string): Promise<BaselineCountPayload> {
  const ro = await request<BaselineCountPayload>({
    url: `${productParametersBase(productId)}/baseline-count`,
    method: 'GET',
  })
  return ro.data
}

/** spec-03 §3.6：变更类型字典 */
export async function fetchConfigChangeTypes(): Promise<ConfigChangeTypeItem[]> {
  const ro = await request<ConfigChangeTypeItem[]>({
    url: '/config-change-types',
    method: 'GET',
  })
  return ro.data
}

/** spec-03 §1.2：命令 → 类型树（按产品） */
export async function fetchParameterCommandTree(productId: string): Promise<ParameterCommandTreeNode[]> {
  const ro = await request<ParameterCommandTreeNode[]>({
    url: `/products/${enc(productId)}/parameter-command-tree`,
    method: 'GET',
  })
  return ro.data
}

/** 当前版本已基线参数条数（批量写入口置灰用） */
export async function fetchBaselineCount(productId: string, versionId: string): Promise<BaselineCountPayload> {
  const ro = await request<BaselineCountPayload>({
    url: `${basePath(productId, versionId)}/baseline-count`,
    method: 'GET',
  })
  return ro.data
}

export async function fetchParameterPage(
  productId: string,
  versionId: string,
  params: {
    page: number
    size: number
    commandId?: string
    commandTypeId?: string
    /** @deprecated 与 commandTypeId 二选一 */
    commandTypeCode?: string
  },
): Promise<PageResponse<Record<string, unknown>>> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: basePath(productId, versionId),
    method: 'GET',
    params,
  })
  return ro.data
}

export async function fetchAvailableSequences(
  productId: string,
  versionId: string,
  commandId: string,
  commandTypeId: string,
): Promise<AvailableSequencesData> {
  const ro = await request<AvailableSequencesData>({
    url: `${basePath(productId, versionId)}/available-sequences`,
    method: 'GET',
    params: { commandId, commandTypeId },
  })
  return ro.data
}

export async function fetchAvailableBits(
  productId: string,
  versionId: string,
  commandId: string,
  commandTypeId: string,
  sequence: number,
): Promise<AvailableBitsData> {
  const ro = await request<AvailableBitsData>({
    url: `${basePath(productId, versionId)}/available-bits`,
    method: 'GET',
    params: { commandId, commandTypeId, sequence },
  })
  return ro.data
}

export async function createParameter(productId: string, versionId: string, body: ParameterSaveRequest) {
  const ro = await request<Record<string, unknown>>({
    url: basePath(productId, versionId),
    method: 'POST',
    data: body,
  })
  return ro.data
}

export async function updateParameter(
  productId: string,
  versionId: string,
  parameterId: number,
  body: ParameterSaveRequest,
) {
  const ro = await request<Record<string, unknown>>({
    url: `${basePath(productId, versionId)}/${parameterId}`,
    method: 'PUT',
    data: body,
  })
  return ro.data
}

export async function deleteParameter(productId: string, versionId: string, parameterId: number) {
  await request<void>({
    url: `${basePath(productId, versionId)}/${parameterId}`,
    method: 'DELETE',
  })
}

export async function baselineParameter(productId: string, versionId: string, parameterId: number) {
  await request<void>({
    url: `${basePath(productId, versionId)}/${parameterId}/baseline`,
    method: 'POST',
  })
}

export async function unbaselineParameter(productId: string, versionId: string, parameterId: number) {
  await request<void>({
    url: `${basePath(productId, versionId)}/${parameterId}/unbaseline`,
    method: 'POST',
  })
}

export type ParameterImportMode = 'FULL' | 'INCREMENTAL'

export async function importParameters(
  productId: string,
  versionId: string,
  file: File,
  opts: { mode: ParameterImportMode; commandId: string; commandTypeCode?: string },
): Promise<BatchImportResult> {
  const fd = new FormData()
  fd.append('file', file)
  const ro = await request<BatchImportResult>({
    url: `${basePath(productId, versionId)}/import`,
    method: 'POST',
    params: {
      mode: opts.mode,
      commandId: opts.commandId,
      commandTypeCode: opts.commandTypeCode,
    },
    data: fd,
  })
  return ro.data
}

/** 解析 axios 错误体中的黑名单命中正则（HTTP 500 + JSON body） */
export function extractViolatedKeywordRegex(err: unknown): string | undefined {
  const e = err as {
    cause?: { response?: { data?: { data?: { violatedKeywordRegex?: string } } } }
    response?: { data?: { data?: { violatedKeywordRegex?: string } } }
  }
  return e?.cause?.response?.data?.data?.violatedKeywordRegex || e?.response?.data?.data?.violatedKeywordRegex
}
