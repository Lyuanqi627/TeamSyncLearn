<template>
  <div class="upload-page">
    <h3 class="page-title">成果上传</h3>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" class="schedule-list-card">
          <template #header>选择日程</template>

          <!-- date navigation calendar -->
          <div class="calendar-wrapper">
            <el-calendar v-model="currentDate">
              <template #date-cell="{ data }">
                <div class="calendar-cell" @click="selectDate(data.day)">
                  <span class="c-day">{{ data.day.split('-')[2] }}</span>
                  <span class="c-dot" v-if="dateStatus(data.day)" :class="dateStatus(data.day)"></span>
                </div>
              </template>
            </el-calendar>
          </div>

          <!-- date filter bar -->
          <div v-if="selectedDate" class="selected-date-bar">
            <span class="selected-date-label">{{ formatDateLabel(selectedDate) }}</span>
            <el-button link type="primary" size="small" @click="clearDateFilter">全部日程</el-button>
          </div>

          <!-- single-date filtered list -->
          <template v-if="selectedDate">
            <div
              v-for="item in schedulesForDate"
              :key="item.id"
              class="schedule-row"
              :class="{ active: selectedSchedule?.id === item.id }"
              @click="selectSchedule(item)"
            >
              <div class="schedule-row-info">
                <span class="schedule-row-title">{{ item.title }}</span>
                <div class="schedule-row-meta">
                  <el-tag :type="item.status === 2 ? 'success' : 'info'" size="small">{{ item.statusText }}</el-tag>
                  <el-tag v-if="item.achievement" size="small" type="success" effect="plain">已上传</el-tag>
                  <el-tag v-else size="small" type="info" effect="plain">未上传</el-tag>
                </div>
              </div>
            </div>
            <el-empty v-if="schedulesForDate.length === 0" description="该日期暂无日程" :image-size="60" />
          </template>

          <!-- all dates grouped view -->
          <template v-else>
            <div class="schedule-group" v-for="group in groupedSchedules" :key="group.date">
              <div class="group-header">
                <span class="group-date">{{ group.date }}</span>
                <el-tag size="small" :type="group.unuploadedCount > 0 ? 'warning' : 'success'">
                  {{ group.unuploadedCount }} 项未上传
                </el-tag>
              </div>
              <div
                v-for="item in group.items"
                :key="item.id"
                class="schedule-row"
                :class="{ active: selectedSchedule?.id === item.id }"
                @click="selectSchedule(item)"
              >
                <div class="schedule-row-info">
                  <span class="schedule-row-title">{{ item.title }}</span>
                  <div class="schedule-row-meta">
                    <el-tag :type="item.status === 2 ? 'success' : 'info'" size="small">{{ item.statusText }}</el-tag>
                    <el-tag v-if="item.achievement" size="small" type="success" effect="plain">已上传</el-tag>
                    <el-tag v-else size="small" type="info" effect="plain">未上传</el-tag>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-if="groupedSchedules.length === 0" description="暂无日程数据" :image-size="60" />
          </template>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover" v-if="selectedSchedule">
          <template #header>
            {{ isReUpload ? '更新成果' : '提交成果' }} - {{ selectedSchedule.title }}
            <el-tag size="small" type="info" style="margin-left: 8px">{{ selectedSchedule.planDate }}</el-tag>
            <el-tag v-if="isReUpload" size="small" type="warning" style="margin-left: 4px">重新上传</el-tag>
          </template>

          <el-form label-width="80px">
            <el-form-item label="学习目标">
              <p class="goal-text">{{ selectedSchedule.goalDesc || '未设置' }}</p>
            </el-form-item>

            <!-- Content blocks -->
            <el-form-item label="成果内容">
              <div class="content-blocks">
                <div
                  v-for="(block, index) in contentBlocks"
                  :key="block.key"
                  class="content-block"
                >
                  <div class="block-header">
                    <el-select v-model="block.contentType" class="block-type-select" size="small" @change="onBlockTypeChange(index)">
                      <el-option value="TEXT" label="纯文本" />
                      <el-option value="MARKDOWN" label="Markdown" />
                      <el-option value="FILE" label="文件上传" />
                    </el-select>
                    <el-button
                      v-if="contentBlocks.length > 1"
                      type="danger"
                      link
                      size="small"
                      @click="removeBlock(index)"
                    >
                      删除
                    </el-button>
                  </div>

                  <div v-if="block.contentType === 'TEXT'" class="block-body">
                    <el-input
                      v-model="block.content"
                      type="textarea"
                      :rows="5"
                      placeholder="描述你的学习成果..."
                    />
                  </div>

                  <div v-if="block.contentType === 'MARKDOWN'" class="block-body">
                    <MarkdownEditor v-model="block.content" />
                  </div>

                  <div v-if="block.contentType === 'FILE'" class="block-body">
                    <!-- Existing files from previous upload (re-upload) -->
                    <div v-if="block.existingFiles.length > 0" class="existing-file-info">
                      <div v-for="(ef, ei) in block.existingFiles" :key="ei" class="existing-file-item">
                        <img
                          v-if="isImageFile(ef.url)"
                          :src="'/uploads/' + ef.url"
                          class="file-thumb"
                          @click="previewImage('/uploads/' + ef.url)"
                        />
                        <span class="file-icon" v-else>
                          <el-icon><Document /></el-icon>
                        </span>
                        <span class="file-name">{{ ef.name || '已有文件' }}</span>
                        <el-button link type="danger" size="small" @click="removeExistingFile(index, ei)">删除</el-button>
                      </div>
                    </div>

                    <!-- Selected files list -->
                    <div v-if="block.files.length > 0" class="selected-files">
                      <div v-for="(f, fi) in block.files" :key="fi" class="selected-file-item">
                        <img
                          v-if="block.previewUrls[fi]"
                          :src="block.previewUrls[fi]"
                          class="file-thumb"
                          @click="previewImage(block.previewUrls[fi])"
                        />
                        <span class="file-icon" v-else>
                          <el-icon><Document /></el-icon>
                        </span>
                        <span class="file-name">{{ f.name }}</span>
                        <el-button link type="danger" size="small" @click="removeFile(index, fi)">删除</el-button>
                      </div>
                    </div>

                    <!-- File selector -->
                    <div class="file-select-row">
                      <el-button type="primary" plain @click="triggerFileInput(index)">
                        {{ block.files.length > 0 ? '添加更多文件' : '选择文件' }}
                      </el-button>
                      <span v-if="block.files.length > 0" class="file-count">已选 {{ block.files.length }} 个文件</span>
                    </div>
                    <input
                      :ref="(el: any) => { if (el) fileInputRefs[index] = el as HTMLInputElement }"
                      type="file"
                      class="hidden-input"
                      multiple
                      accept="image/*,.pdf,.zip,.doc,.docx,.txt,.md"
                      @change="(e: Event) => onFileInputChange(index, e)"
                    />
                    <div class="el-upload__tip">可一次选择多个文件，支持图片、文档、代码等</div>
                  </div>
                </div>
              </div>
            </el-form-item>

            <el-form-item label=" ">
              <el-button type="primary" plain @click="addBlock" size="small">+ 添加内容</el-button>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">
                {{ submitting ? '提交中...' : (isReUpload ? '更新成果' : '提交成果') }}
              </el-button>
              <el-button v-if="isReUpload" @click="resetForm" size="large">取消</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card v-else shadow="hover">
          <el-empty description="请从左侧选择一个日程" />
        </el-card>
      </el-col>
    </el-row>
    <!-- Image preview dialog -->
    <el-dialog v-model="showImagePreview" width="600px" title="图片预览" destroy-on-close>
      <img :src="previewImageUrl" class="full-image" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { getSchedules } from '@/api/schedule'
