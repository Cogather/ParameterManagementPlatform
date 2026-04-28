<template>
  <div>
    <div class="command-toolbar-row command-toolbar-with-oplog">
      <div class="command-toolbar-left">
        <dict-toolbar
          :can-run="!!productId"
          :template-url="templateUrl"
          :export-url="exportUrl"
          @add="openCreate"
          @refresh="loadRows"
          @import="onImport"
        />

        <el-form :inline="true" class="filter-row" label-width="0" @submit.prevent>
          <el-form-item>
            <el-select
              v-model="ownedTypeId"
              clearable
              filterable
              placeholder="按类型筛选（可选）"
              class="type-filter-select"
            >
              <el-option
                v-for="t in typeRows"
                :key="String(t.commandTypeId)"
                :label="typeOptionLabel(t)"
                :value="String(t.commandTypeId)"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="!productId" @click="loadRows">查询</el-button>
          </el-form-item>
        </el-form>
      </div>
      <el-button v-if="productId" link type="primary" class="op-log-link" @click="opLogOpen = true">操作日志</el-button>
    </div>

    <operation-log-drawer
      v-model="opLogOpen"
      :product-id="productId"
      biz-table="command_type_version_range"
      :resource-display-map="opLogResourceMap"
    />

    <el-table
        v-loading="loading"
        :data="rows"
        border
        style="width: 100%; margin-bottom: 12px"
        max-height="520"
        :empty-text="productId ? '暂无数据' : '请先选择产品'"
      >
        <el-table-column label="归属命令" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ commandLabel(String(row.ownedCommandId || '')) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ typeDisplayLabel(String(row.ownedTypeId || '')) }}
          </template>
        </el-table-column>
        <el-table-column label="所属版本" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ versionLabel(String(row.ownedVersionOrBusinessId || '')) }}
          </template>
        </el-table-column>
        <el-table-column label="序号范围" width="120">
          <template #default="{ row }">
            {{ row.startIndex }} ~ {{ row.endIndex }}
          </template>
        </el-table-column>
        <el-table-column prop="rangeDescription" label="说明" min-width="140" show-overflow-tooltip />
        <el-table-column prop="rangeType" label="区段类型" width="110" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadRows"
        @size-change="loadRows"
      />

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close>
        <el-form label-width="140px" @submit.prevent>
          <el-form-item label="归属命令" required>
            <el-select
              v-model="form.ownedCommandId"
              filterable
              placeholder="请选择命令"
              style="width: 100%"
              :disabled="dialogMode === 'edit'"
              :loading="lookupsLoading"
              @change="onFormCommandChange"
            >
              <el-option
                v-for="c in commandOptions"
                :key="c.commandId"
                :label="c.commandName"
                :value="c.commandId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="类型" required>
            <el-select
              v-model="form.ownedTypeId"
              filterable
              placeholder="请选择类型定义"
              style="width: 100%"
              :disabled="dialogMode === 'edit'"
              :loading="lookupsLoading"
            >
              <el-option
                v-for="t in typesForSelectedCommand"
                :key="String(t.commandTypeId)"
                :label="typeOptionLabel(t)"
                :value="String(t.commandTypeId)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="所属版本" required>
            <el-select
              v-model="form.ownedVersionOrBusinessId"
              filterable
              placeholder="请选择产品版本"
              style="width: 100%"
              :loading="lookupsLoading"
            >
              <el-option
                v-for="v in versionOptions"
                :key="v.versionId"
                :label="v.versionName"
                :value="v.versionId"
              />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="起始序号" required>
                <el-input-number v-model="form.startIndex" :min="1" :max="999999" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束序号" required>
                <el-input-number v-model="form.endIndex" :min="1" :max="999999" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="说明">
            <el-input v-model="form.rangeDescription" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="区段划分类型">
            <el-input v-model="form.rangeType" placeholder="可选" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submit">保存</el-button>
        </template>
      </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictToolbar from '../../config/DictToolbar.vue'
import OperationLogDrawer from '../../../components/OperationLogDrawer.vue'
import { request } from '../../../api/http'
import type { PageResponse } from '../../../types/api-response'
import {
  createRange,
  disableRange,
  fetchCommands,
  fetchCommandTypes,
  fetchRanges,
  importRanges,
  rangesExportUrl,
  rangesTemplateUrl,
  updateRange,
} from '../../../api/command-domain'

const props = defineProps<{ productId: string }>()

const ownedTypeId = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const opLogOpen = ref(false)
const opLogResourceCache = ref<Record<string, string>>({})
const opLogResourceMap = computed<Record<string, string>>(() => opLogResourceCache.value)

