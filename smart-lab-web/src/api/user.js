import request from '@/utils/request'

export function getAllUsers(role) {
  return request({ url: '/user/all', method: 'get', params: role ? { role } : {} })
}