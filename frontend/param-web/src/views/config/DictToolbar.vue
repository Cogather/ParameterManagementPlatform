<template>
  <div class="dict-toolbar">
    <el-button type="primary" :disabled="!canRun" @click="$emit('add')">新增</el-button>
    <el-button type="primary" :disabled="!canRun" @click="$emit('refresh')">刷新</el-button>
    <el-button :disabled="!canRun" @click="openUrl(exportUrl)">导出</el-button>
    <el-button :disabled="!canRun" @click="openImport">导入</el-button>
  </div>

  <el-dialog v-model="importVisible" title="导入" width="520px" destroy-on-close>
    <div style="margin-bottom: 10px; color: #666">
      支持 .xlsx。规则：新增时ID必须为空，修改时ID无需更改。
    </div>
    <el-upload
      :disabled="!canRun"
      :show-file-list="true"
      accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      :before-upload="onBeforeUpload"
    >
      <el-button :disabled="!canRun" type="primary">选择文件</el-button>
    </el-upload>

    <div style="margin-top: 12px">
      <a class="template-link" href="javascript:void(0)" @click="openUrl(templateUrl)">下载模板</a>
    </div>

    <template #footer>
      <el-button @click="importVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  canRun: boolean
  templateUrl: string
  exportUrl: string
}>()

const emit = defineEmits<{
  (e: 'add'): void
  (e: 'refresh'): void
  (e: 'import', file: File): void
}>()

function openUrl(u: string) {
  window.open(u, '_blank')
}

const importVisible = ref(false)

function openImport() {
  importVisible.value = true
}

function onBeforeUpload(file: File) {
  emit('import', file)
  importVisible.value = false
  return false
}
</script>

<style scoped>
.dict-toolbar {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

/** EP 相邻按钮默认 margin-left，与 flex gap 叠加会变宽 */
.dict-toolbar :deep(.el-button) {
  margin-left: 0;
}

.template-link {
  color: #2b91f6;
  cursor: pointer;
  text-decoration: underline;
}
</style>