function rangeOpLabel(r: Record<string, unknown>) {
  const t = typeDisplayLabel(String(r.ownedTypeId || ''))
  const start = String(r.startIndex ?? '').trim()
  const end = String(r.endIndex ?? '').trim()
  const range = start && end ? `${start}~${end}` : ''
  return [t, range].filter(Boolean).join(' · ')
}

function cacheOpObject(id: string, label: string) {
  const rid = (id || '').trim()
  const v = (label || '').trim()
  if (!rid || !v) return
  if (opLogResourceCache.value[rid] === v) return
  opLogResourceCache.value = { ...opLogResourceCache.value, [rid]: v }
}
const lookupsLoading = ref(false)

const commandOptions = ref<{ commandId: string; commandName: string }[]>([])
const typeRows = ref<Record<string, unknown>[]>([])
const versionOptions = ref<{ versionId: string; versionName: string }[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref('')
const form = reactive<Record<string, unknown>>({})

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增版本区段' : '编辑版本区段'))
const templateUrl = computed(() => (props.productId ? rangesTemplateUrl(props.productId) : ''))
const exportUrl = computed(() => (props.productId ? rangesExportUrl(props.productId, { ownedTypeId: ownedTypeId.value || undefined }) : ''))

const typesForSelectedCommand = computed(() => {
  const cid = String(form.ownedCommandId || '')
  if (!cid) return typeRows.value
  return typeRows.value.filter((t) => String(t.ownedCommandId || '') === cid)
})

function commandLabel(id: string) {
  if (!id) return '—'
  const c = commandOptions.value.find((x) => x.commandId === id)
  return c?.commandName ?? '—'
}

function typeOptionLabel(t: Record<string, unknown>) {
  return `${commandLabel(String(t.ownedCommandId || ''))} · ${String(t.commandTypeName || '')}`
}

function typeDisplayLabel(typeId: string) {
  if (!typeId) return '—'
  const t = typeRows.value.find((x) => String(x.commandTypeId) === typeId)
  return t ? typeOptionLabel(t) : '—'
}

function versionLabel(id: string) {
  if (!id) return '—'
  const v = versionOptions.value.find((x) => x.versionId === id)
  return v?.versionName ?? '—'
}

async function loadLookups() {
  if (!props.productId) {
    commandOptions.value = []
    typeRows.value = []
    versionOptions.value = []
    return
  }
  lookupsLoading.value = true
  try {
    const [cmdData, typeData, verResp] = await Promise.all([
      fetchCommands(props.productId, { page: 1, size: 500 }),
      fetchCommandTypes(props.productId, { page: 1, size: 500 }),
      request<PageResponse<Record<string, unknown>>>({
        url: `/products/${encodeURIComponent(props.productId)}/versions`,
        method: 'GET',
        params: { page: 1, size: 500 },
      }),
    ])
    const cmdRec = cmdData.records || []
    // 与「命令」页保持一致：过滤掉已删除/已禁用（status=0）的命令
    commandOptions.value = cmdRec
      .filter((r) => Number(r?.commandStatus) !== 0)
      .map((r) => ({
        commandId: String(r.commandId || ''),
        commandName: String(r.commandName || ''),
      }))
    typeRows.value = typeData.records || []
    versionOptions.value = (verResp.data.records || []).map((r) => ({
      versionId: String(r.versionId || ''),
      versionName: String(r.versionName || ''),
    }))
  } finally {
    lookupsLoading.value = false
  }
}

function onFormCommandChange() {
  const tid = String(form.ownedTypeId || '')
  if (tid && !typesForSelectedCommand.value.some((t) => String(t.commandTypeId) === tid)) {
    form.ownedTypeId = ''
  }
}

function resetForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.versionRangeId = ''
  form.ownedCommandId = ''
  form.ownedTypeId = ''
  form.ownedVersionOrBusinessId = ''
  form.startIndex = undefined
  form.endIndex = undefined
  form.rangeDescription = ''
  form.rangeType = ''
}

