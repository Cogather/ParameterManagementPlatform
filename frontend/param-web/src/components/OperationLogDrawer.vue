<template>
  <el-drawer
    v-model="innerVisible"
    :title="title"
    :size="width"
    class="oplog-drawer"
    destroy-on-close
    @open="onOpen"
  >
    <template v-if="groupByOperation">
      <div v-loading="loading" class="oplog-groups">
        <el-collapse v-model="openGroups">
          <el-collapse-item v-for="g in groups" :key="g.key" :name="g.key">
            <template #title>
              <span class="g-title">
                <span class="g-badge">{{ opLabel(g.operationType) }}</span>
                <span class="g-resource">{{ resourceLabelFor(g.resourceId, g.resourceName) }}</span>
                <span class="g-meta">修改人：{{ g.operatorId || '—' }}</span>
                <span class="g-meta">时间：{{ formatAt(g.operatedAt) }}</span>
                <span class="g-meta">变更：{{ g.items.length }} 项</span>
              </span>
            </template>
            <el-table :data="g.items" border style="width: 100%">
              <el-table-column prop="fieldLabelCn" label="变更项" min-width="140" show-overflow-tooltip />
              <el-table-column prop="oldValue" label="原值" min-width="140" show-overflow-tooltip />
              <el-table-column prop="newValue" label="新值" min-width="140" show-overflow-tooltip />
            </el-table>
          </el-collapse-item>
        </el-collapse>
        <div v-if="!groups.length && !loading" class="empty-hint">暂无数据</div>
      </div>
    </template>
    <template v-else>
      <el-table v-loading="loading" :data="rows" border style="width: 100%; margin-bottom: 12px" max-height="480">
        <el-table-column label="操作类型" width="90">
          <template #default="{ row }">
            {{ opLabel((row as OperationLogRow).operationType) }}
          </template>
        </el-table-column>
        <el-table-column label="操作对象" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resourceLabelFor((row as OperationLogRow).resourceId, (row as OperationLogRow).resourceName) }}
          </template>
        </el-table-column>
        <el-table-column prop="fieldLabelCn" label="变更项" min-width="120" show-overflow-tooltip />
        <el-table-column prop="oldValue" label="原值" min-width="100" show-overflow-tooltip />
        <el-table-column prop="newValue" label="新值" min-width="100" show-overflow-tooltip />
        <el-table-column prop="operatorId" label="修改人" width="120" show-overflow-tooltip />
        <el-table-column label="修改时间" width="180">
          <template #default="{ row }">
            {{ formatAt((row as OperationLogRow).operatedAt) }}
          </template>
        </el-table-column>
      </el-table>
    </template>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @current-change="load"
      @size-change="load"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { fetchOperationLogGroups, fetchOperationLogs, type OperationLogGroupItem, type OperationLogRow } from '../api/operation-log'

const props = withDefaults(
  defineProps<{
    productId: string
    /** 业务主表物理表名，如 entity_command_mapping */
    bizTable: string
    versionId?: string
    /** 为 true 且未传 versionId 时查该产品该表下所有版本相关审计行 */
    ignoreVersionFilter?: boolean
    /**
     * 展示「资源」列：resourceId -> 资源展示名（如 参数编码 + 参数名）。
     * 不传则回退显示 resourceId。
     */
    resourceDisplayMap?: Record<string, string>
    /** 是否按“同一次操作”分组折叠展示（适合参数新增/修改字段很多的场景） */
    groupByOperation?: boolean
    title?: string
    width?: string | number
  }>(),
  { title: '操作日志', width: '70%', ignoreVersionFilter: false },
)

const innerVisible = defineModel<boolean>({ default: false })

const loading = ref(false)
const rows = ref<OperationLogRow[]>([])
const grouped = ref<OperationLogGroupItem[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const openGroups = ref<string[]>([])

type OpGroup = {
  key: string
  resourceId: string
  resourceName?: string | null
  operationType: string
  operatorId?: string | null
  operatedAt?: string | null
  items: OperationLogRow[]
}

const groups = computed<OpGroup[]>(() => {
  const out: OpGroup[] = []
  for (const g of grouped.value || []) {
    out.push({
      key: g.groupKey,
      resourceId: String(g.resourceId || ''),
      resourceName: g.resourceName,
      operationType: g.operationType || '',
      operatorId: g.operatorId,
      operatedAt: g.operatedAt,
      items: (g.items || []).map((x) => ({
        logId: x.logId,
        bizTable: g.bizTable,
        ownedProductId: g.ownedProductId,
        ownedVersionId: g.ownedVersionId,
        resourceId: g.resourceId,
        resourceName: g.resourceName,
        operationType: g.operationType,
        fieldLabelCn: x.fieldLabelCn,
        oldValue: x.oldValue,
        newValue: x.newValue,
        operatorId: g.operatorId,
        operatedAt: x.operatedAt || g.operatedAt,
        logBatchId: g.logBatchId,
      })),
    })
  }
  return out
})

function resourceLabelFor(resourceId: string | null | undefined, resourceName?: string | null) {
  const rid = String(resourceId || '').trim()
  if (!rid) return '—'
  const rn = String(resourceName || '').trim()
  if (rn) return rn
  const map = props.resourceDisplayMap || {}
  const v = map[rid]
  return (v || '').trim() || rid
}

function opLabel(t: string) {
  if (t === 'CREATE') return '新增'
  if (t === 'UPDATE') return '修改'
  if (t === 'DELETE') return '删除'
  return t || '—'
}

function formatAt(v: string | null | undefined) {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  return d.toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  if (!props.productId) return
  loading.value = true
  try {
    if (props.groupByOperation) {
      const res = await fetchOperationLogGroups({
        productId: props.productId,
        bizTable: props.bizTable,
        versionId: props.versionId,
        ignoreVersionFilter: props.ignoreVersionFilter,
        page: page.value,
        size: size.value,
      })
      grouped.value = res.records || []
      total.value = res.total
      // 默认全部折叠：避免“第一条默认展开”导致视觉噪音
      openGroups.value = []
      rows.value = []
    } else {
      const res = await fetchOperationLogs({
        productId: props.productId,
        bizTable: props.bizTable,
        versionId: props.versionId,
        ignoreVersionFilter: props.ignoreVersionFilter,
        page: page.value,
        size: size.value,
      })
      rows.value = res.records || []
      total.value = res.total
      grouped.value = []
    }
  } finally {
    loading.value = false
  }
}

function onOpen() {
  page.value = 1
  load()
}

</script>

<style scoped>
:global(.oplog-drawer .el-drawer__body) {
  padding-top: 0 !important;
}

.oplog-groups {
  margin-bottom: 12px;
}
.g-title {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  width: 100%;
}
.g-badge {
  font-weight: 700;
  color: #333;
}
.g-resource {
  font-weight: 600;
  color: #2b91f6;
}
.g-meta {
  color: #666;
  font-size: 12px;
}
.empty-hint {
  padding: 12px 0;
  color: #888;
  text-align: center;
}
</style>
