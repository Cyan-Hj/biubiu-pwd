import request from './request'

// 获取老板列表
export const getBosses = (params) => {
  return request.get('/boss', { params })
}

// 创建老板
export const createBoss = (data) => {
  return request.post('/boss', data)
}

// 更新老板
export const updateBoss = (id, data) => {
  return request.put(`/boss/${id}`, data)
}

// 禁用老板
export const disableBoss = (id) => {
  return request.post(`/boss/${id}/disable`)
}

// 启用老板
export const enableBoss = (id) => {
  return request.post(`/boss/${id}/enable`)
}

// 删除老板
export const deleteBoss = (id) => {
  return request.delete(`/boss/${id}`)
}

// 获取老板详情
export const getBoss = (id) => {
  return request.get(`/boss/${id}`)
}

// 预存充值
export const rechargeBoss = (id, data) => {
  return request.post(`/boss/${id}/recharge`, data)
}

// 手动修改余额
export const updateBossBalance = (id, data) => {
  return request.post(`/boss/${id}/balance`, data)
}

// 获取充值记录
export const getBossRecords = (id, params) => {
  return request.get(`/boss/${id}/records`, { params })
}

// 获取VIP等级配置
export const getVipLevels = () => {
  return request.get('/system/vip-levels')
}

// 创建VIP等级
export const createVipLevel = (data) => {
  return request.post('/system/vip-levels', data)
}

// 更新VIP等级
export const updateVipLevel = (id, data) => {
  return request.put(`/system/vip-levels/${id}`, data)
}

// 删除VIP等级
export const deleteVipLevel = (id) => {
  return request.delete(`/system/vip-levels/${id}`)
}
