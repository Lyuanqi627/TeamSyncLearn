<template>
  <div class="member-detail-page">
    <el-button text @click="router.back()" class="back-btn">
      <el-icon><ArrowLeft /></el-icon>返回团队看板
    </el-button>

    <div v-loading="loading">
      <h3 class="page-title">成员详情: {{ memberName }}</h3>

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
          <el-card shadow="hover" class="stat-card primary">
            <div class="stat-value">{{ dashboard.avgDiligenceScore }}</div>
            <div class="stat-label">平均勤奋值</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card warning">
            <div class="stat-value">{{ dashboard.totalAchievements }}</div>
            <div class="stat-label">成果数量</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="hover" class="chart-card">
        <template #header>近7天勤奋值趋势</template>
        <div ref="chartRef" style="height: 300px"></div>
      </el-card>

      <el-card shadow="hover" class="schedules-card">
        <template #header>所有日程</template>
        <el-table :data="schedules" stripe size="small">
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="planDate" label="日期" width="120" />
          <el-table-column prop="statusText" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'warning' : 'info'" size="small">
                {{ row.statusText }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="勤奋值" width="100" align="center">
            <template #default="{ row }">
              {{ row.aiResult?.diligenceScore || '-' }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMemberDetail } from '@/api/admin'
import { getSchedules } from '@/api/schedule'
import { getUserInfo } from '@/api/user'
import * as echarts from 'echarts'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const memberName = ref('')
const dashboard = ref<any>({})
const schedules = ref<any[]>([])
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

async function fetchData() {
  const userId = Number(route.params.id)
  loading.value = true
  try {
    const [dashRes, schedRes]: any = await Promise.all([
      getMemberDetail(userId),
      getSchedules({ userId })
    ])
    if (dashRes.code === 200) dashboard.value = dashRes.data
    if (schedRes.code === 200) schedules.value = schedRes.data || []

    // Get member name from first schedule or fetch users
    if (schedules.value.length > 0) {
      const userRes: any = await getUserInfo()
      // We'll just use userId as fallback
    }
    memberName.value = schedules.value[0]?.title || `成员 #${userId}`

    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  const scores = dashboard.value.recentWeekScores || []
  if (!chartRef.value || scores.length === 0) return

  if (!chart) chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: scores.map((s: any) => s.date.slice(5)) },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{
      data: scores.map((s: any) => s.score),
      type: 'line', smooth: true,
      areaStyle: { opacity: 0.3 },
      lineStyle: { color: '#409eff', width: 3 },
      itemStyle: { color: '#409eff' }
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
  })
}

onMounted(fetchData)
onBeforeUnmount(() => chart?.dispose())
</script>

<style scoped>
.member-detail-page { max-width: 1000px; margin: 0 auto; }
.back-btn { margin-bottom: 16px; }
.page-title { margin-bottom: 20px; font-size: 20px; color: #303133; }
.stat-cards { margin-bottom: 20px; }
.stat-card { text-align: center; }
.stat-value { font-size: 32px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.success .stat-value { color: #67c23a; }
.primary .stat-value { color: #409eff; }
.warning .stat-value { color: #e6a23c; }
.chart-card, .schedules-card { margin-top: 20px; }
</style>
