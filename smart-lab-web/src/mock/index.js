const isPagesEnv = () => {
  return typeof window !== 'undefined' && window.location.hostname.includes('github.io')
}

const mockUsers = {
  admin: { id: 1, username: 'admin', role: 'ADMIN', phone: '13800138000', email: 'admin@smartlab.com', avatar: '' },
  teacher: { id: 2, username: 'teacher', role: 'TEACHER', phone: '13900139000', email: 'teacher@smartlab.com', avatar: '' },
  student: { id: 3, username: 'student', role: 'STUDENT', phone: '13700137000', email: 'student@smartlab.com', avatar: '' },
}

function makeToken(username) {
  return 'mock-token-' + btoa(username + '-' + Date.now())
}

function mockLogin(data) {
  const user = mockUsers[data.username]
  if (user && data.password) {
    return { code: 200, message: '登录成功', data: { token: makeToken(user.username), userInfo: user } }
  }
  return { code: 400, message: '用户名或密码错误', data: null }
}

const mockRoutes = {
  'POST /auth/login': mockLogin,
  'GET /user/info': () => ({ code: 200, message: 'ok', data: mockUsers.admin }),
}

function matchMock(method, url) {
  const key = method + ' ' + url.replace(/\/api/, '').split('?')[0]
  return mockRoutes[key]
}

export { isPagesEnv, matchMock, mockUsers }