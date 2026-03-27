import request from './request'

// 获取系统配置
export const getSystemConfig = () => {
  return request.get('/system/config')
}

// 更新系统配置
export const updateSystemConfig = (data) => {
  return request.post('/system/config', data)
}

// 获取等级价格
export const getLevelPrices = () => {
  return request.get('/system/levels')
}

// 更新等级价格
export const updateLevelPrice = (data) => {
  return request.post('/system/levels', data)
}

// 创建等级
export const createLevelPrice = (data) => {
  return request.post('/system/levels/create', data)
}

// 删除等级
export const deleteLevelPrice = (id) => {
  return request.delete(`/system/levels/${id}`)
}

// 更新等级排序
export const sortLevelPrices = (data) => {
  return request.post('/system/levels/sort', data)
}

// ==================== 系统选项管理（注意事项 & 服务项目） ====================

// 获取系统选项
export const getSystemOptions = (type) => {
  return request.get('/system/options', { params: { type } })
}

// 创建系统选项
export const createSystemOption = (data) => {
  return request.post('/system/options', data)
}

// 更新系统选项
export const updateSystemOption = (id, data) => {
  return request.put(`/system/options/${id}`, data)
}

// 删除系统选项
export const deleteSystemOption = (id) => {
  return request.delete(`/system/options/${id}`)
}

// 更新系统选项排序
export const sortSystemOptions = (data) => {
  return request.post('/system/options/sort', data)
}
