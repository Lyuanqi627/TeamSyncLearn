<template>
  <div class="layout-wrapper">
    <header class="layout-header">
      <div class="header-left">
        <div class="logo-area">
          <span class="logo-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </span>
          <h2 class="header-title">TeamSync Learn 管理后台</h2>
        </div>
      </div>
      <div class="header-right">
        <el-button text @click="router.push('/dashboard')">
          <el-icon><Back /></el-icon>返回成员端
        </el-button>
        <el-divider direction="vertical" />
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :icon="UserFilled" />
            <span class="username">{{ userStore.username }}</span>
            <el-tag :type="userStore.isSuperAdmin ? 'danger' : 'primary'" size="small" effect="plain">
              {{ userStore.roleLabel }}
            </el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="layout-body">
      <aside class="layout-sidebar">
        <el-menu
          :default-active="route.path"
          router
          class="sidebar-menu"
          background-color="#1e1e2f"
          text-color="rgba(255,255,255,0.65)"
          active-text-color="#409eff"
        >
          <div class="sidebar-title">管理导航</div>
          <el-menu-item index="/admin/teamboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>团队看板</span>
          </el-menu-item>
          <el-menu-item index="/admin/wordcloud">
            <el-icon><Cloudy /></el-icon>
            <span>词云分析</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isSuperAdmin" index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { DataAnalysis, Cloudy, Back, UserFilled, User } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示').then(async () => {
      await userStore.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.layout-wrapper {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ===== Header ===== */
.layout-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  color: #409eff;
  display: flex;
  align-items: center;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d1e1f;
  white-space: nowrap;
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

/* ===== Body ===== */
.layout-body {
  display: flex;
  flex: 1;
  margin-top: 60px;
  height: calc(100vh - 60px);
}

/* ===== Sidebar ===== */
.layout-sidebar {
  width: 220px;
  background: #1e1e2f;
  overflow-y: auto;
  flex-shrink: 0;
  position: fixed;
  top: 60px;
  left: 0;
  bottom: 0;
  z-index: 999;
}

.layout-sidebar::-webkit-scrollbar {
  width: 4px;
}

.layout-sidebar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

.sidebar-menu {
  border-right: none;
  height: 100%;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}

.sidebar-title {
  padding: 16px 20px 8px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.35);
  font-weight: 500;
}

/* Override el-menu hover/active styles for dark theme */
.sidebar-menu .el-menu-item {
  margin: 2px 8px;
  border-radius: 6px;
  width: calc(100% - 16px);
}

.sidebar-menu .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.08) !important;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: rgba(64, 158, 255, 0.15) !important;
}

/* ===== Main Content ===== */
.layout-main {
  flex: 1;
  margin-left: 220px;
  overflow-y: auto;
  padding: 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}
</style>
