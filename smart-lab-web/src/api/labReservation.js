import request from '@/utils/request'

export function applyReservation(data) {
  return request({ url: '/lab-reservation/apply', method: 'post', data })
}

export function getReservationList(params) {
  return request({ url: '/lab-reservation/list', method: 'get', params })
}

export function approveReservation(data) {
  return request({ url: '/lab-reservation/approve', method: 'put', data })
}

export function cancelReservation(id) {
  return request({ url: `/lab-reservation/cancel/${id}`, method: 'put' })
}

export function adminCancelReservation(id, remark) {
  return request({ url: `/lab-reservation/admin-cancel/${id}`, method: 'put', data: { remark } })
}

export function deleteReservation(id) {
  return request({ url: `/lab-reservation/${id}`, method: 'delete' })
}