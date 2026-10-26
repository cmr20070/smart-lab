import request from '@/utils/request'

export function getDeviceStatusStats() {
  return request({ url: '/statistics/device-status', method: 'get' })
}

export function getBorrowStats() {
  return request({ url: '/statistics/borrow', method: 'get' })
}

export function getDashboardStats() {
  return request({ url: '/statistics/dashboard', method: 'get' })
}
