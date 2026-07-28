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

      <el-row :gutter="20">
        <el-col :span="9">
          <el-card shadow="hover" class="calendar-card">
            <template #header>
              <span>日程日历</span>
              <el-tag v-if="selectedDate" type="primary" size="small" effect="plain" closable @close="clearFilter">
                {{ selectedDateLabel }}
              </el-tag>
            </template>
            <el-calendar v-model="currentDate">
              <template #date-cell="{ data }">
                <div class="calendar-cell" :class="{ active: data.day === selectedDate }" @click="selectDate(data.day)">
                  <span class="day-number">{{ data.day.split('-')[2] }}</span>
                  <div class="schedule-info-badge" v-if="hasScheduleOnDate(data.day)">
                    <span class="schedule-count">{{ scheduleCountOnDate(data.day) }}</span>
                    <span class="schedule-label">项</span>
                  </div>
                </div>
              </template>
            </el-calendar>
          </el-card>
        </el-col>
        <el-col :span="15">
          <el-card shadow="hover" class="schedules-card">
            <template #header>
              <span>{{ selectedDate ? selectedDateLabel + ' 的' : '所有' }}日程 ({{ filteredSchedules.length }})</span>
              <el-button v-if="selectedDate" text type="primary" size="small" @click="clearFilter">清除筛选</el-button>
            </template>
            <el-table :data="filteredSchedules" stripe size="small" max-height="480" highlight-current-row @row-click="viewDetail">
              <el-table-column prop="title" label="标题" />
              <el-table-column prop="planDate" label="日期" width="110" />
              <el-table-column prop="statusText" label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'warning' : 'info'" size="small">
                    {{ row.statusText }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="勤奋值" width="80" align="center">
                <template #default="{ row }">
                  {{ row.aiResult?.diligenceScore || '-' }}
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- Detail drawer -->
    <el-drawer v-model="showDetail" :title="detailItem?.title" size="400px" destroy-on-close>
      <template v-if="detailItem">
        <p><strong>状态：</strong>{{ detailItem.statusText }}</p>
        <p><strong>计划日期：</strong>{{ detailItem.planDate }}</p>
        <p v-if="detailItem.goalDesc"><strong>学习目标：</strong>{{ detailItem.goalDesc }}</p>

        <el-divider />
        <h4>成果</h4>
        <div v-if="detailItem.achievement">
          <!-- Multi-item display (new format) -->
          <div v-if="detailItem.achievement.items && detailItem.achievement.items.length > 0" class="achievement-items">
            <!-- Non-FILE items: each gets its own card -->
            <div v-for="(item, i) in nonFileItems" :key="i" class="achievement-item">
              <div class="item-type-badge">
                <el-tag size="small" type="primary">
                  {{ item.contentType === 'TEXT' ? '纯文本' : 'Markdown' }}
                </el-tag>
                <span class="item-index">#{{ Number(i) + 1 }}</span>
              </div>
              <div v-if="item.content" class="item-content">{{ item.content }}</div>
            </div>

            <!-- All FILE items grouped in one attachments section -->
            <div v-if="fileItems.length > 0" class="achievement-item">
              <div class="item-type-badge">
                <el-tag size="small" type="warning">附件 ({{ fileItems.length }})</el-tag>
              </div>
              <div class="file-gallery">
                <template v-for="(item, fi) in fileItems" :key="fi">
                  <img v-if="isImageFile(item.fileUrl)"
                       :src="'/uploads/' + item.fileUrl"
                       class="preview-image"
                       @click="previewImage(item.fileUrl)" />
                  <el-link v-else type="primary" :href="'/uploads/' + item.fileUrl" target="_blank">
                    <el-icon><Download /></el-icon> {{ item.fileName || '下载附件' }}
                  </el-link>
                </template>
              </div>
            </div>
          </div>
          <!-- Fallback for old single-item data -->
          <div v-else>
            <p>类型：{{ detailItem.achievement.contentType }}</p>
            <p v-if="detailItem.achievement.content && detailItem.achievement.contentType !== 'FILE'">{{ detailItem.achievement.content }}</p>
            <div v-if="detailItem.achievement.fileUrl">
              <img v-if="isImageFile(detailItem.achievement.fileUrl)"
                   :src="'/uploads/' + detailItem.achievement.fileUrl"
                   class="preview-image"
                   @click="previewImage(detailItem.achievement.fileUrl)" />
              <el-link v-else type="primary" :href="'/uploads/' + detailItem.achievement.fileUrl" target="_blank">
                <el-icon><Download /></el-icon> 下载附件
              </el-link>
            </div>
          </div>
        </div>
        <div v-else>
          <el-empty description="暂未提交成果" />
        </div>

        <el-divider />
        <AiSummaryCard :result="detailItem.aiResult" :loading="false" />
      </template>
    </el-drawer>

    <!-- Image preview dialog -->
    <el-dialog v-model="showImagePreview" width="600px" title="图片预览" destroy-on-close>
      <img :src="previewImageUrl" class="full-image" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMemberDetail } from '@/api/admin'