import { uploadAchievement } from '@/api/achievement'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg']

function isImageFile(name: string) {
  const ext = name.split('.').pop()?.toLowerCase()
  return ext ? IMAGE_EXTENSIONS.includes(ext) : false
}

interface ExistingFile {
  url: string
  name: string
}

interface ContentBlock {
  key: number
  contentType: 'TEXT' | 'MARKDOWN' | 'FILE'
  content: string
  files: File[]
  previewUrls: string[]
  existingFiles: ExistingFile[]
}

const schedules = ref<any[]>([])
const selectedSchedule = ref<any>(null)
const contentBlocks = ref<ContentBlock[]>([])
const submitting = ref(false)
const fileInputRefs = reactive<Record<number, HTMLInputElement>>({})
const showImagePreview = ref(false)
const previewImageUrl = ref('')
const currentDate = ref(new Date())
const selectedDate = ref<string | null>(null)

let blockKeyCounter = 0

function createBlock(type: 'TEXT' | 'MARKDOWN' | 'FILE' = 'TEXT'): ContentBlock {
  return {
    key: blockKeyCounter++,
    contentType: type,
    content: '',
    files: [],
    previewUrls: [],
    existingFiles: [],
  }
}

const isReUpload = computed(() => {
  return selectedSchedule.value?.achievement != null
})

