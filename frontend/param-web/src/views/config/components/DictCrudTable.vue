<template>
  <div class="dict-toolbar-with-oplog">
    <div class="dict-toolbar-left">
      <dict-toolbar
        :can-run="canRun"
        :template-url="templateUrl"
        :export-url="exportUrl"
        @add="openCreate"
        @refresh="load"
        @import="(f) => doImport(f)"
      />
    </div>
    <el-button v-if="showOpLog" link type="primary" class="op-log-link" @click="opLogOpen = true">操作日志</el-button>
  </div>
  <operation-log-drawer
    v-if="showOpLog"
    v-model="opLogOpen"
    :product-id="opLogProductId || ''"
    :biz-table="opLogBizTable || ''"
    :version-id="opLogVersionId"
    :ignore-version-filter="opLogIgnoreVersionFilter"
    :resource-display-map="opLogResourceMap"
  />

  <el-table
    :data="rows"
    border
    style="width: 100%"
    max-height="520"
    :empty-text="tableEmptyText"
  >
    <el-table-column
      v-for="c in columns"
      :key="c.prop"
      :prop="c.prop"
      :label="c.label"
      :width="c.width"
      :min-width="c.minWidth"
      :show-overflow-tooltip="c.showOverflowTooltip"
    />

    <el-table-column label="操作" width="200" fixed="right">
      <template #default="{ row }">
        <el-button link type="primary" :disabled="!canRun" @click="openEdit(row)">修改</el-button>
        <el-button link type="danger" :disabled="!canRun" @click="deleteRow(row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close @closed="onDialogClosed">
    <el-form label-width="140px" @submit.prevent>
      <slot name="form" :form="form" :mode="dialogMode" />
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :disabled="!canRun" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictToolbar from '../DictToolbar.vue'
import OperationLogDrawer from '../../../components/OperationLogDrawer.vue'
import { request } from '../../../api/http'
import type { BatchImportResult, PageResponse } from '../../../types/api-response'

type ColumnDef = {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  showOverflowTooltip?: boolean
}

const props = defineProps<{
  /** 外部决定是否可操作（比如 features 需要 product+version） */
  canRun: boolean
  /** canRun 为 false 时表格空文案（默认「请先选择产品」） */
  emptyHintWhenDisabled?: string
  /** 标题用于弹窗 */
  title: string
  /** 列配置（不含状态/操作列） */
  columns: ColumnDef[]

  /** 列表/新增/更新/删除/导入接口 */
  listUrl: string
  createUrl: string
  updateUrl: (id: string) => string
  deleteUrl: (id: string) => string
  importUrl: string
  templateUrl: string
  exportUrl: string

  /** 主键字段名（行对象里用于更新/删除） */
  idField: string
  /**
   * 状态字段名（可选）。
   * 不在页面展示，但用于过滤“已删除/已禁用”（status=0）的记录，让删除后列表不再显示。
   */
  statusKey?: string

  /** 生成/填充/校验/构建请求体 */
  defaultForm: () => Record<string, unknown>
  fillFormForEdit: (form: Record<string, unknown>, row: Record<string, unknown>) => void
  validate: (form: Record<string, unknown>, mode: 'create' | 'edit') => string | undefined
  buildCreateBody: (form: Record<string, unknown>) => Record<string, unknown>
  buildUpdateBody: (form: Record<string, unknown>, editingRow: Record<string, unknown>) => Record<string, unknown>

  /** 操作日志：与命令页同模式，见 OperationLogAppService 中 biz_table 常量名 */
  opLogProductId?: string
  opLogBizTable?: string
  /** 有版本维度的子模块（如版本特性）须传；与 opLogRequireVersion 配合 */
  opLogVersionId?: string
  opLogIgnoreVersionFilter?: boolean
  /** 为 true 时仅当 opLogVersionId 有值才显示操作日志 */
  opLogRequireVersion?: boolean
  /**
   * 操作日志「操作对象」展示用：行里哪个字段作为可读名称（默认取 columns 第一列 prop）
   */
  opLogResourceLabelProp?: string
}>()

const rows = ref<Record<string, unknown>[]>([])

const opLogOpen = ref(false)

const opLogResourceCache = ref<Record<string, string>>({})
const opLogResourceMap = computed<Record<string, string>>(() => opLogResourceCache.value)

