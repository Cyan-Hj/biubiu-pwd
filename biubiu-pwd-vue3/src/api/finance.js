import request from './request'

export const getIncome = () => {
  return request.get('/finance/income')
}

export const getIncomeRecords = (params) => {
  return request.get('/finance/income/records', { params })
}

export const withdraw = (data) => {
  return request.post('/finance/withdraw', data)
}

export const getPendingWithdrawals = () => {
  return request.get('/finance/withdraw/pending')
}

export const reviewWithdrawal = (id, data) => {
  return request.post(`/finance/withdraw/${id}/review`, data)
}

export const getFinanceStatistics = () => {
  return request.get('/finance/statistics')
}