const groupedSchedules = computed(() => {
  const dateMap = new Map<string, any[]>()

  for (const s of schedules.value) {
    const date = s.planDate || '未知日期'
    if (!dateMap.has(date)) {
      dateMap.set(date, [])
    }
    dateMap.get(date)!.push(s)
  }

  const sortedDates = Array.from(dateMap.keys()).sort()

  return sortedDates.map(date => {
    const items = dateMap.get(date)!
    // Unuploaded first within each date group
    items.sort((a, b) => {
      if (!a.achievement && b.achievement) return -1
      if (a.achievement && !b.achievement) return 1
      return 0
    })
    return {
      date,
      items,
      unuploadedCount: items.filter(s => !s.achievement).length,
    }
  })
})

const schedulesForDate = computed(() => {
  if (!selectedDate.value) return []
  return schedules.value.filter(s => s.planDate === selectedDate.value)
})

// 按 planDate 归组，计算每个日期的完成状态：全部完成→blue，存在未完成→orange
const dateStatusMap = computed(() => {
  const map = new Map<string, 'blue' | 'orange'>()
  const byDate = new Map<string, any[]>()
  for (const s of schedules.value) {
    const d = s.planDate
    if (!d) continue
    if (!byDate.has(d)) byDate.set(d, [])
    byDate.get(d)!.push(s)
  }
  for (const [d, items] of byDate) {
    map.set(d, items.every(s => s.status === 2) ? 'blue' : 'orange')
  }
  return map
})

function dateStatus(dateStr: string) {
  return dateStatusMap.value.get(dateStr) || null
}

function formatDateLabel(dateStr: string) {
  const [y, m, d] = dateStr.split('-')
  return `${y}年${parseInt(m)}月${parseInt(d)}日`
}

function selectDate(dateStr: string) {
  selectedDate.value = dateStr
}

function clearDateFilter() {
  selectedDate.value = null
}

async function fetchSchedules() {
  const res: any = await getSchedules()
  if (res.code === 200) {
    schedules.value = res.data || []
  }
}

function selectSchedule(row: any) {
  selectedSchedule.value = row
  resetForm()

  if (row.achievement) {
    const ach = row.achievement
    if (ach.items && ach.items.length > 0) {
      const blocks: ContentBlock[] = []
      const fileItems: ExistingFile[] = []

      for (const item of ach.items) {
        if (item.contentType === 'FILE' && item.fileUrl) {
          fileItems.push({ url: item.fileUrl, name: item.fileName || '' })
        } else {
          // TEXT / MARKDOWN get their own block
          blocks.push({
            key: blockKeyCounter++,
            contentType: item.contentType || 'TEXT',
            content: item.content || '',
            files: [],
            previewUrls: [],
            existingFiles: [],
          })
        }
      }

      // Single FILE block holding ALL existing files
      if (fileItems.length > 0) {
        blocks.push({
          key: blockKeyCounter++,
          contentType: 'FILE',
          content: '',
          files: [],
          previewUrls: [],
          existingFiles: fileItems,
        })
      }

      if (blocks.length > 0) {
        contentBlocks.value = blocks
      }
    } else {
      // Fallback for old data without items
      const type = ach.contentType || 'TEXT'
      const fileUrl = ach.fileUrl || null
      contentBlocks.value = [{
        key: blockKeyCounter++,
        contentType: type === 'FILE' ? 'FILE' : (type === 'MARKDOWN' ? 'MARKDOWN' : 'TEXT'),
        content: type !== 'FILE' ? (ach.content || '') : '',
        files: [],
        previewUrls: [],
        existingFiles: fileUrl ? [{ url: fileUrl, name: decodeURIComponent(fileUrl.substring(fileUrl.lastIndexOf('/') + 1)) }] : [],
      }]
    }
  }
}

