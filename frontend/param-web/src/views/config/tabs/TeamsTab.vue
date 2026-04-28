<template>
  <dict-crud-table
    :can-run="!!productId"
    title="项目组"
    :columns="columns"
    :list-url="`/products/${encodeURIComponent(productId)}/project-teams`"
    :create-url="`/products/${encodeURIComponent(productId)}/project-teams`"
    :update-url="(id) => `/products/${encodeURIComponent(productId)}/project-teams/${encodeURIComponent(id)}`"
    :delete-url="(id) => `/products/${encodeURIComponent(productId)}/project-teams/${encodeURIComponent(id)}`"
    :import-url="`/products/${encodeURIComponent(productId)}/project-teams/imports`"
    :template-url="`/api/v1/products/${encodeURIComponent(productId)}/project-teams/import-templates`"
    :export-url="`/api/v1/products/${encodeURIComponent(productId)}/project-teams/exports?page=1&size=5000`"
    id-field="teamId"
    status-key="teamStatus"
    :default-form="defaultForm"
    :fill-form-for-edit="fillFormForEdit"
    :validate="validate"
    :build-create-body="buildCreateBody"
    :build-update-body="buildUpdateBody"
    :op-log-product-id="productId"
    op-log-biz-table="project_team_dict"
  >
    <template #form="{ form }">
      <el-form-item label="名称" required>
        <el-input v-model="form.teamName" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.teamDescription" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="责任人" required>
        <el-input v-model="form.ownerList" placeholder="多人可用逗号分隔" />
      </el-form-item>
    </template>
  </dict-crud-table>
</template>

<script setup lang="ts">
import DictCrudTable from '../components/DictCrudTable.vue'

defineProps<{ productId: string }>()

const columns = [
  { prop: 'teamName', label: '名称', minWidth: 140 },
  { prop: 'ownerList', label: '责任人', minWidth: 160, showOverflowTooltip: true },
  { prop: 'teamDescription', label: '描述', minWidth: 180, showOverflowTooltip: true },
]

function defaultForm() {
  return { teamName: '', teamDescription: '', ownerList: '' }
}

function fillFormForEdit(form: Record<string, unknown>, row: Record<string, unknown>) {
  form.teamName = String(row.teamName ?? '')
  form.teamDescription = String(row.teamDescription ?? '')
  form.ownerList = String(row.ownerList ?? '')
}

function validate(form: Record<string, unknown>) {
  const name = String(form.teamName || '').trim()
  const owners = String(form.ownerList || '').trim()
  if (!name || !owners) return '请填写名称与责任人'
  return undefined
}

function buildCreateBody(form: Record<string, unknown>) {
  return {
    teamName: String(form.teamName || '').trim(),
    teamDescription: form.teamDescription || null,
    ownerList: String(form.ownerList || '').trim(),
  }
}

function buildUpdateBody(form: Record<string, unknown>, editingRow: Record<string, unknown>) {
  const merged = { ...editingRow, ...form }
  if (merged.teamStatus !== undefined) {
    merged.teamStatus = Number(editingRow.teamStatus)
  }
  return merged
}
</script>

