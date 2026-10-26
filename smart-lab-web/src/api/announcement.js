import request from '@/utils/request'

export function getAnnouncementList(params) {
  return request({ url: '/announcement/list', method: 'get', params })
}

export function getAllAnnouncementList(params) {
  return request({ url: '/announcement/all', method: 'get', params })
}

export function publishAnnouncement(data) {
  return request({ url: '/announcement', method: 'post', data })
}

export function updateAnnouncement(id, data) {
  return request({ url: '/announcement/' + id, method: 'put', data })
}

export function withdrawAnnouncement(id) {
  return request({ url: '/announcement/withdraw/' + id, method: 'put' })
}

export function deleteAnnouncement(id) {
  return request({ url: '/announcement/' + id, method: 'delete' })
}