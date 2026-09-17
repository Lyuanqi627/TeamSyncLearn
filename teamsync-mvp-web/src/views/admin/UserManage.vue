<template>
  <div class="usermanage-page">
    <h3 class="page-title">用户管理</h3>
    <p class="page-desc">查看所有用户并授予 / 收回“管理员”权限（仅超级管理员可用）</p>

    <el-card shadow="hover">
      <el-table :data="users" stripe v-loading="loading">
        <el-table-column label="ID" prop="id" width="80" align="center" />
        <el-table-column label="用户名" prop="username" min-width="140" />
        <el-table-column label="角色" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="plain">{{ roleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? String(row.createdAt).replace('T', ' ').slice(0, 19) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center">
          <template #default="{ row }">
            <div class="op-cell">
              <el-tooltip v-if="isDisabled(row)" :content="disableReason(row)" placement="top">
                <span><el-select :model-value="row.role" size="small" style="width: 105px" disabled>
                  <el-option label="管理员" value="ADMIN" />
                  <el-option label="成员" value="MEMBER" />
                </el-select></span>
              </el-tooltip>
              <el-select
                v-else
                :model-value="row.role"
                size="small"
                style="width: 105px"
                @change="(val: string) => onChangeRole(row, val)"
              >
                <el-option label="管理员" value="ADMIN" />
                <el-option label="成员" value="MEMBER" />
              </el-select>
              <el-tooltip v-if="isDeleteDisabled(row)" :content="disableReason(row)" placement="top">
                <span><el-button type="danger" text size="small" disabled>删除</el-button></span>
              </el-tooltip>
              <el-button v-else type="danger" text size="small" @click="onDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateUserRole, deleteUser } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const users = ref<any[]>([])
const loading = ref(false)

function roleText(role: string) {
  return ({ SUPER_ADMIN: '超级管理员', ADMIN: '管理员', MEMBER: '成员' }[role] || '成员')
}

function roleTagType(role: string) {
  if (role === 'SUPER_ADMIN') return 'danger'
  if (role === 'ADMIN') return 'warning'
  return 'info'
}

function isDisabled(row: any) {
  return row.role === 'SUPER_ADMIN' || row.id === userStore.userInfo?.userId
}

function isDeleteDisabled(row: any) {
  return row.role === 'SUPER_ADMIN' || row.id === userStore.userInfo?.userId
}

function disableReason(row: any) {
  if (row.role === 'SUPER_ADMIN') return '超级管理员角色不可变更'
  if (row.id === userStore.userInfo?.userId) return '不能修改自己的角色'
  return ''
}

async function onChangeRole(row: any, newRole: string) {
  if (newRole === row.role) return
  const action = newRole === 'ADMIN' ? '授予' : '收回'
  try {
    await ElMessageBox.confirm(`确定${action}「${row.username}」的「${roleText(newRole)}」权限吗？`, '提示', {
      type: 'warning'
    })
  } catch {
    await fetchData() // 取消 → 还原下拉框
    return
  }
  try {
    const res: any = await updateUserRole(row.id, newRole)
    if (res.code === 200) {
      ElMessage.success(`已${action}「${row.username}」的权限`)
    }
  } finally {
    await fetchData()
  }
}

async function onDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${row.username}」吗？将级联清除其全部日程、成果与 AI 评分记录，此操作不可恢复！`,
      '危险操作',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return // 取消
  }
  try {
    const res: any = await deleteUser(row.id)
    if (res.code === 200) {
      ElMessage.success(`已删除用户「${row.username}」`)
    }
  } finally {
    await fetchData()
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getUsers()
    if (res.code === 200) {
      users.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.usermanage-page { max-width: 1000px; margin: 0 auto; }
.page-title { margin-bottom: 8px; font-size: 20px; color: #303133; }
.page-desc { margin-bottom: 20px; color: #909399; font-size: 14px; }
.op-cell { display: flex; align-items: center; justify-content: center; gap: 6px; }
</style>
