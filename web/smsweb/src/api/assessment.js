import request from '@/utils/request'

export const assessmentApi = {
  list: (params) => request.get('/assessment/list', { params }),
  getById: (id) => request.get(`/assessment/${id}`),
  save: (data) => request.post('/assessment', data),
  update: (id, data) => request.put(`/assessment/${id}`, data),
  remove: (id) => request.delete(`/assessment/${id}`)
}
