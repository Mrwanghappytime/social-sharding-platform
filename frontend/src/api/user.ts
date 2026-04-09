import request from '@/utils/axios'

// 用户注册
export const register = (data: { username: string; password: string }) => {
  return request.post('/users/register', data)
}

// 用户登录
export const login = (data: { username: string; password: string }) => {
  return request.post('/users/login', data)
}

// 获取用户信息
export const getUserInfo = () => {
  return request.get('/users/me')
}

// 更新用户头像
export const updateAvatar = (avatar: string) => {
  const formData = new FormData()
  formData.append('avatar', avatar)
  return request.put('/users/avatar', formData)
}

// 获取指定用户信息
export const getUserById = (userId: number) => {
  return request.get(`/users/${userId}`)
}
