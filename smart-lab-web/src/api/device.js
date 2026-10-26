import request from '@/utils/request'

export function getDeviceList(params) {
  return request({ url: '/device/list', method: 'get', params })
}

export function getAllDevices(params) {
  if (params) {
    return request({ url: '/device/list', method: 'get', params: { page: 1, size: 1000, ...params } })
  }
  return request({ url: '/device/all', method: 'get' })
}

export function addDevice(data) {
  return request({ url: '/device', method: 'post', data })
}

export function updateDevice(id, data) {
  return request({ url: '/device/' + id, method: 'put', data })
}

export function deleteDevice(id) {
  return request({ url: '/device/' + id, method: 'delete' })
}

export function updateDeviceStatus(id, data) {
  return request({ url: '/device/' + id + '/status', method: 'put', data })
}

export function getDiscardList(params) {
  return request({ url: '/device/discard/list', method: 'get', params })
}

export function submitDiscard(id, reason) {
  return request({ url: '/device/' + id + '/discard', method: 'put', data: { reason } })
}

export function approveDiscard(id) {
  return request({ url: '/device/' + id + '/discard/approve', method: 'put' })
}

export function approveFinalDiscard(id) {
  return request({ url: '/device/' + id + '/discard/approve-final', method: 'put' })
}

export function rejectDiscard(id) {
  return request({ url: '/device/' + id + '/discard/reject', method: 'put' })
}

export function getOverdueBorrows(params) {
  return request({ url: '/device/overdue/borrows', method: 'get', params })
}