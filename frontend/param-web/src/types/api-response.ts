export interface ResponseObject<T> {
  success: boolean
  message: string
  data: T
}

export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface BatchImportFailure {
  rowNumber: number
  reason: string
}

export interface BatchImportResult {
  totalRows: number
  successCount: number
  failureCount: number
  successRowNumbers: number[]
  failures: BatchImportFailure[]
}