function resetForm() {
  for (const block of contentBlocks.value) {
    revokePreviews(block)
  }
  contentBlocks.value = [createBlock('TEXT')]
}

function addBlock() {
  contentBlocks.value.push(createBlock('TEXT'))
}

function removeBlock(index: number) {
  revokePreviews(contentBlocks.value[index])
  contentBlocks.value.splice(index, 1)
}

function onBlockTypeChange(index: number) {
  const block = contentBlocks.value[index]
  if (block.contentType === 'FILE') {
    block.content = ''
  } else {
    revokePreviews(block)
    block.files = []
    block.previewUrls = []
    block.existingFiles = []
  }
}

function triggerFileInput(index: number) {
  fileInputRefs[index]?.click()
}

function onFileInputChange(index: number, event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return

  const block = contentBlocks.value[index]

  for (const f of target.files) {
    block.files.push(f)
    block.previewUrls.push(isImageFile(f.name) ? URL.createObjectURL(f) : '')
  }
  // Reset the input so selecting the same files again triggers change
  target.value = ''
}

function removeFile(blockIndex: number, fileIndex: number) {
  const block = contentBlocks.value[blockIndex]
  const url = block.previewUrls[fileIndex]
  if (url) URL.revokeObjectURL(url)
  block.files.splice(fileIndex, 1)
  block.previewUrls.splice(fileIndex, 1)
}

function removeExistingFile(blockIndex: number, fileIndex: number) {
  contentBlocks.value[blockIndex].existingFiles.splice(fileIndex, 1)
}

function revokePreviews(block: ContentBlock) {
  for (const url of block.previewUrls) {
    if (url) URL.revokeObjectURL(url)
  }
}

function previewImage(url: string) {
  previewImageUrl.value = url
  showImagePreview.value = true
}

