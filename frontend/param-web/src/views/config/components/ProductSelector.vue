<template>
  <div class="header-right">
    <span class="header-label">产品：</span>
    <el-select
      v-model="innerProductId"
      filterable
      :loading="loading"
      placeholder="请输入产品名称"
      style="width: 240px"
      clearable
      @change="onChanged"
    >
      <el-option v-for="p in options" :key="p.productPbiId" :label="p.entityName" :value="p.productPbiId" />
    </el-select>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

export type ProductOption = { productPbiId: string; entityName: string }

const props = defineProps<{
  modelValue: string
  options: ProductOption[]
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void
  (e: 'changed', v: { productId: string; productName: string }): void
}>()

const innerProductId = ref(props.modelValue || '')

watch(
  () => props.modelValue,
  (v) => {
    innerProductId.value = v || ''
  },
)

function onChanged(v: string) {
  const pid = v || ''
  emit('update:modelValue', pid)
  const hit = props.options.find((x) => x.productPbiId === pid)
  emit('changed', { productId: pid, productName: hit?.entityName || '' })
}
</script>

<style scoped>
.header-right {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-label {
  color: #555;
  font-size: 14px;
  margin-right: 6px;
}
</style>

