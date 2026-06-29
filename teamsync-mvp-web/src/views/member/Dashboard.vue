<template>
  <div class="dashboard">
    <h3 class="page-title">我的学习看板</h3>

    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ dashboard.totalSchedules }}</div>
          <div class="stat-label">总日程</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card success">
          <div class="stat-value">{{ dashboard.completedSchedules }}</div>
          <div class="stat-label">已完成</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card warning">
          <div class="stat-value">{{ dashboard.pendingSchedules }}</div>
          <div class="stat-label">待开始</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card primary">
          <div class="stat-value">{{ dashboard.avgDiligenceScore }}</div>
          <div class="stat-label">平均勤奋值</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-section">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>近7天勤奋值趋势</template>
          <div ref="lineChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>目标达成度</template>
          <div ref="pieChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { getDashboard } from '@/api/schedule'
import * as echarts from 'echarts'

const dashboard = ref<any>({
  totalSchedules: 0, completedSchedules: 0, pendingSchedules: 0,
  totalAchievements: 0, avgDiligenceScore: 0, recentWeekScores: []
})

const lineChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const completionRate = computed(() => {
  if (!dashboard.value.totalSchedules) return 0
  return Math.round(dashboard.value.completedSchedules / dashboard.value.totalSchedules * 100)
})

async function fetchDashboard() {
  const res: any = await getDashboard()
  if (res.code === 200) {
    dashboard.value = res.data
    renderCharts()
  }
}

function renderCharts() {
  const scores = dashboard.value.recentWeekScores || []

  if (lineChartRef.value) {
    if (!lineChart) lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: scores.map((s: any) => s.date.slice(5)) },
      yAxis: { type: 'value', min: 0, max: 100 },
      series: [{
        data: scores.map((s: any) => s.score),
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.3 },
        lineStyle: { color: '#409eff', width: 3 },
        itemStyle: { color: '#409eff' }
      }],
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
    })
  }

  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '50%'],
        data: [
          { value: dashboard.value.completedSchedules, name: '已完成', itemStyle: { color: '#67c23a' } },
          { value: dashboard.value.pendingSchedules, name: '未完成', itemStyle: { color: '#e6a23c' } }
        ],
        label: { show: true, formatter: '{b}: {d}%' }
      }]
    })
  }
}

onMounted(fetchDashboard)

onBeforeUnmount(() => {
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard { max-width: 1200px; margin: 0 auto; }
.page-title { margin-bottom: 20px; font-size: 20px; color: #303133; }
.stat-cards { margin-bottom: 20px; }
.stat-card { text-align: center; }
.stat-value { font-size: 32px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.success .stat-value { color: #67c23a; }
.warning .stat-value { color: #e6a23c; }
.primary .stat-value { color: #409eff; }
.chart-section { margin-top: 20px; }
</style>
