export const STATUS_KEY: Record<string, string> = {
  versions: 'versionStatus',
  categories: 'categoryStatus',
  keywords: 'keywordStatus',
  nes: 'neTypeStatus',
  nfs: 'nfConfigStatus',
  modes: 'effectiveModeStatus',
  forms: 'effectiveFormStatus',
  teams: 'teamStatus',
  features: 'featureStatus',
}

export function rowActive(row: Record<string, unknown>, kind: keyof typeof STATUS_KEY): boolean {
  const k = STATUS_KEY[kind]
  if (!k) {
    return true
  }
  return Number(row[k]) !== 0
}

