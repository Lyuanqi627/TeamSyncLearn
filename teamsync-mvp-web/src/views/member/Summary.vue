<template>
  <div class="summary-page">
    <h3 class="page-title">学习总结</h3>
    <p class="page-desc">查看所有已完成日程的AI评估结果与学习总结</p>

    <el-timeline>
      <el-timeline-item
        v-for="item in completedSchedules"
        :key="item.id"
        :timestamp="item.planDate"
        placement="top"
        :color="item.aiResult ? '#67c23a' : '#c0c4cc'"
      >
        <el-card shadow="hover">
          <div class="summary-header">
            <h4>{{ item.title }}</h4>
            <el-tag type="success" size="small" v-if="item.aiResult">
              勤奋值: {{ item.aiResult.diligenceScore }}
            </el-tag>
            <el-tag type="info" size="small" v-else>
              AI评估中...
            </el-tag>
          </div>
          <p class="goal-desc" v-if="item.goalDesc">目标: {{ item.goalDesc }}</p>

          <div v-if="item.achievement" class="achievement-section">
            <el-divider />
            <p class="section-label">成果内容:</p>
            <p v-if="item.achievement.content && item.achievement.contentType !== 'FILE'" class="achievement-content">{{ item.achievement.content }}</p>
            <div v-if="item.achievement.fileUrl" class="file-section">
              <img v-if="isImageFile(item.achievement.fileUrl)"
                   :src="'/uploads/' + item.achievement.fileUrl"
                   class="preview-image"
                   @click="previewImg(item.achievement.fileUrl)" />
              <el-link v-else type="primary" :href="'/uploads/' + item.achievement.fileUrl" target="_blank">
                下载附件
              </el-link>
            </div>
          </div>

          <el-divider v-if="item.aiResult" />
          <div v-if="item.aiResult" class="ai-result">
            <p class="ai-label">AI点评:</p>
            <p class="ai-text">{{ item.aiResult.aiComment }}</p>
            <p class="ai-label" style="margin-top: 12px">知识总结:</p>
            <p class="ai-text">{{ item.aiResult.aiSummary }}</p>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>

    <el-empty v-if="completedSchedules.length === 0" description="暂无已完成的学习日程" />

    <el-dialog v-model="showPreview" width="600px" title="图片预览" destroy-on-close>
      <img :src="previewUrl" class="full-image" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSchedules } from '@/api/schedule'

const completedSchedules = ref<any[]>([])
const showPreview = ref(false)
const previewUrl = ref('')

const IMAGE_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg']

function isImageFile(fileUrl: string) {
  const ext = fileUrl.split('.').pop()?.toLowerCase()
  return ext ? IMAGE_EXTS.includes(ext) : false
}

function previewImg(fileUrl: string) {
  previewUrl.value = '/uploads/' + fileUrl
  showPreview.value = true
}

async function fetchData() {
  const res: any = await getSchedules()
  if (res.code === 200) {
    completedSchedules.value = (res.data || []).filter((s: any) => s.status === 2)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.summary-page { max-width: 800px; margin: 0 auto; }
.page-title { margin-bottom: 8px; font-size: 20px; color: #303133; }
.page-desc { margin-bottom: 24px; color: #909399; font-size: 14px; }
.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.summary-header h4 { margin: 0; color: #303133; }
.goal-desc { color: #909399; font-size: 13px; margin-top: 8px; }
.section-label { font-weight: bold; color: #606266; margin-bottom: 4px; }
.achievement-content { color: #606266; font-size: 14px; line-height: 1.7; }
.file-section { margin-top: 8px; }
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
.ai-label { font-weight: bold; color: #409eff; margin-bottom: 4px; }
.ai-text {
  color: #606266;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
}
</style>
