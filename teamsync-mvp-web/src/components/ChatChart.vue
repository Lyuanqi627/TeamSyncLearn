<template>
  <div ref="chartEl" class="chat-chart"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const props = defineProps<{ option: EChartsOption }>()

const chartEl = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
let ro: ResizeObserver | null = null

function render() {
  if (!chartEl.value || !props.option) return
  if (!chart) chart = echarts.init(chartEl.value)
  // option 引用可能复用,setOption 为增量合并,重复调用无害
  chart.setOption(props.option)
}

onMounted(() => {
  render()
  ro = new ResizeObserver(() => chart?.resize())
  ro.observe(chartEl.value!)
})

// 只做增量 setOption,绝不 re-init/dispose
watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  ro?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.chat-chart {
  width: 100%;
  height: 320px;
  margin: 8px 0;
}
</style>
