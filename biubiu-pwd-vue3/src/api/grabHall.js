import request from './request'

export const getGrabHallOrders = (params) => request.get('/grab-hall/orders', { params })
export const grabOrder = (orderId, data) => request.post(`/grab-hall/orders/${orderId}/grab`, data)
export const joinTeam = (orderId) => request.post(`/grab-hall/orders/${orderId}/join-team`)
export const cancelGrab = (orderId) => request.post(`/grab-hall/orders/${orderId}/cancel-grab`)
export const getMyGrabStatus = () => request.get('/grab-hall/my-status')
export const searchPlayers = (keyword) => request.get('/grab-hall/search-players', { params: { keyword } })
export const publishToHall = (orderId, data) => request.post(`/orders/${orderId}/publish-to-hall`, data)
export const withdrawFromHall = (orderId) => request.post(`/orders/${orderId}/withdraw-from-hall`)
