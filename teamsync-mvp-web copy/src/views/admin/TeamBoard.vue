<template>
  <div class="teamboard-page">
    <h3 class="page-title">团队看板</h3>
    <p class="page-desc">查看所有成员的学习进度与表现</p>

    <el-card shadow="hover">
      <el-table :data="teamBoard" stripe v-loading="loading">
        <el-table-column label="成员" prop="username" width="150" />
        <el-table-column label="总日程" prop="totalSchedules" width="100" align="center" />
        <el-table-column label="已完成" prop="completedSchedules" width="100" align="center" />
        <el-table-column label="完成率" width="150" align="center">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(row.completionRate)" :status="row.completionRate >= 80 ? 'success' : 'warning'" />
          </template>
        </el-table-column>
        <el-table-column label="平均勤奋值" prop="avgDiligenceScore" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="scoreTag(row.avgDiligenceScore)" effect="plain">
              {{ row.avgDiligenceScore || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" text @click="viewDetail(row.userId)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTeamBoard } from '@/api/admin'

const router = useRouter()
const teamBoard = ref<any[]>([])
const loading = ref(false)

function scoreTag(score: number) {
  if (!score) return 'info'
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

function viewDetail(userId: number) {
  router.push(`/admin/member/${userId}`)
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getTeamBoard()
    if (res.code === 200) {
      teamBoard.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.teamboard-page { max-width: 1000px; margin: 0 auto; }
.page-title { margin-bottom: 8px; font-size: 20px; color: #303133; }
.page-desc { margin-bottom: 20px; color: #909399; font-size: 14px; }
</style>
