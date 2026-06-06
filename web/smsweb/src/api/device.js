import request from '@/utils/request'

export const deviceApi = {
  list: (params) => request.get('/device/list', { params }),
  getById: (id) => request.get(`/device/${id}`),
  save: (data) => request.post('/device', data),
  update: (id, data) => request.put(`/device/${id}`, data),
  remove: (id) => request.delete(`/device/${id}`)
}
