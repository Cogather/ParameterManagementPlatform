<template>
  <dict-crud-table
    :can-run="!!productId"
    title="生效形态"
    :columns="columns"
    :list-url="`/products/${encodeURIComponent(productId)}/effective-forms`"
    :create-url="`/products/${encodeURIComponent(productId)}/effective-forms`"
    :update-url="(id) => `/products/${encodeURIComponent(productId)}/effective-forms/${encodeURIComponent(id)}`"
    :delete-url="(id) => `/products/${encodeURIComponent(productId)}/effective-forms/${encodeURIComponent(id)}`"
    :import-url="`/products/${encodeURIComponent(productId)}/effective-forms/imports`"
    :template-url="`/api/v1/products/${encodeURIComponent(productId)}/effective-forms/import-templates`"
    :export-url="`/api/v1/products/${encodeURIComponent(productId)}/effective-forms/exports?page=1&size=5000`"
    id-field="effectiveFormId"
    status-key="effectiveFormStatus"
    :default-form="defaultForm"
    :fill-form-for-edit="fillFormForEdit"
    :validate="validate"
    :build-create-body="buildCreateBody"
    :build-update-body="buildUpdateBody"
    :op-log-product-id="productId"
    op-log-biz-table="entity_effective_form_dict"
  >
    <template #form="{ form }">
      <el-form-item label="中文名" required>
        <el-input v-model="form.effectiveFormNameCn" />
      </el-form-item>
      <el-form-item label="英文名" required>
        <el-input v-model="form.effectiveFormNameEn" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.effectiveFormDescription" type="textarea" :rows="2" />
      </el-form-item>
    </template>
  </dict-crud-table>
</template>

<script setup lang="ts">
import DictCrudTable from '../components/DictCrudTable.vue'

defineProps<{ productId: string }>()

const columns = [
  { prop: 'effectiveFormNameCn', label: '中文', minWidth: 120 },
  { prop: 'effectiveFormNameEn', label: '英文', minWidth: 120 },
  { prop: 'effectiveFormDescription', label: '描述', minWidth: 180, showOverflowTooltip: true },
]

function defaultForm() {
  return { effectiveFormNameCn: '', effectiveFormNameEn: '', effectiveFormDescription: '' }
}

function fillFormForEdit(form: Record<string, unknown>, row: Record<string, unknown>) {
  form.effectiveFormNameCn = String(row.effectiveFormNameCn ?? '')
  form.effectiveFormNameEn = String(row.effectiveFormNameEn ?? '')
  form.effectiveFormDescription = String(row.effectiveFormDescription ?? '')
}

function validate(form: Record<string, unknown>) {
  const cn = String(form.effectiveFormNameCn || '').trim()
  const en = String(form.effectiveFormNameEn || '').trim()
  if (!cn || !en) return '请填写中英文名称'
  return undefined
}

function buildCreateBody(form: Record<string, unknown>) {
  return {
    effectiveFormNameCn: String(form.effectiveFormNameCn || '').trim(),
    effectiveFormNameEn: String(form.effectiveFormNameEn || '').trim(),
    effectiveFormDescription: form.effectiveFormDescription || null,
  }
}

function buildUpdateBody(form: Record<string, unknown>, editingRow: Record<string, unknown>) {
  const merged = { ...editingRow, ...form }
  if (merged.effectiveFormStatus !== undefined) {
    merged.effectiveFormStatus = Number(editingRow.effectiveFormStatus)
  }
  return merged
}
</script>

