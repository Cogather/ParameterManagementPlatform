<template>
  <el-main class="common-main">
    <div class="page-card card-item">
      <div v-if="!productId" class="empty-wrap">
        <el-empty description="请先在上方选择产品，再配置该产品下的产品形态" />
      </div>
      <template v-else>
        <div class="toolbar toolbar-with-oplog">
          <div class="toolbar-left">
            <el-button type="primary" :disabled="!productId" @click="openAdd">新增产品形态</el-button>
          </div>
          <el-button v-if="productId" link type="primary" class="op-log-link" @click="opLogOpen = true">操作日志</el-button>
        </div>
        <operation-log-drawer
          v-model="opLogOpen"
          :product-id="(productId || '').trim()"
          biz-table="entity_basic_info"
          :resource-display-map="opLogResourceMap"
        />
        <el-table v-loading="loading" :data="rows" border style="width: 100%; margin-top: 12px" max-height="520">
          <el-table-column label="产品形态" min-width="200">
            <template #default="{ row }">
              <template v-if="editingId === row.productFormId">
                <el-input v-model="editDraft.productForm" maxlength="255" clearable />
              </template>
              <span v-else>{{ row.productForm || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="参数类型" width="200">
            <template #default="{ row }">
              <template v-if="editingId === row.productFormId">
                <el-select v-model="editDraft.productSoftParamType" placeholder="请选择" style="width: 100%">
                  <el-option label="Single" value="Single" />
                  <el-option label="Multi" value="Multi" />
                </el-select>
              </template>
              <span v-else>{{ row.productSoftParamType || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="责任人" min-width="220">
            <template #default="{ row }">
              <template v-if="editingId === row.productFormId">
                <el-input v-model="editDraft.ownerList" maxlength="255" placeholder="英文逗号分隔" clearable />
              </template>
              <span v-else>{{ row.ownerList || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <template v-if="editingId === row.productFormId">
                <el-button link type="primary" :loading="saving" @click="saveEdit">保存</el-button>
                <el-button link @click="cancelEdit">取消</el-button>
              </template>
              <template v-else>
                <el-button link type="primary" :disabled="!!editingId" @click="startEdit(row)">编辑</el-button>
                <el-button link type="danger" :disabled="!!editingId" @click="onRemove(row)">删除</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          class="pg"
          @current-change="load"
          @size-change="load"
        />
      </template>
    </div>

    <el-dialog v-model="addVisible" title="新增产品形态" width="480px" destroy-on-close @closed="resetAdd">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="100px">
        <el-form-item label="产品形态" prop="productForm">
          <el-input v-model="addForm.productForm" maxlength="255" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="参数类型" prop="productSoftParamType">
          <el-select v-model="addForm.productSoftParamType" placeholder="请选择" style="width: 100%">
            <el-option label="Single" value="Single" />
            <el-option label="Multi" value="Multi" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人" prop="ownerList">
          <el-input v-model="addForm.ownerList" maxlength="255" placeholder="英文逗号分隔" show-word-limit clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </el-main>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, reactive, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useProductContextStore } from '../../stores/productContext'
import {
  createEntityBasicInfo,
  deleteEntityBasicInfo,
  fetchEntityBasicInfoPage,
  type EntityBasicInfoRow,
  updateEntityBasicInfo,
} from '../../api/entityBasicInfo'
import OperationLogDrawer from '../../components/OperationLogDrawer.vue'

const reloadProductOptions = inject<() => Promise<void>>('reloadProductOptions')
const productStore = useProductContextStore()
const { ownedProductId: productId, ownedProductName: productName } = storeToRefs(productStore)

const loading = ref(false)
const saving = ref(false)
const rows = ref<EntityBasicInfoRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

/** 行内编辑：当前行 productFormId，或空 */
const editingId = ref<string>('')
const editDraft = ref<EntityBasicInfoRow>({})

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
const addVisible = ref(false)
const addForm = reactive<{
  productForm: string
  productSoftParamType: string
  ownerList: string
}>({
  productForm: '',
  productSoftParamType: 'Single',
  ownerList: '',
})
const addFormRef = ref<FormInstance>()

const addRules: FormRules = {
  productForm: [{ required: true, message: '请输入产品形态', trigger: 'blur' }],
  productSoftParamType: [{ required: true, message: '请选择参数类型', trigger: 'change' }],
  ownerList: [{ required: true, message: '请输入责任人', trigger: 'blur' }],
}

async function load() {
  const pid = (productId.value || '').trim()
  if (!pid) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const data = await fetchEntityBasicInfoPage({
      page: page.value,
      size: size.value,
      productId: pid,
    })
    rows.value = data.records
    for (const r of rows.value || []) {
      cacheOpObject(String(r.productFormId || ''), String(r.productForm || ''))
    }
    total.value = data.total
  } catch (e) {
    ElMessage.error((e as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

function startEdit(row: EntityBasicInfoRow) {
  if (!row.productFormId) {
    return
  }
  editingId.value = row.productFormId
  editDraft.value = {
    productForm: row.productForm,
    productSoftParamType: row.productSoftParamType,
    ownerList: row.ownerList,
  }
}

function cancelEdit() {
  editingId.value = ''
  editDraft.value = {}
}

async function saveEdit() {
  const id = editingId.value
  if (!id) {
    return
  }
  const f = (editDraft.value.productForm || '').trim()
  if (!f) {
    ElMessage.warning('请填写产品形态')
    return
  }
  if (!editDraft.value.productSoftParamType) {
    ElMessage.warning('请选择参数类型')
    return
  }
  if (!(editDraft.value.ownerList || '').trim()) {
    ElMessage.warning('请填写责任人')
    return
  }
  saving.value = true
  try {
    await updateEntityBasicInfo(id, {
      productForm: editDraft.value.productForm,
      productSoftParamType: editDraft.value.productSoftParamType,
      ownerList: editDraft.value.ownerList,
      updaterId: 'web',
    })
    ElMessage.success('已保存')
    cancelEdit()
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

function openAdd() {
  if (!(productId.value || '').trim()) {
    ElMessage.warning('请先选择产品')
    return
  }
  addForm.productForm = ''
  addForm.productSoftParamType = 'Single'
  addForm.ownerList = ''
  addVisible.value = true
}

function resetAdd() {
  addFormRef.value?.resetFields()
}

async function submitAdd() {
  if (!addFormRef.value) {
    return
  }
  await addFormRef.value.validate()
  const pid = (productId.value || '').trim()
  if (!pid) {
    ElMessage.warning('请先选择产品')
    return
  }
  const en = (productName.value || '').trim() || pid
  saving.value = true
  try {
    await createEntityBasicInfo({
      productId: pid,
      entityName: en,
      productForm: addForm.productForm.trim(),
      productSoftParamType: addForm.productSoftParamType,
      ownerList: addForm.ownerList.trim(),
      creatorId: 'web',
      updaterId: 'web',
    })
    ElMessage.success('已新增')
    addVisible.value = false
    page.value = 1
    await load()
    if (reloadProductOptions) {
      await reloadProductOptions()
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

function onRemove(row: EntityBasicInfoRow) {
  const id = row.productFormId
  if (!id) {
    return
  }
  cacheOpObject(String(id), String(row.productForm || ''))
  void ElMessageBox.confirm('确定删除该产品形态？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteEntityBasicInfo(id)
        ElMessage.success('已删除')
        if (editingId.value === id) {
          cancelEdit()
        }
        await load()
        if (reloadProductOptions) {
          await reloadProductOptions()
        }
      } catch (e) {
        ElMessage.error((e as Error).message || '删除失败')
      }
    })
    .catch(() => {})
}

watch(
  () => productId.value,
  () => {
    editingId.value = ''
    page.value = 1
    void load()
  },
)

onMounted(() => {
  void load()
})
</script>

<style scoped>
.toolbar {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-with-oplog {
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
  min-width: 0;
  align-items: center;
}
.op-log-link {
  flex-shrink: 0;
}
.empty-wrap {
  padding: 32px 0;
}
.pg {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
