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
      <el-tab-pane label="产品版本" name="versions"><versions-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="业务分类" name="categories"><categories-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="变更来源关键字" name="keywords"><keywords-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="适用网元" name="nes"><nes-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="NF 配置" name="nfs"><nfs-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="生效方式" name="modes"><modes-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="生效形态" name="forms"><forms-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="项目组" name="teams"><teams-tab :product-id="selectedProductId" /></el-tab-pane>
      <el-tab-pane label="版本特性" name="features"><features-tab :product-id="selectedProductId" /></el-tab-pane>
    </el-tabs>
  </el-main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useProductContextStore } from '../../stores/productContext'
import { useVersionContextStore } from '../../stores/versionContext'
import VersionsTab from './tabs/VersionsTab.vue'
import CategoriesTab from './tabs/CategoriesTab.vue'
import KeywordsTab from './tabs/KeywordsTab.vue'
import NesTab from './tabs/NesTab.vue'
import NfsTab from './tabs/NfsTab.vue'
import ModesTab from './tabs/ModesTab.vue'
import FormsTab from './tabs/FormsTab.vue'
import TeamsTab from './tabs/TeamsTab.vue'
import FeaturesTab from './tabs/FeaturesTab.vue'

const productContext = useProductContextStore()
const versionContext = useVersionContextStore()

/** 与 Header 产品选择器共用 Pinia，须响应式订阅，否则仅首屏有值、切换产品后按钮仍禁用 */
const selectedProductId = computed(() => productContext.ownedProductId || '')
const activeTab = ref('versions')

// 产品选择器已提升到全局 Header；本页仅消费上下文
versionContext.setVersionId('')
</script>