const resourceLabelProp = computed(() => {
  const manual = (props.opLogResourceLabelProp || '').trim()
  if (manual) return manual
  const first = props.columns?.[0]?.prop
  return typeof first === 'string' && first ? first : props.idField
})

function cacheOpObjectFromRow(row: Record<string, unknown>) {
  const id = String(row[props.idField] || '').trim()
  if (!id) return
  const lp = resourceLabelProp.value
  const label = lp ? String(row[lp] ?? '').trim() : ''
  if (!label) return
  if (opLogResourceCache.value[id] === label) return
  opLogResourceCache.value = { ...opLogResourceCache.value, [id]: label }
}

const showOpLog = computed(() => {
  if (!props.opLogProductId?.trim() || !props.opLogBizTable?.trim()) {
    return false
  }
  if (props.opLogRequireVersion) {
    return !!props.opLogVersionId?.trim()
  }
  return true
})

const tableEmptyText = computed(() => {
  if (props.canRun) {
    return '暂无数据'
  }
  return props.emptyHintWhenDisabled || '请先选择产品'
})

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingRow = ref<Record<string, unknown> | null>(null)
const form = reactive<Record<string, unknown>>({})

const dialogTitle = computed(() => (dialogMode.value === 'create' ? `新增 — ${props.title}` : `修改 — ${props.title}`))

watch(
  () => props.listUrl,
  async (u) => {
    if (!props.canRun || !u) {
      rows.value = []
      return
    }
    await load()
  },
  { immediate: true },
)

async function load() {
  if (!props.canRun) return
  const resp = await request<PageResponse<Record<string, unknown>>>({
    url: props.listUrl,
    method: 'GET',
    params: { page: 1, size: 50 },
  })
  const rec = resp.data.records || []
  if (!props.statusKey) {
    rows.value = rec
    for (const r of rows.value) cacheOpObjectFromRow(r)
    return
  }
  const k = props.statusKey
  rows.value = rec.filter((r) => Number(r?.[k]) !== 0)
  for (const r of rows.value) cacheOpObjectFromRow(r)
}

function clearForm() {
  Object.keys(form).forEach((k) => delete form[k])
}

function resetFormForCreate() {
  clearForm()
  Object.assign(form, props.defaultForm())
}

function openCreate() {
  if (!props.canRun) {
    ElMessage.warning('请先选择必要上下文')
    return
  }
  dialogMode.value = 'create'
  editingRow.value = null
  resetFormForCreate()
  dialogVisible.value = true
}

function openEdit(row: Record<string, unknown>) {
  if (!props.canRun) return
  dialogMode.value = 'edit'
  editingRow.value = JSON.parse(JSON.stringify(row)) as Record<string, unknown>
  clearForm()
  props.fillFormForEdit(form, row)
  dialogVisible.value = true
}

function onDialogClosed() {
  editingRow.value = null
  resetFormForCreate()
}

async function submit() {
  if (!props.canRun) return
  const msg = props.validate(form, dialogMode.value)
  if (msg) {
    ElMessage.warning(msg)
    return
  }

  try {
    if (dialogMode.value === 'create') {
      await request({
        url: props.createUrl,
        method: 'POST',
        data: props.buildCreateBody(form),
      })
    } else {
      const id = String(editingRow.value?.[props.idField] || '')
      await request({
        url: props.updateUrl(id),
        method: 'PUT',
        data: props.buildUpdateBody(form, editingRow.value || {}),
      })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch (e) {
    const m = e instanceof Error ? e.message : '保存失败'
    ElMessage.error(m)
  }
}

async function deleteRow(row: Record<string, unknown>) {
  if (!props.canRun) return
  cacheOpObjectFromRow(row)
  await ElMessageBox.confirm('确认删除？', '确认', { type: 'warning' })
  const id = String(row[props.idField])
  await request({ url: props.deleteUrl(id), method: 'DELETE' })
  ElMessage.success('已删除')
  await load()
}

async function doImport(file: File) {
  if (!props.canRun) return
  const formData = new FormData()
  formData.append('file', file)
  const ro = await request<BatchImportResult>({
    url: props.importUrl,
    method: 'POST',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  const data = ro.data
  ElMessage.success(`导入结束：成功 ${data.successCount}，失败 ${data.failureCount}`)
  await load()
}
</script>

<style scoped>
.dict-toolbar-with-oplog {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.dict-toolbar-left {
  flex: 1;
  min-width: 0;
}
.op-log-link {
  flex-shrink: 0;
  align-self: center;
}
</style>

