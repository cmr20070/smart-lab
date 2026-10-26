﻿<template>
  <el-container class="layout-container" :class="[isHorizontal ? 'layout-horizontal' : 'layout-vertical', roleTheme]">
    <!-- ============== 渚ц竟鏍?(绠＄悊鍛?瀹為獙瀹ょ鐞嗗憳) ============== -->
    <el-aside v-if="!isHorizontal" :width="isCollapse ? '72px' : '248px'" class="layout-aside">
      <div class="logo-container">
        <div class="logo-icon">
          <el-icon :size="22" color="#fff"><Cpu /></el-icon>
        </div>
        <transition name="fade">
          <div v-show="!isCollapse" class="logo-text-wrap">
            <span class="logo-title">智慧校园设备管理系统</span>
            <span class="logo-sub">智慧校园设备管理</span>
          </div>
        </transition>
      </div>

      <div class="role-badge">
        <div class="role-avatar" :style="{ background: roleGradient }">
          <el-icon :size="16"><component :is="roleIcon" /></el-icon>
        </div>
        <transition name="fade">
          <div v-show="!isCollapse" class="role-info">
            <span class="role-name">{{ userStore.userInfo?.username }}</span>
            <span class="role-type">{{ roleLabel }}</span>
          </div>
        </transition>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        :background-color="'transparent'"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#ffffff"
        router
        class="main-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>{{ isAdmin || isLabAdmin ? '工作台' : '首页' }}</template>
        </el-menu-item>

        <el-menu-item v-if="isAdmin" index="/user">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('lab')" index="/lab">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>{{ isAdmin || isLabAdmin ? '实验室管理' : '实验室' }}</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('labReservationManage')" index="/lab-reservation-manage">
          <el-icon><Calendar /></el-icon>
          <template #title>预约管理</template>
        </el-menu-item>

        <el-menu-item v-if="isTeacher" index="/sub-student-borrow">
          <el-icon><UserFilled /></el-icon>
          <template #title>👤 代学生申请</template>
        </el-menu-item>

        <el-menu-item v-if="isTeacher" index="/lab-reserve">
          <el-icon><Calendar /></el-icon>
          <template #title>🏢 实验室预约</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('device')" index="/device">
          <el-icon><Monitor /></el-icon>
          <template #title>{{ isAdmin || isLabAdmin ? '设备管理' : '全部设备' }}</template>
        </el-menu-item>

        <el-menu-item index="/borrow">
          <el-icon><Tickets /></el-icon>
          <template #title>{{ isLabAdmin ? '借用审批' : isAdmin ? '借用管理' : '我的借用' }}</template>
        </el-menu-item>

        <el-menu-item index="/repair">
          <el-icon><Tools /></el-icon>
          <template #title>维修维护</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('statistics')" index="/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>统计分析</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('log')" index="/log">
          <el-icon><Document /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>

        <el-menu-item v-if="isAdmin || isLabAdmin" index="/recyclebin">
          <el-icon><Delete /></el-icon>
          <template #title>回收站</template>
        </el-menu-item>

        <el-menu-item v-if="isAdmin" index="/discard">
          <el-icon><Files /></el-icon>
          <template #title>报废管理</template>
        </el-menu-item>

        <el-menu-item v-if="isAdmin || isLabAdmin" index="/announcement">
          <el-icon><Bell /></el-icon>
          <template #title>公告管理</template>
        </el-menu-item>
        <el-menu-item v-else index="/announcement">
          <el-icon><Bell /></el-icon>
          <template #title>公告通知</template>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <transition name="fade">
          <div v-show="!isCollapse" class="version-info">v2.0.0</div>
        </transition>
      </div>
    </el-aside>

    <!-- ============== 椤堕儴 Header (妯悜瀵艰埅) ============== -->
    <el-container class="main-container">
      <el-header class="layout-header" :class="{ 'header-horizontal': isHorizontal }">
        <!-- 妯悜甯冨眬鐨?Logo + 瀵艰埅 -->
        <template v-if="isHorizontal">
          <div class="header-left">
            <div class="horizontal-logo">
              <div class="logo-icon small">
                <el-icon :size="18" color="#fff"><Cpu /></el-icon>
              </div>
              <span class="logo-title text-dark">智慧校园设备管理系统</span>
            </div>

            <span v-if="isLoggedIn" class="role-chip" :class="isTeacher ? 'chip-teacher' : 'chip-student'">
              <el-icon :size="14"><component :is="isTeacher ? UserIcon : SchoolIcon" /></el-icon>
              {{ isTeacher ? '教师' : '学生' }}
            </span>

            <el-menu
              v-if="isLoggedIn"
              :default-active="activeMenu"
              mode="horizontal"
              router
              class="horizontal-menu"
              :ellipsis="false"
            >
              <el-menu-item index="/dashboard">
                <el-icon><HomeFilled /></el-icon>{{ isAdmin || isLabAdmin ? '工作台' : '首页' }}
              </el-menu-item>
              <el-menu-item index="/borrow">
                <el-icon><Tickets /></el-icon>{{ isLabAdmin ? '借用审批' : isAdmin ? '借用管理' : '我的借用' }}
              </el-menu-item>
              <el-menu-item v-if="isAdmin || isLabAdmin" index="/lab-reservation-manage">
                <el-icon><Calendar /></el-icon>预约管理
              </el-menu-item>
              <el-menu-item v-if="isTeacher" index="/sub-student-borrow">
                <el-icon><UserFilled /></el-icon>代学生申请
              </el-menu-item>
              <el-menu-item v-if="isTeacher" index="/lab-reserve">
                <el-icon><OfficeBuilding /></el-icon>实验室预约
              </el-menu-item>
              <el-menu-item v-else index="/lab">
                <el-icon><OfficeBuilding /></el-icon>{{ isAdmin || isLabAdmin ? '实验室管理' : '实验室' }}
              </el-menu-item>
              <el-menu-item index="/device">
                <el-icon><Monitor /></el-icon>{{ isAdmin || isLabAdmin ? '设备管理' : '全部设备' }}
              </el-menu-item>
              <el-menu-item index="/repair">
                <el-icon><Tools /></el-icon>维修维护
              </el-menu-item>
              <el-menu-item index="/profile">
                <el-icon><User /></el-icon>我的用户
              </el-menu-item>
              <el-menu-item v-if="isAdmin || isLabAdmin" index="/statistics">
                <el-icon><DataAnalysis /></el-icon>统计分析
              </el-menu-item>
              <el-menu-item v-if="isAdmin || isLabAdmin" index="/announcement">
                <el-icon><Bell /></el-icon>公告管理
              </el-menu-item>
              <el-menu-item v-else index="/announcement">
                <el-icon><Bell /></el-icon>公告通知
              </el-menu-item>
            </el-menu>
          </div>
        </template>

        <!-- 侧边栏布局的折叠按钮 + 面包屑 -->
        <template v-else>
          <div class="header-left">
            <div class="collapse-btn" @click="isCollapse = !isCollapse">
              <el-icon :size="18"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
            </div>
            <div class="breadcrumb" v-if="currentPageTitle">
              <span class="breadcrumb-sep">/</span>
              <span class="breadcrumb-current">{{ currentPageTitle }}</span>
            </div>
          </div>
        </template>

        <div class="header-right">
          <!-- 未登录：显示 登录/注册 按钮 -->
          <template v-if="!isLoggedIn">
            <el-button type="primary" round @click="router.push('/login')" class="guest-btn">
              <el-icon :size="14"><User /></el-icon>登录
            </el-button>
            <el-button round @click="router.push('/register')" class="guest-btn ghost">
              <el-icon :size="14"><UserFilled /></el-icon>注册
            </el-button>
          </template>

          <!-- 已登录：用户下拉 -->
          <template v-else>
            <div class="header-clock">
              <span class="clock-time">{{ clockTime }}</span>
              <span class="clock-date">{{ today }}</span>
            </div>

            <el-dropdown @command="handleCommand" trigger="click">
              <div class="user-dropdown">
                <el-avatar :src="avatarDisplayUrl || userStore.userInfo?.avatar" :size="36" class="user-avatar">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <div class="user-info">
                  <span class="user-name">{{ userStore.userInfo?.username }}</span>
                  <span class="user-role">{{ roleLabel }}</span>
                </div>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu class="custom-dropdown">
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </el-header>

      <el-main class="layout-main" :class="{ 'main-dashboard': isDashboard }">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getDashboardStats } from '@/api/statistics'
