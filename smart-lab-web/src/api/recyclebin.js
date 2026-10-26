import request from '@/utils/request'

export function getTrashList(type, page = 1, size = 10) {
  return request.get('/recyclebin/list', { params: { type, page, size } })
}

export function getTrashCounts() {
  return request.get('/recyclebin/counts')
}

export function restore(type, id) {
  return request.put(`/recyclebin/restore/${type}/${id}`)
}

export function purge(type, id) {
  return request.delete(`/recyclebin/purge/${type}/${id}`)
}

export function purgeAll(type) {
  return request.delete(`/recyclebin/purge/${type}`)
}