<template>
  <dict-crud-table
    :can-run="!!productId"
    title="业务分类"
    :columns="columns"
    :list-url="`/products/${encodeURIComponent(productId)}/business-categories`"
    :create-url="`/products/${encodeURIComponent(productId)}/business-categories`"
    :update-url="(id) => `/products/${encodeURIComponent(productId)}/business-categories/${encodeURIComponent(id)}`"
    :delete-url="(id) => `/products/${encodeURIComponent(productId)}/business-categories/${encodeURIComponent(id)}`"
    :import-url="`/products/${encodeURIComponent(productId)}/business-categories/imports`"
    :template-url="`/api/v1/products/${encodeURIComponent(productId)}/business-categories/import-templates`"
    :export-url="`/api/v1/products/${encodeURIComponent(productId)}/business-categories/exports?page=1&size=5000`"
    id-field="categoryId"
    status-key="categoryStatus"
    :default-form="defaultForm"
    :fill-form-for-edit="fillFormForEdit"
    :validate="validate"
    :build-create-body="buildCreateBody"
    :build-update-body="buildUpdateBody"
    :op-log-product-id="productId"
    op-log-biz-table="entity_business_category"
  >
    <template #form="{ form }">
      <el-form-item label="分类名称（中文）" required>
        <el-input v-model="form.categoryNameCn" />
      </el-form-item>
      <el-form-item label="分类名称（英文）" required>
        <el-input v-model="form.categoryNameEn" />
      </el-form-item>
      <el-form-item label="包含特性范围">
        <el-input v-model="form.featureRange" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="所属类别">
        <el-input v-model="form.categoryType" />
      </el-form-item>
    </template>
  </dict-crud-table>
</template>

<script setup lang="ts">
const props = defineProps<{ productId: string }>()
import DictCrudTable from '../components/DictCrudTable.vue'

const columns = [
  { prop: 'categoryNameCn', label: '名称（中）', minWidth: 160 },
  { prop: 'categoryNameEn', label: '名称（英）', minWidth: 160 },
  { prop: 'categoryType', label: '所属类别', width: 120 },
]

function defaultForm() {
  return { categoryNameCn: '', categoryNameEn: '', featureRange: '', categoryType: '' }
}

function fillFormForEdit(form: Record<string, unknown>, row: Record<string, unknown>) {
  form.categoryNameCn = String(row.categoryNameCn ?? '')
  form.categoryNameEn = String(row.categoryNameEn ?? '')
  form.featureRange = String(row.featureRange ?? '')
  form.categoryType = String(row.categoryType ?? '')
}

function validate(form: Record<string, unknown>) {
  const cn = String(form.categoryNameCn || '').trim()
  const en = String(form.categoryNameEn || '').trim()
  if (!cn) return '请填写分类中文名称'
  if (!en) return '请填写分类英文名称'
  return undefined
}

function buildCreateBody(form: Record<string, unknown>) {
  return {
    categoryNameCn: String(form.categoryNameCn || '').trim(),
    categoryNameEn: String(form.categoryNameEn || '').trim(),
    featureRange: form.featureRange || null,
    categoryType: form.categoryType || null,
  }
}

function buildUpdateBody(form: Record<string, unknown>, editingRow: Record<string, unknown>) {
  const merged = { ...editingRow, ...form }
  if (merged.categoryStatus !== undefined) {
    merged.categoryStatus = Number(editingRow.categoryStatus)
  }
  return merged
}
</script>
