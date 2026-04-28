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

export const deletePlayer = (id) => {
  return request.delete(`/users/${id}`)
}

// 客服账号管理
export const getCustomerServiceList = () => {
  return request.get('/users/customer-service')
}

export const createCustomerService = (data) => {
  return request.post('/users/customer-service', data)
}

export const updateCustomerService = (id, data) => {
  return request.put(`/users/customer-service/${id}`, data)
}

export const resetCustomerServicePassword = (id, data) => {
  return request.post(`/users/customer-service/${id}/reset-password`, data)
}

export const deleteCustomerService = (id) => {
  return request.delete(`/users/customer-service/${id}`)
}

export const updateDeposit = (id, data) => {
  return request.put(`/users/${id}/deposit`, data)
}

export const payDeposit = (id, data) => {
  return request.post(`/users/${id}/deposit/pay`, data)
}

export const setMyDepositMode = (data) => {
  return request.put('/users/me/deposit-mode', data)
}
