<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :xs="24" :lg="8">
        <div class="profile-card">
          <div class="profile-bg" :class="'bg-' + (userStore.userInfo?.role || 'STUDENT')"></div>
          <div class="profile-body">
            <div class="avatar-section">
              <el-upload
                class="avatar-uploader"
                action=""
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
              >
                <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" />
                <el-icon v-else class="avatar-uploader-icon"><UserFilled /></el-icon>
                <div class="avatar-edit-hint">
                  <el-icon :size="14"><Camera /></el-icon>
                </div>
              </el-upload>
            </div>
            <h3 class="profile-name">{{ userStore.userInfo?.username }}</h3>
            <div class="profile-role">
              <span class="role-badge" :class="'role-' + userStore.userInfo?.role">
                <el-icon :size="14"><component :is="roleIcon(userStore.userInfo?.role)" /></el-icon>
                {{ roleLabel(userStore.userInfo?.role) }}
              </span>
            </div>

            <div class="profile-stats">
              <div class="profile-info">
                <el-icon :size="16"><User /></el-icon>
                <span>{{ userStore.userInfo?.username }}</span>
              </div>
              <div class="profile-info">
                <el-icon :size="16"><Phone /></el-icon>
                <span>{{ userStore.userInfo?.phone || '未填写' }}</span>
              </div>
              <div class="profile-info">
                <el-icon :size="16"><Message /></el-icon>
                <span>{{ userStore.userInfo?.email || '未填写' }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :lg="16">
        <div class="settings-card">
          <el-tabs v-model="activeTab" class="profile-tabs">
            <el-tab-pane label="基本资料" name="info">
              <div class="tab-header">
                <div class="tab-icon" style="background: linear-gradient(135deg, #2563eb, #1d4ed8);">
                  <el-icon :size="20"><EditPen /></el-icon>
                </div>
                <div class="tab-title-wrap">
                  <h4>编辑基本资料</h4>
                  <p>更新您的联系信息和个人偏好</p>
                </div>
              </div>
              <el-form ref="infoFormRef" :model="infoForm" :rules="infoRules" label-width="100px" class="profile-form">
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="infoForm.username" placeholder="请输入用户名" clearable>
                    <template #prefix>
                      <el-icon><User /></el-icon>
                    </template>
                  </el-input>
                  <div class="form-tip">支持修改用户名（3-20位，可含字母、数字、下划线、中文）</div>
                </el-form-item>
                <el-form-item label="角色">
                  <el-input :model-value="roleLabel(userStore.userInfo?.role)" disabled />
                </el-form-item>
                <el-form-item label="手机号" prop="phone">
                  <el-input v-model="infoForm.phone" placeholder="请输入手机号" clearable>
                    <template #prefix>
                      <el-icon><Phone /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="infoForm.email" placeholder="请输入邮箱地址" clearable>
                    <template #prefix>
                      <el-icon><Message /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="infoLoading" @click="handleUpdateInfo" :icon="Check">
                    保存修改
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <div class="tab-header">
                <div class="tab-icon" style="background: linear-gradient(135deg, #60a5fa, #2563eb);">
                  <el-icon :size="20"><Lock /></el-icon>
                </div>
                <div class="tab-title-wrap">
                  <h4>修改登录密码</h4>
                  <p>定期更换密码可以提高账户安全性</p>
                </div>
              </div>
              <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" class="profile-form">
                <el-form-item label="旧密码" prop="oldPassword">
                  <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少6位字符" />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword" :icon="Lock">
                    修改密码
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getUserInfo, updateUserInfo, changePassword, uploadAvatar } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { UserFilled, User, Phone, Message, Camera, Lock, Check, EditPen, Monitor, Reading } from '@element-plus/icons-vue'

const userStore = useUserStore()
const activeTab = ref('info')
const avatarUrl = ref('')

const infoFormRef = ref(null)
const infoLoading = ref(false)
const infoForm = reactive({
  username: '',
  phone: '',
  email: '',
  avatar: '',
})

const infoRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需为3-20位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线和中文', trigger: 'blur' },
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [
    {
      pattern: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
      message: '邮箱格式不正确',
      trigger: 'blur',
    },
  ],
}

const passwordFormRef = ref(null)
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const roleIconMap = {
  ADMIN: Lock,
  LAB_ADMIN: Monitor,
  TEACHER: Reading,
  STUDENT: UserFilled
}

