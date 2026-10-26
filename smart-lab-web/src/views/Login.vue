<template>
  <div class="login-page">
    <div class="login-wrapper">
      <div class="login-brand">
        <div class="brand-content">
          <div class="brand-icon">
            <el-icon :size="56" color="#fff"><Cpu /></el-icon>
          </div>
          <h1 class="brand-title">智慧校园实验室设备管理系统</h1>
          <p class="brand-desc">Smart Campus Laboratory Equipment Management System</p>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon :size="18"><CircleCheckFilled /></el-icon>
              <span>实验室设备全生命周期管理</span>
            </div>
            <div class="feature-item">
              <el-icon :size="18"><CircleCheckFilled /></el-icon>
              <span>智能预约与审批流程</span>
            </div>
            <div class="feature-item">
              <el-icon :size="18"><CircleCheckFilled /></el-icon>
              <span>多维度数据统计分析</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-form-panel">
        <div class="form-inner">
          <div class="form-back">
            <el-link :icon="ArrowLeft" @click="router.push('/')">返回浏览首页</el-link>
          </div>
          <h2 class="form-title">欢迎登录</h2>
          <p class="form-subtitle">请输入您的账号信息</p>
          <el-form ref="formRef" :model="form" :rules="rules" size="large">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>还没有账号？</span>
            <router-link to="/register">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { login } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.userInfo)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // error handled by interceptor
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
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 50%, #f0f9ff 100%);
}

.login-wrapper {
  display: flex;
  width: 960px;
  height: 560px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow:
    0 8px 40px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(0, 0, 0, 0.04);
}

.login-brand {
  flex: 1;
  background: linear-gradient(160deg, #1e40af 0%, #1e3a8a 40%, #0c2340 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  top: -60px;
  right: -60px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.03);
}

.login-brand::after {
  content: '';
  position: absolute;
  bottom: -40px;
  left: -40px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.03);
}

.brand-content {
  text-align: center;
  padding: 40px;
  position: relative;
  z-index: 1;
}

.brand-icon {
  width: 96px;
  height: 96px;
  margin: 0 auto 24px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.brand-title {
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.brand-desc {
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
  letter-spacing: 1px;
  margin-bottom: 36px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  text-align: left;
  padding: 0 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
}

.feature-item .el-icon {
  color: #67c23a;
}

.login-form-panel {
  flex: 1;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-inner {
  width: 340px;
  position: relative;
}

.form-back {
  position: absolute;
  top: -38px;
  left: 0;
}

.form-back .el-link {
  font-size: 14px;
  color: #2563eb;
}

.form-back .el-link:hover {
  color: #1d4ed8;
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
  margin-bottom: 32px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border: none;
}

.login-btn:hover {
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