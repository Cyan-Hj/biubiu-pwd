import request from './request'

export const getPlayers = (params) => {
  return request.get('/users/players', { params })
}

export const getPlayerLevels = () => {
  return request.get('/users/players/levels')
}

export const approvePlayer = (id, data) => {
  return request.post(`/users/${id}/approve`, data)
}

export const updatePlayer = (id, data) => {
  return request.put(`/users/${id}`, data)
}

export const resetPassword = (id, data) => {
  return request.post(`/users/${id}/reset-password`, data)
}
