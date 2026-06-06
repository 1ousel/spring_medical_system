import request from '@/utils/request'

export const alertApi = {
  list: (params) => request.get('/health-alert/list', { params }),
  getById: (id) => request.get(`/health-alert/${id}`),
  save: (data) => request.post('/health-alert', data),
  update: (id, data) => request.put(`/health-alert/${id}`, data),
  process: (id, remark) => request.put(`/health-alert/${id}/process`, { remark }),
  remove: (id) => request.delete(`/health-alert/${id}`),
  todayCount: () => request.get('/health-alert/today-count')
}
