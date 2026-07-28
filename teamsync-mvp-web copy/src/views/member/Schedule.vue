<template>
  <div class="schedule-page">
    <div class="page-header">
      <h3 class="page-title">日程管理</h3>
      <el-button type="primary" @click="showDialog = true">
        <el-icon><Plus /></el-icon>新建日程
      </el-button>
    </div>

    <!-- Calendar view -->
    <el-card shadow="hover" class="calendar-card">
      <el-calendar v-model="currentDate">
        <template #date-cell="{ data }">
          <div class="calendar-cell" @click="selectDate(data.day)">
            <span class="day-number">{{ data.day.split('-')[2] }}</span>
            <div class="schedule-dot" v-if="hasScheduleOnDate(data.day)">
              <span class="dot"></span>
            </div>
          </div>
        </template>
      </el-calendar>
    </el-card>

    <!-- Schedule list for selected date -->
    <el-card shadow="hover" class="list-card">
      <template #header>
        <span>{{ currentDateLabel }} 的日程 ({{ schedules.length }})</span>
      </template>

      <div v-if="schedules.length === 0" class="empty-state">
        <el-empty description="暂无日程" />
      </div>

      <div v-for="item in schedules" :key="item.id" class="schedule-item">
        <el-tag :type="statusTag(item.status)" size="small">{{ item.statusText }}</el-tag>
        <div class="schedule-info">
          <span class="schedule-title">{{ item.title }}</span>
          <span class="schedule-goal" v-if="item.goalDesc">{{ item.goalDesc }}</span>
        </div>
        <div class="schedule-actions">
          <el-button text type="primary" @click="viewDetail(item)">详情</el-button>
          <el-button text type="danger" @click="handleDelete(item.id)">删除</el-button>
        </div>
      </div>
    </el-card>

    <!-- Create/Edit dialog -->
    <el-dialog v-model="showDialog" :title="editingId ? '编辑日程' : '新建日程'" width="520px">
      <el-form :model="scheduleForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="scheduleForm.title" placeholder="日程标题" />
        </el-form-item>
        <el-form-item label="学习目标">
          <el-input v-model="scheduleForm.goalDesc" type="textarea" :rows="3" placeholder="本次学习的目标描述" />
        </el-form-item>
        <el-form-item label="计划日期">
          <el-date-picker v-model="scheduleForm.planDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { getSchedules, createSchedule, updateSchedule, deleteSchedule } from '@/api/schedule'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import AiSummaryCard from '@/components/AiSummaryCard.vue'

const currentDate = ref(new Date())

function formatDate(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatDateLabel(d: Date) {
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}
const schedules = ref<any[]>([])
const showDialog = ref(false)
const showDetail = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
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

const scheduleForm = reactive({
  title: '',
  goalDesc: '',
  planDate: ''
})

const currentDateLabel = computed(() => formatDateLabel(currentDate.value))

const nonFileItems = computed(() => {
  return detailItem.value?.achievement?.items?.filter((i: any) => i.contentType !== 'FILE') || []
})

const fileItems = computed(() => {
  return detailItem.value?.achievement?.items?.filter((i: any) => i.contentType === 'FILE') || []
})

function hasScheduleOnDate(dateStr: string) {
  return schedules.value.some(s => s.planDate === dateStr)
}

function selectDate(dateStr: string) {
  currentDate.value = new Date(dateStr)
}

watch(currentDate, () => {
  fetchSchedules()
})

function statusTag(status: number) {
  return ['info', 'warning', 'success'][status] as any
}

function resetForm() {
  scheduleForm.title = ''
  scheduleForm.goalDesc = ''
  scheduleForm.planDate = ''
  editingId.value = null
}

async function fetchSchedules() {
  const dateStr = formatDate(currentDate.value)
  const res: any = await getSchedules({ date: dateStr })
  if (res.code === 200) {
    schedules.value = res.data || []
  }
}

async function handleSave() {
  if (!scheduleForm.title) {
    ElMessage.warning('请输入标题')
    return
  }
  saving.value = true
  try {
    const data = { ...scheduleForm }
    if (editingId.value) {
      await updateSchedule(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createSchedule(data)
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    resetForm()
    fetchSchedules()
  } finally {
    saving.value = false
  }
}

function viewDetail(item: any) {
  detailItem.value = item
  showDetail.value = true
}

function handleDelete(id: number) {
  ElMessageBox.confirm('确定删除此日程吗？', '提示').then(async () => {
    await deleteSchedule(id)
    ElMessage.success('删除成功')
    fetchSchedules()
  }).catch(() => {})
}

onMounted(fetchSchedules)
</script>

<style scoped>
.schedule-page { max-width: 1000px; margin: 0 auto; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title { font-size: 20px; color: #303133; margin: 0; }
.calendar-card { margin-bottom: 20px; }
/* Make el-calendar-day padding transparent so .calendar-cell fills the entire td */
:deep(.el-calendar-day) { padding: 0; }
.calendar-cell {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-sizing: border-box;
  padding: 4px;
}
.day-number { font-size: 14px; }
.schedule-dot { margin-top: 2px; }
.dot {
  display: inline-block;
  width: 6px; height: 6px;
  background: #409eff;
  border-radius: 50%;
}
.list-card { margin-bottom: 20px; }
.empty-state { padding: 40px 0; }
.schedule-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.schedule-item:last-child { border-bottom: none; }
.schedule-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.schedule-title { font-size: 15px; color: #303133; }
.schedule-goal { font-size: 12px; color: #909399; }
.schedule-actions { display: flex; gap: 4px; }
.preview-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #e0e0e0;
  transition: opacity 0.2s;
}
.preview-image:hover { opacity: 0.85; }
.full-image { max-width: 100%; display: block; margin: 0 auto; }

.achievement-items { display: flex; flex-direction: column; gap: 12px; }
.achievement-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px;
  background: #fafafa;
}
.item-type-badge { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.item-index { font-size: 11px; color: #909399; }
.item-content { white-space: pre-wrap; font-size: 13px; color: #303133; line-height: 1.5; }
.item-file { margin-top: 4px; }
.file-gallery {
  display: flex; flex-direction: column; gap: 8px; margin-top: 4px;
}
.file-gallery .preview-image {
  max-height: 200px; width: auto;
}
</style>
