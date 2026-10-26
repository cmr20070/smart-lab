import request from '@/utils/request'

export function getBorrowList(params) {
  return request({ url: '/borrow/list', method: 'get', params })
}

export function applyBorrow(data) {
  return request({ url: '/borrow/apply', method: 'post', data })
}

export function approveBorrow(data) {
  return request({ url: '/borrow/approve', method: 'put', data })
}

export function returnDevice(id) {
  return request({ url: '/borrow/return/' + id, method: 'put' })
}

export function cancelBorrow(id) {
  return request({ url: '/borrow/cancel/' + id, method: 'put' })
}

export function deleteBorrow(id) {
  return request({ url: '/borrow/' + id, method: 'delete' })
}

export function batchApplyBorrow(data) {
  return request({ url: '/borrow/batch', method: 'post', data })
}

export function remindBorrow(id) {
  return request({ url: '/borrow/remind/' + id, method: 'put' })
}