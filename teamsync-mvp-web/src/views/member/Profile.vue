<template>
  <div class="profile-page">
    <el-card shadow="hover" class="profile-card">
      <template #header>
        <span>个人中心</span>
      </template>

      <!-- 头像 -->
      <div class="avatar-section">
        <el-avatar
          :size="80"
          :src="avatarPreview"
          :icon="UserFilled"
          class="profile-avatar"
          @click="triggerFileSelect"
        />
        <div class="avatar-tip">点击头像更换</div>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleFileChange"
        />
      </div>

      <!-- 表单 -->
      <el-form :model="form" label-width="80px" class="profile-form">
        <el-form-item label="登录账号">
          <el-input :model-value="userStore.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.nickname" placeholder="请输入姓名" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.bio" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="介绍一下自己" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" placeholder="请选择" clearable style="width: 180px">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
            <el-option label="保密" value="保密" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="0" :max="150" controls-position="right" style="width: 180px" />
        </el-form-item>
        <el-form-item label="住址">
          <el-input v-model="form.address" placeholder="请输入住址" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <div class="logout-section">
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { updateProfile } from '@/api/user'
import { uploadFile } from '@/api/upload'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const fileInputRef = ref<HTMLInputElement>()
const saving = ref(false)
const newAvatar = ref('') // 本次新上传的头像相对路径,未保存前用于预览

const form = reactive({
  nickname: '',
  bio: '',
  gender: '',
  age: null as number | null,
  address: ''
})

const avatarPreview = computed(() =>
  newAvatar.value ? '/uploads/' + newAvatar.value : userStore.avatarUrl
)

function triggerFileSelect() {
  fileInputRef.value?.click()
}

async function handleFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const res: any = await uploadFile(file)
  if (res.code === 200) {
    newAvatar.value = res.data.fileUrl
    ElMessage.success('头像已上传,保存后生效')
  } else {
    ElMessage.error(res.msg || '头像上传失败')
  }
  ;(e.target as HTMLInputElement).value = ''
}

async function handleSave() {
  saving.value = true
  try {
    const res: any = await updateProfile({
      avatar: newAvatar.value || userStore.userInfo.avatar || '',
      nickname: form.nickname,
      bio: form.bio,
      gender: form.gender,
      age: form.age ?? null,
      address: form.address
    })
    if (res.code === 200) {
      await userStore.fetchUserInfo() // 刷新 store 并持久化,顶部头像/姓名即时更新
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

function handleLogout() {
  ElMessageBox.confirm('确定退出登录吗？', '提示').then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}

onMounted(async () => {
  await userStore.fetchUserInfo()
  form.nickname = userStore.userInfo.nickname ?? ''
  form.bio = userStore.userInfo.bio ?? ''
  form.gender = userStore.userInfo.gender ?? ''
  form.age = userStore.userInfo.age ?? null
  form.address = userStore.userInfo.address ?? ''
})
</script>

<style scoped>
.profile-page {
  max-width: 600px;
  margin: 0 auto;
}
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}
.profile-avatar {
  cursor: pointer;
  transition: opacity 0.2s;
}
.profile-avatar:hover {
  opacity: 0.85;
}
.avatar-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
.profile-form {
  margin-top: 8px;
}
.logout-section {
  text-align: center;
}
</style>
