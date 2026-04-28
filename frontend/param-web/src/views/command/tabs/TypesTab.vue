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
            <el-input v-model="keyword" placeholder="按类型名称模糊查询" clearable class="keyword-input" />
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
      biz-table="command_type_definition"
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
        <el-table-column label="归属命令" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ commandDisplayName(String(row.ownedCommandId || '')) }}
          </template>
        </el-table-column>
        <el-table-column prop="commandTypeName" label="类型名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="commandType" label="类型枚举" width="100" />
        <el-table-column prop="minValue" label="最小序号" width="90" />
        <el-table-column prop="maxValue" label="最大序号" width="90" />
        <el-table-column prop="occupiedSerialNumber" label="占用序号" min-width="140" show-overflow-tooltip />
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
              placeholder="请选择命令（来自「命令」页配置）"
              style="width: 100%"
              :disabled="dialogMode === 'edit'"
              :loading="commandsLoading"
            >
              <el-option
                v-for="c in commandOptions"
                :key="c.commandId"
                :label="c.commandName"
                :value="c.commandId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="类型名称" required>
            <el-input v-model="form.commandTypeName" placeholder="同产品内唯一" />
          </el-form-item>
          <el-form-item label="类型枚举" required>
            <el-select v-model="form.commandType" placeholder="请选择" style="width: 240px" :loading="typeBitsLoading">
              <el-option v-for="t in typeEnumOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="最小序号">
                <el-input-number v-model="form.minValue" :min="1" :max="999999" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最大序号">
                <el-input-number v-model="form.maxValue" :min="1" :max="999999" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
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
import {
  commandTypesExportUrl,
  commandTypesTemplateUrl,
  createCommandType,
  fetchCommands,
  fetchCommandTypes,
  importCommandTypes,
  updateCommandType,
} from '../../../api/command-domain'
import { fetchTypeBits } from '../../../api/type-bits'

const props = defineProps<{ productId: string }>()

const typeEnumOptions = ref<string[]>([])
const typeBitsLoading = ref(false)

const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const opLogOpen = ref(false)
const opLogResourceCache = ref<Record<string, string>>({})
const opLogResourceMap = computed<Record<string, string>>(() => opLogResourceCache.value)

