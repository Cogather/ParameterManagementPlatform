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
            <el-input v-model="keyword" placeholder="按命令名称模糊查询" clearable class="keyword-input" />
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
      biz-table="entity_command_mapping"
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
        <el-table-column prop="commandName" label="命令" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ownerList" label="责任人" min-width="160" show-overflow-tooltip />
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

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" destroy-on-close>
        <el-form label-width="140px" @submit.prevent>
          <el-form-item label="命令" required>
            <el-input v-model="form.commandName" placeholder="命令名称（同产品内唯一）" />
          </el-form-item>
          <el-form-item label="责任人(英文逗号)" required>
            <el-input v-model="form.ownerList" placeholder="a,b,c" />
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
import { createCommand, disableCommand, fetchCommands, importCommands, updateCommand, commandsExportUrl, commandsTemplateUrl } from '../../../api/command-domain'

const props = defineProps<{ productId: string }>()

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
const editingId = ref('')
const form = reactive<Record<string, unknown>>({})

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增命令' : '编辑命令'))

const templateUrl = computed(() => (props.productId ? commandsTemplateUrl(props.productId) : ''))
const exportUrl = computed(() => (props.productId ? commandsExportUrl(props.productId, { keyword: keyword.value || undefined }) : ''))

function resetForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.commandId = ''
  form.commandName = ''
  form.ownerList = ''
}

function openCreate() {
  if (!props.productId) return
  dialogMode.value = 'create'
  editingId.value = ''
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: Record<string, unknown>) {
  dialogMode.value = 'edit'
  editingId.value = String(row.commandId || '')
  resetForm()
  form.commandId = String(row.commandId || '')
  form.commandName = String(row.commandName || '')
  form.ownerList = String(row.ownerList || '')
  dialogVisible.value = true
}

async function submit() {
  if (!props.productId) return
  if (!String(form.commandName || '').trim()) {
    ElMessage.warning('请填写命令')
    return
  }
  if (!String(form.ownerList || '').trim()) {
    ElMessage.warning('请填写责任人')
    return
  }
  try {
    if (dialogMode.value === 'create') {
      await createCommand(props.productId, form)
    } else {
      await updateCommand(props.productId, editingId.value, form)
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
  const id = String(row.commandId || '')
  if (!id) return
  cacheOpObject(id, String(row.commandName || ''))
  await ElMessageBox.confirm('确认删除该命令？', '确认', { type: 'warning' })
  try {
    await disableCommand(props.productId, id)
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
    const data = await fetchCommands(props.productId, {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
    })
    const rec = data.records || []
    rows.value = rec.filter((r) => Number(r?.commandStatus) !== 0)
    for (const r of rows.value || []) {
      cacheOpObject(String(r.commandId || ''), String(r.commandName || ''))
    }
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function onImport(file: File) {
  if (!props.productId) return
  try {
    const data = await importCommands(props.productId, file)
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
    if (pid) await loadRows()
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
