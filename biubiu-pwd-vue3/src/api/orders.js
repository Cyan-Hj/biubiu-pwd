import request from './request'

export const getOrders = (params) => {
  return request.get('/orders', { params })
}

export const getOrderById = (id) => {
  return request.get(`/orders/${id}`)
}

export const getOrderByOrderNo = (orderNo) => {
  return request.get(`/orders/by-order-no/${orderNo}`)
}

export const getMyInServiceOrders = () => {
  return request.get('/orders/my-in-service')
}

export const createOrder = (data) => {
  return request.post('/orders', data)
}

export const assignOrder = (id, data) => {
  return request.post(`/orders/${id}/assign`, data)
}

export const acceptOrder = (id) => {
  return request.post(`/orders/${id}/accept`)
}

export const startOrder = (id) => {
  return request.post(`/orders/${id}/start`)
}

export const completeOrder = (id, data) => {
  return request.post(`/orders/${id}/complete`, data)
}

export const cancelOrder = (id, data) => {
  return request.post(`/orders/${id}/cancel`, data)
}

export const batchDeleteOrders = (ids) => {
  return request.delete('/orders/batch', { data: ids })
}