function cacheOpObject(id: string, label: string) {
  const rid = (id || '').trim()
  const v = (label || '').trim()
  if (!rid || !v) return
  if (opLogResourceCache.value[rid] === v) return
  opLogResourceCache.value = { ...opLogResourceCache.value, [rid]: v }
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingTypeId = ref('')
const form = reactive<Record<string, unknown>>({})

/** 当前产品下「命令」页配置的命令，供归属命令下拉使用 */
const commandOptions = ref<{ commandId: string; commandName: string }[]>([])
const commandsLoading = ref(false)

function commandDisplayName(commandId: string) {
  if (!commandId) return '—'
  const hit = commandOptions.value.find((c) => c.commandId === commandId)
  return hit ? hit.commandName : '—'
}

async function loadCommandOptions() {
  if (!props.productId) {
    commandOptions.value = []
    return
  }
  commandsLoading.value = true
  try {
    const data = await fetchCommands(props.productId, { page: 1, size: 500 })
    const rec = data.records || []
    // 与「命令」页保持一致：过滤掉已删除/已禁用（status=0）的命令
    commandOptions.value = rec
      .filter((r) => Number(r?.commandStatus) !== 0)
      .map((r) => ({
        commandId: String(r.commandId || ''),
        commandName: String(r.commandName || ''),
      }))
  } finally {
    commandsLoading.value = false
  }
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增类型定义' : '编辑类型定义'))
const templateUrl = computed(() => (props.productId ? commandTypesTemplateUrl(props.productId) : ''))
const exportUrl = computed(() => (props.productId ? commandTypesExportUrl(props.productId, { keyword: keyword.value || undefined }) : ''))

function resetForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.ownedCommandId = ''
  form.commandTypeId = ''
  form.commandTypeName = ''
  form.commandType = 'BIT'
  form.minValue = undefined
  form.maxValue = undefined
  form.occupiedSerialNumber = ''
}

async function loadTypeEnumOptions() {
  typeBitsLoading.value = true
  try {
    const rows = await fetchTypeBits()
    const opts = rows.map((r) => String(r.typeEnum || '').trim()).filter(Boolean)
    // 防御：空表时仍保留默认选项，避免表单不可用
    typeEnumOptions.value = opts.length ? opts : ['BIT', 'BYTE', 'DWORD', 'STRING', 'INT']
    // 编辑场景：若当前值不在字典中，仍允许展示
    const current = String(form.commandType || '').trim()
    if (current && !typeEnumOptions.value.includes(current)) {
      typeEnumOptions.value = [...typeEnumOptions.value, current]
    }
  } finally {
    typeBitsLoading.value = false
  }
}

async function openCreate() {
  dialogMode.value = 'create'
  editingTypeId.value = ''
  resetForm()
  await loadTypeEnumOptions()
  await loadCommandOptions()
  if (!commandOptions.value.length) {
    ElMessage.warning('请先在「命令」页为当前产品配置至少一条命令')
    return
  }
  dialogVisible.value = true
}

async function openEdit(row: Record<string, unknown>) {
  dialogMode.value = 'edit'
  editingTypeId.value = String(row.commandTypeId || '')
  resetForm()
  form.commandType = String(row.commandType || 'BIT')
  await loadTypeEnumOptions()
  await loadCommandOptions()
  const oid = String(row.ownedCommandId || '')
  if (oid && !commandOptions.value.some((c) => c.commandId === oid)) {
    commandOptions.value = [...commandOptions.value, { commandId: oid, commandName: '（不在当前列表）' }]
  }
  form.ownedCommandId = oid
  form.commandTypeId = String(row.commandTypeId || '')
  form.commandTypeName = String(row.commandTypeName || '')
  form.minValue = row.minValue === null || row.minValue === undefined ? undefined : Number(row.minValue)
  form.maxValue = row.maxValue === null || row.maxValue === undefined ? undefined : Number(row.maxValue)
  form.occupiedSerialNumber = String(row.occupiedSerialNumber || '')
  dialogVisible.value = true
}

async function deleteRow(row: Record<string, unknown>) {
  if (!props.productId) return
  const id = String(row.commandTypeId || '')
  if (!id) return
  cacheOpObject(id, String(row.commandTypeName || ''))
  await ElMessageBox.confirm('确认删除该类型定义？', '确认', { type: 'warning' })
  try {
    // 后端 DELETE 为逻辑删除/禁用
    await updateCommandType(props.productId, id, { ...row, commandTypeStatus: 0 })
    ElMessage.success('已删除')
    await loadRows()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function submit() {
  if (!props.productId) return
  if (!String(form.ownedCommandId || '').trim()) {
    ElMessage.warning('请选择归属命令')
    return
  }
  if (!String(form.commandTypeName || '').trim()) {
    ElMessage.warning('请填写类型名称')
    return
  }
  if (!String(form.commandType || '').trim()) {
    ElMessage.warning('请填写类型枚举')
    return
  }
  try {
    if (dialogMode.value === 'create') {
      await createCommandType(props.productId, form)
    } else {
      await updateCommandType(props.productId, editingTypeId.value, form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadRows()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function loadRows() {
  if (!props.productId) return
  loading.value = true
  try {
    const data = await fetchCommandTypes(props.productId, {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
    })
    const rec = data.records || []
    rows.value = rec.filter((r) => Number(r?.commandTypeStatus) !== 0)
    for (const r of rows.value || []) {
      cacheOpObject(String(r.commandTypeId || ''), String(r.commandTypeName || ''))
    }
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function onImport(file: File) {
  if (!props.productId) return
  try {
    const data = await importCommandTypes(props.productId, file)
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
    commandOptions.value = []
    typeEnumOptions.value = []
    if (pid) {
      await loadCommandOptions()
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
.keyword-input {
  width: min(260px, 36vw);
  min-width: 160px;
}
.op-log-link {
  flex-shrink: 0;
  align-self: center;
}
</style>
