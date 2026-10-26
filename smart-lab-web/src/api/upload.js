import request from '@/utils/request'

export function uploadImage(file, type) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload/' + type,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteImage(url) {
  return request({
    url: '/upload/delete',
    method: 'delete',
    params: { url }
  })
}