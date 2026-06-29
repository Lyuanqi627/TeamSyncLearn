<template>
  <div class="wordcloud-container" ref="chartRef" :style="{ height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import 'echarts-wordcloud'

const props = defineProps<{
  data: Array<{ name: string; value: number }>
  height?: string
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

function renderChart() {
  if (!chartRef.value || !props.data?.length) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  chart.setOption({
    tooltip: { show: true },
    series: [{
      type: 'wordCloud',
      gridSize: 10,
      sizeRange: [14, 50],
      rotationRange: [-30, 30],
      shape: 'circle',
      width: '90%',
      height: '90%',
      textStyle: {
        color: () => {
          const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb']
          return colors[Math.floor(Math.random() * colors.length)]
        }
      },
      data: props.data
    }]
  })
}

onMounted(renderChart)

watch(() => props.data, () => renderChart(), { deep: true })

onBeforeUnmount(() => {
  chart?.dispose()
})
</script>