const roleIcon = (role) => roleIconMap[role] || UserFilled

const roleLabel = (role) => {
  if (role === 'ADMIN') return '系统管理员'
  if (role === 'LAB_ADMIN') return '实验室管理员'
  if (role === 'TEACHER') return '教师'
  return '学生'
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options) => {
  try {
    const res = await uploadAvatar(options.file)
    avatarUrl.value = res.data.url
    infoForm.avatar = res.data.url
    await updateUserInfo({ avatar: res.data.url })
    userStore.setUserInfo({ ...userStore.userInfo, avatar: res.data.url })
    userStore.refreshAvatar()
    ElMessage.success('头像更新成功')
  } catch (e) {
    ElMessage.error('头像上传失败')
  }
}

const handleUpdateInfo = async () => {
  const valid = await infoFormRef.value.validate().catch(() => false)
  if (!valid) return

  infoLoading.value = true
  try {
    const data = { ...infoForm }
    if (!data.avatar) delete data.avatar
    await updateUserInfo(data)
    userStore.setUserInfo({ ...userStore.userInfo, ...data })
    ElMessage.success('资料更新成功')
  } catch (e) {
    console.error(e)
  } finally {
    infoLoading.value = false
  }
}

const handleChangePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功')
    passwordFormRef.value.resetFields()
  } catch (e) {
    console.error(e)
  } finally {
    passwordLoading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getUserInfo()
    userStore.setUserInfo(res.data)
    infoForm.username = res.data.username || ''
    infoForm.phone = res.data.phone || ''
    infoForm.email = res.data.email || ''
    infoForm.avatar = res.data.avatar || ''
    avatarUrl.value = res.data.avatar || ''
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
.page-container {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.profile-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  position: relative;
}

.profile-bg {
  height: 120px;
  position: relative;
}

.profile-bg.bg-ADMIN {
  background: linear-gradient(135deg, #2563eb 0%, #1e40af 100%);
}

.profile-bg.bg-LAB_ADMIN {
  background: linear-gradient(135deg, #3b82f6 0%, #0ea5e9 100%);
}

.profile-bg.bg-TEACHER {
  background: linear-gradient(135deg, #60a5fa 0%, #2563eb 100%);
}

.profile-bg.bg-STUDENT {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
}

.profile-bg::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 40px;
  background: #fff;
  border-radius: 20px 20px 0 0;
}

.profile-body {
  padding: 0 24px 28px;
  text-align: center;
  position: relative;
}

.avatar-section {
  margin-top: -60px;
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}

.avatar-uploader :deep(.el-upload) {
  position: relative;
  border: 4px solid #fff;
  border-radius: 50%;
  cursor: pointer;
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  background: #f0f0f0;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #2563eb;
  transform: scale(1.05);
}

.avatar-img {
  width: 100px;
  height: 100px;
  object-fit: cover;
}

.avatar-uploader-icon {
  font-size: 36px;
  color: #c0c4cc;
}

.avatar-edit-hint {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 32px;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-uploader :deep(.el-upload:hover) .avatar-edit-hint {
  opacity: 1;
}

.profile-name {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.profile-role {
  margin-bottom: 24px;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.role-badge.role-ADMIN { background: #fde2e2; color: #d10000; }
.role-badge.role-LAB_ADMIN { background: #e1f3d8; color: #529b2e; }
.role-badge.role-TEACHER { background: #faecd8; color: #c68c06; }
.role-badge.role-STUDENT { background: #ecf5ff; color: #409eff; }

.profile-stats {
  background: #f8f9fc;
  border-radius: 12px;
  padding: 16px;
  text-align: left;
}

.profile-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
  color: #606266;
}

.profile-info:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.profile-info:first-child {
  padding-top: 0;
}

.profile-info .el-icon {
  color: #909399;
  flex-shrink: 0;
}

.settings-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  height: 100%;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.profile-tabs :deep(.el-tabs__item) {
  font-weight: 500;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: #2563eb;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #2563eb, #1d4ed8);
  height: 3px;
  border-radius: 3px;
}

.tab-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  border-radius: 12px;
  margin-bottom: 20px;
}

.tab-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.tab-title-wrap h4 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.tab-title-wrap p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.profile-form {
  max-width: 480px;
}

.form-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: all 0.3s;
}

.profile-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #2563eb inset;
}
</style>