import request from '@/utils/request'

export function getLabList(params) {
  return request({ url: '/lab/list', method: 'get', params })
}

export function getAllLabs() {
  return request({ url: '/lab/all', method: 'get' })
}

export function addLab(data) {
  return request({ url: '/lab', method: 'post', data })
}

export function updateLab(id, data) {
  return request({ url: '/lab/' + id, method: 'put', data })
}

export function deleteLab(id) {
  return request({ url: '/lab/' + id, method: 'delete' })
}

export function getManagedLabs() {
  return request({ url: '/lab/all', method: 'get' })
}