import {
  HomeFilled, User, OfficeBuilding, Monitor, Tickets, Tools, DataAnalysis,
  Document, Delete, Fold, Expand, Bell, UserFilled, ArrowDown, SwitchButton,
  Cpu, User as UserIcon, Avatar as AvatarIcon, School as SchoolIcon, Calendar, Files, CircleCheckFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const stats = ref({ pendingBorrow: 0, pendingRepair: 0 })

const role = computed(() => userStore.userInfo?.role || 'STUDENT')
const isAdmin = computed(() => role.value === 'ADMIN')
const isLabAdmin = computed(() => role.value === 'LAB_ADMIN')
const isTeacher = computed(() => role.value === 'TEACHER')
const isStudent = computed(() => role.value === 'STUDENT')
const isStudentOrTeacher = computed(() => isStudent.value || isTeacher.value)
const isDashboard = computed(() => route.path === '/dashboard' || route.path === '/')
const isLoggedIn = computed(() => !!userStore.token)

const isHorizontal = computed(() => !isAdmin.value && !isLabAdmin.value)

const roleTheme = computed(() => {
  const map = { ADMIN: 'theme-admin', LAB_ADMIN: 'theme-lab', TEACHER: 'theme-teacher' }
  return map[role.value] || 'theme-student'
})

const roleGradient = computed(() => {
  const map = {
    ADMIN: 'linear-gradient(135deg, #2563eb, #1d4ed8)',
    LAB_ADMIN: 'linear-gradient(135deg, #2563eb, #1d4ed8)',
    TEACHER: 'linear-gradient(135deg, #2563eb, #1d4ed8)',
  }
  return map[role.value] || 'linear-gradient(135deg, #2563eb, #1d4ed8)'
})

const roleLabel = computed(() => {
  const map = { ADMIN: '系统管理员', LAB_ADMIN: '实验室管理员', TEACHER: '教师' }
  return map[role.value] || '学生'
})

const roleIcon = computed(() => {
  const map = { ADMIN: AvatarIcon, LAB_ADMIN: SchoolIcon, TEACHER: UserIcon }
  return map[role.value] || UserIcon
})

const titleMap = {
  '/dashboard': isAdmin || isLabAdmin ? '工作台' : '首页',
  '/user': '用户管理',
  '/lab': isAdmin || isLabAdmin ? '实验室管理' : '实验室',
  '/lab-reservation-manage': '预约管理',
  '/device': isAdmin || isLabAdmin ? '设备管理' : '全部设备',
  '/borrow': isLabAdmin.value ? '借用审批' : isAdmin ? '借用管理' : '我的借用',
  '/sub-student-borrow': '代学生申请',
  '/lab-reserve': '实验室预约',
  '/repair': '维修维护',
  '/statistics': '统计分析',
  '/log': '操作日志',
  '/recyclebin': '回收站',
  '/discard': '报废管理',
  '/announcement': isAdmin || isLabAdmin ? '公告管理' : '公告通知',
  '/profile': '我的用户',
}

const currentPageTitle = computed(() => titleMap[route.path] || '')

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/user')) return '/user'
  if (path.startsWith('/lab-reservation-manage')) return '/lab-reservation-manage'
  if (path.startsWith('/lab')) return '/lab'
  if (path.startsWith('/device')) return '/device'
  if (path.startsWith('/borrow')) return '/borrow'
  if (path.startsWith('/sub-student-borrow')) return '/sub-student-borrow'
  if (path.startsWith('/lab-reserve')) return '/lab-reserve'
  if (path.startsWith('/repair')) return '/repair'
  if (path.startsWith('/statistics')) return '/statistics'
  if (path.startsWith('/recyclebin')) return '/recyclebin'
  if (path.startsWith('/discard')) return '/discard'
  if (path.startsWith('/log')) return '/log'
  if (path.startsWith('/profile')) return ''
  return '/dashboard'
})

