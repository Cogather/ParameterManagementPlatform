<template>
  <el-main class="common-main page-main">
    <el-alert
      v-if="!selectedProductId"
      title="请先选择产品"
      type="warning"
      show-icon
      :closable="false"
      style="margin-bottom: 12px"
    />
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="命令" name="commands"><commands-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="类型定义" name="types"><types-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="版本区段" name="ranges"><ranges-tab :product-id="selectedProductId" /></el-tab-pane>
    </el-tabs>
  </el-main>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useProductContextStore } from '../../stores/productContext'
import CommandsTab from './tabs/CommandsTab.vue'
import TypesTab from './tabs/TypesTab.vue'
import RangesTab from './tabs/RangesTab.vue'

const productContext = useProductContextStore()

const selectedProductId = ref(productContext.ownedProductId || '')
const activeTab = ref('commands')

watch(
  () => productContext.ownedProductId,
  (pid) => {
    selectedProductId.value = pid || ''
  },
  { immediate: true },
)
</script>
