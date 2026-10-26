import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/UserManage.vue'),
        meta: { title: '用户管理', role: 'ADMIN' },
      },
      {
        path: 'lab',
        name: 'LabManage',
        component: () => import('@/views/LabManage.vue'),
        meta: { title: '实验室' },
      },
      {
        path: 'device',
        name: 'DeviceManage',
        component: () => import('@/views/DeviceManage.vue'),
        meta: { title: '全部设备' },
      },
      {
        path: 'borrow',
        name: 'BorrowManage',
        component: () => import('@/views/BorrowManage.vue'),
        meta: { title: '借用预约' },
      },
      {
        path: 'batch-borrow',
        name: 'BatchBorrow',
        component: () => import('@/views/BatchBorrow.vue'),
        meta: { title: '批量申领', roles: ['TEACHER'] },
      },
      {
        path: 'sub-student-borrow',
        name: 'SubStudentBorrow',
        component: () => import('@/views/SubStudentBorrow.vue'),
        meta: { title: '代学生申请', roles: ['TEACHER'] },
      },
      {
        path: 'lab-reserve',
        name: 'LabReserve',
        component: () => import('@/views/LabReserve.vue'),
        meta: { title: '实验室预约', roles: ['TEACHER', 'ADMIN', 'LAB_ADMIN'] },
      },
      {
        path: 'lab-reservation-manage',
        name: 'LabReservationManage',
        component: () => import('@/views/LabReservationManage.vue'),
        meta: { title: '预约管理', roles: ['ADMIN', 'LAB_ADMIN'] },
      },
      {
        path: 'repair',
        name: 'RepairManage',
        component: () => import('@/views/RepairManage.vue'),
        meta: { title: '维修维护' },
      },
      {
        path: 'log',
        name: 'LogView',
        component: () => import('@/views/LogView.vue'),
        meta: { title: '操作日志', roles: ['ADMIN'] },
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '统计分析', roles: ['ADMIN', 'LAB_ADMIN'] },
      },
      {
        path: 'recyclebin',
        name: 'RecycleBin',
        component: () => import('@/views/RecycleBin.vue'),
        meta: { title: '回收站', roles: ['ADMIN', 'LAB_ADMIN'] },
      },
      {
        path: 'discard',
        name: 'DiscardManage',
        component: () => import('@/views/DiscardManage.vue'),
        meta: { title: '报废管理', roles: ['ADMIN'] },
      },
      {
        path: 'announcement',
        name: 'Announcement',
        component: () => import('@/views/Announcement.vue'),
        meta: { title: '公告通知' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? to.meta.title + ' - 智慧校园设备管理系统' : '智慧校园设备管理系统'
  const userStore = useUserStore()
  // 未登录：仅允许访问首页/设备浏览/登录/注册页，其它一律跳到首页
  if (!userStore.token) {
    const publicPaths = ['/', '/dashboard', '/device', '/login', '/register']
    if (!publicPaths.includes(to.path)) {
      next('/')
      return
    }
  } else {
    // 已登录：跳回首页，避免重复登录
    if (to.path === '/login' || to.path === '/register') {
      next('/')
      return
    }
  }
  if (to.meta.role && userStore.userInfo) {
    if (to.meta.role === 'ADMIN' && userStore.userInfo.role !== 'ADMIN') {
      next('/dashboard')
      return
    }
  }
  if (to.meta.roles && userStore.userInfo) {
    if (!to.meta.roles.includes(userStore.userInfo.role)) {
      next('/dashboard')
      return
    }
  }
  next()
})

export default router