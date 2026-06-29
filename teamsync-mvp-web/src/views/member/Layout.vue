<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">
        <h2 class="header-title">TeamSync Learn</h2>
        <el-menu :default-active="route.path" router mode="horizontal" class="header-menu">
          <el-menu-item index="/dashboard">
            <el-icon><DataBoard /></el-icon>我的看板
          </el-menu-item>
          <el-menu-item index="/schedule">
            <el-icon><Calendar /></el-icon>日程管理
          </el-menu-item>
          <el-menu-item index="/upload">
            <el-icon><Upload /></el-icon>成果上传
          </el-menu-item>
          <el-menu-item index="/summary">
            <el-icon><Document /></el-icon>学习总结
          </el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <template v-if="userStore.isAdmin">
          <el-button text type="primary" @click="router.push('/admin/teamboard')">
            <el-icon><Setting /></el-icon>管理后台
          </el-button>
          <el-divider direction="vertical" />
        </template>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :icon="UserFilled" />
            <span class="username">{{ userStore.username }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="layout-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { DataBoard, Calendar, Upload, Document, Setting, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示').then(() => {
      userStore.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 24px;
  height: 60px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}
.header-title {
  font-size: 18px;
  color: #409eff;
  white-space: nowrap;
}
.header-menu {
  border-bottom: none;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.username {
  font-size: 14px;
  color: #606266;
}
.layout-main {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
</style>
