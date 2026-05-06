import request from './request'

export const applyLevelUpgrade = (data) => {
  return request.post('/level-upgrade/apply', data)
}

export const getMyLevelApplications = () => {
  return request.get('/level-upgrade/my-applications')
}

export const getPendingLevelApplications = () => {
  return request.get('/level-upgrade/pending')
}

export const getAllLevelApplications = () => {
  return request.get('/level-upgrade/all')
}

export const approveLevelApplication = (id, data) => {
  return request.post(`/level-upgrade/${id}/approve`, data)
}

export const rejectLevelApplication = (id, data) => {
  return request.post(`/level-upgrade/${id}/reject`, data)
}

export const batchApproveLevelApplications = (ids) => {
  return request.post('/level-upgrade/batch-approve', ids)
}
