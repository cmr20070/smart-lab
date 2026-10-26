import request from '@/utils/request'

export function getRepairList(params) {
  return request({ url: '/repair/list', method: 'get', params })
}

export function reportFault(data) {
  return request({ url: '/repair/report', method: 'post', data })
}

export function assignRepair(id) {
  return request({ url: '/repair/assign/' + id, method: 'put' })
}

export function completeRepair(id, data) {
  return request({ url: '/repair/complete/' + id, method: 'put', data })
}

export function updateRepair(id, data) {
  return request({ url: '/repair/' + id, method: 'put', data })
}

export function deleteRepair(id) {
  return request({ url: '/repair/' + id, method: 'delete' })
}