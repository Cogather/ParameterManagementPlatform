<template>
  <el-container direction="vertical" :class="['app-shell', { 'app-shell--embedded': isEmbedded }]">
    <!-- spec-04 §3.1：单行上下文工具条（分段控件 + 产品），避免与宿主顶栏/页签叠套 -->
    <div class="app-context-bar" role="navigation" aria-label="参数管理子模块">
      <div class="app-context-bar__modules">
        <el-radio-group :model-value="menuActive" @change="onModuleChange">
          <el-radio-button label="/product">产品配置</el-radio-button>
          <el-radio-button label="/command">命令管理</el-radio-button>
          <el-radio-button label="/config">配置管理</el-radio-button>
          <el-radio-button label="/parameter">参数管理</el-radio-button>
        </el-radio-group>
      </div>
      <div class="app-context-bar__product">
        <product-selector
        
          v-model="selectedProductId"
          :options="productOptions"
          :loading="productLoading"
          @changed="onProductChanged"
        />
      </div>
    </div>
    <router-view />
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, provide, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProductContextStore } from './stores/productContext'
import { useVersionContextStore } from './stores/versionContext'
import ProductSelector, { type ProductOption } from './views/config/components/ProductSelector.vue'
import { isEmbeddedFromEnv, isEmbeddedFromQuery } from './utils/embed'
import { fetchProductChoices } from './api/entityBasicInfo'

const route = useRoute()
const router = useRouter()
const productContext = useProductContextStore()
const versionContext = useVersionContextStore()

/** spec-04：宿主嵌入时可通过 Query 注入 productId / versionId / productName（与 Pinia 对齐） */
function parseHostQuery(q: typeof route.query) {
  const pick = (k: string) => {
    const v = q[k]
    if (typeof v === 'string') {
      return v.trim()
    }
    if (Array.isArray(v) && typeof v[0] === 'string') {
      return v[0].trim()
    }
    return ''
  }
  return {
    productId: pick('productId'),
    versionId: pick('versionId'),
    productName: pick('productName'),
  }
}

const host = parseHostQuery(route.query)
if (host.productId) {
  productContext.setOwnedProductId(host.productId)
  if (host.productName) {
    productContext.setOwnedProductName(host.productName)
  }
}
if (host.versionId) {
  versionContext.setVersionId(host.versionId)
}

const selectedProductId = ref(productContext.ownedProductId || '')
const productLoading = ref(false)
const allProducts = ref<ProductOption[]>([])
const productOptions = ref<ProductOption[]>([])

const menuActive = computed(() => {
  const p = route.path
  if (p.startsWith('/product')) {
    return '/product'
  }
  if (p.startsWith('/config')) {
    return '/config'
  }
  if (p.startsWith('/parameter')) {
    return '/parameter'
  }
  if (p.startsWith('/command')) {
    return '/command'
  }
  return p
})

/** 紧凑模式：VITE_EMBEDDED=true 或 ?embed=1 */
const isEmbedded = computed(() => isEmbeddedFromEnv() || isEmbeddedFromQuery(route.query as Record<string, unknown>))

function onModuleChange(path: string | number | boolean | undefined) {
  const p = String(path ?? '')
  if (p && p !== route.path) {
    void router.push(p)
  }
}

async function searchProducts(keyword: string) {
  productLoading.value = true
  try {
    const kw = (keyword || '').trim().toLowerCase()
    productOptions.value = allProducts.value.filter((p) => p.entityName.toLowerCase().includes(kw)).slice(0, 20)
  } finally {
    productLoading.value = false
  }
}

async function loadProductOptionsFromServer() {
  productLoading.value = true
  try {
    const rows = await fetchProductChoices()
    allProducts.value = rows
      .map((r) => ({
        productPbiId: (r.productId || '').trim(),
        entityName: (r.entityName || '').trim() || (r.productId || '').trim(),
      }))
      .filter((p) => p.productPbiId)
  } catch {
    allProducts.value = [
      { productPbiId: 'product_demo', entityName: '演示产品' },
      { productPbiId: 'product_alpha', entityName: 'Alpha 产品' },
      { productPbiId: 'product_beta', entityName: 'Beta 产品' },
    ]
  } finally {
    productLoading.value = false
    productOptions.value = allProducts.value.slice(0, 20)
  }
}
provide('reloadProductOptions', loadProductOptionsFromServer)

function onProductChanged(v: { productId: string; productName: string }) {
  selectedProductId.value = v.productId || ''
  productContext.setOwnedProductId(selectedProductId.value.trim())
  productContext.setOwnedProductName(v.productName || '')
  // 切产品后清空版本上下文，避免串版
  versionContext.setVersionId('')
}

/** 产品 / 版本变更时回写 URL，便于宿主页签与外链联调 */
watch(
  () => [productContext.ownedProductId, versionContext.versionId] as const,
  () => {
    const pid = (productContext.ownedProductId || '').trim()
    const vid = (versionContext.versionId || '').trim()
    const curPid = typeof route.query.productId === 'string' ? route.query.productId : ''
    const curVid = typeof route.query.versionId === 'string' ? route.query.versionId : ''
    if (curPid === pid && curVid === vid) {
      return
    }
    const q = { ...route.query }
    if (pid) {
      q.productId = pid
    } else {
      delete q.productId
    }
    if (vid) {
      q.versionId = vid
    } else {
      delete q.versionId
    }
    void router.replace({ path: route.path, query: q })
  },
  { flush: 'post' },
)

onMounted(() => {
  void loadProductOptionsFromServer()
})
</script>

<style scoped>
.app-shell {
  background-color: #ecf0fe;
  min-height: 100vh;
}

.app-shell--embedded {
  min-height: auto;
}

.app-context-bar {
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #eee;
  box-sizing: border-box;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  justify-content: space-between;
  min-height: 40px;
  padding: 8px 0;
}

.app-shell--embedded .app-context-bar {
  min-height: 36px;
  padding: 6px 10px;
}

.app-context-bar__modules {
  flex: 1;
  min-width: 0;
}

.app-context-bar__product {
  flex-shrink: 0;
}

::v-deep(.app-context-bar__modules .el-radio-group) {
  flex-wrap: wrap;
}

::v-deep(.app-context-bar__modules .el-radio-button__inner) {
  font-size: 15px;
  padding: 8px 16px;
}

.app-shell--embedded ::v-deep(.app-context-bar__modules .el-radio-button__inner) {
  font-size: 14px;
  padding: 6px 12px;
}
</style>
