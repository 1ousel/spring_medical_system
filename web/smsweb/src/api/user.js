import request from '@/utils/request'

export const userApi = {
  list: (params) => request.get('/user/list', { params }),
  getById: (id) => request.get(`/user/${id}`),
  save: (data) => request.post('/user', data),
  update: (id, data) => request.put(`/user/${id}`, data),
  toggle: (id) => request.put(`/user/${id}/toggle`),
  resetPassword: (id) => request.put(`/user/${id}/reset-password`),
  remove: (id) => request.delete(`/user/${id}`),
  getDoctors: () => request.get('/user/doctors'),
  changePassword: (id, data) => request.put(`/user/${id}/change-password`, data)
}
