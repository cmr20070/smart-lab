import request from '@/utils/request'

export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

export function register(data) {
  return request({ url: '/auth/register', method: 'post', data })
}

export function getUserInfo() {
  return request({ url: '/user/info', method: 'get' })
}

export function updateUserInfo(data) {
  return request({ url: '/user/info', method: 'put', data })
}

export function changePassword(data) {
  return request({ url: '/user/password', method: 'put', data })
}

export function getUserList(params) {
  return request({ url: '/admin/user/list', method: 'get', params })
}

export function getAllUsers(params) {
  return request({ url: '/user/all', method: 'get', params })
}

export function addUser(data) {
  return request({ url: '/admin/user', method: 'post', data })
}

export function updateUser(id, data) {
  return request({ url: '/admin/user/' + id, method: 'put', data })
}

export function deleteUser(id) {
  return request({ url: '/admin/user/' + id, method: 'delete' })
}

export function toggleUserStatus(id, status) {
  return request({ url: '/admin/user/' + id + '/status', method: 'put', params: { status } })
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/upload/avatar', method: 'post', data: formData })
}