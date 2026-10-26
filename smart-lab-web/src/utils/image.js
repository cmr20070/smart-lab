// 统一处理图片地址：对含空格、中文等特殊字符的本地 /uploads 路径做 URL 编码，
// 避免在 CSS background-image / <img src> 中被截断导致图片不显示。
export function getImageUrl(url) {
  if (!url) return ''
  if (typeof url !== 'string') return url
  if (/^https?:\/\//i.test(url)) return url
  return encodeURI(url)
}