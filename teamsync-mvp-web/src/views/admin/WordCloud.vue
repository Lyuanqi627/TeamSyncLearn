<template>
  <div class="wordcloud-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">团队词云分析</h3>
        <p class="page-desc">基于团队成员学习成果内容的高频词分析</p>
      </div>
      <div class="date-filter">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="fetchData"
        />
      </div>
    </div>

    <el-card shadow="hover">
      <WordCloud :data="wordData" height="500px" />
      <el-empty v-if="wordData.length === 0" description="暂无数据" />
    </el-card>

    <el-card shadow="hover" class="rank-card" v-if="wordData.length > 0">
      <template #header>Top 20 词汇统计</template>
      <el-table :data="wordData.slice(0, 20)" stripe size="small">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="词汇" />
        <el-table-column prop="value" label="出现次数" width="120" align="center" sortable />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getWordCloud } from '@/api/admin'
import WordCloud from '@/components/WordCloud.vue'

const dateRange = ref<[string, string]>([
  new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10),
  new Date().toISOString().slice(0, 10)
])
const wordData = ref<Array<{ name: string; value: number }>>([])

async function fetchData() {
  const params: any = {}
  if (dateRange.value?.[0]) params.startDate = dateRange.value[0]
  if (dateRange.value?.[1]) params.endDate = dateRange.value[1]

  const res: any = await getWordCloud(params)
  if (res.code === 200) {
    wordData.value = res.data || []
  }
}

onMounted(fetchData)
</script>

<style scoped>
.wordcloud-page { max-width: 1000px; margin: 0 auto; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.page-title { margin-bottom: 8px; font-size: 20px; color: #303133; }
.page-desc { color: #909399; font-size: 14px; }
.rank-card { margin-top: 20px; }
</style>
