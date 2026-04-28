import { request } from './http'
import type { BatchImportResult, PageResponse } from '../types/api-response'

const enc = encodeURIComponent

function commandsBase(productId: string) {
  return `/products/${enc(productId)}/commands`
}

function typesBase(productId: string) {
  return `/products/${enc(productId)}/command-types`
}

function rangesBase(productId: string) {
  return `/products/${enc(productId)}/command-type-version-ranges`
}

export async function fetchCommands(
  productId: string,
  params: { page: number; size: number; keyword?: string },
): Promise<PageResponse<Record<string, unknown>>> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: commandsBase(productId),
    method: 'GET',
    params,
  })
  return ro.data
}

export async function createCommand(productId: string, body: Record<string, unknown>) {
  const ro = await request<Record<string, unknown>>({
    url: commandsBase(productId),
    method: 'POST',
    data: body,
  })
  return ro.data
}

export async function updateCommand(productId: string, commandId: string, body: Record<string, unknown>) {
  const ro = await request<Record<string, unknown>>({
    url: `${commandsBase(productId)}/${enc(commandId)}`,
    method: 'PUT',
    data: body,
  })
  return ro.data
}

export async function disableCommand(productId: string, commandId: string) {
  await request<void>({
    url: `${commandsBase(productId)}/${enc(commandId)}`,
    method: 'DELETE',
  })
}

export async function importCommands(productId: string, file: File): Promise<BatchImportResult> {
  const fd = new FormData()
  fd.append('file', file)
  const ro = await request<BatchImportResult>({
    url: `${commandsBase(productId)}/import`,
    method: 'POST',
    data: fd,
  })
  return ro.data
}

export function commandsTemplateUrl(productId: string) {
  return `/api/v1${commandsBase(productId)}/import-templates`
}

export function commandsExportUrl(productId: string, params: { keyword?: string }) {
  const q = new URLSearchParams()
  if (params.keyword) q.set('keyword', params.keyword)
  return `/api/v1${commandsBase(productId)}/export?${q.toString()}`
}

export async function fetchCommandTypes(
  productId: string,
  params: { page: number; size: number; keyword?: string },
): Promise<PageResponse<Record<string, unknown>>> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: typesBase(productId),
    method: 'GET',
    params,
  })
  return ro.data
}

export async function createCommandType(productId: string, body: Record<string, unknown>) {
  const ro = await request<Record<string, unknown>>({
    url: typesBase(productId),
    method: 'POST',
    data: body,
  })
  return ro.data
}

export async function updateCommandType(productId: string, commandTypeId: string, body: Record<string, unknown>) {
  const ro = await request<Record<string, unknown>>({
    url: `${typesBase(productId)}/${enc(commandTypeId)}`,
    method: 'PUT',
    data: body,
  })
  return ro.data
}

export async function importCommandTypes(productId: string, file: File): Promise<BatchImportResult> {
  const fd = new FormData()
  fd.append('file', file)
  const ro = await request<BatchImportResult>({
    url: `${typesBase(productId)}/import`,
    method: 'POST',
    data: fd,
  })
  return ro.data
}

export function commandTypesTemplateUrl(productId: string) {
  return `/api/v1${typesBase(productId)}/import-templates`
}

export function commandTypesExportUrl(productId: string, params: { keyword?: string }) {
  const q = new URLSearchParams()
  if (params.keyword) q.set('keyword', params.keyword)
  return `/api/v1${typesBase(productId)}/export?${q.toString()}`
}

export async function fetchRanges(
  productId: string,
  params: { page: number; size: number; ownedTypeId?: string },
): Promise<PageResponse<Record<string, unknown>>> {
  const ro = await request<PageResponse<Record<string, unknown>>>({
    url: rangesBase(productId),
    method: 'GET',
    params,
  })
  return ro.data
}

export async function createRange(productId: string, body: Record<string, unknown>) {
  const ro = await request<Record<string, unknown>>({
    url: rangesBase(productId),
    method: 'POST',
    data: body,
  })
  return ro.data
}

export async function updateRange(productId: string, rangeId: string, body: Record<string, unknown>) {
  const ro = await request<Record<string, unknown>>({
    url: `${rangesBase(productId)}/${enc(rangeId)}`,
    method: 'PUT',
    data: body,
  })
  return ro.data
}

export async function disableRange(productId: string, rangeId: string) {
  await request<void>({
    url: `${rangesBase(productId)}/${enc(rangeId)}`,
    method: 'DELETE',
  })
}

export async function importRanges(productId: string, file: File): Promise<BatchImportResult> {
  const fd = new FormData()
  fd.append('file', file)
  const ro = await request<BatchImportResult>({
    url: `${rangesBase(productId)}/import`,
    method: 'POST',
    data: fd,
  })
  return ro.data
}

export function rangesTemplateUrl(productId: string) {
  return `/api/v1${rangesBase(productId)}/import-templates`
}

export function rangesExportUrl(productId: string, params: { ownedTypeId?: string }) {
  const q = new URLSearchParams()
  if (params.ownedTypeId) q.set('ownedTypeId', params.ownedTypeId)
  const qs = q.toString()
  return `/api/v1${rangesBase(productId)}/export${qs ? `?${qs}` : ''}`
}