const avatarDisplayUrl = ref('')

watch(
  () => [userStore.avatarVersion, userStore.userInfo?.avatar],
  () => {
    const avatar = userStore.userInfo?.avatar
    avatarDisplayUrl.value = avatar ? avatar + '?t=' + Date.now() : ''
  },
  { immediate: true }
)

const canAccess = (page) => {
  if (isAdmin.value) return true
  if (page === 'lab' || page === 'recyclebin' || page === 'labReservationManage') return isLabAdmin.value
  if (page === 'statistics') return isLabAdmin.value
  if (page === 'log') return false
  return true
}

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

const clockTime = ref('')
const today = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getMonth() + 1}月${d.getDate()}日 周${week[d.getDay()]}`
})
let clockTimer = null

const updateClock = () => {
  const d = new Date()
  clockTime.value = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const goToBorrow = () => router.push('/borrow')

onMounted(async () => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  try {
    const res = await getDashboardStats()
    stats.value = res.data
  } catch (e) {}
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-horizontal .main-container {
  background: var(--bg-primary, #f8fafc);
}

/* ============ 渚ц竟鏍?============ */
.layout-aside {
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  overflow: hidden;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 4px 0 24px rgba(15, 23, 42, 0.15);
}

.layout-aside::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
}

.theme-admin .layout-aside {
  background: linear-gradient(180deg, #1e3a8a 0%, #0c2340 100%);
}
.theme-lab .layout-aside {
  background: linear-gradient(180deg, #1e3a8a 0%, #0c2340 100%);
}
.theme-teacher .layout-aside {
  background: linear-gradient(180deg, #1e3a8a 0%, #0c2340 100%);
}
.theme-student .layout-aside {
  background: linear-gradient(180deg, #1e3a8a 0%, #0c2340 100%);
}

.logo-container {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo-icon {
  width: 40px; height: 40px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
}

.logo-icon.small {
  width: 34px; height: 34px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

.theme-admin .logo-icon { background: linear-gradient(135deg, #60a5fa, #2563eb); }
.theme-lab .logo-icon { background: linear-gradient(135deg, #60a5fa, #2563eb); }
.theme-teacher .logo-icon { background: linear-gradient(135deg, #60a5fa, #2563eb); }
.theme-student .logo-icon { background: linear-gradient(135deg, #60a5fa, #2563eb); }



.logo-text-wrap {
  display: flex; flex-direction: column; overflow: hidden;
}

.logo-title {
  color: #fff; font-size: 17px; font-weight: 700; letter-spacing: 0.5px;
}
.logo-title.text-dark {
  color: #0f172a;
  font-size: 18px;
  background: linear-gradient(135deg, #0ea5e9, #3b82f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.theme-teacher .logo-title.text-dark {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.theme-student .logo-title.text-dark {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-sub {
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px; letter-spacing: 1px;
}

.role-badge {
  display: flex; align-items: center;
  padding: 16px; gap: 12px;
  margin: 12px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  min-height: 64px;
}

.role-avatar {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0;
}

.role-info { display: flex; flex-direction: column; overflow: hidden; }
.role-name {
  color: #fff; font-size: 14px; font-weight: 600;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.role-type { color: rgba(255, 255, 255, 0.5); font-size: 11px; margin-top: 2px; }

.main-menu {
  flex: 1; border-right: none !important; padding: 8px; overflow-y: auto;
}

.main-menu .el-menu-item {
  height: 44px; line-height: 44px;
  border-radius: 10px; margin-bottom: 4px;
  font-size: 14px; transition: all 0.25s ease;
}
.main-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #fff !important;
}
.main-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.9), rgba(30, 64, 175, 0.9)) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}
.theme-admin .main-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.95), rgba(37, 99, 235, 0.95)) !important;
}
.theme-lab .main-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.95), rgba(37, 99, 235, 0.95)) !important;
}
.theme-teacher .main-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.95), rgba(37, 99, 235, 0.95)) !important;
}
.theme-student .main-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.95), rgba(37, 99, 235, 0.95)) !important;
}

.main-menu .el-menu-item .el-icon { font-size: 18px; }

.aside-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}
.version-info {
  color: rgba(255, 255, 255, 0.3);
  font-size: 11px; text-align: center; letter-spacing: 2px;
}

/* ============ Main Container ============ */
.main-container { background: var(--bg-primary, #f8fafc); }

/* ============ Header ============ */
.layout-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid #e2e8f0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 28px;
  height: 64px;
  position: sticky; top: 0; z-index: 100;
}

.header-horizontal {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 2px 12px rgba(29, 78, 216, 0.3);
}

.header-left {
  display: flex; align-items: center; gap: 20px;
}

.horizontal-logo {
  display: flex; align-items: center; gap: 10px;
  flex-shrink: 0;
}

.role-chip {
  display: inline-flex; align-items: center; gap: 5px;
  height: 26px; padding: 0 12px;
  border-radius: 999px;
  font-size: 12px; font-weight: 600;
  color: #1d4ed8;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  border: 1px solid #bfdbfe;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(37, 99, 235, 0.12);
}
.chip-teacher { color: #1e40af; background: linear-gradient(135deg, #eff6ff, #dbeafe); border-color: #bfdbfe; }
.chip-student { color: #1d4ed8; background: linear-gradient(135deg, #eff6ff, #dbeafe); border-color: #bfdbfe; }

.horizontal-menu {
  border-bottom: none !important;
  background: transparent !important;
  display: flex !important;
  gap: 4px;
}

.horizontal-menu :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  border-radius: 10px !important;
  margin: 0 2px !important;
  font-size: 14px !important;
  color: #64748b !important;
  border-bottom: none !important;
  padding: 0 16px !important;
  transition: all 0.25s ease !important;
}

.horizontal-menu :deep(.el-menu-item:hover) {
  background: #f1f5f9 !important;
  color: #0f172a !important;
}

/* 实验室中心下拉子菜单 (教师) */
.horizontal-menu :deep(.el-sub-menu > .el-sub-menu__title) {
  height: 40px;
  line-height: 40px;
  border-radius: 10px !important;
  margin: 0 2px !important;
  font-size: 14px !important;
  color: #64748b !important;
  border-bottom: none !important;
  padding: 0 16px !important;
  transition: all 0.25s ease !important;
}
.horizontal-menu :deep(.el-sub-menu > .el-sub-menu__title:hover) {
  background: #f1f5f9 !important;
  color: #0f172a !important;
}
.horizontal-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  background: linear-gradient(135deg, #0ea5e9, #3b82f6) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
}
.horizontal-menu :deep(.el-sub-menu .el-menu) {
  border-radius: 12px;
  padding: 6px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.12);
}
.horizontal-menu :deep(.el-sub-menu .el-menu .el-menu-item) {
  border-radius: 8px;
  margin: 2px 0;
}

.menu-divider {
  width: 1px; height: 22px;
  background: #e2e8f0;
  margin: 0 10px;
  align-self: center;
}

.horizontal-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #0ea5e9, #3b82f6) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
  border-bottom: none !important;
}

.theme-teacher .horizontal-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #0ea5e9, #3b82f6) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25) !important;
}
.theme-student .horizontal-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #0ea5e9, #3b82f6) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25) !important;
}
.theme-teacher .user-avatar {
  background: linear-gradient(135deg, #60a5fa, #2563eb) !important;
}
.theme-student .user-avatar {
  background: linear-gradient(135deg, #60a5fa, #2563eb) !important;
}



.horizontal-menu :deep(.el-menu-item .el-icon) {
  font-size: 16px;
  margin-right: 6px;
}

/* ===== 蓝色顶部导航（教师/学生）统一文字与高亮 ===== */
.header-horizontal .logo-title.text-dark {
  color: #fff; background: none; -webkit-text-fill-color: #fff; background-clip: border-box;
}
.header-horizontal :deep(.el-menu-item),
.header-horizontal :deep(.el-sub-menu > .el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.85) !important;
}
.header-horizontal :deep(.el-menu-item:hover),
.header-horizontal :deep(.el-sub-menu > .el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.14) !important;
  color: #fff !important;
}
.header-horizontal :deep(.el-menu-item.is-active),
.header-horizontal :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  background: rgba(255, 255, 255, 0.22) !important;
  color: #fff !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.3);
}
.theme-teacher .header-horizontal :deep(.el-menu-item.is-active),
.theme-student .header-horizontal :deep(.el-menu-item.is-active),
.theme-teacher .header-horizontal :deep(.el-sub-menu.is-active > .el-sub-menu__title),
.theme-student .header-horizontal :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  background: rgba(255, 255, 255, 0.22) !important;
  color: #fff !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.3);
}
.header-horizontal .clock-time { color: #fff; }
.header-horizontal .clock-date { color: rgba(255, 255, 255, 0.7); }
.header-horizontal .user-dropdown:hover { background: rgba(255, 255, 255, 0.14); }
.header-horizontal .user-name { color: #fff; }
.header-horizontal .user-role { color: rgba(255, 255, 255, 0.7); }
.header-horizontal .arrow-icon { color: rgba(255, 255, 255, 0.7); }

/* collapse btn & breadcrumb */
.collapse-btn {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10px; cursor: pointer;
  color: #64748b; transition: all 0.2s;
}
.collapse-btn:hover { background: #f1f5f9; color: #3b82f6; }

.breadcrumb { display: flex; align-items: center; gap: 8px; font-size: 14px; }
.breadcrumb-sep { color: #94a3b8; }
.breadcrumb-current { color: #0f172a; font-weight: 600; }

/* header right */
.header-right { display: flex; align-items: center; gap: 20px; }

/* 未登录访客 登录/注册 按钮 */
.guest-btn { min-width: 86px; font-weight: 600; }
.guest-btn.ghost { background: #fff; border: 1px solid var(--el-color-primary); color: var(--el-color-primary); }
.guest-btn.ghost:hover { background: var(--el-color-primary-light-9); }

.header-clock {
  display: flex; flex-direction: column; align-items: flex-end; line-height: 1.1;
}
.clock-time {
  font-size: 15px; font-weight: 600; color: #0f172a;
  font-family: 'SF Mono', Consolas, monospace;
}
.clock-date { font-size: 11px; color: #94a3b8; margin-top: 2px; }

.user-dropdown {
  display: flex; align-items: center; gap: 10px;
  cursor: pointer; padding: 4px 12px 4px 4px;
  border-radius: 24px; transition: all 0.2s;
}
.user-dropdown:hover { background: #f1f5f9; }

.user-avatar {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}

.user-info { display: flex; flex-direction: column; line-height: 1.2; }
.user-name { font-size: 14px; font-weight: 600; color: #0f172a; }
.user-role { font-size: 11px; color: #94a3b8; }
.arrow-icon { font-size: 12px; color: #94a3b8; transition: transform 0.2s; }

/* main */
.layout-main {
  padding: 24px;
  overflow-y: auto;
  background: var(--bg-primary, #f8fafc);
}
.layout-main.main-dashboard {
  padding: 0 24px 24px;
}

/* transitions */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.fade-transform-enter-active, .fade-transform-leave-active { transition: all 0.3s ease; }
.fade-transform-enter-from { opacity: 0; transform: translateY(12px); }
.fade-transform-leave-to { opacity: 0; transform: translateY(-12px); }

.custom-dropdown :deep(.el-dropdown-menu__item) { padding: 10px 20px; border-radius: 0; }
.custom-dropdown :deep(.el-dropdown-menu__item .el-icon) { margin-right: 8px; color: #94a3b8; }

/* role theme for horizontal header - teacher */


/* responsive */
@media (max-width: 1024px) {
  .horizontal-menu {
    display: none !important;
  }
}

@media (max-width: 768px) {
  .header-clock, .role-info { display: none; }
  .horizontal-logo .logo-title.text-dark { display: none; }
  .layout-main { padding: 16px; }
}
</style>