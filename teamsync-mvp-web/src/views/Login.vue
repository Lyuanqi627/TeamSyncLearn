<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1 class="logo">TeamSync Learn</h1>
        <p class="subtitle">团队学习协作平台</p>
      </div>
      <el-form :model="form" @keyup.enter="handleLogin" class="login-form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码（可选）" size="large" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
            {{ loading ? '登录中...' : '登录 / 注册' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>首次登录将自动注册</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  loading.value = true
  try {
    await userStore.login({
      username: form.username.trim(),
      password: form.password || undefined
    })
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e: any) {
    ElMessage.error(e?.msg || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.logo {
  font-size: 28px;
  color: #409eff;
  margin-bottom: 8px;
}
.subtitle {
  color: #909399;
  font-size: 14px;
}
.login-form {
  margin-bottom: 16px;
}
.login-btn {
  width: 100%;
}
.login-footer {
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
}
</style>
