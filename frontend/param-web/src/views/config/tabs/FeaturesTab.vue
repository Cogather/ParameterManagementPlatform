<template>
  <el-form :inline="true" style="margin-bottom: 10px">
    <el-form-item label="版本">
      <el-select
        v-model="versionId"
        placeholder="请选择版本"
        style="width: 260px"
        clearable
        filterable
        :disabled="!productId"
      >
        <el-option v-for="v in versionOptions" :key="v.versionId" :label="v.versionName" :value="v.versionId" />
      </el-select>
    </el-form-item>
  </el-form>

  <dict-crud-table
    :can-run="!!productId && !!versionId"
    :empty-hint-when-disabled="!productId ? '请先选择产品' : '请先选择版本'"
    title="版本特性"
    :columns="columns"
    :list-url="listUrl"
    :create-url="listUrl"
    :update-url="(id) => `${listUrl}/${encodeURIComponent(id)}`"
    :delete-url="(id) => `${listUrl}/${encodeURIComponent(id)}`"
    :import-url="`${listUrl}/imports`"
    :template-url="`${apiV1Prefix}${listUrl}/import-templates`"
    :export-url="`${apiV1Prefix}${listUrl}/exports?page=1&size=5000`"
    id-field="featureId"
    status-key="featureStatus"
    :default-form="defaultForm"
    :fill-form-for-edit="fillFormForEdit"
    :validate="validate"
    :build-create-body="buildCreateBody"
    :build-update-body="buildUpdateBody"
    :op-log-product-id="productId"
    op-log-biz-table="version_feature_dict"
    :op-log-version-id="versionId"
    :op-log-require-version="true"
  >
    <template #form="{ form }">
      <el-form-item label="中文名" required>
        <el-input v-model="form.featureNameCn" />
      </el-form-item>
      <el-form-item label="英文名" required>
        <el-input v-model="form.featureNameEn" />
      </el-form-item>
      <el-form-item label="引入类型" required>
        <el-input v-model="form.introduceType" placeholder="如 incremental" />
      </el-form-item>
      <el-form-item label="继承参考版本">
        <el-select
          v-model="form.inheritReferenceVersionId"
          clearable
          filterable
          placeholder="可选"
          style="width: 100%"
        >
          <el-option
            v-for="v in versionOptions"
            :key="v.versionId"
            :label="v.versionName"
            :value="v.versionId"
          />
        </el-select>
      </el-form-item>
    </template>
  </dict-crud-table>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { request } from '../../../api/http'
import type { PageResponse } from '../../../types/api-response'
import DictCrudTable from '../components/DictCrudTable.vue'

const apiV1Prefix = '/api/v1'
const props = defineProps<{ productId: string }>()

const versionId = ref('')
const versionOptions = ref<{ versionId: string; versionName: string }[]>([])

const columns = [
  { prop: 'featureNameCn', label: '中文名', minWidth: 140 },
  { prop: 'featureNameEn', label: '英文名', minWidth: 140, showOverflowTooltip: true },
  { prop: 'introduceType', label: '引入类型', width: 120 },
]

const listUrl = computed(() => {
  if (!props.productId || !versionId.value) return ''
  return `/products/${encodeURIComponent(props.productId)}/versions/${encodeURIComponent(versionId.value)}/features`
})

watch(
  () => props.productId,
  async (pid) => {
    versionId.value = ''
    versionOptions.value = []
    if (!pid) return
    // 版本下拉来源：产品版本配置列表
    const resp = await request<PageResponse<Record<string, unknown>>>({
      url: `/products/${encodeURIComponent(pid)}/versions`,
      method: 'GET',
      params: { page: 1, size: 200 },
    })
    versionOptions.value = (resp.data.records as any[]).map((r) => ({
      versionId: String(r.versionId || ''),
      versionName: String(r.versionName || r.versionId || ''),
    }))
  },
  { immediate: true },
)

function defaultForm() {
  return {
    featureNameCn: '',
    featureNameEn: '',
    introduceType: 'incremental',
    inheritReferenceVersionId: '',
  }
}

function fillFormForEdit(form: Record<string, unknown>, row: Record<string, unknown>) {
  form.featureNameCn = String(row.featureNameCn ?? '')
  form.featureNameEn = String(row.featureNameEn ?? '')
  form.introduceType = String(row.introduceType ?? 'incremental')
  const ir = String(row.inheritReferenceVersionId ?? '')
  form.inheritReferenceVersionId = ir
  if (ir && !versionOptions.value.some((v) => v.versionId === ir)) {
    versionOptions.value = [...versionOptions.value, { versionId: ir, versionName: '（不在当前列表）' }]
  }
}

function validate(form: Record<string, unknown>) {
  const cn = String(form.featureNameCn || '').trim()
  const en = String(form.featureNameEn || '').trim()
  const t = String(form.introduceType || '').trim()
  if (!cn || !en || !t) return '请填写中英文名称与引入类型'
  return undefined
}

function buildCreateBody(form: Record<string, unknown>) {
  return {
    featureNameCn: String(form.featureNameCn || '').trim(),
    featureNameEn: String(form.featureNameEn || '').trim(),
    introduceType: String(form.introduceType || '').trim(),
    inheritReferenceVersionId: form.inheritReferenceVersionId || null,
  }
}

function buildUpdateBody(form: Record<string, unknown>, editingRow: Record<string, unknown>) {
  const merged = { ...editingRow, ...form }
  if (merged.featureStatus !== undefined) {
    merged.featureStatus = Number(editingRow.featureStatus)
  }
  return merged
}
</script>