async function handleSubmit() {
  if (!selectedSchedule.value) return

  const hasContent = contentBlocks.value.some(block => {
    if (block.contentType === 'FILE') return block.files.length > 0 || block.existingFiles.length > 0
    return block.content.trim().length > 0
  })
  if (!hasContent) {
    ElMessage.warning('请至少填写一项成果内容')
    return
  }

  // Confirm for re-upload
  if (isReUpload.value) {
    try {
      await ElMessageBox.confirm('该日程已有成果记录，确定要更新吗？', '确认更新', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    const formData = new FormData()
    formData.append('scheduleId', String(selectedSchedule.value.id))

    const items: any[] = []
    let fileIndex = 0
    for (const block of contentBlocks.value) {
      if (block.contentType === 'FILE') {
        // Existing files kept as-is
        for (const ef of block.existingFiles) {
          items.push({
            contentType: 'FILE',
            fileName: ef.name,
            fileIndex: null,
            existingFileUrl: ef.url,
          })
        }
        // Each newly selected file becomes one item
        for (let fi = 0; fi < block.files.length; fi++) {
          items.push({
            contentType: 'FILE',
            fileName: block.files[fi].name,
            fileIndex,
          })
          formData.append('files', block.files[fi])
          fileIndex++
        }
      } else {
        items.push({ contentType: block.contentType, content: block.content })
      }
    }
    formData.append('items', JSON.stringify(items))

    const res: any = await uploadAchievement(formData)
    if (res.code === 200) {
      ElMessage.success(isReUpload.value ? '成果更新成功！AI将重新评估，请稍后查看结果。' : '成果提交成功！AI评估将在后台进行，请稍后查看结果。')
      selectedSchedule.value = null
      resetForm()
      fetchSchedules()
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.msg || e?.message || '提交失败，请检查网络连接')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchSchedules)
</script>

<style scoped>
.upload-page { max-width: 1200px; margin: 0 auto; }
.page-title { margin-bottom: 20px; font-size: 20px; color: #303133; }
.schedule-list-card { margin-bottom: 20px; }
:deep(.el-card__body) { padding-bottom: 8px; }

/* compact calendar */
.calendar-wrapper { margin-bottom: 12px; }
.calendar-wrapper :deep(.el-calendar) { --el-calendar-cell-width: auto; }
.calendar-wrapper :deep(.el-calendar-table) { table-layout: fixed; }
.calendar-wrapper :deep(.el-calendar-table td) { padding: 0; }
.calendar-wrapper :deep(.el-calendar-day) { padding: 0; height: 32px; }
.calendar-wrapper :deep(.el-calendar-table thead th) { padding: 2px 0; font-size: 11px; }
.calendar-wrapper :deep(.el-calendar__header) { padding: 6px 8px; }
.calendar-wrapper :deep(.el-calendar__title) { font-size: 13px; }
.calendar-wrapper :deep(.el-calendar__button-group) .el-button-group { transform: scale(0.85); transform-origin: right center; }
.calendar-cell {
  width: 100%; height: 100%;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  cursor: pointer; box-sizing: border-box;
}
.calendar-cell:hover .c-day { color: #409eff; }
.c-day { font-size: 12px; line-height: 1.2; color: #606266; transition: color 0.15s; }
.c-dot {
  display: inline-block;
  width: 4px; height: 4px;
  background: #409eff;
  border-radius: 50%;
}
.c-dot.blue { background: #409eff; }
.c-dot.orange { background: #e6a23c; }
.calendar-wrapper :deep(.is-selected .c-day) {
  color: #fff;
  background: #409eff;
  border-radius: 50%;
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
}
.calendar-wrapper :deep(.is-selected .c-dot) { background: #fff; }

.selected-date-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 4px; margin-bottom: 8px;
  background: #ecf5ff; border-radius: 4px;
}
.selected-date-label { font-size: 14px; font-weight: 600; color: #409eff; }

.schedule-group {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.schedule-group:last-child { border-bottom: none; padding-bottom: 0; }
.schedule-group:first-child { padding-top: 0; }
.group-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px; padding: 0 4px;
}
.group-date {
  font-size: 14px; font-weight: 600; color: #303133;
}
.schedule-row {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  border: 1px solid transparent;
}
.schedule-row:hover { background: #f5f7fa; }
.schedule-row.active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}
.schedule-row + .schedule-row { margin-top: 4px; }
.schedule-row-info {
  flex: 1; display: flex; flex-direction: column; gap: 2px;
  min-width: 0;
}
.schedule-row-title { font-size: 14px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.schedule-row-meta { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.goal-text { color: #606266; font-size: 14px; line-height: 1.6; }

.content-blocks { display: flex; flex-direction: column; gap: 16px; width: 100%; }
.content-block {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}
.block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.block-type-select { width: 140px; }
.block-body { width: 100%; }
.file-select-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.file-count { font-size: 12px; color: #909399; }
.existing-file-info { margin-bottom: 10px; }
.existing-file-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
}
.hidden-input { display: none; }
.selected-files { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.selected-file-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fff;
}
.file-icon { display: flex; align-items: center; color: #909399; }
.file-name { flex: 1; font-size: 13px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-thumb {
  width: 36px; height: 36px;
  border-radius: 4px; object-fit: cover;
  cursor: pointer; border: 1px solid #e4e7ed;
}
.file-thumb:hover { opacity: 0.8; }
.full-image { max-width: 100%; display: block; margin: 0 auto; }
</style>
