<template>
  <dict-crud-table
    :can-run="!!productId"
    title="适用网元"
    :columns="columns"
    :list-url="`/products/${encodeURIComponent(productId)}/applicable-nes`"
    :create-url="`/products/${encodeURIComponent(productId)}/applicable-nes`"
    :update-url="(id) => `/products/${encodeURIComponent(productId)}/applicable-nes/${encodeURIComponent(id)}`"
    :delete-url="(id) => `/products/${encodeURIComponent(productId)}/applicable-nes/${encodeURIComponent(id)}`"
    :import-url="`/products/${encodeURIComponent(productId)}/applicable-nes/imports`"
    :template-url="`/api/v1/products/${encodeURIComponent(productId)}/applicable-nes/import-templates`"
    :export-url="`/api/v1/products/${encodeURIComponent(productId)}/applicable-nes/exports?page=1&size=5000`"
    id-field="neTypeId"
    status-key="neTypeStatus"
    :default-form="defaultForm"
    :fill-form-for-edit="fillFormForEdit"
    :validate="validate"
    :build-create-body="buildCreateBody"
    :build-update-body="buildUpdateBody"
    :op-log-product-id="productId"
    op-log-biz-table="entity_applicable_ne_dict"
  >
    <template #form="{ form }">
      <el-form-item label="名称" required>
        <el-input v-model="form.neTypeNameCn" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.neTypeDescription" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="产品形态">
        <el-input v-model="form.productForm" />
      </el-form-item>
    </template>
  </dict-crud-table>
</template>

<script setup lang="ts">
import DictCrudTable from '../components/DictCrudTable.vue'

defineProps<{ productId: string }>()

const columns = [
  { prop: 'neTypeNameCn', label: '名称', minWidth: 140 },
  { prop: 'neTypeDescription', label: '描述', minWidth: 180, showOverflowTooltip: true },
  { prop: 'productForm', label: '产品形态', minWidth: 120, showOverflowTooltip: true },
]

function defaultForm() {
  return { neTypeNameCn: '', neTypeDescription: '', productForm: '' }
}

function fillFormForEdit(form: Record<string, unknown>, row: Record<string, unknown>) {
  form.neTypeNameCn = String(row.neTypeNameCn ?? '')
  form.neTypeDescription = String(row.neTypeDescription ?? '')
  form.productForm = String(row.productForm ?? '')
}

function validate(form: Record<string, unknown>) {
  const name = String(form.neTypeNameCn || '').trim()
  if (!name) return '请填写名称'
  return undefined
}

function buildCreateBody(form: Record<string, unknown>) {
  return {
    neTypeNameCn: String(form.neTypeNameCn || '').trim(),
    neTypeDescription: form.neTypeDescription || null,
    productForm: form.productForm || null,
  }
}

function buildUpdateBody(form: Record<string, unknown>, editingRow: Record<string, unknown>) {
  const merged = { ...editingRow, ...form }
  if (merged.neTypeStatus !== undefined) {
    merged.neTypeStatus = Number(editingRow.neTypeStatus)
  }
  return merged
}
</script>