import { getSchedules } from '@/api/schedule'
import { getUserInfo } from '@/api/user'
import * as echarts from 'echarts'
import { ArrowLeft, Download } from '@element-plus/icons-vue'
import AiSummaryCard from '@/components/AiSummaryCard.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const memberName = ref('')
const dashboard = ref<any>({})
const schedules = ref<any[]>([])
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const currentDate = ref(new Date())
const selectedDate = ref('')

function hasScheduleOnDate(dateStr: string) {
  return schedules.value.some(s => s.planDate === dateStr)
}

function scheduleCountOnDate(dateStr: string) {
  return schedules.value.filter(s => s.planDate === dateStr).length
}

function selectDate(dateStr: string) {
  selectedDate.value = selectedDate.value === dateStr ? '' : dateStr
}

function clearFilter() {
  selectedDate.value = ''
}

const filteredSchedules = computed(() => {
  if (!selectedDate.value) return schedules.value
  return schedules.value.filter(s => s.planDate === selectedDate.value)
})

const selectedDateLabel = computed(() => {
  if (!selectedDate.value) return ''
  const parts = selectedDate.value.split('-')
  return `${parts[0]}年${parseInt(parts[1])}月${parseInt(parts[2])}日`
})

const showDetail = ref(false)
const detailItem = ref<any>(null)
const showImagePreview = ref(false)
const previewImageUrl = ref('')

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg']

function isImageFile(fileUrl: string) {
  const ext = fileUrl.split('.').pop()?.toLowerCase()
  return ext ? IMAGE_EXTENSIONS.includes(ext) : false
}

function previewImage(fileUrl: string) {
  previewImageUrl.value = '/uploads/' + fileUrl
  showImagePreview.value = true
}

const nonFileItems = computed(() => {
  return detailItem.value?.achievement?.items?.filter((i: any) => i.contentType !== 'FILE') || []
})

const fileItems = computed(() => {
  return detailItem.value?.achievement?.items?.filter((i: any) => i.contentType === 'FILE') || []
})

function viewDetail(row: any) {
  detailItem.value = row
  showDetail.value = true
}

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
.chart-card { margin-top: 20px; }
.calendar-card :deep(.el-calendar) { --el-calendar-cell-width: auto; }
.calendar-card :deep(.el-calendar__header) {
  display: flex; justify-content: space-between; align-items: center;
  padding: 4px 8px;
}
.calendar-card :deep(.el-calendar__title) { font-size: 13px; }
.calendar-card :deep(.el-calendar__button-group) .el-button-group { transform: scale(0.85); }
.calendar-card :deep(.el-calendar__body) { padding: 4px; }
.calendar-card :deep(.el-calendar-table) { table-layout: fixed; }
.calendar-card :deep(.el-calendar-table th) {
  padding: 2px 0; font-size: 11px; font-weight: 500;
}
.calendar-card :deep(.el-calendar-table td) {
  border: none; padding: 0; vertical-align: top;
}
.calendar-card :deep(.el-calendar-table td.is-selected) { background: transparent; }
.calendar-card :deep(.el-calendar-table .el-calendar-day) {
  height: 44px; padding: 1px; box-sizing: border-box;
}
.calendar-card :deep(.el-calendar-table .prev-month .el-calendar-day),
.calendar-card :deep(.el-calendar-table .next-month .el-calendar-day) {
  opacity: 0.35;
}
.calendar-card :deep(.el-calendar-table .current) .el-calendar-day {
  background: transparent;
}
.calendar-cell {
  width: 100%; height: 100%; min-height: 42px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  cursor: pointer; box-sizing: border-box; padding: 2px;
  border-radius: 4px; transition: background 0.2s;
}
.calendar-cell:hover { background: #ecf5ff; }
.calendar-cell.active { background: #409eff; }
.calendar-cell.active .day-number { color: #fff; font-weight: bold; }
.calendar-cell.active .schedule-count { color: #fff; }
.calendar-cell.active .schedule-label { color: rgba(255,255,255,0.7); }
.day-number { font-size: 13px; color: #303133; }
.schedule-info-badge {
  display: flex; align-items: center; gap: 1px;
  margin-top: 0; line-height: 1;
}
.schedule-count { font-size: 10px; font-weight: bold; color: #409eff; }
.schedule-label { font-size: 9px; color: #909399; }
.schedules-card { margin: 0; }
.schedules-card :deep(.el-card__header) {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 16px;
}
.schedules-card :deep(.el-card__body) { padding: 0; }
.achievement-items { display: flex; flex-direction: column; gap: 12px; }
.achievement-item {
  border: 1px solid #e4e7ed; border-radius: 6px; padding: 10px; background: #fafafa;
}
.item-type-badge { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.item-index { font-size: 11px; color: #909399; }
.item-content { white-space: pre-wrap; font-size: 13px; color: #303133; line-height: 1.5; }
.file-gallery {
  display: flex; flex-direction: column; gap: 8px; margin-top: 4px;
}
.file-gallery .preview-image {
  max-height: 200px; width: auto;
}
.preview-image {
  max-width: 100%; max-height: 300px; border-radius: 6px; cursor: pointer;
  border: 1px solid #e0e0e0; transition: opacity 0.2s;
}
.preview-image:hover { opacity: 0.85; }
.full-image { max-width: 100%; display: block; margin: 0 auto; }
</style>
