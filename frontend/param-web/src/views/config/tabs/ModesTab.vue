<template>
  <dict-crud-table
    :can-run="!!productId"
    title="生效方式"
    :columns="columns"
    :list-url="`/products/${encodeURIComponent(productId)}/effective-modes`"
    :create-url="`/products/${encodeURIComponent(productId)}/effective-modes`"
    :update-url="(id) => `/products/${encodeURIComponent(productId)}/effective-modes/${encodeURIComponent(id)}`"
    :delete-url="(id) => `/products/${encodeURIComponent(productId)}/effective-modes/${encodeURIComponent(id)}`"
    :import-url="`/products/${encodeURIComponent(productId)}/effective-modes/imports`"
    :template-url="`/api/v1/products/${encodeURIComponent(productId)}/effective-modes/import-templates`"
    :export-url="`/api/v1/products/${encodeURIComponent(productId)}/effective-modes/exports?page=1&size=5000`"
    id-field="effectiveModeId"
    status-key="effectiveModeStatus"
    :default-form="defaultForm"
    :fill-form-for-edit="fillFormForEdit"
    :validate="validate"
    :build-create-body="buildCreateBody"
    :build-update-body="buildUpdateBody"
    :op-log-product-id="productId"
    op-log-biz-table="entity_effective_mode_dict"
  >
    <template #form="{ form }">
      <el-form-item label="中文名" required>
        <el-input v-model="form.effectiveModeNameCn" />
      </el-form-item>
      <el-form-item label="英文名" required>
        <el-input v-model="form.effectiveModeNameEn" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.effectiveModeDescription" type="textarea" :rows="2" />
      </el-form-item>
    </template>
  </dict-crud-table>
</template>

<script setup lang="ts">
import DictCrudTable from '../components/DictCrudTable.vue'

defineProps<{ productId: string }>()

const columns = [
  { prop: 'effectiveModeNameCn', label: '中文', minWidth: 120 },
  { prop: 'effectiveModeNameEn', label: '英文', minWidth: 120 },
  { prop: 'effectiveModeDescription', label: '描述', minWidth: 180, showOverflowTooltip: true },
]

function defaultForm() {
  return { effectiveModeNameCn: '', effectiveModeNameEn: '', effectiveModeDescription: '' }
}

function fillFormForEdit(form: Record<string, unknown>, row: Record<string, unknown>) {
  form.effectiveModeNameCn = String(row.effectiveModeNameCn ?? '')
  form.effectiveModeNameEn = String(row.effectiveModeNameEn ?? '')
  form.effectiveModeDescription = String(row.effectiveModeDescription ?? '')
}

function validate(form: Record<string, unknown>) {
  const cn = String(form.effectiveModeNameCn || '').trim()
  const en = String(form.effectiveModeNameEn || '').trim()
  if (!cn || !en) return '请填写中英文名称'
  return undefined
}

function buildCreateBody(form: Record<string, unknown>) {
  return {
    effectiveModeNameCn: String(form.effectiveModeNameCn || '').trim(),
    effectiveModeNameEn: String(form.effectiveModeNameEn || '').trim(),
    effectiveModeDescription: form.effectiveModeDescription || null,
  }
}

function buildUpdateBody(form: Record<string, unknown>, editingRow: Record<string, unknown>) {
  const merged = { ...editingRow, ...form }
  if (merged.effectiveModeStatus !== undefined) {
    merged.effectiveModeStatus = Number(editingRow.effectiveModeStatus)
  }
  return merged
}
</script>

