<template>
  <dict-crud-table
    :can-run="!!productId"
    title="产品版本"
    :columns="columns"
    :list-url="`/products/${encodeURIComponent(productId)}/versions`"
    :create-url="`/products/${encodeURIComponent(productId)}/versions`"
    :update-url="(id) => `/products/${encodeURIComponent(productId)}/versions/${encodeURIComponent(id)}`"
    :delete-url="(id) => `/products/${encodeURIComponent(productId)}/versions/${encodeURIComponent(id)}`"
    :import-url="`/products/${encodeURIComponent(productId)}/versions/imports`"
    :template-url="`/api/v1/products/${encodeURIComponent(productId)}/versions/import-templates`"
    :export-url="`/api/v1/products/${encodeURIComponent(productId)}/versions/exports?page=1&size=5000`"
    id-field="versionId"
    status-key="versionStatus"
    :default-form="defaultForm"
    :fill-form-for-edit="fillFormForEdit"
    :validate="validate"
    :build-create-body="buildCreateBody"
    :build-update-body="buildUpdateBody"
    :op-log-product-id="productId"
    op-log-biz-table="entity_version_info"
    :op-log-ignore-version-filter="true"
  >
    <template #form="{ form }">
      <el-form-item label="版本名称" required>
        <el-input v-model="form.versionName" />
      </el-form-item>
      <el-form-item label="版本类型" required>
        <el-select v-model="form.versionType" placeholder="请选择" style="width: 100%">
          <el-option v-for="opt in versionTypeOptions" :key="opt" :label="opt" :value="opt" />
        </el-select>
      </el-form-item>
      <el-form-item label="支持版本">
        <el-input v-model="form.supportedVersion" />
      </el-form-item>
      <el-form-item label="版本说明(短)">
        <el-input v-model="form.versionDescription" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="版本描述(长)">
        <el-input v-model="form.versionDesc" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="责任人" required>
        <el-input v-model="form.ownerList" placeholder="英文逗号分隔" />
      </el-form-item>
    </template>
  </dict-crud-table>
</template>

<script setup lang="ts">
const props = defineProps<{ productId: string }>()
import DictCrudTable from '../components/DictCrudTable.vue'

/** 与 entity_version_info.version_type 枚举一致（中文入库） */
const versionTypeOptions = ['在研', '补丁'] as const

const columns = [
  { prop: 'versionName', label: '名称', minWidth: 140 },
  { prop: 'versionType', label: '类型', width: 120 },
  { prop: 'supportedVersion', label: '支持版本', width: 120 },
  { prop: 'versionDescription', label: '说明', minWidth: 160, showOverflowTooltip: true },
]

function defaultForm() {
  return {
    versionName: '',
    versionType: versionTypeOptions[0],
    supportedVersion: '',
    versionDescription: '',
    versionDesc: '',
    ownerList: 'system',
  }
}

function fillFormForEdit(form: Record<string, unknown>, row: Record<string, unknown>) {
  form.versionName = String(row.versionName ?? '')
  const vt = String(row.versionType ?? '').trim()
  form.versionType = versionTypeOptions.includes(vt as (typeof versionTypeOptions)[number]) ? vt : versionTypeOptions[0]
  form.supportedVersion = String(row.supportedVersion ?? '')
  form.versionDescription = String(row.versionDescription ?? '')
  form.versionDesc = String(row.versionDesc ?? '')
  form.ownerList = String(row.ownerList ?? 'system')
}

function validate(form: Record<string, unknown>) {
  const name = String(form.versionName || '').trim()
  if (!name) return '请填写版本名称'
  const vt = String(form.versionType || '').trim()
  if (!vt) return '请选择版本类型'
  if (!versionTypeOptions.includes(vt as (typeof versionTypeOptions)[number])) return '版本类型须为「在研」或「补丁」'
  if (!String(form.ownerList || '').trim()) return '请填写责任人'
  return undefined
}

function buildCreateBody(form: Record<string, unknown>) {
  return {
    versionName: String(form.versionName || '').trim(),
    versionType: String(form.versionType || '').trim(),
    supportedVersion: form.supportedVersion || null,
    versionDescription: form.versionDescription || null,
    versionDesc: form.versionDesc || null,
    ownerList: String(form.ownerList || '').trim(),
  }
}

function buildUpdateBody(form: Record<string, unknown>, editingRow: Record<string, unknown>) {
  const merged = { ...editingRow, ...form }
  if (merged.versionStatus !== undefined) {
    merged.versionStatus = Number(editingRow.versionStatus)
  }
  return merged
}
</script>
