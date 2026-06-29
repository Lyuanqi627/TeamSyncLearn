import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAiResult } from '@/api/ai'

export const useAiStore = defineStore('ai', () => {
  const loading = ref(false)
  const results = ref<Record<number, any>>({})

  async function fetchResult(achievementId: number) {
    loading.value = true
    try {
      const res: any = await getAiResult(achievementId)
      if (res.code === 200 && res.data) {
        results.value[achievementId] = res.data
      }
    } finally {
      loading.value = false
    }
  }

  function getResult(achievementId: number) {
    return results.value[achievementId] || null
  }

  return { loading, results, fetchResult, getResult }
})
