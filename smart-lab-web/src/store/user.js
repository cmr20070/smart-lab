import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const avatarVersion = ref(0)

  function setToken(val) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function setUserInfo(info) {
    userInfo.value = info
    avatarVersion.value++
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function refreshAvatar() {
    avatarVersion.value++
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    avatarVersion.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, avatarVersion, setToken, setUserInfo, refreshAvatar, logout }
})