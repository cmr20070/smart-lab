import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    if (response.config.url && response.config.url.startsWith('/export/')) {
      return response
    }
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        // 未登录/登录过期：清空凭证并跳回首页(展示登录/注册按钮)，而非登录页
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        const cur = router.currentRoute.value.path
        if (cur !== '/' && cur !== '/dashboard') {
          router.push('/')
        }
        return Promise.reject(new Error('未登录或登录已过期'))
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      const cur = router.currentRoute.value.path
      if (cur !== '/' && cur !== '/dashboard') {
        router.push('/')
      }
      return Promise.reject(new Error('未登录或登录已过期'))
    }
    if (error.response?.status === 403 && !localStorage.getItem('token')) {
      return Promise.reject(error)
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export function downloadFile(url, filename) {
  const token = localStorage.getItem('token')
  return axios.get(url, {
    baseURL: '/api',
    responseType: 'blob',
    headers: { Authorization: 'Bearer ' + token }
  }).then(response => {
    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename + '.xlsx'
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  }).catch(error => {
    ElMessage.error('导出失败')
  })
}

export default request