function ensureOptionOrphans(row: Record<string, unknown>) {
  const cmdId = String(row.ownedCommandId || '')
  if (cmdId && !commandOptions.value.some((c) => c.commandId === cmdId)) {
    commandOptions.value = [...commandOptions.value, { commandId: cmdId, commandName: '（不在当前列表）' }]
  }
  const tid = String(row.ownedTypeId || '')
  if (tid && !typeRows.value.some((t) => String(t.commandTypeId) === tid)) {
    typeRows.value = [
      ...typeRows.value,
      {
        commandTypeId: tid,
        commandTypeName: '（不在当前列表）',
        ownedCommandId: cmdId,
      },
    ]
  }
  const vid = String(row.ownedVersionOrBusinessId || '')
  if (vid && !versionOptions.value.some((v) => v.versionId === vid)) {
    versionOptions.value = [...versionOptions.value, { versionId: vid, versionName: '（不在当前列表）' }]
  }
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = ''
  resetForm()
  if (ownedTypeId.value) {
    const t = typeRows.value.find((x) => String(x.commandTypeId) === ownedTypeId.value)
    if (t) {
      form.ownedCommandId = String(t.ownedCommandId || '')
      form.ownedTypeId = ownedTypeId.value
    }
  }
  dialogVisible.value = true
}

function openEdit(row: Record<string, unknown>) {
  dialogMode.value = 'edit'
  editingId.value = String(row.versionRangeId || '')
  resetForm()
  ensureOptionOrphans(row)
  form.versionRangeId = String(row.versionRangeId || '')
  form.ownedCommandId = String(row.ownedCommandId || '')
  form.ownedTypeId = String(row.ownedTypeId || '')
  form.ownedVersionOrBusinessId = String(row.ownedVersionOrBusinessId || '')
  form.startIndex = row.startIndex === null || row.startIndex === undefined ? undefined : Number(row.startIndex)
  form.endIndex = row.endIndex === null || row.endIndex === undefined ? undefined : Number(row.endIndex)
  form.rangeDescription = String(row.rangeDescription || '')
  form.rangeType = String(row.rangeType || '')
  dialogVisible.value = true
}

async function submit() {
  if (!props.productId) return
  if (!String(form.ownedCommandId || '').trim() || !String(form.ownedTypeId || '').trim()) {
    ElMessage.warning('请选择归属命令与类型')
    return
  }
  if (!String(form.ownedVersionOrBusinessId || '').trim()) {
    ElMessage.warning('请选择所属版本')
    return
  }
  if (form.startIndex === undefined || form.endIndex === undefined) {
    ElMessage.warning('请填写起始/结束序号')
    return
  }
  try {
    if (dialogMode.value === 'create') {
      await createRange(props.productId, form)
    } else {
      await updateRange(props.productId, editingId.value, form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadRows()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function deleteRow(row: Record<string, unknown>) {
  if (!props.productId) return
  const id = String(row.versionRangeId || '')
  if (!id) return
  cacheOpObject(id, rangeOpLabel(row))
  await ElMessageBox.confirm('确认删除该区段？', '确认', { type: 'warning' })
  try {
    await disableRange(props.productId, id)
    ElMessage.success('已删除')
    await loadRows()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function loadRows() {
  if (!props.productId) return
  loading.value = true
  try {
    const data = await fetchRanges(props.productId, {
      page: page.value,
      size: size.value,
      ownedTypeId: ownedTypeId.value || undefined,
    })
    const rec = data.records || []
    rows.value = rec.filter((r) => Number(r?.rangeStatus) !== 0)
    for (const r of rows.value || []) {
      cacheOpObject(String(r.versionRangeId || ''), rangeOpLabel(r))
    }
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function onImport(file: File) {
  if (!props.productId) return
  try {
    const data = await importRanges(props.productId, file)
    ElMessage.success(`导入结束：成功 ${data.successCount}，失败 ${data.failureCount}`)
    await loadRows()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导入失败')
  }
}

watch(
  () => props.productId,
  async (pid) => {
    rows.value = []
    total.value = 0
    page.value = 1
    ownedTypeId.value = ''
    if (pid) {
      await loadLookups()
      await loadRows()
    }
  },
  { immediate: true },
)
</script>

<style scoped lang="scss">
.command-toolbar-with-oplog {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: nowrap;
  gap: 8px;
  margin-bottom: 8px;
  min-width: 0;
}
.command-toolbar-left {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-wrap: nowrap;
  align-items: center;
  gap: 12px;
  overflow-x: auto;
}
.command-toolbar-left :deep(.dict-toolbar) {
  flex-shrink: 0;
  margin-bottom: 0;
}
.filter-row {
  flex: 0 1 auto;
  min-width: 0;
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  margin-bottom: 0;
  vertical-align: middle;
}
.filter-row :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 8px;
}
.filter-row :deep(.el-form-item:last-child) {
  margin-right: 0;
}
.type-filter-select {
  width: min(320px, 42vw);
  min-width: 200px;
}
.op-log-link {
  flex-shrink: 0;
  align-self: center;
}
</style>
