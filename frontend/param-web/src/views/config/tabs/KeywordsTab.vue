<template>
  <dict-crud-table
    :can-run="!!productId"
    title="变更来源关键字"
    :columns="columns"
    :list-url="`/products/${encodeURIComponent(productId)}/change-source-keywords`"
    :create-url="`/products/${encodeURIComponent(productId)}/change-source-keywords`"
    :update-url="(id) => `/products/${encodeURIComponent(productId)}/change-source-keywords/${encodeURIComponent(id)}`"
    :delete-url="(id) => `/products/${encodeURIComponent(productId)}/change-source-keywords/${encodeURIComponent(id)}`"
    :import-url="`/products/${encodeURIComponent(productId)}/change-source-keywords/imports`"
    :template-url="`/api/v1/products/${encodeURIComponent(productId)}/change-source-keywords/import-templates`"
    :export-url="`/api/v1/products/${encodeURIComponent(productId)}/change-source-keywords/exports?page=1&size=5000`"
    id-field="keywordId"
    status-key="keywordStatus"
    :default-form="defaultForm"
    :fill-form-for-edit="fillFormForEdit"
    :validate="validate"
    :build-create-body="buildCreateBody"
    :build-update-body="buildUpdateBody"
    :op-log-product-id="productId"
    op-log-biz-table="config_change_source_keyword"
  >
    <template #form="{ form }">
      <el-form-item label="正则" required>
        <el-input v-model="form.keywordRegex" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="原因" required>
        <el-input v-model="form.reason" type="textarea" :rows="2" />
      </el-form-item>
    </template>
  </dict-crud-table>
</template>

<script setup lang="ts">
const props = defineProps<{ productId: string }>()
import DictCrudTable from '../components/DictCrudTable.vue'

const columns = [{ prop: 'keywordRegex', label: '正则', minWidth: 220, showOverflowTooltip: true }]

function defaultForm() {
  return { keywordRegex: '', reason: '' }
}

function fillFormForEdit(form: Record<string, unknown>, row: Record<string, unknown>) {
  form.keywordRegex = String(row.keywordRegex ?? '')
  form.reason = String(row.reason ?? '')
}

function validate(form: Record<string, unknown>) {
  const regex = String(form.keywordRegex || '').trim()
  if (!regex) return '请填写正则'
  if (!String(form.reason || '').trim()) return '请填写原因'
  return undefined
}

function buildCreateBody(form: Record<string, unknown>) {
  return {
    keywordRegex: String(form.keywordRegex || '').trim(),
    reason: String(form.reason || '').trim(),
  }
}

function buildUpdateBody(form: Record<string, unknown>, editingRow: Record<string, unknown>) {
  const merged = { ...editingRow, ...form }
  if (merged.keywordStatus !== undefined) {
    merged.keywordStatus = Number(editingRow.keywordStatus)
  }
  return merged
}
</script>
