<template>
  <div class="register-page">
    <div class="register-wrapper">
      <div class="register-brand">
        <div class="brand-content">
          <div class="brand-icon">
            <el-icon :size="48" color="#fff"><Cpu /></el-icon>
          </div>
          <h1 class="brand-title">智慧校园实验室设备管理系统</h1>
          <p class="brand-desc">Smart Campus Laboratory Equipment Management System</p>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon :size="16"><CircleCheckFilled /></el-icon>
              <span>快速注册，即刻体验</span>
            </div>
            <div class="feature-item">
              <el-icon :size="16"><CircleCheckFilled /></el-icon>
              <span>多角色权限管理</span>
            </div>
            <div class="feature-item">
              <el-icon :size="16"><CircleCheckFilled /></el-icon>
              <span>安全可靠的数据保障</span>
            </div>
          </div>
        </div>
      </div>
      <div class="register-form-panel">
        <div class="form-inner">
          <h2 class="form-title">创建账号</h2>
          <p class="form-subtitle">填写信息完成注册</p>
          <el-form ref="formRef" :model="form" :rules="rules" size="large">
            <el-form-item class="avatar-item">
              <el-upload
                class="avatar-uploader"
                action=""
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
              >
                <img v-if="avatarUrl" :src="avatarUrl" class="avatar-preview" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <span class="avatar-label">上传头像</span>
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="24">
                <el-form-item prop="username">
                  <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item prop="password">
                  <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item prop="confirmPassword">
                  <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item prop="phone">
                  <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Phone" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item prop="role">
                  <el-select v-model="form.role" placeholder="选择角色" style="width: 100%">
                    <el-option label="学生" value="STUDENT" />
                    <el-option label="教师" value="TEACHER" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item prop="email">
              <el-input v-model="form.email" placeholder="邮箱（选填）" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="register-btn" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>已有账号？</span>
            <router-link to="/login">立即登录</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register, uploadAvatar } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { Plus, User, Lock, Phone, Message } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const avatarUrl = ref('')

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
  role: 'STUDENT',
  avatar: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需为3-20位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线和中文', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需为6-20位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  email: [
    {
      pattern: /^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
      message: '邮箱格式不正确',
      trigger: 'blur',
    },
  ],
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
    form.avatar = res.data.url
    avatarUrl.value = res.data.url
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error('头像上传失败')
  }
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      phone: form.phone,
      email: form.email,
      role: form.role,
      avatar: form.avatar,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 50%, #f0f9ff 100%);
  padding: 20px;
}

.register-wrapper {
  display: flex;
  width: 1000px;
  min-height: 640px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow:
    0 8px 40px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(0, 0, 0, 0.04);
}

.register-brand {
  flex: 0 0 380px;
  background: linear-gradient(160deg, #1e40af 0%, #1e3a8a 40%, #0c2340 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.register-brand::before {
  content: '';
  position: absolute;
  top: -60px;
  right: -60px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.03);
}

.register-brand::after {
  content: '';
  position: absolute;
  bottom: -40px;
  left: -40px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.03);
}

.brand-content {
  text-align: center;
  padding: 30px;
  position: relative;
  z-index: 1;
}

.brand-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.brand-title {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
  letter-spacing: 2px;
}

.brand-desc {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  letter-spacing: 1px;
  margin-bottom: 28px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: left;
  padding: 0 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.feature-item .el-icon {
  color: #67c23a;
}

.register-form-panel {
  flex: 1;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 30px;
}

.form-inner {
  width: 100%;
  max-width: 420px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e40af;
  margin-bottom: 6px;
}

.form-subtitle {
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}

.avatar-item {
  text-align: center;
  margin-bottom: 8px;
}

.avatar-item .avatar-label {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 4px;
}

.avatar-uploader :deep(.el-upload) {
  border: 2px dashed #d9d9d9;
  border-radius: 50%;
  cursor: pointer;
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: border-color 0.3s;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #2563eb;
}

.avatar-uploader-icon {
  font-size: 24px;
  color: #8c939d;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border: none;
  margin-top: 4px;
}

.register-btn:hover {
  background: linear-gradient(135deg, #60a5fa 0%, #2563eb 100%);
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-top: -8px;
}

.form-footer a {
  color: #2563eb;
  margin-left: 4px;
  text-decoration: none;
  font-weight: 500;
}

.form-footer a:hover {
  color: #1d4ed8;
}
</style>