import request from './request'

export const login = (data) => {
  return request.post('/auth/login', data)
}

export const register = (data) => {
  return request.post('/auth/register', data)
}

export const getUserInfo = () => {
  return request.get('/users/me')
}

export const getProfile = () => {
  return request.get('/users/profile')
}

export const updateNickname = (data) => {
  return request.put('/users/nickname', data)
}

export const changePassword = (data) => {
  return request.put('/users/password', data)
}
