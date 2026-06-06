import request from '@/utils/request'

export const elderApi = {
  list: (params) => request.get('/elder/list', { params }),
  getById: (id) => request.get(`/elder/${id}`),
  save: (data) => request.post('/elder', data),
  update: (id, data) => request.put(`/elder/${id}`, data),
  remove: (id) => request.delete(`/elder/${id}`)
}
