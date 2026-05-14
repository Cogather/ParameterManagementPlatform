<template>
  <el-select
    :model-value="selectedIds"
    class="user-select"
    multiple
    filterable
    remote
    :reserve-keyword="false"
    :remote-method="onRemoteQuery"
    :loading="loading"
    value-key="value"
    :placeholder="placeholder"
    :disabled="disabled"
    :clearable="clearable"
    :multiple-limit="multipleLimit"
    collapse-tags
    collapse-tags-tooltip
    style="width: 100%"
    @update:model-value="onSelectChange"
    @visible-change="onVisibleChange"
  >
    <el-option v-for="o in mergedOptions" :key="o.value" :label="o.label" :value="o.value">
      <span>{{ o.dropdownLabel }}</span>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { searchUserSuggestions } from '../api/user-suggest'

/** el-option：value 存 name；label 仅 name（选中标签）；dropdownLabel 为下拉行「name dept」 */
interface SuggestionRow {
  value: string
  label: string
  dropdownLabel: string
}

function formatPersonLabel(name: string, dept: string): string {
  const n = name.trim()
  const d = dept.trim()
  if (n && d) {
    return `${n} ${d}`
  }
  return n || d
}

function mapUserDetailToOptions(data: unknown): SuggestionRow[] {
  if (!Array.isArray(data)) {
    return []
  }
  const out: SuggestionRow[] = []
  const seen = new Set<string>()
  for (const item of data) {
    if (!item || typeof item !== 'object') {
      continue
    }
    const rec = item as Record<string, unknown>
    const name = String(rec.name ?? '').trim()
    if (!name || seen.has(name)) {
      continue
    }
    seen.add(name)
    const dept = String(rec.dept ?? '')
    out.push({
      value: name,
      label: name,
      dropdownLabel: formatPersonLabel(name, dept),
    })
  }
  return out
}

const props = withDefaults(
  defineProps<{
    /** 与后端一致：英文逗号分隔的责任人 name 串 */
    modelValue: string
    placeholder?: string
    /** 拼接后整体最大长度（与后端字段长度对齐，超出会截断尾部责任人） */
    maxlength?: number
    disabled?: boolean
    clearable?: boolean
    /** 最多可选人数，防止异常超长 */
    multipleLimit?: number
  }>(),
  {
    placeholder: '请搜索后从列表中选择责任人，可多选',
    maxlength: 255,
    disabled: false,
    clearable: true,
    multipleLimit: 30,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const loading = ref(false)
const suggestionList = ref<SuggestionRow[]>([])

function parseOwnerList(raw: string): string[] {
  return (raw || '')
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
}

function joinOwnerList(ids: string[]): string {
  return ids.join(',')
}

/** 在不超过 maxlength 的前提下保留尽量多的前缀责任人 */
function capByMaxLength(ids: string[], maxLen: number): string[] {
  const out: string[] = []
  for (const id of ids) {
    const trial = [...out, id].join(',')
    if (trial.length > maxLen) {
      break
    }
    out.push(id)
  }
  return out
}

const selectedIds = computed(() => parseOwnerList(props.modelValue))

const mergedOptions = computed((): SuggestionRow[] => {
  const map = new Map<string, SuggestionRow>()
  for (const o of suggestionList.value) {
    map.set(o.value, o)
  }
  for (const id of selectedIds.value) {
    if (!map.has(id)) {
      map.set(id, { value: id, label: id, dropdownLabel: id })
    }
  }
  return [...map.values()]
})

async function onRemoteQuery(query: string): Promise<void> {
  loading.value = true
  try {
    const data = await searchUserSuggestions(query.trim())
    suggestionList.value = mapUserDetailToOptions(data)
  } catch {
    suggestionList.value = []
  } finally {
    loading.value = false
  }
}

function onVisibleChange(open: boolean): void {
  if (open && suggestionList.value.length === 0) {
    void onRemoteQuery('')
  }
}

function onSelectChange(next: unknown): void {
  const raw = Array.isArray(next) ? next : []
  const normalized = [...new Set(raw.map((x) => String(x).trim()).filter(Boolean))]
  const limited = normalized.slice(0, props.multipleLimit)
  const capped = capByMaxLength(limited, props.maxlength)
  emit('update:modelValue', joinOwnerList(capped))
}
</script>

<style scoped>
.user-select {
  width: 100%;
}
</style>
