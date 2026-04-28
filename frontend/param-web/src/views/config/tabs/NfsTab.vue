<template>
  <dict-crud-table
    :can-run="!!productId"
    title="NF 配置"
    :columns="columns"
    :list-url="`/products/${encodeURIComponent(productId)}/nf-configs`"
    :create-url="`/products/${encodeURIComponent(productId)}/nf-configs`"
    :update-url="(id) => `/products/${encodeURIComponent(productId)}/nf-configs/${encodeURIComponent(id)}`"
    :delete-url="(id) => `/products/${encodeURIComponent(productId)}/nf-configs/${encodeURIComponent(id)}`"
    :import-url="`/products/${encodeURIComponent(productId)}/nf-configs/imports`"
    :template-url="`/api/v1/products/${encodeURIComponent(productId)}/nf-configs/import-templates`"
    :export-url="`/api/v1/products/${encodeURIComponent(productId)}/nf-configs/exports?page=1&size=5000`"
    id-field="nfConfigId"
    status-key="nfConfigStatus"
    :default-form="defaultForm"
    :fill-form-for-edit="fillFormForEdit"
    :validate="validate"
    :build-create-body="buildCreateBody"
    :build-update-body="buildUpdateBody"
    :op-log-product-id="productId"
    op-log-biz-table="entity_nf_config_dict"
  >
    <template #form="{ form }">
      <el-form-item label="名称" required>
        <el-input v-model="form.nfConfigNameCn" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.nfConfigDescription" type="textarea" :rows="2" />
      </el-form-item>
    </template>
  </dict-crud-table>
</template>

<script setup lang="ts">
import DictCrudTable from '../components/DictCrudTable.vue'

defineProps<{ productId: string }>()

const columns = [
  { prop: 'nfConfigNameCn', label: '名称', minWidth: 140 },
  { prop: 'nfConfigDescription', label: '描述', minWidth: 180, showOverflowTooltip: true },
]

function defaultForm() {
  return { nfConfigNameCn: '', nfConfigDescription: '' }
}

function fillFormForEdit(form: Record<string, unknown>, row: Record<string, unknown>) {
  form.nfConfigNameCn = String(row.nfConfigNameCn ?? '')
  form.nfConfigDescription = String(row.nfConfigDescription ?? '')
}

function validate(form: Record<string, unknown>) {
  const name = String(form.nfConfigNameCn || '').trim()
  if (!name) return '请填写名称'
  return undefined
}

function buildCreateBody(form: Record<string, unknown>) {
  return {
    nfConfigNameCn: String(form.nfConfigNameCn || '').trim(),
    nfConfigDescription: form.nfConfigDescription || null,
  }
}

function buildUpdateBody(form: Record<string, unknown>, editingRow: Record<string, unknown>) {
  const merged = { ...editingRow, ...form }
  if (merged.nfConfigStatus !== undefined) {
    merged.nfConfigStatus = Number(editingRow.nfConfigStatus)
  }
  return merged
}
</script>

