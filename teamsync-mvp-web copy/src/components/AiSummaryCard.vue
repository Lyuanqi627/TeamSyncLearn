<template>
  <div class="ai-summary-card" v-loading="loading">
    <div v-if="result" class="card-content">
      <div class="score-section">
        <div class="score-ring">
          <el-progress type="circle" :percentage="result.diligenceScore" :stroke-width="8" :width="80">
            <span class="score-text">{{ result.diligenceScore }}</span>
          </el-progress>
          <div class="score-label">勤奋值</div>
        </div>
      </div>
      <el-divider />
      <div class="comment-section">
        <h4><el-icon><ChatDotRound /></el-icon> AI 点评</h4>
        <p class="comment">{{ result.aiComment }}</p>
      </div>
      <el-divider />
      <div class="summary-section">
        <h4><el-icon><Reading /></el-icon> 知识总结</h4>
        <p class="summary">{{ result.aiSummary }}</p>
      </div>
    </div>
    <el-empty v-else-if="!loading" description="暂无评估结果" />
  </div>
</template>

<script setup lang="ts">
import { ChatDotRound, Reading } from '@element-plus/icons-vue'

defineProps<{
  result: any
  loading: boolean
}>()
</script>

<style scoped>
.ai-summary-card {
  padding: 16px;
}
.score-section {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}
.score-ring {
  text-align: center;
}
.score-text {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}
.score-label {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}
.comment-section, .summary-section {
  text-align: left;
}
.comment-section h4, .summary-section h4 {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #303133;
}
.comment, .summary {
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
